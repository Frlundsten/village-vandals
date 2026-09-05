## Context

Every backend call goes through `apiRequest()` in `util/api/api.js`. It is 47 lines and has no test coverage. It injects the JWT, and on failure either navigates to `/login` (401) or throws one hardcoded string. Nothing else in the app touches `fetch`, except `AuthView.vue`, which calls `/auth/callback` directly because no token exists yet.

The backend issues a 30-minute access token (`JwtService.ACCESS_EXP_MS`) and a 24-hour refresh cookie, and exposes `POST /auth/refresh` as a `permitAll` endpoint. The refresh half has been implemented on the server since the Keycloak-only auth change and has never had a client.

`stores/resources.js` holds four amounts and four per-hour rates. Three separate components compute affordability from those amounts. Nothing advances them between explicit `refresh()` calls.

`VillageNew.vue` boots PixiJS in an `async onMounted` with six `await` points and tears down in `onBeforeUnmount`, with no coordination between the two.

## Goals / Non-Goals

**Goals:**
- A session that survives past the access token's 30-minute lifetime.
- Failures that reach the player as failures, carrying the server's reason.
- A map view that renders, resizes, and tears down correctly.
- Resource numbers that move, so affordability checks are correct.
- Client-side input bounds that match the server's.

**Non-Goals:**
- No backend changes.
- No restructure of `VillageNew.vue` into composables (separate refactor).
- No new features — Reports/Messages become honest placeholders, not implementations.
- No global toast/notification system; each surface shows its own inline error.

## Decisions

**1. Refresh-on-401 lives inside `apiRequest`, with a single-flight guard.**
A retry wrapper is the only place that sees every call, and callers should not each have to know about token lifetimes. The flow is: send → if `401` and this is not itself an auth call, `await attemptRefresh()` → if that yields a token, store it and send once more → otherwise clear the session, route to `/login`, and throw.

The single-flight guard matters because Home's mount fires `/user`, `/resources/refresh`, `/unit`, and `/unit/training` in sequence and the village view fires more in parallel; without it an expired token would trigger four concurrent refreshes, and since the backend *rotates* the refresh cookie on every use, the later ones would present an already-revoked token and fail. One in-flight refresh promise is shared by all callers.

Exactly one retry. A second `401` after a successful refresh means something other than expiry is wrong, and retrying again risks a loop.

**2. `apiRequest` imports `useSessionStore`.**
Clearing the session requires updating the store's `token`, not just `localStorage` — `isAuthenticated` reads the in-memory value, and leaving it true is precisely what causes the `/` ↔ `/login` bounce today. `stores/pinia.js` imports nothing from the api layer, so there is no cycle. Tests must call `setActivePinia` before exercising `apiRequest`.

**3. `ApiError` carries `status` and the server's message.**
A dedicated `ApiError extends Error` with a `status` field, so a caller can distinguish "you cannot afford this" (`400`) from "that is not your village" (`403`) if it wants to, while `err.message` stays useful for direct display.

The body reader is deliberately tolerant, because the backend does not speak one shape: the global exception handler returns `{"message":{"message":"…"}}`, `POST /unit/train` returns a bare string, and Spring's own error page returns `{"timestamp":…,"error":…}`. The reader tries nested `message.message`, then `message`, then `error`, then the raw text, then a caller-supplied fallback — taking only string values.

**4. Resources accrue from elapsed wall time, not from a fixed per-tick increment.**
A `setInterval` that adds `rate/3600` per tick drifts whenever the browser throttles background tabs. Instead each tick measures the real elapsed time since the previous tick and accrues `rate * elapsedSeconds / 3600`, keeping the fractional part in a carry so nothing is lost between ticks — the same "credit whole units, keep the remainder" rule the server now uses.

The amounts stay writable `ref`s rather than becoming computed projections. Making them computed would be marginally cleaner but would break every existing caller and test that assigns to them, for no behavioural gain.

The tick is authoritative only *between* syncs: `refresh()` overwrites both the amounts and the carry from the server response, so the local projection can never drift permanently.

**5. Resize reads the resize target element, not the renderer.**
PixiJS's `ResizePlugin` registers its own `window` resize listener (during `app.init`, therefore before ours) and defers `renderer.resize()` to `requestAnimationFrame`. Reading `app.renderer.width/height` in our handler therefore sees the *previous* size.

Rather than chaining our work onto a `requestAnimationFrame` and depending on listener-registration order, we read `clientWidth`/`clientHeight` from the element that `resizeTo` points at. That is the size PixiJS is about to adopt, and it is correct synchronously. It falls back to `app.renderer` when the element reports zero, which is what happens under jsdom.

**6. One idempotent `teardown()`, plus a `cancelled` flag.**
`onBeforeUnmount` sets `cancelled = true` and calls `teardown()`. The async mount checks `cancelled` after each `await` and calls `teardown()` on the way out, so whichever finishes last cleans up. `teardown()` nulls what it releases, so running twice is harmless. The resize listener is tracked by a boolean rather than relying on `removeEventListener` being a no-op, so it is never registered after removal.

**7. The buildings fetch is decoupled from map rendering.**
It moves into its own `try/catch` that records a banner message and continues with an empty list. The map does not depend on building data for anything but placing building sprites, so a failure should cost the player their buildings' sprites, not their whole screen.

**8. `army.js` propagates; deliberate degradation moves to the callers.**
Swallowing inside the store made a legitimate error branch in ArmyView unreachable, which is worse than the failure it was hiding. The one caller that genuinely must not throw — `trainingStore.tick()`, which is fire-and-forget from an interval — already has its own `.catch(() => {})`.

**9. Shared constants mirror `GameDefaults.java` in one file.**
`util/gameConfig.js` carries `VANDAL_FOOD_COST`, `VANDAL_IRON_COST`, `TRAINING_DURATION_MS`, and `MAX_TRAINING_BATCH_SIZE`, with a comment naming the Java file they mirror. This is duplication, but it is duplication in one declared place instead of scattered literals in `BuildingUpgradeCard`, and it puts the client's input bounds and the server's in the same line of sight. Generating them from the backend is out of scope.

## Risks / Trade-offs

- **A refresh loop.** Mitigated by the single retry and by never attempting refresh for `/auth/*` paths.
- **`gameConfig.js` can drift from `GameDefaults.java`.** No test can catch that without a running backend. The mismatch this change fixes (999 vs 50) is exactly that failure mode, so the file carries an explicit pointer to its source of truth.
- **The local resource tick can disagree with the server** if resources change from another tab or session. Bounded: any action re-syncs, and the client is only ever optimistic about *gains*, never about spending — the server re-checks affordability on every deduction.
- **Reports/Messages become visibly disabled.** That is a small UX regression from "appears to work" to "clearly not available yet", but the current behaviour is an exception on click.
