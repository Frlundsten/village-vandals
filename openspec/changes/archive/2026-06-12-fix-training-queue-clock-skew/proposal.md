## Why

After clicking "Train Vandal", the training queue panel in the Barrack's `BuildingUpgradeCard` appears for roughly one tick (~100ms) and then disappears, the pulsing training indicator on the village map never shows, and the army roster mini-panel does not update until the player navigates to another tab. Root cause: the frontend computes remaining training time as `new Date(order.finishesAt).getTime() - Date.now()`, comparing an absolute server-issued timestamp (`finishesAt`) against the client's own clock. Any meaningful skew between the backend clock and the client clock (observed: Docker/WSL2 backend clock lagging the host browser clock by tens of seconds) makes `finishesAt` look like it's already in the past, so the order is immediately treated as complete and dropped from the visible queue — even though the backend has not actually finished the order yet.

## What Changes

- Backend responses for `POST /unit/train` and `GET /unit/training` include the server's current time (`serverTime`) alongside the existing training order data, captured at the moment the response is built.
- Frontend (`BuildingUpgradeCard.vue` and `useTrainingQueue.js`) derives a clock offset (`serverTime - Date.now()`) from each response and uses `Date.now() + clockOffset` (instead of raw `Date.now()`) whenever computing `remainingMs` against `finishesAt`, for both the initial enrichment and the periodic countdown tick.
- Training queue countdown, progress bar, and the map's pulsing training indicator remain visible and accurate for the full training duration regardless of client/server clock differences.
- The existing "countdown reaches zero → `armyStore.refresh()`" mechanism now fires at the correct (skew-corrected) time, so the army roster mini-panel updates reactively without requiring a tab switch.

## Capabilities

### New Capabilities
- `training-queue-clock-sync`: Defines how the frontend reconciles server-issued `finishesAt` timestamps with the client clock using a server-time-derived offset, ensuring training queue countdowns and completion detection are accurate regardless of clock skew.

### Modified Capabilities
- none (no existing capability specs describe the training queue countdown's clock handling)

## Impact

- **Backend**: `TrainingOrderDTO`/response shape for `POST /unit/train` and `GET /unit/training` gains a `serverTime` field (or equivalent wrapper); `UnitController` and `UnitService` updated accordingly.
- **Frontend**: `BuildingUpgradeCard.vue` (`enrichOrder`, `startCountdown`) and `useTrainingQueue.js` (`enrichOrder`, `tick`) updated to apply a clock offset.
- **Tests**: `UnitControllerTest`, `BuildingUpgradeCard.spec.js`, `useTrainingQueue.spec.js` updated/extended to cover skewed-clock scenarios.
- No database schema changes.
