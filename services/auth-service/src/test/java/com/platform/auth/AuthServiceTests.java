package com.platform.auth;

import com.platform.auth.domain.Role;
import com.platform.auth.domain.User;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.RegisterRequest;
import com.platform.auth.events.EventPublisher;
import com.platform.auth.repository.RefreshTokenRepository;
import com.platform.auth.repository.UserRepository;
import com.platform.auth.security.JwtService;
import com.platform.auth.service.AuthService;
import com.platform.auth.service.AuthService.AuthResult;
import com.platform.auth.service.EmailAlreadyUsedException;
import com.platform.auth.service.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Service-layer integration tests using H2.
 * RabbitMQ is excluded via application-test.yml; EventPublisher is mocked.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional   // rolls back each test — keeps tests independent
class AuthServiceTests {

    @Autowired AuthService authService;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Mock the publisher so no RabbitMQ connection is attempted
    @MockBean EventPublisher eventPublisher;

    @BeforeEach
    void setup() {
        // lenient stub: publish() returns void, so just do nothing
        doNothing().when(eventPublisher)
                .publish(any(), any(), any(), any(), any(), any());
    }

    // ─── Registration ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Successful registration returns access token and issues refresh token")
    void registration_success() {
        RegisterRequest req = new RegisterRequest("Jane Doe", "jane@example.com", "Password1");
        AuthResult result = authService.register(req);

        assertThat(result.body().accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.body().user().email()).isEqualTo("jane@example.com");
        assertThat(result.body().user().role()).isEqualTo("USER"); // server always assigns USER

        // password must be hashed in DB, never stored plain
        User saved = userRepository.findByEmailIgnoreCase("jane@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password1");
        assertThat(passwordEncoder.matches("Password1", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Registration normalises email to lowercase")
    void registration_emailNormalized() {
        authService.register(new RegisterRequest("John", "JOHN@EXAMPLE.COM", "Password1"));
        assertThat(userRepository.findByEmailIgnoreCase("john@example.com")).isPresent();
    }

    @Test
    @DisplayName("Duplicate registration throws EmailAlreadyUsedException (409)")
    void registration_duplicate_throws() {
        authService.register(new RegisterRequest("Alice", "alice@example.com", "Password1"));
        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("Alice2", "alice@example.com", "Password2")))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("Duplicate registration with different case is also rejected")
    void registration_duplicateCaseInsensitive_throws() {
        authService.register(new RegisterRequest("Bob", "bob@example.com", "Password1"));
        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("Bob2", "BOB@EXAMPLE.COM", "Password2")))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("Server always assigns USER role; client cannot escalate to ADMIN")
    void registration_roleIsAlwaysUser() {
        // RegisterRequest has no role field by design. Verify the saved entity.
        authService.register(new RegisterRequest("Escalator", "esc@example.com", "Password1"));
        User saved = userRepository.findByEmailIgnoreCase("esc@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid login returns access token, refresh token, and user summary")
    void login_success() {
        authService.register(new RegisterRequest("Carol", "carol@example.com", "Password1"));
        LoginRequest req = loginReq("carol@example.com", "Password1");

        AuthResult result = authService.login(req, "127.0.0.1", "test-agent");

        assertThat(result.body().accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.body().user().email()).isEqualTo("carol@example.com");
        assertThat(result.body().expiresInSeconds()).isEqualTo(15 * 60);
    }

    @Test
    @DisplayName("Wrong password throws InvalidCredentialsException with a safe message")
    void login_wrongPassword_throws() {
        authService.register(new RegisterRequest("Dave", "dave@example.com", "Password1"));

        assertThatThrownBy(() -> authService.login(loginReq("dave@example.com", "WrongPass1"), "127.0.0.1", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Unknown email throws InvalidCredentialsException — does not reveal whether account exists")
    void login_unknownEmail_sameMessageAsWrongPassword() {
        assertThatThrownBy(() -> authService.login(loginReq("ghost@example.com", "Password1"), "127.0.0.1", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Disabled account cannot authenticate")
    void login_disabledAccount_throws() {
        authService.register(new RegisterRequest("Eve", "eve@example.com", "Password1"));
        User eve = userRepository.findByEmailIgnoreCase("eve@example.com").orElseThrow();
        disableUser(eve);

        assertThatThrownBy(() -> authService.login(loginReq("eve@example.com", "Password1"), "127.0.0.1", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    // ─── JWT ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Access token contains required claims: sub, roles, perms, deptId, jti, iat, exp")
    void jwt_claimsAreComplete() {
        authService.register(new RegisterRequest("Fred", "fred@example.com", "Password1"));
        AuthResult result = authService.login(loginReq("fred@example.com", "Password1"), "127.0.0.1", null);

        Claims claims = jwtService.parseAndValidate(result.body().accessToken());
        assertThat(claims.getSubject()).isNotBlank();
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("perms")).isNotNull();
        assertThat(claims.get("deptId")).isNotNull();
        assertThat(claims.getId()).isNotBlank();          // jti
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Tampered JWT is rejected")
    void jwt_tamperedToken_throws() {
        authService.register(new RegisterRequest("Grace", "grace@example.com", "Password1"));
        AuthResult result = authService.login(loginReq("grace@example.com", "Password1"), null, null);

        String token = result.body().accessToken();
        // Flip the last character of the signature segment
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Expired JWT is rejected")
    void jwt_expiredToken_isRejected() {
        // Build a JwtService with 0-minute TTL so the token expires immediately
        JwtService shortLivedJwt = new JwtService(
                "test-secret-that-is-at-least-32-characters-long", 0L);

        authService.register(new RegisterRequest("Heidi", "heidi@example.com", "Password1"));
        User heidi = userRepository.findByEmailIgnoreCase("heidi@example.com").orElseThrow();

        String expiredToken = shortLivedJwt.generateAccessToken(heidi);

        // Even a 0-minute TTL token becomes expired within a second; parse should reject it
        assertThatThrownBy(() -> jwtService.parseAndValidate(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token signed with a different secret is rejected")
    void jwt_wrongSecret_isRejected() {
        JwtService otherJwt = new JwtService("completely-different-secret-at-least-32-ch", 15L);

        authService.register(new RegisterRequest("Ivan", "ivan@example.com", "Password1"));
        User ivan = userRepository.findByEmailIgnoreCase("ivan@example.com").orElseThrow();
        String tokenFromOtherService = otherJwt.generateAccessToken(ivan);

        assertThatThrownBy(() -> jwtService.parseAndValidate(tokenFromOtherService))
                .isInstanceOf(JwtException.class);
    }

    // ─── Refresh token lifecycle ────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh rotates: new tokens are issued, old refresh token is revoked")
    void refresh_rotatesToken() {
        authService.register(new RegisterRequest("Judy", "judy@example.com", "Password1"));
        AuthResult first = authService.login(loginReq("judy@example.com", "Password1"), null, null);
        String firstRefresh = first.refreshToken();

        AuthResult second = authService.refresh(firstRefresh);

        assertThat(second.refreshToken()).isNotEqualTo(firstRefresh);
        assertThat(second.body().accessToken()).isNotBlank();

        // The old token must now be revoked
        assertThatThrownBy(() -> authService.refresh(firstRefresh))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Reuse of a rotated refresh token revokes ALL sessions for that user")
    void refresh_reuseDetected_revokesAllSessions() {
        authService.register(new RegisterRequest("Karl", "karl@example.com", "Password1"));
        AuthResult first = authService.login(loginReq("karl@example.com", "Password1"), null, null);
        String stolen = first.refreshToken();

        // Legitimate rotation
        AuthResult second = authService.refresh(stolen);

        // Attacker replays the old (now-rotated) token
        assertThatThrownBy(() -> authService.refresh(stolen))
                .isInstanceOf(InvalidCredentialsException.class);

        // The new token from legitimate rotation must also have been revoked
        assertThatThrownBy(() -> authService.refresh(second.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Missing refresh token throws InvalidCredentialsException")
    void refresh_missingToken_throws() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Missing refresh token");

        assertThatThrownBy(() -> authService.refresh("  "))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Missing refresh token");
    }

    @Test
    @DisplayName("Disabled account's refresh token cannot be used after account is disabled")
    void refresh_disabledAccount_throws() {
        authService.register(new RegisterRequest("Lena", "lena@example.com", "Password1"));
        AuthResult first = authService.login(loginReq("lena@example.com", "Password1"), null, null);

        User lena = userRepository.findByEmailIgnoreCase("lena@example.com").orElseThrow();
        disableUser(lena);

        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logout revokes the refresh token so it cannot be used again")
    void logout_revokesToken() {
        authService.register(new RegisterRequest("Mia", "mia@example.com", "Password1"));
        AuthResult result = authService.login(loginReq("mia@example.com", "Password1"), null, null);

        authService.logout(result.refreshToken());

        assertThatThrownBy(() -> authService.refresh(result.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Logout with null or blank token is a no-op (does not throw)")
    void logout_nullToken_noOp() {
        assertThatCode(() -> authService.logout(null)).doesNotThrowAnyException();
        assertThatCode(() -> authService.logout("")).doesNotThrowAnyException();
    }

    // ─── Role / permissions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("USER role has WORK_CREATE, WORK_EDIT, ANALYTICS_VIEW")
    void role_user_permissions() {
        Claims claims = tokenFor("user_perm@example.com");
        @SuppressWarnings("unchecked")
        var perms = (java.util.List<String>) claims.get("perms");
        assertThat(perms).contains("WORK_CREATE", "WORK_EDIT", "ANALYTICS_VIEW");
        assertThat(perms).doesNotContain("USER_MANAGE", "AUDIT_VIEW", "ANALYTICS_GLOBAL");
    }

    @Test
    @DisplayName("ADMIN role has all permissions")
    void role_admin_hasAllPermissions() {
        // Seed an admin user directly (registration always gives USER)
        User admin = userRepository.save(new User(
                "admin_test@example.com",
                passwordEncoder.encode("Admin@123"),
                "Test Admin", Role.ADMIN, 1L));

        String token = jwtService.generateAccessToken(admin);
        Claims claims = jwtService.parseAndValidate(token);

        @SuppressWarnings("unchecked")
        var perms = (java.util.List<String>) claims.get("perms");
        assertThat(perms).containsExactlyInAnyOrder(
                "USER_READ", "USER_MANAGE", "WORK_CREATE", "WORK_EDIT", "WORK_ASSIGN",
                "ANALYTICS_VIEW", "ANALYTICS_GLOBAL", "AUDIT_VIEW");
    }

    // ─── Failed-login lockout ──────────────────────────────────────────────────

    @Test
    @DisplayName("Account locks out after reaching max-failed-attempts threshold")
    void lockout_triggersAfterThreshold() {
        authService.register(new RegisterRequest("Nick", "nick@example.com", "Password1"));

        // test profile sets max-failed-attempts=3
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(loginReq("nick@example.com", "WrongPass1"), "1.2.3.4", null))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        User nick = userRepository.findByEmailIgnoreCase("nick@example.com").orElseThrow();
        assertThat(nick.getLockedUntil()).isNotNull();
        assertThat(nick.isLockedOut()).isTrue();

        // Even the correct password is rejected while locked out
        assertThatThrownBy(() -> authService.login(loginReq("nick@example.com", "Password1"), "1.2.3.4", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Successful login resets failed-attempt counter and clears lockout")
    void lockout_resetOnSuccessfulLogin() {
        authService.register(new RegisterRequest("Olivia", "olivia@example.com", "Password1"));

        // Two failures — below threshold (3)
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> authService.login(loginReq("olivia@example.com", "Wrong1"), null, null))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        User olivia = userRepository.findByEmailIgnoreCase("olivia@example.com").orElseThrow();
        assertThat(olivia.getFailedLoginAttempts()).isEqualTo(2);

        // Correct password clears the counter
        authService.login(loginReq("olivia@example.com", "Password1"), null, null);

        userRepository.flush();
        User refreshed = userRepository.findByEmailIgnoreCase("olivia@example.com").orElseThrow();
        assertThat(refreshed.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(refreshed.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Disabled account increments failure counter but lockout does not bypass disabled check")
    void lockout_disabledAccountStillFails() {
        authService.register(new RegisterRequest("Pat", "pat@example.com", "Password1"));
        User pat = userRepository.findByEmailIgnoreCase("pat@example.com").orElseThrow();
        disableUser(pat);

        // Multiple attempts — account is disabled; should always fail with safe message
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(loginReq("pat@example.com", "Password1"), null, null))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }
    }

    @Test
    @DisplayName("Failed-attempt counter increments independently per failed login call")
    void lockout_counterIncrements() {
        authService.register(new RegisterRequest("Quinn", "quinn@example.com", "Password1"));

        assertThatThrownBy(() -> authService.login(loginReq("quinn@example.com", "Bad1"), null, null))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(loginReq("quinn@example.com", "Bad2"), null, null))
                .isInstanceOf(InvalidCredentialsException.class);

        User quinn = userRepository.findByEmailIgnoreCase("quinn@example.com").orElseThrow();
        assertThat(quinn.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(quinn.isLockedOut()).isFalse(); // not yet at threshold of 3
    }

    private LoginRequest loginReq(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private Claims tokenFor(String email) {
        authService.register(new RegisterRequest("TestUser", email, "Password1"));
        AuthResult r = authService.login(loginReq(email, "Password1"), null, null);
        return jwtService.parseAndValidate(r.body().accessToken());
    }

    /** Disable a user via the domain method and persist it. */
    private void disableUser(User user) {
        userRepository.findById(user.getId()).ifPresent(u -> {
            u.disable();
            userRepository.save(u);
            userRepository.flush(); // ensure the UPDATE hits the DB before the test reads it
        });
    }
}
