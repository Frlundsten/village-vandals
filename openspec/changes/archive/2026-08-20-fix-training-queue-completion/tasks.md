## 1. Training store — failing tests first

- [x] 1.1 Create `frontend/src/stores/__tests__/training.spec.js` with fake timers and a mocked `@/util/api/units.js`; assert `hydrate(villageId)` seeds `orders` from `GET /unit/training`, starts the countdown, and leaves `orders` untouched when the fetch rejects
- [x] 1.2 Add a test asserting an empty `hydrate` response leaves `orders` empty and starts no interval
- [x] 1.3 Add a test asserting the countdown survives with no component mounted: after `setOrders`, advancing timers past `finishesAt` calls `armyStore.refresh(villageId)` exactly once and removes the order
- [x] 1.4 Add a test asserting `queuePosition` is renumbered from 1 after a removal, and that the *second* order of a three-order queue also triggers `armyStore.refresh` when it later expires
- [x] 1.5 Add a test asserting two orders expiring within the same tick are both removed and the remainder renumbered from 1
- [x] 1.6 Add a test asserting the interval is cleared once the queue empties, and that `startCountdown` never produces a second concurrent interval
- [x] 1.7 Add a test asserting `ordersForBuilding(buildingId)` and `hasActiveFor(buildingId)` match on `buildingId` and return nothing for an unrelated id
- [x] 1.8 Add a test asserting the clock offset is derived from the first order's `serverTime` on both `hydrate` and `setOrders`, is used for `remainingMs`, and is left unchanged by an empty response
- [x] 1.9 Add a test asserting `stop()` clears the interval and resets `orders` and the cached `villageId`
- [x] 1.10 Add a test asserting an `armyStore.refresh` rejection after a completion is swallowed and leaves `orders` intact

## 2. Training store — implementation

- [x] 2.1 Create `frontend/src/stores/training.js` with `useTrainingStore`: reactive `orders`, cached `villageId`, module-scoped `intervalId` and `clockOffsetMs`
- [x] 2.2 Implement `hydrate(villageId)` — cache the id, fetch `GET /unit/training`, update the clock offset, enrich orders with `remainingMs`, start the countdown; swallow errors
- [x] 2.3 Implement `setOrders(rawOrders)` — replace `orders`, update the clock offset, start the countdown
- [x] 2.4 Implement `startCountdown()` guarded against double-start, on the existing 100 ms cadence
- [x] 2.5 Implement `tick()` — recompute `remainingMs` using `Date.now() + clockOffsetMs`, remove expired orders, renumber `queuePosition` from 1 in `finishesAt` order, fire-and-forget `armyStore.refresh(villageId)` on each completion, and clear the interval when the queue empties
- [x] 2.6 Implement `ordersForBuilding(buildingId)` and `hasActiveFor(buildingId)` matching on `TrainingOrderDTO.buildingId`
- [x] 2.7 Implement `stop()` — clear the interval, reset `orders` and `villageId`
- [x] 2.8 Run `npm run test:unit` and confirm the store suite from section 1 passes

## 3. VillageNew — read from the store

- [x] 3.1 Extend `frontend/src/components/__tests__/VillageNew.spec.js` with a failing test: given a pending order for a barrack whose `buildingId` differs from its `constructionSiteId`, the pulsing training indicator renders
- [x] 3.2 Add `buildingId` to the badge objects built in `updateBadgePositions()`, sourced from `buildingsBySiteId`
- [x] 3.3 Replace the `useTrainingQueue` import and call with `useTrainingStore`, and change the template to `trainingStore.hasActiveFor(badge.buildingId)`
- [x] 3.4 Run `npm run test:unit -- VillageNew` and confirm green

## 4. BuildingUpgradeCard — pure view over the store

- [x] 4.1 Extend `frontend/src/components/__tests__/BuildingUpgradeCard.spec.js` with a failing test: unmounting the card while an order is pending does not stop the countdown, and the order still completes and refreshes the roster
- [x] 4.2 Add a failing test: opening the card while the store already holds a pending order for that barrack renders the queue and countdown immediately, with no `GET /unit/training` call
- [x] 4.3 Add a failing test: a successful `trainUnit` passes the returned queue to `trainingStore.setOrders`
- [x] 4.4 Remove `trainingOrders`, `startCountdown`, `stopCountdown`, `clockOffsetMs`, `enrichOrder`, `updateClockOffset`, `mountFetchCancelled`, and the `onMounted`/`onUnmounted` hooks from the card
- [x] 4.5 Derive `activeOrder`, `pendingOrders`, `activeCountdown`, `elapsedMs`, and `activeOrderDurationMs` from `trainingStore.ordersForBuilding(props.building.buildingId)`; guard the training-queue section on that list being non-empty
- [x] 4.6 Change `handleTrainVandal` to call `trainingStore.setOrders(response)` and keep the existing `resourceStore.refresh` and error handling
- [x] 4.7 Run `npm run test:unit -- BuildingUpgradeCard` and confirm green

## 5. Session wiring

- [x] 5.1 Add a failing test to `frontend/src/views/__tests__/Home.spec.js`: `trainingStore.hydrate` is called with the current village id once the village is loaded
- [x] 5.2 Add a failing test: `handleLogout` calls `trainingStore.stop()`
- [x] 5.3 Call `trainingStore.hydrate(village.id)` alongside `armyStore.refresh(village.id)` in `Home.vue`
- [x] 5.4 Call `trainingStore.stop()` in `handleLogout` before clearing the session
- [x] 5.5 Run `npm run test:unit -- Home` and confirm green

## 6. Cleanup and verification

- [x] 6.1 Delete `frontend/src/composables/useTrainingQueue.js` and `frontend/src/composables/__tests__/useTrainingQueue.spec.js`
- [x] 6.2 Grep for remaining `useTrainingQueue` references and remove any stragglers
- [x] 6.3 Run the full `npm run test:unit` suite and confirm green
- [x] 6.4 Run `npm run format`
- [ ] 6.5 Manual check against the running stack: train a Vandal, close the building card immediately, and confirm the unit appears in the Home army mini-panel when the timer expires
- [ ] 6.6 Manual check: queue three Vandal orders, leave the card open, and confirm each completes in turn with the progress bar advancing to the next order
- [ ] 6.7 Manual check: start training, confirm the pulsing indicator appears on the barrack on the village map, and that it clears when the queue empties
- [ ] 6.8 Manual check: train a batch, log out, log back in, and confirm the roster and remaining queue are correct
