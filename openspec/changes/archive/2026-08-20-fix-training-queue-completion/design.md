## Context

Training a Vandal writes a `TrainingOrder` row with a `finishesAt` timestamp. The backend never runs a scheduler: `UnitService.resolveCompletedOrders` is called at the top of both `getRoster` and `getTrainingQueue`, so expired orders are promoted to `VillageUnit` rows lazily, the first time anything reads. Durability across logout, tab close, or server restart is therefore already guaranteed — a player who trains 50 Vandals and logs out will find them waiting on return.

The defect is entirely client-side, and it is a duplication problem. Two near-identical countdown implementations exist:

```
VillageNew.vue  (stays mounted)
 └── useTrainingQueue(villageId)          ← interval #1
      ├── queue = []                      ← seeded once at village mount
      ├── setQueue()                      ← exported, never called by anyone
      └── tick() { if (queue.length === 0) return }   ← dead forever

 └── <BuildingUpgradeCard v-if="showUpgradeCard">     ← DESTROYED on close
      ├── trainingOrders = ref([])        ← private copy of the queue
      ├── startCountdown()                ← interval #2
      │     └── on finish → armyStore.refresh()   ← the ONLY roster trigger
      └── onUnmounted → stopCountdown()
```

The card owns the only live timer and the only `armyStore.refresh` call, and `v-if` destroys it on close. The composable's `setQueue` was designed as the handoff from card to parent and was never wired up, so `queue` stays empty and its `tick` returns immediately, forever.

Two further defects are masked by this one:

1. **Positions are never renumbered.** Both ticks drop finished orders with `filter(o => o.remainingMs > 0)` while leaving `queuePosition` untouched. Once position 1 is removed, `find(o => o.queuePosition === 1)` returns `undefined` permanently — completion detection stops, and `activeOrder` goes null so the progress bar vanishes while later orders still render as "queued".
2. **`hasActiveFor` compares two different identifier spaces.** `VillageNew.vue:26` passes `badge.constructionSiteId`; the matcher compares against `TrainingOrderDTO.buildingId`. Per the project's architecture these are deliberately distinct, so the pulsing barrack indicator never appears.

## Goals / Non-Goals

**Goals:**
- A training countdown that runs for as long as orders are pending, regardless of which components are mounted or which route is active.
- Exactly one owner of training queue state, its interval, and its clock offset.
- Roster refresh fires for every completed order, not just the first in a queue.
- The village-map training indicator works.
- Net deletion of code: two implementations collapse into one.

**Non-Goals:**
- Any backend change. Lazy promotion on read already satisfies durability; adding a scheduler would be redundant work.
- Server push (SSE/WebSocket) for completion events. The polling-free lazy model is sufficient at this scale.
- Changing training mechanics: costs, durations, queue ordering, or the `POST /unit/train` contract.
- Offline/service-worker behaviour, or notifying the player when training completes while the tab is closed.

## Decisions

### Pinia store, not a composable

`useTrainingStore` in `frontend/src/stores/training.js`, sitting beside `useArmyStore` and `useResourceStore`.

A composable's state and lifecycle hooks belong to the component that calls it — which is exactly how the bug arose. A Pinia store is a module-level singleton: its state and its interval outlive every component, including route changes that unmount `VillageNew.vue` entirely. It also matches the precedent already set by `army-roster-store`, where the same "shared state across views" problem was solved this way.

*Alternative considered:* keep `useTrainingQueue` and have the card call its `setQueue`. This is a two-line fix and resolves the reported symptom, but leaves the countdown scoped to `VillageNew.vue` — training would still freeze when the player visits the Army or Buildings tab — and keeps both duplicate implementations and both latent bugs alive.

### One interval, owned by the store, self-starting and self-stopping

The store holds a module-scoped `intervalId`. `startCountdown()` is a no-op when an interval is already running; `tick()` clears the interval when `orders` becomes empty. Every mutation path (`hydrate`, `setOrders`) calls `startCountdown()` unconditionally and relies on the guard.

The 100 ms cadence is kept — the progress bar needs it to look smooth — and costs nothing when idle, because no interval exists while the queue is empty.

*Alternative considered:* one `setTimeout` per order, armed at `finishesAt`. Fewer wakeups, but the progress bar still needs a render loop, so it would mean two mechanisms instead of one. Not worth it.

### Renumber `queuePosition` on every removal

`tick()` recomputes `remainingMs` for all orders, filters out the expired ones, then reassigns `queuePosition` sequentially from 1 in `finishesAt` order. `activeOrder` is then always "the order at position 1", and detection works for the whole queue.

*Alternative considered:* drop `queuePosition` from the client model and treat `orders[0]` as active, relying on the backend's `finishesAt ASC` ordering. Cleaner in isolation, but `queuePosition` is part of `TrainingOrderDTO` and is what the existing tests and templates key on; renumbering is the smaller, lower-risk change.

### `buildingId` is the matching key, and badges carry it

The store's `ordersForBuilding` / `hasActiveFor` match on `TrainingOrderDTO.buildingId`. `VillageNew.vue` already holds full building objects in `buildingsBySiteId` (keyed by construction-site id, each value carrying `buildingId`), so the badge objects built in `updateBadgePositions()` gain a `buildingId` field and the template passes that.

*Alternative considered:* add `constructionSiteId` to `TrainingOrderDTO`. It would work, but it is a backend change in service of a frontend lookup, and it would put two identifiers for the same building into the wire format.

### The store is hydrated from `Home.vue`, and cleared on logout

`Home.vue` is the authenticated layout wrapper — it hosts the `RouterView` and stays mounted across every in-app route change. It already calls `armyStore.refresh(village.id)` once the current village is known; `trainingStore.hydrate(village.id)` joins it there, so the countdown is live for the whole session no matter which route the player lands on.

`handleLogout` calls `trainingStore.stop()` alongside `session.clearSession()`, clearing the interval and dropping `orders` and the cached `villageId`, so nothing leaks into the next account signed in on the same tab.

### The card becomes a pure view over the store

`BuildingUpgradeCard.vue` loses `trainingOrders`, `startCountdown`, `stopCountdown`, `clockOffsetMs`, `enrichOrder`, `mountFetchCancelled`, and both lifecycle hooks. It derives `activeOrder` / `pendingOrders` from `trainingStore.ordersForBuilding(props.building.buildingId)` and, on a successful `trainUnit`, hands the response to `trainingStore.setOrders(response)`.

Its `onMounted` fetch disappears along with the `mountFetchCancelled` race it existed to guard: the store is already hydrated, so reopening the card renders the live queue immediately instead of flashing empty until a fetch returns.

## Risks / Trade-offs

- **A store-owned interval leaks if `stop()` is missed on logout** → `stop()` is called from `handleLogout`, and `tick()` self-clears whenever the queue empties, so the worst case is one idle interval that terminates on the next completion. Covered by a test asserting the interval is cleared on logout.

- **Background-tab throttling delays completion detection.** Browsers clamp `setInterval` to roughly once per minute in hidden tabs, so a completion may be noticed late → the units themselves are unaffected (the backend promotes them on the next read), and the roster corrects itself on the next tick or route change. Re-hydrating on `visibilitychange` would tighten this and is noted as an open question rather than built now.

- **Vite HMR can leave a stale interval behind during development** → the `startCountdown` guard plus store-level `stop()` make this recoverable with a page reload; it does not affect production builds.

- **Client-computed zero could beat the backend's `Instant.now()` by a few milliseconds**, so a refresh could return a roster that does not yet include the unit → the existing clock-offset correction (`training-queue-clock-sync`) already aligns the two clocks. This risk exists in the current code and is not made worse.

- **Deleting `useTrainingQueue.js` removes its test file**, temporarily reducing the visible test count → its coverage is reproduced and extended in `frontend/src/stores/__tests__/training.spec.js`, which additionally covers the two latent bugs the composable never exercised.

## Migration Plan

Frontend-only; no data migration, no API change, no deploy coordination. Ordered so the suite stays green at each step:

1. Add `frontend/src/stores/training.js` with tests. Nothing consumes it yet.
2. Point `VillageNew.vue` at the store, add `buildingId` to badges, delete the `useTrainingQueue` import.
3. Strip training state from `BuildingUpgradeCard.vue`; wire `setOrders` and the store-derived computeds.
4. Hydrate in `Home.vue`; call `stop()` in `handleLogout`.
5. Delete `useTrainingQueue.js` and its spec.
6. Run `npm run test:unit` and verify manually: train, close the card, confirm the unit appears.

**Rollback:** revert the commit. No state outside the browser session is touched.

## Open Questions

- Should the store re-hydrate on `visibilitychange` / window focus to correct for background-tab throttling? Cheap to add, but it introduces a second refresh path — deferred unless the delay proves noticeable in practice.
- Should a completion produce any player-visible feedback (toast, sound) now that it can happen while the card is closed? Out of scope here, but the store is the natural place to hang it later.
