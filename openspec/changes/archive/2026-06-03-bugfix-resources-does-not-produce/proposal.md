## Why

The previous fix (`bugfix-resource-per-hour-presentation`) correctly removed the phantom +18000 offset from `ResourceProduction.withDefaults()`, but set the base to zero. This means a brand-new village produces nothing at all — players accumulate no resources without buildings, but building the first building requires resources they can't earn. The game is unplayable from a fresh start.

The fix is to introduce a small, named "starter" base production rate that all villages have regardless of buildings, and to add it back to existing village rows via a Liquibase migration. Building contributions (which sum correctly per building and level) continue to work unchanged on top of this base.

## What Changes

- Add `DEFAULT_BASE_PRODUCTION_RATE` constant to `GameDefaults` (value: `3600` — one unit per second for each resource, one-fifth of a single building's rate).
- Change `ResourceProduction.withDefaults()` to return `new ResourceProduction(DEFAULT_BASE_PRODUCTION_RATE)` instead of `0`.
- Add a Liquibase changeset that adds `DEFAULT_BASE_PRODUCTION_RATE` (3600) to each `*_per_hour` column in the `village` table for all existing rows (since those rows currently hold correct building-only values and need the base added back).
- The per-hour display on the resource bar already shows the raw `*PerHour` value from the village — it will automatically reflect base + building contributions with no frontend changes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `resource-production`: Village base production rate is non-zero; displayed `/hr` values include base + building contributions.

## Impact

- **`GameDefaults.java`**: new `DEFAULT_BASE_PRODUCTION_RATE = 3600` constant.
- **`ResourceProduction.java`**: `withDefaults()` returns `DEFAULT_BASE_PRODUCTION_RATE` instead of `0`.
- **Database**: Liquibase changeset adds `3600` to all `*_per_hour` columns.
- **Tests**: `VillageTest` and `ResourceProductionTest` assertions of `isZero()` must be updated to `isEqualTo(DEFAULT_BASE_PRODUCTION_RATE)`.
- No frontend changes needed.
