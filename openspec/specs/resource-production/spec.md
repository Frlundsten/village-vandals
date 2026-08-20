# Spec: Resource Production

## Purpose

Defines how village resource production rates are initialised, updated, and persisted. Covers the relationship between economic buildings and per-hour production values stored on the village.

---

## Requirements

### Requirement: Village production starts at the base rate
A newly created village SHALL have a non-zero base production rate (`DEFAULT_BASE_PRODUCTION_RATE`) for all four resources. This base rate represents manual village labor before any economic buildings are constructed. The `ResourceProduction.withDefaults()` factory method SHALL return an instance with all rates set to `DEFAULT_BASE_PRODUCTION_RATE`.

#### Scenario: New village has base production rate for all resources
- **WHEN** a new village is created
- **THEN** `woodPerHour`, `bricksPerHour`, `ironPerHour`, and `foodPerHour` SHALL each equal `DEFAULT_BASE_PRODUCTION_RATE`

#### Scenario: Building a level-1 LumberMill adds to the base rate
- **WHEN** a level-1 LumberMill is constructed in a village that previously had no buildings
- **THEN** `woodPerHour` SHALL equal `DEFAULT_BASE_PRODUCTION_RATE + DEFAULT_ECONOMICAL_PRODUCTION_RATE`

#### Scenario: Two buildings of the same type sum their contributions on top of base
- **WHEN** two level-1 Farm buildings have been constructed
- **THEN** `foodPerHour` SHALL equal `DEFAULT_BASE_PRODUCTION_RATE + (DEFAULT_ECONOMICAL_PRODUCTION_RATE * 2)`

#### Scenario: Upgrading one of two buildings reflects only that building's level increase
- **WHEN** one Farm is upgraded from level 1 to level 2 while another Farm stays at level 1
- **THEN** `foodPerHour` SHALL equal `DEFAULT_BASE_PRODUCTION_RATE + DEFAULT_ECONOMICAL_PRODUCTION_RATE * 1 + DEFAULT_ECONOMICAL_PRODUCTION_RATE * 2`

---

### Requirement: Existing village production rates are corrected by migration
The database SHALL contain a Liquibase changeset that subtracts the erroneous 18000 offset from all `*_per_hour` columns in the `village` table, floored at zero, to remove the phantom starter-village production that was previously baked in by `withDefaults()`.

#### Scenario: Migration corrects an over-inflated production rate
- **WHEN** the Liquibase migration runs on a village row where `wood_per_hour = 36000` (18000 offset + one level-1 LumberMill)
- **THEN** `wood_per_hour` SHALL become `18000` after the migration

---

### Requirement: Existing village production rates include the base rate via migration
A Liquibase changeset SHALL add `DEFAULT_BASE_PRODUCTION_RATE` (3600) to each `*_per_hour` column for all existing village rows, restoring the base that was absent after the previous migration.

#### Scenario: Migration adds base rate to an existing row
- **WHEN** the Liquibase migration runs on a village row where `wood_per_hour = 18000` (one level-1 LumberMill, no base)
- **THEN** `wood_per_hour` SHALL become `21600` (18000 + 3600) after the migration

### Requirement: Snapshotting resources credits and consumes exactly the same elapsed time
When resources are snapshotted, the system SHALL credit production for the whole seconds elapsed since `lastUpdate` and SHALL advance `lastUpdate` by exactly those seconds — not to the current instant. The sub-second remainder SHALL carry forward to the next snapshot, so repeated snapshots never discard production time.

#### Scenario: Two snapshots inside the same second still credit the full elapsed time
- **GIVEN** a village whose `lastUpdate` was 1.5 seconds ago
- **WHEN** resources are snapshotted twice in quick succession, the second snapshot 0.2 s after the first
- **THEN** the total credited production equals one whole second at the first snapshot
- **AND** the remaining 0.7 s is still pending, so a later snapshot credits it rather than losing it

#### Scenario: A sub-second snapshot leaves the clock untouched
- **GIVEN** a village whose `lastUpdate` was 0.4 seconds ago
- **WHEN** resources are snapshotted
- **THEN** no production is credited
- **AND** `lastUpdate` is unchanged, so the 0.4 s is not lost

#### Scenario: A whole-second snapshot advances the clock by that many seconds
- **GIVEN** a village whose `lastUpdate` was exactly 10 seconds ago
- **WHEN** resources are snapshotted
- **THEN** production for 10 seconds is credited
- **AND** `lastUpdate` has advanced by 10 seconds

### Requirement: Elapsed time is never negative
If `lastUpdate` is in the future relative to the current instant (clock adjustment, clock skew, or a bad write), the system SHALL treat the elapsed time as zero. Production SHALL never be subtracted from a village's stored resources.

#### Scenario: A future lastUpdate does not remove resources
- **GIVEN** a village whose `lastUpdate` is one hour in the future
- **WHEN** the current resource amounts are calculated
- **THEN** the amounts equal the stored amounts, unchanged
- **AND** no negative production is applied

### Requirement: Production accumulation cannot overflow into a negative balance
Accumulated production SHALL be computed in 64-bit arithmetic and the resulting stored amount SHALL saturate at `Integer.MAX_VALUE` rather than wrapping. A village left idle for a long time at a high production rate SHALL NOT end up with a negative resource balance.

#### Scenario: A very long idle period saturates instead of wrapping
- **GIVEN** a village with a high production rate whose `lastUpdate` is many years in the past
- **WHEN** the current resource amounts are calculated
- **THEN** each amount is `Integer.MAX_VALUE` at most
- **AND** no amount is negative
