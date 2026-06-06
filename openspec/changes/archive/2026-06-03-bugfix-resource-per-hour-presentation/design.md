## Context

Every new `Village` calls `ResourceProduction.withDefaults()` in its constructor, which sets all four `*PerHour` fields to `DEFAULT_ECONOMICAL_PRODUCTION_RATE` (18000). The intent was probably "give the starter village some production", but the correct model is that production starts at zero and each constructed building adds its rate. This results in a hidden +18000 offset on every resource rate that compounds: a village with one level-1 LumberMill shows `+36000/hr` wood instead of the correct `+18000/hr`.

The existing DB migration `reset-production-rates-to-zero` patched *existing* rows to zero, but only changed the column `defaultValueNumeric` — new villages created after that migration still get 18000 from the Java code.

## Goals / Non-Goals

**Goals:**
- `ResourceProduction.withDefaults()` returns zero production for all resources.
- Existing `village` rows in the database have their `*_per_hour` columns corrected to remove the 18000 offset.
- The formula `amountProduced = perHour * (seconds / 3600.0)` and the `DEFAULT_ECONOMICAL_PRODUCTION_RATE` constant are unchanged.

**Non-Goals:**
- Changing how buildings contribute their production rate on construct or upgrade.
- Altering the resource accumulation formula.
- Any frontend changes — the display is correct, the raw values are wrong.

## Decisions

### 1. Zero out `withDefaults()`, keep the factory method name

`withDefaults()` is called in the `Village` constructor. Changing it to return all-zeros is the minimum-scope fix. The method name stays — "default" now correctly means "no buildings yet = no production".

**Alternative considered:** Remove `withDefaults()` and use the no-arg constructor directly in `Village`. Rejected — the factory method name is self-documenting and other code may call it.

### 2. Liquibase correction: subtract 18000, floor at 0

For each `*_per_hour` column, apply:
```sql
UPDATE village SET
  wood_per_hour   = GREATEST(wood_per_hour   - 18000, 0),
  bricks_per_hour = GREATEST(bricks_per_hour - 18000, 0),
  iron_per_hour   = GREATEST(iron_per_hour   - 18000, 0),
  food_per_hour   = GREATEST(food_per_hour   - 18000, 0);
```

This removes exactly the erroneous offset. `GREATEST(..., 0)` protects against going negative if any row was already at 0 (e.g. previously zeroed by the old migration and then had buildings added from zero — those rows would show the correct building-only rate and the migration shouldn't touch them negatively).

**Risk:** If a village was created after the old `reset-production-rates-to-zero` migration and has exactly the building contributions (i.e., `woodPerHour = buildingContributions`), subtracting 18000 would be wrong — it would subtract from legitimate building production. However, because new villages call `withDefaults()` which adds 18000 at creation time, the per-hour values in the DB are always `18000 (offset) + building contributions`. The subtraction is therefore safe for all rows created via the current code path.

**Alternative considered:** Recompute production from scratch by summing active building levels. Rejected — requires joining village → construction_site → building, which is more complex and error-prone than a simple arithmetic correction.

### 3. No frontend changes needed

`ResourceStorageResponse` returns the raw `*PerHour` values from `ResourceProduction`. After the fix, those values will be correct and the frontend display (`+{{ woodPerHour }}/hr`) will automatically show the right number.

## Risks / Trade-offs

- **One-way migration** → If the fix is rolled back without reverting the Liquibase changeset, production rates will be under-counted by 18000. Standard rollback procedure: roll back code AND revert the changeset manually.
- **Existing tests** → Tests that assert specific `perHour` values based on the 18000 default will fail and need updating. This is expected and is part of the task list.

## Open Questions

None — the root cause is confirmed and the fix is straightforward.
