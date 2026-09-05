## 1. API client — failing tests first

- [x] 1.1 Create `frontend/src/util/api/__tests__/api.spec.js` with a mocked `fetch` and an active Pinia; assert a successful call returns parsed JSON and sends the Bearer token
- [x] 1.2 Add a test: a `401` followed by a successful `/auth/refresh` stores the new token and replays the original request once, returning its data and never navigating
- [x] 1.3 Add a test: a `401` where refresh also fails clears the session, routes to `/login`, and rejects with `status === 401` (not a JSON parse error)
- [x] 1.4 Add a test: a replayed request that returns `401` again is not refreshed or replayed a second time
- [x] 1.5 Add a test: three concurrent calls that all receive `401` trigger exactly one `/auth/refresh`
- [x] 1.6 Add a test: a `401` from an `/auth/*` path does not attempt a refresh
- [x] 1.7 Add tests for error-message extraction from `{"message":{"message":…}}`, a bare string body, plain text, and an empty body — each carrying the right `status`
- [x] 1.8 Add a test: a `200` with an empty body resolves instead of rejecting

## 2. API client — implementation

- [x] 2.1 Add `frontend/src/util/api/apiError.js` exporting `ApiError` with a `status` field
- [x] 2.2 Rewrite `apiRequest`: `send()` helper, refresh-and-replay on `401`, single-flight `attemptRefresh()`, session clear + redirect + throw when unrecoverable
- [x] 2.3 Add the tolerant error-body reader (nested `message.message`, `message`, `error`, raw text, fallback)
- [x] 2.4 Remove the dead `/user/register` and `/user/auth/generateToken` branches
- [x] 2.5 Run `npm run test:unit -- api` and confirm green

## 3. Resource store — failing tests first

- [x] 3.1 Extend `stores/__tests__/resources.spec.js` with fake timers: amounts advance according to the per-hour rate as time passes
- [x] 3.2 Add a test: a rate below one unit per tick still credits a whole unit once enough time has passed (fractional carry)
- [x] 3.3 Add a test: a long delay between ticks credits the real elapsed time, not one nominal interval
- [x] 3.4 Add a test: a zero rate leaves the amount unchanged
- [x] 3.5 Add a test: `refresh` overwrites a locally projected amount and restarts accrual from that point
- [x] 3.6 Add a test: `reset()` zeroes amounts and rates and stops the accrual timer

## 4. Resource store — implementation

- [x] 4.1 Add elapsed-time accrual with a per-resource fractional carry and a 1 s tick, started lazily on first `refresh`
- [x] 4.2 Reset amounts, rates, carry, and the accrual clock inside `refresh`
- [x] 4.3 Add `reset()` and expose it
- [x] 4.4 Run `npm run test:unit -- resources` and confirm green

## 5. Army store and failure feedback — failing tests first

- [x] 5.1 Change the `stores/__tests__/army.spec.js` failure case to assert `refresh` rejects and leaves the roster unchanged
- [x] 5.2 Add an `ArmyView.spec.js` test: a rejected roster fetch renders an error, not "No units yet"
- [x] 5.3 Add a `BuildingsPanel.spec.js` test: a rejected buildings fetch renders an error, not "No buildings yet", with no unhandled rejection
- [x] 5.4 Create `views/__tests__/WorldView.spec.js`: a rejected `/user/all` still centres the grid and shows an error; a successful fetch marks occupied tiles
- [x] 5.5 Add a `Home.spec.js` test: the header shows the username from `/user`
- [x] 5.6 Add a `Home.spec.js` test: logout resets the army roster and resource totals
- [x] 5.7 Add a `Home.spec.js` test: `safeVillageId` falls back to the stored id while the current village id is still `0`
- [x] 5.8 Add a `training.spec.js` test: `stop()` clears the derived clock offset

## 6. Failure feedback — implementation

- [x] 6.1 `stores/army.js`: propagate the rejection; add `reset()`
- [x] 6.2 `stores/training.js`: reset `clockOffsetMs` in `stop()`
- [x] 6.3 `views/ArmyView.vue`: keep its `catch`, now reachable
- [x] 6.4 `components/BuildingsPanel.vue`: add a `catch` and a `loadError` state distinct from the empty state; refresh resources on mount
- [x] 6.5 `views/WorldView.vue`: wrap the fetch, keep the centring block on the failure path, show an error
- [x] 6.6 `views/Home.vue`: populate `player.name`, identify the village by coordinates, fix `safeVillageId` to `||`, guard `updateResourceUI`, reset army and resource stores on logout, and render Reports/Messages as disabled placeholders
- [x] 6.7 Run `npm run test:unit` for the affected suites and confirm green

## 7. Village map lifecycle — failing tests first

- [x] 7.1 Add a `VillageNew.spec.js` test: a rejected `fetchBuildings` still renders the map, dismisses the loading overlay, and shows an error banner
- [x] 7.2 Add a test: unmounting before the mount chain finishes leaves no window `resize` listener registered and destroys the PixiJS application
- [x] 7.3 Add a test: a failed upgrade keeps the card open and shows the reason
- [x] 7.4 Add a test: a failed construction surfaces an error
- [x] 7.5 Add a test: resize computes the fit from the resize target's client dimensions, not the renderer's stale ones

## 8. Village map lifecycle — implementation

- [x] 8.1 Add a `cancelled` flag and an idempotent `teardown()`; check `cancelled` after each await; track the resize listener with a boolean
- [x] 8.2 Move the buildings fetch into its own try/catch and continue rendering with an empty list
- [x] 8.3 Add `loadError` / `actionError` state and render it
- [x] 8.4 Read viewport dimensions from the resize target element, falling back to the renderer when it reports zero
- [x] 8.5 Keep the card open and show the reason when an upgrade fails; surface construction failures
- [x] 8.6 Run `npm run test:unit -- VillageNew` and confirm green

## 9. Shared game config

- [x] 9.1 Add a `BuildingUpgradeCard.spec.js` test: quantity clamps to `[1, MAX_TRAINING_BATCH_SIZE]`
- [x] 9.2 Create `frontend/src/util/gameConfig.js` mirroring `GameDefaults.java`
- [x] 9.3 Replace the literals in `BuildingUpgradeCard.vue` and use the shared bounds in `clampQuantity`
- [x] 9.4 Run `npm run test:unit -- BuildingUpgradeCard` and confirm green

## 10. Verify

- [x] 10.1 Run the full `npm run test:unit` suite and confirm green
- [x] 10.2 Run `npm run format`
- [x] 10.3 Re-read the diff against the delta specs and confirm every requirement has covering tests

## 11. Notes recorded during implementation

- [x] 11.1 `BuildingsPanel` now syncs resources on mount — the `/buildings` route is reachable without ever passing through the village map, and affordability is evaluated against the store.
- [x] 11.2 `Home.loadUserData` runs the resource, army, and training loads with `Promise.allSettled` so one failure no longer skips the others or blanks the header.
- [x] 11.3 The unmount-mid-mount test splits in two: cancelling during the buildings fetch must build nothing at all, and cancelling during `app.init()` must still destroy what was built.
- [ ] 11.4 Follow-up: `VillageNew.vue` is still ~500 lines mixing Pixi bootstrap, Tiled scanning, input handling, and API orchestration. Extracting the Pixi lifecycle into a composable is refactor work, not a bug fix.
