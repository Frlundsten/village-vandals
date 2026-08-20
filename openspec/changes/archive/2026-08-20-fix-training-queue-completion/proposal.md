## Why

When a player starts training in a Barrack and then closes the building card, the countdown dies with the card and the trained Vandals never appear in the UI — the roster only updates if the card is left open until the timer expires. The units *are* trained (the backend promotes completed orders lazily on read), so this is purely a client-side defect: the only live countdown lives inside `BuildingUpgradeCard.vue`, which is destroyed by `v-if` on close, and nothing else ever polls again.

## What Changes

- Introduce a persistent Pinia store, `useTrainingStore`, that owns the training queue and a single countdown interval for the whole app. The interval keeps running while the player closes the card, navigates between the village / buildings / army routes, or sits idle — training visibly completes without anyone watching it.
- `BuildingUpgradeCard.vue` stops owning training state. It reads `trainingStore.ordersForBuilding(buildingId)` for display and hands the `POST /unit/train` response to the store. Its local `trainingOrders` ref, `startCountdown`/`stopCountdown`, `clockOffsetMs`, `enrichOrder`, and `mountFetchCancelled` guard are removed.
- `useTrainingQueue.js` is removed. Its `queue` / `refresh` / `setQueue` / `hasActiveFor` surface is absorbed by the store; `VillageNew.vue` reads the store directly. (The composable's `setQueue` hook was never wired up by any caller, which is the root cause of the bug.)
- **Fix queue-position renumbering.** Both current countdowns drop finished orders with `filter(o => o.remainingMs > 0)` without renumbering `queuePosition`, so after the first order completes no order has `queuePosition === 1`. Completion detection then stops permanently: in a queue of several orders only the first ever triggers a roster refresh, and the active-order progress bar disappears while later orders still render. The store renumbers positions after every removal.
- **Fix the barrack training indicator on the village map.** `VillageNew.vue` calls `hasActiveFor(badge.constructionSiteId)` while the matcher compares against `TrainingOrderDTO.buildingId` — two distinct identifier spaces, so the pulsing ring never shows. Badges will carry `buildingId` (already available on the building objects in `buildingsBySiteId`) and the store will match on it.
- Training state is reconciled from the server on store hydration and after every completion, so a player who logs out mid-training and returns sees the correct queue and roster.

No breaking changes. No backend or API changes.

## Capabilities

### New Capabilities
- `training-queue-store`: A persistent, app-level Pinia store owning the training queue, its countdown lifecycle, clock-offset correction, per-building lookups, and completion-driven roster refresh — independent of which components are mounted.

### Modified Capabilities
- `army-roster-store`: The "Training completion triggers roster refresh" requirement moves ownership of the completion trigger from the building card's local countdown to `useTrainingStore`, and must now fire for *every* order in a queue rather than only the first.
- `training-queue-clock-sync`: The clock-offset derivation and its use in `remainingMs` calculations move out of `BuildingUpgradeCard.vue` and `useTrainingQueue.js` into `useTrainingStore` as the single owner. Backend `serverTime` requirements are unchanged.

## Impact

**Frontend (all changes are frontend-only):**
- New: `frontend/src/stores/training.js`
- Removed: `frontend/src/composables/useTrainingQueue.js` and `frontend/src/composables/__tests__/useTrainingQueue.spec.js`
- Modified: `frontend/src/components/BuildingUpgradeCard.vue` — training state and interval removed, reads from the store
- Modified: `frontend/src/components/VillageNew.vue` — reads the store instead of the composable; badges carry `buildingId`
- Modified: `frontend/src/views/Home.vue` — hydrates the training store alongside the army store once the current village is known, so the countdown is alive for the whole session
- Tests: `frontend/src/components/__tests__/BuildingUpgradeCard.spec.js`, `frontend/src/components/__tests__/VillageNew.spec.js`, plus a new `frontend/src/stores/__tests__/training.spec.js`

**Backend:** none. `UnitService.resolveCompletedOrders` already runs lazily on both `GET /unit` and `GET /unit/training`, so durability across logout is already satisfied.

**Dependencies:** none added.
