## Context

The backend has two auth paths built side-by-side: a local username/password system (BCrypt, `UserDetailsService`, `AuthenticationManager`, `/auth/login`, `/user/register`) and a Keycloak OAuth2 path (`/auth/callback`). The frontend already routes all users through Keycloak via `LoginOrRegister.vue`; the local path is unreachable in normal use. This design removes the local path entirely, leaving only the Keycloak-backed flow.

The **internal JWT layer** (`JwtService`, `JwtAuthFilter`, `RefreshToken`) is NOT removed — it decouples the rest of the backend from Keycloak tokens. After a Keycloak code exchange, the backend issues its own short-lived JWT. All existing village/building/unit API authorization logic remains unchanged.

## Goals / Non-Goals

**Goals:**
- Remove all code, DB columns, and endpoints that exist solely to support local password authentication
- Ensure the Keycloak OAuth2 → internal JWT path continues to work exactly as before
- Leave no password hashes in the `users` table
- Remove dead frontend views and routes associated with local auth

**Non-Goals:**
- Changing how the internal JWT is structured or validated
- Switching the Keycloak grant type (stays authorization code)
- Adding PKCE (out of scope)
- Multi-realm or multi-IdP support

## Decisions

### 1. Keep the internal JWT layer

**Decision:** Retain `JwtService`, `JwtAuthFilter`, and `RefreshTokenService`.

**Rationale:** The rest of the API (village, building, unit endpoints) reads `Principal` from the JWT. Replacing this with Keycloak token validation would require injecting Spring Security OAuth2 Resource Server config and changing how every controller resolves identity — a much larger change. The current two-step (Keycloak code → internal JWT) keeps the blast radius small.

**Alternative considered:** Validate Keycloak tokens directly in `JwtAuthFilter` by fetching the JWKS endpoint. Rejected — adds runtime dependency on Keycloak availability for every request.

### 2. Drop `UserDetailsService` / `AuthenticationManager` entirely

**Decision:** Remove `UserInfoService`, `UserInfoDetails`, `UserInfoRepository`, and the `AuthenticationManager` + `PasswordEncoder` beans from `SecurityConfig`.

**Rationale:** These classes exist only to support `DaoAuthenticationProvider`, which is used only by the two local login endpoints being removed. Once those endpoints are gone, nothing calls `AuthenticationManager`.

**Alternative considered:** Keep them but disable the endpoints. Rejected — dead beans mislead future readers into thinking local auth is a valid path.

### 3. Drop `password` column via Liquibase migration

**Decision:** Add a Liquibase changeset that drops the `password` column from the `users` table. Remove the `password` field from the `User` entity.

**Rationale:** Keeping the column after removing all code that writes to it means old BCrypt hashes sit in the DB with no purpose, which is a minor security hygiene issue and confuses the schema.

**Migration:** This is a backwards-incompatible schema change. Any deployment must apply the Liquibase migration before starting the new code (Liquibase runs on startup, so this is automatic in the normal Docker Compose flow).

### 4. Remove `POST /user/register`, keep `provisionKeycloakUser`

**Decision:** Remove the public registration endpoint. User creation happens exclusively via `UserService.provisionKeycloakUser()` on first Keycloak login.

**Rationale:** With Keycloak managing identity, self-service registration goes through Keycloak's own registration flow (`kc_action=register` in `LoginOrRegister.vue`). The `/user/register` endpoint accepts a plaintext password and is therefore redundant and risky to keep open.

**`newUser()` impact:** `UserService.newUser()` is called only by `provisionKeycloakUser()`. The `UserInfo` record will have its `password` field removed. `newUser()` no longer calls `passwordEncoder.encode()`.

### 5. Frontend: remove `RegisterView.vue`, `/register` route, and `LoginView.vue`

**Decision:** Delete the three dead views and their route.

**Rationale:** The router already points `/login` to `LoginOrRegister.vue` (Keycloak redirect). `LoginView.vue` has no route. `RegisterView.vue` / `/register` is never linked from the Keycloak-only login flow.

## Risks / Trade-offs

- **Existing BCrypt password hashes lost** → No mitigation needed; those accounts will authenticate via Keycloak after this change. Any user who only ever used local login will need to register through Keycloak. This is acceptable given Keycloak was already the intended sole IdP.
- **Schema migration is irreversible** → Rolling back requires re-adding the column and re-populating it. Given no user-facing password auth exists in production, this risk is negligible.
- **`UserInfo` record removal** → `UserController.addNewUser()` builds a `UserInfo` to call `newUser()`. After this change, the registration endpoint is removed entirely, so this call chain disappears. `provisionKeycloakUser()` will construct `User` directly.

## Migration Plan

1. Apply this change on a local/staging environment first.
2. On deploy: Spring Boot starts → Liquibase runs → `password` column dropped.
3. No data migration needed (hashes are discarded).
4. Rollback: restore previous jar + manually re-add `password` column (nullable) to allow old code to start; existing users will need new passwords.

## Open Questions

- Should `POST /auth/logout` also revoke the Keycloak session (backchannel logout)? Currently it only clears the internal refresh token. Out of scope for this change but worth a follow-up ticket.
