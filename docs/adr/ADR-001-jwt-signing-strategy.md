# ADR-001: JWT Signing Strategy — HS256 vs RS256

**Status:** Proposed  
**Deciders:** Student 2 (Security/Gateway lead) — requires team sign-off before production deploy  
**Date:** 2026-10-23

---

## Context

All services (auth, user, core, analytics) validate the JWT on every request as a defence-in-depth measure (see `architecture.md`). The choice of algorithm determines how that shared validation works.

The current implementation uses **HS256** (HMAC-SHA256 with a shared secret). The architecture document explicitly lists this as an open decision.

---

## Options

### Option A — HS256 (current)

- Auth-service signs tokens with a secret; every other service holds the **same secret** to verify.
- The secret is delivered via `JWT_SECRET` environment variable to all containers.
- Simple to set up; one secret to rotate.

**Risks:**
- Any service that holds the secret can also **forge** tokens. A compromised analytics-service or core-service could mint admin tokens.
- Secret rotation requires a coordinated redeploy of all services simultaneously.
- Violates the principle of least privilege: verifiers should not be able to sign.

### Option B — RS256 (recommended for production)

- Auth-service holds the **private key** (signs only).
- All other services hold the **public key** (verify only; cannot forge).
- Public key can be distributed via a JWKS endpoint (`/api/auth/.well-known/jwks.json`) so downstream services fetch it automatically.

**Benefits:**
- A compromised downstream service cannot forge tokens.
- Key rotation does not require redeploying every service; services re-fetch the JWKS.
- Industry standard for multi-service architectures.

**Cost:**
- Auth-service needs an RSA key pair generated and stored securely.
- All downstream `JwtDecoder` beans switch from `NimbusJwtDecoder.withSecretKey(...)` to `NimbusJwtDecoder.withJwkSetUri(...)`.
- One extra HTTP call per service start (JWKS fetch), then cached.

---

## Decision

**For Sprint 1–2 (current):** retain HS256. All services are already wired; switching mid-sprint would break every downstream service simultaneously. The risk is acceptable for a dev/demo environment where all services run in a trusted private network.

**Before production deployment:** switch to RS256.

Migration path:
1. Generate an RSA-2048 key pair in auth-service.
2. Expose `GET /api/auth/.well-known/jwks.json` (public endpoint, no auth).
3. Replace each downstream `NimbusJwtDecoder.withSecretKey(...)` with `NimbusJwtDecoder.withJwkSetUri("${AUTH_SERVICE_URL}/api/auth/.well-known/jwks.json")`.
4. Remove `JWT_SECRET` from every service except auth-service (auth-service retains only the private key).
5. Add a key-rotation test to the security test report (Sprint 3).

---

## Consequences

- Current 22/22 auth-service tests continue to pass unchanged.
- This ADR must be reviewed and its status updated to **Accepted** before a production release.
- The migration is a breaking change across all services and must be done in a single coordinated PR.
