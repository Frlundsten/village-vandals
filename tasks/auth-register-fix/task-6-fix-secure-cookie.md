# Task 6 — Fix refresh cookie secure flag for local dev

## Problem
Refresh cookie is set with `.secure(true)`, which means browsers only send it over HTTPS. Local dev uses plain HTTP, so `/auth/refresh` never receives the cookie, making silent token renewal impossible in dev.

## Fix
- Add `app.secure-cookie=${APP_SECURE_COOKIE:false}` to `application.properties`
- Add `APP_SECURE_COOKIE: "true"` to Docker Compose backend environment (production path)
- Inject `@Value("${app.secure-cookie:false}") boolean secureCookie` in `AuthController`
- Extract cookie builder into `buildRefreshCookie(token)` helper, use `secureCookie` flag

## Files changed
- `src/main/resources/application.properties`
- `compose.yaml`
- `AuthController.java` (done as part of Task 4 refactor)

## Status: DONE
