# Spec: Session Lifecycle

## Purpose

How a session ends and how it is renewed: logout revokes every refresh token for the user, and a missing, unknown, or expired refresh token is reported as `401` — the ordinary end-of-session case, not a server fault.

---

## Requirements

### Requirement: Logout revokes the user's refresh tokens
`POST /auth/logout` SHALL delete every refresh token belonging to the authenticated user. Because the deletion is a Spring Data derived delete query, it SHALL run inside a write transaction; without one the deletion fails and the tokens survive the logout.

#### Scenario: Logout deletes the user's refresh tokens
- **WHEN** an authenticated user calls `POST /auth/logout`
- **THEN** all refresh tokens for that username are deleted
- **AND** the response is `200 OK` with the refresh cookie cleared

#### Scenario: Revocation runs in a write transaction
- **WHEN** the token revocation path is invoked
- **THEN** it executes within a read-write transaction, so the derived delete query is permitted

### Requirement: An unusable refresh token is reported as 401
`POST /auth/refresh` SHALL respond `401 Unauthorized` when no refresh cookie is present, when the cookie's token is unknown, and when the token has expired. These ordinary end-of-session cases SHALL NOT surface as `500`.

#### Scenario: No cookies at all
- **WHEN** `POST /auth/refresh` is called with no cookies
- **THEN** the response is `401`

#### Scenario: Cookies present but no refresh cookie among them
- **WHEN** `POST /auth/refresh` is called with cookies that do not include `refreshToken`
- **THEN** the response is `401`

#### Scenario: Unknown refresh token
- **WHEN** `POST /auth/refresh` is called with a `refreshToken` cookie that is not in the store
- **THEN** the response is `401`
- **AND** no new access token or refresh cookie is issued

#### Scenario: Expired refresh token
- **WHEN** `POST /auth/refresh` is called with an expired `refreshToken` cookie
- **THEN** the response is `401`

#### Scenario: Valid refresh token still rotates
- **WHEN** `POST /auth/refresh` is called with a valid `refreshToken` cookie
- **THEN** the response is `200 OK` with a new access token
- **AND** the old refresh token is revoked and a new refresh cookie is set
