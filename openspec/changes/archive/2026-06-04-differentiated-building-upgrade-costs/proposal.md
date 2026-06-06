## Why

Upgrade costs are currently a flat `100 × nextLevel` across all four resources for every building type, which treats a Farm the same as a Forge and creates no strategic texture. The "Pay what you lack" model ties each building's upgrade cost to its real-world dependencies: buildings pay a premium for the resources they consume most and a discount on the resource they produce, rewarding players who balance their economy.

## What Changes

- **New cost constants** in `GameDefaults`: `UPGRADE_BASE_COST = 80`, `UPGRADE_HEAVY_COST = 160`.
- **Per-building upgrade costs**: each building subclass overrides `getUpgradeCostAsResourceMap()` with its own resource weights, replacing the uniform formula in `Building`.
- **`getUpgradeCost()`** (returns `Map<String, Integer>` for the frontend DTO) is refactored to delegate to `getUpgradeCostAsResourceMap()` so the two maps stay in sync.

Cost table — base amounts before level multiplier (`× nextLevel()`):

| Building   | Wood | Bricks | Food | Iron | Heavy resource (why)            |
|------------|------|--------|------|------|---------------------------------|
| LumberMill | 80   | 160    | 80   | 80   | Bricks — scaffolding to expand  |
| Brickyard  | 160  | 80     | 80   | 80   | Wood — kiln fuel                |
| Forge      | 160  | 160    | 80   | 80   | Wood+Bricks — charcoal & walls  |
| Farm       | 160  | 80     | 80   | 80   | Wood — fencing, barn expansion, tool handles |
| Barrack    | 80   | 160    | 80   | 160  | Bricks+Iron — walls & weapons   |

Farm pays a Wood premium (fences, barns, tool handles) — wood is available early from a LumberMill, so Farm upgrades are accessible without needing a Forge first.

## Capabilities

### New Capabilities
- `building-upgrade-costs`: Per-building-type upgrade cost tables following the "Pay what you lack" rule, scaling linearly with target level.

### Modified Capabilities
- `building-overview-panel`: The upgrade button affordability check is unchanged in mechanics but will now reflect the new per-building cost amounts. No spec-level requirement changes.

## Impact

- `GameDefaults.java` — two new constants
- `Building.java` — `getUpgradeCostAsResourceMap()` becomes per-building override; `getUpgradeCost()` delegates to it
- `Farm.java`, `LumberMill.java`, `Brickyard.java`, `Forge.java`, `Barrack.java` — each overrides `getUpgradeCostAsResourceMap()`
- Backend tests for `BuildingService.upgradeBuilding` and per-building cost assertions
- Frontend `BuildingUpgradeCard.vue` — no code change needed; the API response already returns `upgradeCost`
