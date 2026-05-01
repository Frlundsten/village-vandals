# Task 3 — Fix AuthView.vue

## Problems
1. `response.data` — `fetch` has no `.data` property; response body must be awaited with `.json()`
2. `localStorage.setItem('accessToken', ...)` — wrong key; `api.js` and pinia read from `jwt_token`
3. No `useSessionStore().setToken()` call — pinia state never updated
4. No redirect after successful token exchange — user left on blank `/auth` page
5. `redirectUri` in POST body was `http://localhost:5173/` — must match the auth request redirect URI

## Files changed
- `frontend/src/views/AuthView.vue`

## Status: DONE
