## Why

The backend currently maintains two parallel auth paths: a local username/password system (login endpoint, password hashing, `UserDetailsService`, refresh tokens) and a Keycloak OAuth2 path. The local path is no longer used — the frontend already sends all users through Keycloak — but its infrastructure (BCrypt passwords in the DB, `PasswordEncoder`, `AuthenticationManager`, registration endpoint) remains as dead code that creates a false impression that local credentials are valid and adds unnecessary attack surface.

## What Changes

- **BREAKING** Remove `POST /auth/login` (local password login endpoint)
- **BREAKING** Remove `POST /auth/generateToken` (legacy token endpoint)
- **BREAKING** Remove `POST /user/register` (local password registration endpoint)
- Remove `PasswordEncoder` bean and BCrypt dependency from `SecurityConfig`
- Remove `AuthenticationManager` bean from `SecurityConfig`
- Remove `UserInfoService` (implements `UserDetailsService`) — no longer needed without `AuthenticationManager`
- Remove `UserInfoDetails` and `UserInfo` record (Spring Security wrapper types for local auth)
- Remove `UserInfoRepository` (password-bearing DB queries)
- Remove `password` field from `User` entity and drop the column via Liquibase migration
- Remove `PasswordEncoder` injection from `UserService`; remove password encoding from `newUser()`
- Remove `RegisterView.vue` and the `/register` frontend route
- Remove dead `LoginView.vue` (not routed anywhere)
- Keep: `POST /auth/callback` (Keycloak code exchange), `POST /auth/refresh`, `POST /auth/logout`
- Keep: `JwtService`, `JwtAuthFilter`, `RefreshToken` / `RefreshTokenService` (internal JWT layer stays)
- Keep: `UserService.provisionKeycloakUser()` (auto-creates local `User` row on first Keycloak login)

## Capabilities

### New Capabilities
- `keycloak-only-auth`: Authentication is delegated entirely to Keycloak. The backend provisions a local `User` row on first SSO login but stores no credentials. The internal JWT layer is retained so the rest of the backend (village, building, unit APIs) is unchanged.

### Modified Capabilities

## Impact

- **Backend**: `web` package loses ~4 classes (`UserInfoService`, `UserInfoDetails`, `UserInfoRepository`, `UserInfo` record); `AuthController` loses 3 endpoints; `SecurityConfig` simplified; `UserService` drops `PasswordEncoder` dependency; `User` entity drops `password` field
- **Database**: Liquibase changeset drops the `password` column from the `users` table
- **Frontend**: `RegisterView.vue` deleted, `/register` route removed; `LoginView.vue` deleted
- **Tests**: Any tests covering local login or registration need removal or replacement with Keycloak-path tests
- **Dependencies**: `jjwt` (internal JWT) and Spring Security stay; no new deps
