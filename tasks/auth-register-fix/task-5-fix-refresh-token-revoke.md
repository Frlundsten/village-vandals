# Task 5 — Fix refresh token revocation (logout)

## Problem
`RefreshTokenService.revokeByUsername()` has the deletion call commented out and replaced with `System.out.println`. `RefreshTokenRepository` also doesn't declare `deleteByUsername`. Logout does not invalidate tokens.

## Fix
- Add `void deleteByUsername(String username)` to `RefreshTokenRepository`
- Uncomment and fix `revokeByUsername` in `RefreshTokenService`

## Files changed
- `RefreshTokenRepository.java`
- `RefreshTokenService.java`

## Tests
- `RefreshTokenServiceTest` — covers revokeByUsername, validateRefreshToken expired/valid, createRefreshToken

## Status: DONE
