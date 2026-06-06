## 1. Constants and Base Refactor

- [x] 1.1 Add `UPGRADE_BASE_COST = 80` and `UPGRADE_HEAVY_COST = 160` to `GameDefaults.java`
- [x] 1.2 Write a failing test: `BuildingUpgradeCostTest` — assert that a fresh `LumberMill` at level 1 returns `{WOOD=160, BRICKS=320, FOOD=160, IRON=160}` from `getUpgradeCostAsResourceMap()`
- [x] 1.3 Refactor `Building.getUpgradeCost()` to delegate to `getUpgradeCostAsResourceMap()`, eliminating the duplicate formula
- [x] 1.4 Remove the `@Transient` cost fields (`woodCost`, `bricksCost`, `foodCost`, `ironCost`) from `Building` — they are no longer used after delegation

## 2. Per-Building Override Implementation

- [x] 2.1 Override `getUpgradeCostAsResourceMap()` in `LumberMill` — heavy: Bricks (160), base: Wood/Food/Iron (80), scaled by `nextLevel()`
- [x] 2.2 Override `getUpgradeCostAsResourceMap()` in `Brickyard` — heavy: Wood (160), base: Bricks/Food/Iron (80)
- [x] 2.3 Override `getUpgradeCostAsResourceMap()` in `Forge` — heavy: Wood + Bricks (160 each), base: Food/Iron (80)
- [x] 2.4 Override `getUpgradeCostAsResourceMap()` in `Farm` — heavy: Wood (160), base: Bricks/Food/Iron (80)
- [x] 2.5 Override `getUpgradeCostAsResourceMap()` in `Barrack` — heavy: Bricks + Iron (160 each), base: Wood/Food (80)

## 3. Tests

- [x] 3.1 Add level-1 cost assertions for all five buildings in `BuildingUpgradeCostTest` (covers spec scenarios for each type)
- [x] 3.2 Add level-2 scaling assertion for LumberMill (covers "scales linearly with target level" scenario)
- [x] 3.3 Add consistency assertion: `getUpgradeCost()` string values match `getUpgradeCostAsResourceMap()` for each building type
- [x] 3.4 Run `mvn test` — all tests pass

## 4. Verify

- [ ] 4.1 Start the app (`docker compose up`) and upgrade a building via the UI — confirm the displayed cost and resource deduction match the new table
