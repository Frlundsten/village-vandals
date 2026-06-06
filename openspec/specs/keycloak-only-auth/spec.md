# Spec: keycloak-only-auth

## Purpose

Keycloak becomes the sole authentication provider for Village Vandals. Local username/password login and registration are removed. The backend provisions a local user row on first Keycloak login and issues internal JWTs as before; the frontend routes all login and registration through Keycloak.

## Requirements

### Requirement: Keycloak is the sole authentication provider
The system SHALL NOT accept username/password credentials directly. All authentication SHALL be initiated by redirecting the user to Keycloak. The backend SHALL NOT expose endpoints for local password login or local user registration.

#### Scenario: Local login endpoint removed
- **WHEN** a client sends `POST /auth/login` with a username and password
- **THEN** the server SHALL return 404 (endpoint does not exist)

#### Scenario: Legacy token endpoint removed
- **WHEN** a client sends `POST /auth/generateToken` with a username and password
- **THEN** the server SHALL return 404 (endpoint does not exist)

#### Scenario: Local registration endpoint removed
- **WHEN** a client sends `POST /user/register` with username, email, and password
- **THEN** the server SHALL return 404 (endpoint does not exist)

### Requirement: Keycloak OAuth2 callback provisions a local user on first login
After Keycloak redirects back with an authorization code, the backend SHALL exchange it for a Keycloak token, extract the username and email, and create a local `User` row if one does not already exist. On subsequent logins the existing row SHALL be reused without modification.

#### Scenario: First Keycloak login creates a local user
- **WHEN** `POST /auth/callback` is called with a valid Keycloak authorization code
- **THEN** the server SHALL create a `User` row for the Keycloak username if none exists
- **THEN** the server SHALL return an internal JWT access token

#### Scenario: Second Keycloak login reuses existing user
- **WHEN** `POST /auth/callback` is called for a username that already has a `User` row
- **THEN** the server SHALL NOT create a duplicate `User` row
- **THEN** the server SHALL return a fresh internal JWT access token

### Requirement: No password stored in the users table
The `users` table SHALL NOT contain a `password` column. The `User` entity SHALL NOT have a `password` field.

#### Scenario: User row contains no password
- **WHEN** a `User` row is inserted via the Keycloak provisioning path
- **THEN** the row SHALL NOT contain a password hash or any credential data

### Requirement: Internal JWT and refresh token flow unchanged
The internal JWT access token (issued after Keycloak callback) and the HTTP-only refresh token cookie SHALL continue to work as before. Authenticated API calls to village, building, and unit endpoints SHALL be validated using the internal JWT.

#### Scenario: Authenticated request accepted with internal JWT
- **WHEN** a client sends a request to `/village/{id}` with a valid internal JWT in the `Authorization: Bearer` header
- **THEN** the server SHALL process the request and return the expected response

#### Scenario: Token refresh still works
- **WHEN** a client sends `POST /auth/refresh` with a valid refresh token cookie
- **THEN** the server SHALL return a new internal JWT and rotate the refresh token

#### Scenario: Logout clears the refresh token
- **WHEN** a client sends `POST /auth/logout` while authenticated
- **THEN** the server SHALL revoke the refresh token and clear the refresh token cookie

### Requirement: Frontend routes users to Keycloak for login and registration
The frontend login page SHALL redirect users to the Keycloak authorization endpoint. Registration SHALL be handled via Keycloak's registration flow. No local login form or registration form SHALL be accessible via the frontend router.

#### Scenario: Navigating to /login shows Keycloak-redirect UI
- **WHEN** an unauthenticated user navigates to `/login`
- **THEN** the frontend SHALL display the `LoginOrRegister` component with Keycloak login and register buttons

#### Scenario: /register route does not exist
- **WHEN** a user navigates to `/register`
- **THEN** the frontend router SHALL NOT render `RegisterView` (route is removed)
