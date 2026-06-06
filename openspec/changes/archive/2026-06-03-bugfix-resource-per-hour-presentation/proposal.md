## Why

`ResourceProduction.withDefaults()` initialises every new village's production rates to `DEFAULT_ECONOMICAL_PRODUCTION_RATE` (18000) for all four resources. This means a brand-new village with zero buildings already shows `+18000/hr` for wood, food, bricks, and iron — and when a first building is constructed that delta is added on top, doubling the displayed rate. Buildings are the sole intended source of production; the starter-village default should be zero.

## What Changes

- Fix `ResourceProduction.withDefaults()` to return zero rates for all resources.
- **BREAKING** (data): existing villages in the database have inflated `*_per_hour` column values due to the 18000 offset. A Liquibase changeset will correct these by subtracting 18000 from each column (clamped to 0), matching what buildings alone should contribute.

## Capabilities

### New Capabilities

### Modified Capabilities
- `keycloak-only-auth`: No change — unrelated to auth.

## Impact

- **`ResourceProduction.java`**: `withDefaults()` returns all-zero production instead of `DEFAULT_ECONOMICAL_PRODUCTION_RATE`.
- **`Village.java`**: Starter village constructor calls `withDefaults()` — will now start at zero automatically.
- **Database**: Liquibase changeset subtracts the erroneous 18000 offset from existing `*_per_hour` columns in the `village` table.
- **Display**: Users will see correct rates — `0/hr` with no buildings, `18000/hr` per level-1 economic building.
- **Tests**: Existing tests that assert specific production values need updating.
