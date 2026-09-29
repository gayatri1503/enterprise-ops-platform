package com.platform.auth.config;

import com.platform.auth.domain.Role;
import com.platform.auth.domain.User;
import com.platform.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        users.saveAll(List.of(
                new User("admin@enterprise.com",   encoder.encode("Admin@123"),   "System Admin",   Role.ADMIN,   1L),
                new User("manager@enterprise.com", encoder.encode("Manager@123"), "Maya Manager",   Role.MANAGER, 1L),
                new User("demo@enterprise.com",    encoder.encode("Demo@123"),    "Demo Employee",  Role.USER,    1L),
                new User("auditor@enterprise.com", encoder.encode("Auditor@123"), "Alex Auditor",   Role.AUDITOR, 1L)
        ));
        log.info("Seeded 4 dev users (admin/manager/demo/auditor @enterprise.com)");
    }
}