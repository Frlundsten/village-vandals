## Context

After `bugfix-resource-per-hour-presentation`, the production chain is:

```
Village created  →  withDefaults() = 0/hr for all resources
Build LumberMill →  wood += 18000/hr  (BuildingService.updateProduction)
Upgrade to Lv2  →  wood += 18000/hr   (delta for one level)
Display          →  shows raw *PerHour from DB
```

Everything in the building-contribution chain is correct. The only missing piece is a non-zero starting value. We need a named constant (not a magic number) that represents "base village production before any buildings exist" and is clearly distinct from `DEFAULT_ECONOMICAL_PRODUCTION_RATE` (per-building rate) and `DEFAULT_PRODUCTION_PER_HOUR` (seconds/hour divisor).

## Goals / Non-Goals

**Goals:**
- Villages produce a small base rate of all four resources from the moment they are created.
- The displayed `/hr` value equals `base + sum(building contributions)`.
- Existing rows in the DB get the base rate added back via a migration.

**Non-Goals:**
- Changing how buildings contribute production (already correct).
- Separate UI display for "base" vs "building" production.
- Balancing the exact base rate value (3600 is a reasonable default; tune later).

## Decisions

### 1. `DEFAULT_BASE_PRODUCTION_RATE = 3600`

3600 resources/hour = 1 per second. This is:
- Low enough that buildings matter (one building at 18000/hr is 5× the base)
- High enough to allow progression from a fresh village

Naming it separately from `DEFAULT_PRODUCTION_PER_HOUR` (which is a time divisor, not a rate) prevents the original naming confusion from recurring.

### 2. `withDefaults()` sets all four resources to the base rate

Base production applies equally to all resources. A new village can gather all four basic materials at a slow rate without needing specific buildings first.

### 3. Migration: add base to existing rows (do not subtract first)

After the previous migration, existing rows hold `building-only values` (correct). To restore the base, simply add `DEFAULT_BASE_PRODUCTION_RATE` to each column:

```sql
UPDATE village SET
  wood_per_hour   = wood_per_hour   + 3600,
  bricks_per_hour = bricks_per_hour + 3600,
  iron_per_hour   = iron_per_hour   + 3600,
  food_per_hour   = food_per_hour   + 3600
```

No `GREATEST` guard needed here — column values after the previous migration are guaranteed ≥ 0 and adding a positive constant can't go negative.

### 4. Tests updated, not deleted

The two tests from the previous fix asserted `isZero()` to confirm the wrong default was removed. They now need to assert `isEqualTo(DEFAULT_BASE_PRODUCTION_RATE)` to confirm the correct base is present. Their intent (verifying `withDefaults()` behavior) remains valid.

## Risks / Trade-offs

- **Sequential migrations**: This change's migration adds back a value that the previous migration subtracted. If someone runs only one of the two migrations out of order, data will be wrong. Liquibase enforces run-order by changeset sequence, so this is safe in practice.
- **Value is hardcoded in migration**: The SQL uses the literal `3600`. If `DEFAULT_BASE_PRODUCTION_RATE` is later changed, old migrated rows won't auto-update — that's acceptable; changing balance values requires its own migration anyway.

## Open Questions

None.
