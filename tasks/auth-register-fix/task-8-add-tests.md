# Task 8 — Tests

## AuthControllerTest (WebMvcTest)
Tests for `POST /auth/callback`:
- New Keycloak user: provisions user in DB, returns internal JWT
- Existing user: skips provisioning, still returns JWT
- Missing `code` field: 400 Bad Request
- Missing `redirectUri` field: 400 Bad Request

## RefreshTokenServiceTest (unit test, plain Mockito)
- `revokeByUsername` calls `repository.deleteByUsername`
- `validateRefreshToken` with expired token: deletes token, throws `IllegalStateException`
- `validateRefreshToken` with valid token: returns token
- `createRefreshToken` saves and returns token

## Files created
- `src/test/java/com/villagevandals/vandals/web/AuthControllerTest.java`
- `src/test/java/com/villagevandals/vandals/web/jwt/RefreshTokenServiceTest.java`

## Status: DONE
