## Context

All five building types currently inherit `getUpgradeCostAsResourceMap()` from `Building`, which returns a flat `{WOOD: 100×n, BRICKS: 100×n, FOOD: 100×n, IRON: 100×n}` where `n = nextLevel()`. There is no per-type differentiation. A second method, `getUpgradeCost()`, returns the same data as `Map<String, Integer>` for the frontend DTO — the two methods are independent copies of the same logic, creating a drift risk.

## Goals / Non-Goals

**Goals:**
- Each building type returns its own differentiated upgrade cost from `getUpgradeCostAsResourceMap()`
- `getUpgradeCost()` always reflects the same data as `getUpgradeCostAsResourceMap()` — no duplication
- Cost constants live in `GameDefaults` (not magic numbers in subclasses)
- Linear scaling with target level is preserved (`base × nextLevel()`)

**Non-Goals:**
- Non-linear (exponential) scaling curves — out of scope for this change
- Frontend UI changes — affordability display already works via the existing API response
- Construction cost changes — only upgrade costs are modified

## Decisions

### Decision 1: Override `getUpgradeCostAsResourceMap()` per subclass; make `getUpgradeCost()` delegate

**Why:** The two existing methods carry the same data and will drift whenever costs change. Making `getUpgradeCost()` a non-overridable adapter that converts `getUpgradeCostAsResourceMap()` to `Map<String, Integer>` eliminates the duplication permanently.

`getUpgradeCostAsResourceMap()` in `Building` stays as the override point (can be made abstract or left with a default that throws). Each subclass provides its own implementation.

**Alternative considered:** Keep both methods overridable per-subclass. Rejected — it guarantees future drift.

### Decision 2: Constants in `GameDefaults` (`UPGRADE_BASE_COST`, `UPGRADE_HEAVY_COST`)

**Why:** Named constants make the 1× / 2× intent explicit and allow future tuning in one place. Direct literals (80, 160) in each subclass would scatter the magic numbers.

### Decision 3: Scaling formula stays `base × nextLevel()`

**Why:** Preserves the existing linear growth behaviour that the rest of the game (resource production, storage) is already balanced around. Non-linear scaling is a separate design concern.

**Alternative considered:** `base × level²`. Rejected for this change — too much rebalancing surface area.

### Decision 4: No database migration needed

**Why:** Upgrade costs are computed at runtime from the building entity's `level` field. They are never stored. Changing the formula has immediate effect without a Liquibase changeset.

## Risks / Trade-offs

- **Forge and Barrack cost more (~20%)**: Intended as a "strategic gate" premium. Could feel punishing if players need both. Mitigation: monitor player progression; adjust `UPGRADE_HEAVY_COST` if needed.

## Migration Plan

1. Add two constants to `GameDefaults`.
2. Refactor `Building.getUpgradeCost()` to delegate to `getUpgradeCostAsResourceMap()`.
3. Override `getUpgradeCostAsResourceMap()` in each of the five subclasses.
4. Run existing tests; add new tests per-building.
5. Deploy — no database migration required. No API contract change (field names unchanged, only values differ).

**Rollback:** Revert the Java changes. No data to migrate.

## Open Questions

- None. Cost table is agreed; Farm iron tension is flagged as intentional.
