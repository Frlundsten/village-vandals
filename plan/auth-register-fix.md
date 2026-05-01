# Auth & Register Fix Plan

## Current State

The codebase has **two auth approaches** that are both broken and partially wired together:
- **Native auth**: username/password via `/auth/login` + `/user/register` (backend is complete)
- **Keycloak OAuth2**: browser redirect flow via `LoginOrRegister.vue` (broken end-to-end)

The router currently routes `/login` to `LoginOrRegister.vue` (Keycloak-only buttons), so native login is completely inaccessible. Keycloak flow is also broken for separate reasons.

---

## Identified Bugs

### Bug 1 — `/login` route shows the wrong component
**File:** `frontend/src/router/index.js:48`

The `/login` route renders `LoginOrRegister.vue`, which only has Keycloak redirect buttons. The native login form (`LoginView.vue`) is never rendered. `RegisterView.vue` is imported in the router file but has no route defined at all.

### Bug 2 — LoginView.vue calls a non-existent endpoint
**File:** `frontend/src/views/LoginView.vue:36`

Calls `${BASE_URL}/user/auth/generateToken` — this path does not exist. The actual endpoint is `POST /auth/login` (which also properly sets the refresh cookie). The old `/auth/generateToken` endpoint also exists but doesn't set a refresh cookie.

### Bug 3 — Keycloak redirect_uri points to the backend, not the frontend
**File:** `frontend/src/views/LoginOrRegister.vue:5`

`REDIRECT_URI` is set to `http://localhost:8081/auth/callback`. Keycloak redirects the browser to the backend, which returns raw JSON to the browser tab. The frontend's `AuthView.vue` (at `/auth`) never receives the code, so it is never reached.

### Bug 4 — AuthView.vue has three bugs that make token exchange fail
**File:** `frontend/src/views/AuthView.vue`

Even if the redirect were fixed, AuthView.vue would still fail:

1. **Line 25:** `response.data.accessToken` — the `fetch` API has no `.data` property. The response body must be awaited: `const data = await response.json(); data.accessToken`.
2. **Line 27:** `localStorage.setItem('accessToken', ...)` — wrong key. The rest of the app (`api.js`, pinia store) reads from `jwt_token`.
3. **No store update / no redirect** — `useSessionStore().setToken()` is never called after token exchange, and there is no navigation to `/` on success, leaving the user on a blank page.

### Bug 5 — Backend `/auth/callback` (POST) returns a raw Keycloak JWT, not an internal JWT
**File:** `src/main/java/com/villagevandals/vandals/web/AuthController.java:169–182`

After exchanging the code with Keycloak, the backend returns the raw Keycloak access token to the frontend. `JwtAuthFilter` validates tokens against the app's own `SECRET`, so Keycloak tokens will be rejected on every subsequent request with 401.

The callback should instead: extract the user's identity from the Keycloak token → look up or create the user in the local DB → issue an internal JWT + set a refresh cookie (same as `/auth/login` does).

### Bug 6 — `RefreshTokenRepository` is missing `deleteByUsername`
**File:** `src/main/java/com/villagevandals/vandals/web/jwt/RefreshTokenService.java:41`
**File:** `src/main/java/com/villagevandals/vandals/web/jwt/RefreshTokenRepository.java`

`revokeByUsername()` has `repository.deleteByUsername(username)` commented out and replaced with a `System.out.println`. The repository interface also does not declare the method. Logout does not invalidate refresh tokens.

### Bug 7 — Refresh cookie `secure(true)` breaks local development
**Files:** `AuthController.java:79–86`, `AuthController.java:113–120`

The refresh token cookie is set with `.secure(true)`, which causes browsers to only send it over HTTPS. Local development uses plain HTTP, so the `/auth/refresh` endpoint will never receive the cookie, making silent token renewal impossible in dev.

---

## Proposed Fix (step by step)

### Step 1 — Restore the native auth routes in the router
**File:** `frontend/src/router/index.js`

- Change the `/login` route component from `LoginOrRegister` to `LoginView`.
- Add a `/register` route that renders `RegisterView`.

This immediately makes native login and register accessible without touching any other code.

### Step 2 — Fix LoginView.vue endpoint
**File:** `frontend/src/views/LoginView.vue:36`

Change the fetch URL from `/user/auth/generateToken` to `/auth/login`.

The `/auth/login` response is `{ accessToken: "..." }` (JSON), not plain text, so:
- Change `const token = await response.text()` to `const data = await response.json(); const token = data.accessToken`.

After this, native login works end-to-end and sets both the JWT and the refresh cookie.

### Step 3 — Fix the Keycloak redirect_uri
**File:** `frontend/src/views/LoginOrRegister.vue:5`

Change:
```js
const REDIRECT_URI = encodeURIComponent('http://localhost:8081/auth/callback')
```
To:
```js
const REDIRECT_URI = encodeURIComponent('http://localhost:5173/auth')
```

This makes Keycloak redirect the browser to the frontend's `/auth` page, where `AuthView.vue` can pick up the code.

Also update the Keycloak client configuration (via Compose / Keycloak admin) to whitelist `http://localhost:5173/auth` as a valid redirect URI.

### Step 4 — Fix AuthView.vue
**File:** `frontend/src/views/AuthView.vue`

Fix all three bugs:
1. Replace `response.data` with `const data = await response.json()`.
2. Change `localStorage.setItem('accessToken', ...)` to call `useSessionStore().setToken(data.accessToken)` — this writes to both the store and `jwt_token` in localStorage in one call.
3. After successful token exchange, call `router.push('/')`.

Also fix the `redirectUri` sent in the POST body: change `'http://localhost:5173/'` to `'http://localhost:5173/auth'` to match the redirect_uri used in the auth request.

### Step 5 — Fix backend `/auth/callback` (POST) to issue an internal JWT
**File:** `AuthController.java` — `keycloakCallback(@RequestBody ...)` method

After exchanging the authorization code with Keycloak:
1. Decode the Keycloak `id_token` (or call Keycloak's `/userinfo` endpoint) to get `preferred_username` and `email`.
2. Look up the user in the local DB by username; if not found, auto-create with a random password (or flag as Keycloak-linked).
3. Issue an internal JWT with `jwtService.generateToken(user)` and create a refresh token with `refreshTokenService.createRefreshToken(username)`.
4. Set the refresh cookie the same way `/auth/login` does.
5. Return `AuthResponse(accessToken)` — the same shape as native login.

Also update the hardcoded `REDIRECT_URI` in this method from `http://localhost:8081/auth/callback` to `http://localhost:5173/auth` (matching what Step 3 sends).

### Step 6 — Fix `RefreshTokenRepository` and `revokeByUsername`
**File:** `RefreshTokenRepository.java` — add method:
```java
void deleteByUsername(String username);
```
**File:** `RefreshTokenService.java:41` — uncomment:
```java
repository.deleteByUsername(username);
```
Remove the `System.out.println`.

### Step 7 — Make the refresh cookie conditional on environment
**File:** `AuthController.java` (both login and refresh cookie builders)

Inject `@Value("${app.secure-cookie:true}")` and use it in `.secure(secureCooke)`. Add `app.secure-cookie=false` to `application.properties` for local dev (Docker Compose can override to `true` in production).

---

## Files Changed Summary

| File | Changes |
|---|---|
| `frontend/src/router/index.js` | Route `/login` → `LoginView`, add `/register` → `RegisterView` |
| `frontend/src/views/LoginView.vue` | Fix endpoint path, fix response parsing (text → json) |
| `frontend/src/views/LoginOrRegister.vue` | Fix `REDIRECT_URI` to point to frontend `/auth` |
| `frontend/src/views/AuthView.vue` | Fix `response.data`, fix localStorage key, add store update + redirect, fix `redirectUri` body field |
| `AuthController.java` | Fix `keycloakCallback` POST to issue internal JWT + refresh cookie; fix hardcoded redirect URI; make secure cookie conditional |
| `RefreshTokenRepository.java` | Add `deleteByUsername(String username)` |
| `RefreshTokenService.java` | Uncomment `deleteByUsername` call, remove println |
| `src/main/resources/application.properties` | Add `app.secure-cookie=false` |

---

## Out of Scope

- `LoginOrRegister.vue` (the Keycloak landing page) is not the primary user-facing login once Step 1 is done — it can remain as-is for Keycloak access.
- The `@GetMapping("/callback")` on `AuthController` is now unused (Keycloak redirects to frontend, not backend). It can be deleted separately.
- The `/auth/generateToken` endpoint is redundant with `/auth/login` but can stay until cleaned up later.
- Keycloak auto-provisioning (Step 5) may require a separate DB migration if a `keycloak_id` column is needed on the user table.
