## ADDED Requirements

### Requirement: Each building type has a differentiated upgrade cost
Each building type SHALL return a unique upgrade cost that reflects its real-world resource dependencies ("Pay what you lack"). The base cost per resource is `UPGRADE_BASE_COST` (80) for standard resources and `UPGRADE_HEAVY_COST` (160) for the resource the building most depends on. Both base amounts are multiplied by the target level (`currentLevel + 1`).

A building SHALL pay the base cost (1×) for the resource it produces and `UPGRADE_HEAVY_COST` (2×) for the resource(s) it most depends on.

Per-type cost table (base amounts before level multiplier):

| Building   | Wood | Bricks | Food | Iron |
|------------|------|--------|------|------|
| LumberMill | 80   | 160    | 80   | 80   |
| Brickyard  | 160  | 80     | 80   | 80   |
| Forge      | 160  | 160    | 80   | 80   |
| Farm       | 160  | 80     | 80   | 80   |
| Barrack    | 80   | 160    | 80   | 160  |

#### Scenario: LumberMill upgrade cost at level 1
- **WHEN** a LumberMill at level 1 returns its upgrade cost
- **THEN** the cost SHALL be Wood=160, Bricks=320, Food=160, Iron=160 (base × nextLevel=2)

#### Scenario: Brickyard upgrade cost at level 1
- **WHEN** a Brickyard at level 1 returns its upgrade cost
- **THEN** the cost SHALL be Wood=320, Bricks=160, Food=160, Iron=160

#### Scenario: Forge upgrade cost at level 1
- **WHEN** a Forge at level 1 returns its upgrade cost
- **THEN** the cost SHALL be Wood=320, Bricks=320, Food=160, Iron=160

#### Scenario: Farm upgrade cost at level 1
- **WHEN** a Farm at level 1 returns its upgrade cost
- **THEN** the cost SHALL be Wood=320, Bricks=160, Food=160, Iron=160

#### Scenario: Barrack upgrade cost at level 1
- **WHEN** a Barrack at level 1 returns its upgrade cost
- **THEN** the cost SHALL be Wood=160, Bricks=320, Food=160, Iron=320

#### Scenario: Upgrade cost scales linearly with target level
- **WHEN** a LumberMill at level 2 returns its upgrade cost
- **THEN** the cost SHALL be Wood=240, Bricks=480, Food=240, Iron=240 (base × nextLevel=3)

### Requirement: `getUpgradeCost` and `getUpgradeCostAsResourceMap` return consistent data
The `getUpgradeCost()` method (returning `Map<String, Integer>`) SHALL always return amounts identical to `getUpgradeCostAsResourceMap()` (returning `Map<Resource, Integer>`), so frontend and backend use the same cost values.

#### Scenario: String and Resource map agree for any building type
- **WHEN** any building's `getUpgradeCost()` and `getUpgradeCostAsResourceMap()` are called at the same level
- **THEN** the wood/bricks/food/iron values SHALL be identical between the two maps
