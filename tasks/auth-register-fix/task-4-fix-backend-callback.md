# Task 4 — Fix backend /auth/callback to issue internal JWT

## Problem
The `POST /auth/callback` exchanges the code with Keycloak but returns raw Keycloak tokens. `JwtAuthFilter` validates tokens against the app's own `SECRET`, so Keycloak tokens are rejected on every subsequent API call with 401.

## Fix
After code exchange:
1. Decode the Keycloak `id_token` payload (base64url) to extract `preferred_username` and `email`
2. Call `userService.provisionKeycloakUser(username, email)` — creates a local user + village on first login, no-ops on repeat logins
3. Issue internal JWT via `jwtService.generateTokenWithUsername(username)`
4. Create refresh token and set HTTP-only cookie
5. Return `AuthResponse(accessToken)` — same shape as `/auth/login`

Also:
- Inject `RestTemplate` as a Spring bean (enables mocking in tests)
- Read `redirectUri` from request body (fixes hardcoded value mismatch)
- Remove the unused `@GetMapping("/callback")` — Keycloak now redirects to frontend
- Add `keycloak.base-url` property so the token endpoint URL works in both Docker and local dev

## Files changed
- `AuthController.java`
- `UserService.java` (add `provisionKeycloakUser`)
- `VandalsApplication.java` (add `RestTemplate` bean)
- `src/main/resources/application.properties`

## Tests
- `AuthControllerTest` — covers new user provisioning, existing user skip, missing code, missing redirectUri

## Status: DONE
