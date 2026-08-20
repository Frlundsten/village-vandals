# Spec: Training Queue Store

## Purpose

A persistent, app-level Pinia store (`useTrainingStore`) owning the village training queue, its countdown lifecycle, clock-offset correction, per-building lookups, and completion-driven roster refresh — independent of which components happen to be mounted.

---

## Requirements

### Requirement: Training queue lives in a persistent shared store
The system SHALL provide a Pinia store (`useTrainingStore`) holding the village's pending training orders as a reactive `orders` ref, together with a `hydrate(villageId)` action that fetches `GET /unit/training` and seeds the store. The store SHALL be the single source of truth for training queue state; no component SHALL keep its own copy of the queue or its own countdown interval.

#### Scenario: Hydration seeds the queue
- **WHEN** `trainingStore.hydrate(villageId)` is called and the backend returns two pending orders
- **THEN** `trainingStore.orders` contains both orders with their `remainingMs` computed
- **AND** the countdown is running

#### Scenario: Hydration with an empty queue
- **WHEN** `trainingStore.hydrate(villageId)` is called and the backend returns no pending orders
- **THEN** `trainingStore.orders` is an empty array
- **AND** no countdown interval is left running

#### Scenario: Hydration failure degrades gracefully
- **WHEN** `GET /unit/training` rejects during `hydrate`
- **THEN** the rejection is swallowed
- **AND** `trainingStore.orders` retains its previous value

### Requirement: Countdown survives component unmount and route changes
The store's countdown interval SHALL be owned by the store, not by any component. Closing the building card, navigating between routes, or unmounting the village view SHALL NOT stop the countdown while orders remain pending. The interval SHALL be started when the queue becomes non-empty and stopped when it becomes empty.

#### Scenario: Training completes while the building card is closed
- **GIVEN** the player starts a training order from the Barrack card
- **AND** the player closes the building card before the countdown expires
- **WHEN** the order's `finishesAt` passes
- **THEN** the store's countdown detects the completion
- **AND** `armyStore.refresh(villageId)` is called
- **AND** the newly trained unit appears in the Home army mini-panel without a page reload

#### Scenario: Training completes while the player is on another route
- **GIVEN** an order is pending and the player navigates away from the village view
- **WHEN** the order's `finishesAt` passes
- **THEN** the countdown still detects the completion and refreshes the roster

#### Scenario: Interval stops when the queue empties
- **WHEN** the last pending order completes and is removed from `orders`
- **THEN** the countdown interval is cleared
- **AND** no further ticks occur until new orders are added

#### Scenario: Interval is not started twice
- **WHEN** orders are set on the store while a countdown is already running
- **THEN** exactly one interval remains active

### Requirement: Completed orders are removed and queue positions renumbered
On each tick the store SHALL recompute `remainingMs` for every order, remove orders whose `remainingMs` has reached zero, and renumber the remaining orders' `queuePosition` sequentially from 1 in `finishesAt` order. Completion detection SHALL therefore continue to work for every order in a multi-order queue, not only the first.

#### Scenario: Second order in the queue also completes
- **GIVEN** a queue of three orders at positions 1, 2 and 3
- **WHEN** the order at position 1 completes and is removed
- **THEN** the former position-2 order becomes position 1 and the former position-3 order becomes position 2
- **AND** when that new position-1 order later completes, the roster is refreshed again

#### Scenario: Active order display survives an earlier completion
- **GIVEN** a queue of two orders
- **WHEN** the first order completes
- **THEN** the remaining order is presented as the active order with a live countdown and progress bar
- **AND** it is no longer listed as a pending/queued order

#### Scenario: Multiple orders expiring in the same tick
- **WHEN** a tick finds two orders whose `remainingMs` has reached zero
- **THEN** both are removed
- **AND** the remaining orders are renumbered from 1

### Requirement: Store exposes per-building queue lookups keyed by building id
The store SHALL expose `ordersForBuilding(buildingId)` returning the pending orders for that building and `hasActiveFor(buildingId)` returning whether any pending order exists for it. Both SHALL match against `TrainingOrderDTO.buildingId`. Callers SHALL pass a building id, never a construction-site id.

#### Scenario: Building card shows only its own orders
- **GIVEN** pending orders exist for two different barracks
- **WHEN** the building card for one barrack reads `ordersForBuilding(itsBuildingId)`
- **THEN** only that barrack's orders are returned

#### Scenario: Village map training indicator matches on building id
- **GIVEN** a pending order exists for a barrack whose building id is `B` and construction site id is `S`, where `B !== S`
- **WHEN** `VillageNew.vue` renders the badge for that building
- **THEN** it calls `hasActiveFor(B)` and the pulsing training indicator is shown

#### Scenario: No indicator for a building without orders
- **WHEN** `hasActiveFor` is called with a building id that has no pending orders
- **THEN** it returns false and no indicator is rendered

### Requirement: Starting a training order updates the shared store
When `POST /unit/train` succeeds, the caller SHALL pass the returned queue to the store via `setOrders(orders)`, which SHALL replace `orders`, refresh the clock offset, and start the countdown if it is not already running. The building card SHALL NOT start its own timer.

#### Scenario: Train response drives the shared queue
- **WHEN** the player trains from the Barrack card and the backend returns the updated queue
- **THEN** `trainingStore.orders` reflects the returned queue
- **AND** the countdown is running
- **AND** the village map training indicator for that barrack becomes visible

#### Scenario: Card renders queue from the store after reopening
- **GIVEN** an order is pending and the building card has been closed
- **WHEN** the player reopens the card for that barrack
- **THEN** the training queue section shows the pending order with a correct countdown, without waiting for a fresh fetch to return

### Requirement: Client batch-size bounds match the server's
The training quantity control SHALL be bounded by the same limits the server enforces: at least 1 and at most `MAX_TRAINING_BATCH_SIZE`. Unit costs and training duration SHALL be declared once, in a shared config module that names the backend file it mirrors, rather than as literals inside the building card.

#### Scenario: Quantity cannot be raised past the server limit
- **WHEN** the player increments the training quantity past `MAX_TRAINING_BATCH_SIZE`
- **THEN** it is clamped to `MAX_TRAINING_BATCH_SIZE`

#### Scenario: Quantity cannot be lowered below one
- **WHEN** the player decrements the training quantity below 1
- **THEN** it is clamped to 1

#### Scenario: Affordability uses the shared cost constants
- **WHEN** batch affordability is evaluated
- **THEN** it uses the shared unit cost constants, not literals redeclared in the component

### Requirement: Stopping the store resets all of its state
`stop()` SHALL clear the countdown, the orders, the cached village id, and the derived clock offset, leaving nothing from the previous session behind.

#### Scenario: Clock offset does not survive a stop
- **GIVEN** a clock offset was derived from a previous session's orders
- **WHEN** `stop()` is called and new orders arrive without a `serverTime`
- **THEN** remaining times are computed against the local clock, not the stale offset
