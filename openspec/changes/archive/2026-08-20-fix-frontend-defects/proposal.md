## Why

A full read of the frontend turned up fourteen defects, all verified against the source. Two are session-fatal, several make the game look broken when it is merely failing, and one is a client/server mismatch introduced by the backend fix that landed alongside this change.

The theme is **failures are invisible**. `apiRequest` collapses every error into one hardcoded string, three components swallow rejections entirely, and the two most common failure modes — an expired token and a failed fetch — are presented to the player as an empty screen rather than as an error.

## What Changes

### API client and session (`util/api/api.js`)
- **Refresh the access token instead of ending the session.** The backend issues a 30-minute access token and exposes `POST /auth/refresh` backed by a 24-hour HTTP-only cookie. Nothing in the frontend has ever called it, so every session dies after 30 minutes and dumps the player at the login screen mid-game. `apiRequest` now retries once through `/auth/refresh` on a `401`, using a single-flight guard so concurrent requests share one refresh.
- **A `401` that cannot be refreshed now throws and clears the session.** Today `handleNotOkResponse` *returns* on `401`, so execution falls through to `response.json()` on an error body — callers see a JSON parse error rather than an auth error — and `clearSession()` is never called, leaving `jwt_token` in `localStorage` and `isAuthenticated` true. The router then keeps re-admitting the player, who bounces between `/` and `/login` until they clear storage by hand.
- **Errors carry the server's reason and status.** `throw new Error('Failed to fetch buildings')` was the single response to every failure of all twelve API functions. A new `ApiError` carries `status` and the message the backend actually sent (now meaningful, since the backend change makes error bodies specific).
- Remove the dead `/user/register` and `/user/auth/generateToken` branches — the Keycloak-only flow has no such endpoints.

### Village map lifecycle (`components/VillageNew.vue`)
- **Do not gate the map on the buildings fetch.** `fetchBuildings` is the first `await` in `onMounted`, before the PixiJS `Application` is even constructed, and the catch only hides the loading overlay. Any failure leaves the player staring at an empty white div with no message and no retry. The map now renders regardless, and a failed buildings fetch shows a banner.
- **Guard the async mount against unmount.** With six `await` points and no cancellation check, navigating away mid-mount runs `onBeforeUnmount` while `app` and `canvasCleanup` are still unassigned — so nothing is torn down — and the suspended continuation then registers `window.addEventListener('resize', …)` *after* the matching removal already ran. Each aborted visit leaks a live WebGL context plus a permanent listener. A `cancelled` flag plus a single idempotent `teardown()` fixes both.
- **Fit the map to the new canvas size, not the old one.** `resizeTilemap` reads `app.renderer.width/height` synchronously inside the `resize` event, but PixiJS's `ResizePlugin` defers the actual resize to `requestAnimationFrame`, so the renderer still reports pre-resize dimensions. Scale and centering are computed from stale values and nothing re-runs afterwards. Dimensions now come from the resize target element itself, which reports the new size immediately.
- **A failed upgrade no longer vanishes silently.** `handleUpgrade` closes the card in `finally` regardless of outcome, so a rejected upgrade is indistinguishable from a mis-click. The card now stays open and shows the reason.

### Resource display (`stores/resources.js`, `views/Home.vue`)
- **Resources tick locally.** The store holds amounts and per-hour rates but nothing advances them — the only `setInterval` in the app is the training countdown. The header sits frozen all session, and worse, `canAfford` in three components evaluates against those frozen numbers, so a player who has genuinely accumulated enough sees every Upgrade button disabled. Amounts now accrue from elapsed wall time using the same formula as the server, with a fractional carry so nothing is lost, and re-sync on every `refresh`.
- **Fix the `villageId` fallback.** `safeVillageId` uses `??`, which only falls through on `null`/`undefined` — the initial id is `0`, so the `localStorage` fallback never fires and the player can navigate to `/village/0`.

### Failure feedback
- `stores/army.js` swallows every rejection, which makes ArmyView's `catch` unreachable and its `v-else-if="error"` branch dead markup: a failed roster fetch tells the player *"No units yet"*. The store now propagates; callers that want to degrade quietly do so explicitly.
- `BuildingsPanel` has a `try/finally` with no `catch` — an unhandled rejection on every failure, and a fully built village that renders "No buildings yet".
- `WorldView` has no error handling at all; a failed `/user/all` throws before the centering block, leaving half the grid off-screen.
- The Reports and Messages menu items call `goTo(...)`, which is not defined anywhere — clicking either throws. They become visibly disabled "coming soon" entries.
- `player.name` is never assigned even though `/user` returns `username`, and `currentVillage.name` can never be assigned because `VillageDTO` has no name field — the header renders an empty span and a bare 🏰 forever. The name is now populated and the village is identified by its coordinates.
- Logout stops the training store but leaves the army roster and resource totals in memory, so the previous account's numbers are briefly visible after the next login.

### Training queue store
- `MAX_TRAIN_QUANTITY = 999` and `MIN_TRAIN_QUANTITY = 0` in `BuildingUpgradeCard` now contradict the server, which accepts `1..MAX_TRAINING_BATCH_SIZE`. Shared constants move to `util/gameConfig.js`, mirroring `GameDefaults.java` — this also removes the duplicated `VANDAL_FOOD_COST` / `VANDAL_IRON_COST` / `TRAINING_DURATION_MS` literals.
- `trainingStore.stop()` resets `orders` and `villageId` but leaves `clockOffsetMs` from the previous session.

## Capabilities

### New Capabilities
- `api-client-session-handling`: token refresh, session teardown on an unrecoverable `401`, and errors that carry the server's status and reason.
- `village-map-lifecycle`: the PixiJS map mounts, resizes, and tears down correctly, and reports load failures instead of showing a blank screen.
- `resource-display`: resource amounts advance in real time between server syncs, and the current village id resolves correctly.
- `failure-feedback`: a failed fetch is presented as a failure, never as an empty result.

### Modified Capabilities
- `training-queue-store`: batch-size bounds are shared with the backend, and `stop()` fully resets the store.

## Impact

- New: `frontend/src/util/gameConfig.js`, `frontend/src/util/api/apiError.js`
- Modified: `util/api/api.js`, `stores/resources.js`, `stores/army.js`, `stores/training.js`, `views/Home.vue`, `views/ArmyView.vue`, `views/WorldView.vue`, `components/VillageNew.vue`, `components/BuildingsPanel.vue`, `components/BuildingUpgradeCard.vue`
- New tests: `util/api/__tests__/api.spec.js`, `views/__tests__/WorldView.spec.js`
- Extended tests: the resources, army, training, ArmyView, BuildingsPanel, VillageNew, BuildingUpgradeCard, and Home suites
- **No backend change.** `POST /auth/refresh` already exists and is `permitAll`.

## Out of scope

- Rewriting `VillageNew.vue` into composables (recorded as refactor work).
- Reports and Messages as real features — they become disabled placeholders rather than throwing handlers.
- Adding a village `name` field to `VillageDTO`.
- Optimistic UI or offline queuing.
