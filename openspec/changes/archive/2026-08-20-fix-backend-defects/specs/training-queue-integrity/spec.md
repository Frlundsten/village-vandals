## ADDED Requirements

### Requirement: Training batch size is bounded
`POST /unit/train` SHALL reject any `quantity` below 1 or above `MAX_TRAINING_BATCH_SIZE`, before any resource snapshot or deduction. This closes an integer-overflow path in which a very large `quantity` wraps the computed cost negative, passes the affordability check, and *credits* resources instead of deducting them.

#### Scenario: Quantity above the maximum is rejected
- **WHEN** a train request is made with `quantity` greater than `MAX_TRAINING_BATCH_SIZE`
- **THEN** the request is rejected
- **AND** no resources are snapshotted or deducted, and no training order is created

#### Scenario: Overflow-sized quantity does not grant resources
- **WHEN** a train request is made with a `quantity` large enough to overflow `int` cost arithmetic
- **THEN** the request is rejected
- **AND** the village's stored resources are unchanged

#### Scenario: Quantity below one is rejected
- **WHEN** a train request is made with `quantity` of zero or negative
- **THEN** the request is rejected and no order is created

#### Scenario: A quantity at the maximum is accepted
- **WHEN** a train request is made with `quantity` exactly `MAX_TRAINING_BATCH_SIZE` and the village can afford it
- **THEN** a single order is created for that quantity

### Requirement: Resource deduction rejects negative costs
`ResourcesService.deductResources` SHALL reject any negative cost entry, so that no caller can increase a village's balance through the deduction path. A zero cost remains legal (a free action is a no-op).

#### Scenario: Negative cost is refused
- **WHEN** `deductResources` is called with a negative amount for any resource
- **THEN** it throws and no resource balance is modified

### Requirement: A training order is never scheduled to finish in the past
`trainVandal` SHALL resolve already-completed orders before reading the pending queue, and SHALL compute the new order's `finishesAt` from the later of the current time and the last pending order's finish time. A newly created order SHALL always finish strictly in the future.

#### Scenario: Stale pending order does not make the new order instant
- **GIVEN** the village has one pending order whose `finishesAt` is already in the past because no client has read the queue since
- **WHEN** the player trains another unit
- **THEN** the new order's `finishesAt` is at least the training duration in the future
- **AND** the stale order has been promoted to a unit rather than left in the queue

#### Scenario: Empty queue schedules from now
- **WHEN** a player trains a unit with no pending orders
- **THEN** the order finishes one training duration from now

#### Scenario: Live queue still chains
- **GIVEN** the village has a pending order finishing in the future
- **WHEN** the player trains another unit
- **THEN** the new order finishes one training duration after the existing order

### Requirement: Units are trained only at a barrack belonging to that village
`trainVandal` SHALL verify that the supplied `buildingId` identifies a Barrack occupying a construction site of the village being trained for. A Barrack belonging to a different village SHALL be rejected.

#### Scenario: Barrack from another village is rejected
- **GIVEN** a Barrack that stands in village 2
- **WHEN** a train request names that `buildingId` with `villageId = 1`
- **THEN** the request is rejected and no order is created

#### Scenario: Non-barrack building is rejected
- **WHEN** a train request names a building in the village that is not a Barrack
- **THEN** the request is rejected and no resources are snapshotted

#### Scenario: Barrack in the village is accepted
- **WHEN** a train request names a Barrack standing in the village being trained for
- **THEN** the order is created
