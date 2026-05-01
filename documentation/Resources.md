# Resources

Village Vandals uses four resources that drive all economic and military progression.

## Resource Types

| Resource | Symbol | Produced by  |
|----------|--------|--------------|
| Food     | 🌾     | Farm         |
| Wood     | 🌲     | Lumber Mill  |
| Bricks   | 🧱     | Brickyard    |
| Iron     | ⚒️     | Forge        |

---

## Starting Amounts

Every new village begins with **100 of each resource**.  
A brand-new village has no buildings and therefore **zero production** — resources only grow once buildings are constructed.

---

## Production

### How production works

Each economic building adds a fixed hourly production rate to its resource type when constructed.  
All four building types produce at the same base rate of **450 units per hour per level**.

| Building    | Resource produced | Rate (level 1) |
|-------------|-------------------|----------------|
| Farm        | Food              | 450 / hr       |
| Lumber Mill | Wood              | 450 / hr       |
| Brickyard   | Bricks            | 450 / hr       |
| Forge       | Iron              | 450 / hr       |

Building the same type multiple times stacks their production rates (e.g. two Farms → 900 food/hr).  
Upgrading a building increases its production rate: level 2 produces 900/hr, level 3 produces 1350/hr, etc.

### Production formula

Resources are not ticked in a background job. Instead, the amount is calculated on-demand from the last persisted state:

```
produced = productionRate × (secondsSinceLastUpdate / 3600)
currentAmount = storedAmount + produced
```

This calculation happens every time the resource endpoint is called (`GET /resources/refresh?villageId=<id>`).  
The result is then **persisted back** to the database (`last_update` timestamp + new amounts), so the next call only needs to account for the delta since the last refresh.

### Production snapshot on construction

When a building is constructed, the current resource amounts are **snapshotted** (persisted) before the new production rate is added to the village.  
This ensures the new production rate only applies going forward — it is not retroactively applied to the entire lifespan of the village.

---

## Consumption (Construction Costs)

Building construction costs resources immediately and is checked against the village's **current** (snapshotted) amounts before the build proceeds.  
If any single resource is insufficient the request is rejected with a `400 Bad Request` and no building is placed.

### Construction costs per building

| Building    | Wood | Food | Bricks | Iron |
|-------------|------|------|--------|------|
| Farm        | 60   | 40   | —      | —    |
| Lumber Mill | —    | 50   | 60     | —    |
| Brickyard   | 70   | 50   | —      | —    |
| Forge       | 80   | 40   | 60     | —    |
| Barrack     | 100  | 60   | 80     | 40   |

**Recommended build order for a new village:**
1. Farm or Brickyard — both are affordable from starting resources
2. Lumber Mill — requires some food production first
3. Forge — requires wood and bricks
4. Barrack — requires iron from a Forge first

---

## Storage

Resource amounts are stored in the `resource_storage_resources` table (a keyed collection on the `village` table).  
The last-persisted timestamp lives in `village.last_update`.

There is currently no storage capacity limit — villages can accumulate unlimited resources.

---

## Upgrade Costs

Upgrading an existing building also costs resources. The cost scales with the next level:

```
upgradeCost(level) = baseUpgradeCost × (level + 1)
```

Base upgrade cost is **100 of each resource type** for all buildings.  
Example: upgrading a level-1 Farm to level 2 costs 200 wood, 200 food, 200 bricks, 200 iron.
