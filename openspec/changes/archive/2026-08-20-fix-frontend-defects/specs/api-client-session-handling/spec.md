## ADDED Requirements

### Requirement: An expired access token is refreshed transparently
When a request fails with `401`, `apiRequest` SHALL attempt exactly one token refresh through `POST /auth/refresh` (sending cookies) and, if a new access token is returned, store it and replay the original request once. The player SHALL NOT be sent to the login screen while a usable refresh cookie exists.

#### Scenario: A 401 is recovered by refreshing
- **GIVEN** the stored access token has expired
- **WHEN** the player triggers any API call and the backend answers `401`
- **THEN** `POST /auth/refresh` is called
- **AND** the returned access token is stored in the session
- **AND** the original request is sent again and its result returned to the caller
- **AND** no navigation to `/login` occurs

#### Scenario: Only one retry is attempted
- **WHEN** the replayed request also returns `401`
- **THEN** no further refresh or replay is attempted
- **AND** the session is cleared and the caller receives an error

#### Scenario: Concurrent 401s share a single refresh
- **GIVEN** several requests are in flight when the token expires
- **WHEN** they all receive `401`
- **THEN** `POST /auth/refresh` is called exactly once
- **AND** every request is replayed with the newly issued token

#### Scenario: Auth endpoints are never refreshed recursively
- **WHEN** a call to an `/auth/*` path returns `401`
- **THEN** no refresh attempt is made

### Requirement: An unrecoverable 401 ends the session cleanly
When refresh is impossible or fails, `apiRequest` SHALL clear the session (removing the stored JWT so `isAuthenticated` becomes false), navigate to `/login`, and **throw**. It SHALL NOT fall through and attempt to parse the error response as the caller's data.

#### Scenario: Session state is cleared
- **WHEN** a `401` cannot be refreshed
- **THEN** the stored JWT is removed and `isAuthenticated` is false
- **AND** the app navigates to `/login`

#### Scenario: The caller sees an auth error, not a parse error
- **WHEN** a `401` cannot be refreshed
- **THEN** the promise rejects with an error whose `status` is `401`
- **AND** the rejection is not a JSON parse error

#### Scenario: No re-admission loop
- **GIVEN** a `401` has ended the session
- **WHEN** the player navigates to a route requiring auth
- **THEN** the router guard sends them to `/login` and they are not re-admitted with the dead token

### Requirement: Errors carry the server's status and reason
A failed request SHALL reject with an error exposing the HTTP `status` and, as its message, the reason the server sent. A single hardcoded message SHALL NOT be used for all endpoints.

#### Scenario: The server's message reaches the caller
- **WHEN** the backend rejects a request with a body describing the reason
- **THEN** the thrown error's message is that reason
- **AND** its `status` is the response status

#### Scenario: Several body shapes are understood
- **WHEN** the error body is `{"message":{"message":"..."}}`, or a bare JSON string, or plain text
- **THEN** the reason is extracted from whichever shape was sent

#### Scenario: An unparseable body still produces a usable error
- **WHEN** the error body is empty or not parseable
- **THEN** the error still carries the correct `status` and a non-empty message

### Requirement: Successful responses parse to data
A successful response SHALL be parsed as JSON and returned. A successful response with an empty body SHALL NOT reject.

#### Scenario: Empty success body
- **WHEN** the backend answers `200` with no body
- **THEN** the call resolves rather than rejecting with a parse error
