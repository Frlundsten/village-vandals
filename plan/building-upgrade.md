# Building Upgrade Feature Plan

## Context

Buildings exist on construction sites and have a `level` field (starts at 1). Production scales with level (`productionPerHour = baseRate * level`). The upgrade endpoint (`POST /building/upgrade`) and service method already exist, but: **resource deduction is missing from the upgrade path** (only construction deducts resources today). The frontend has no UI for upgrading an existing building — clicking a building sprite only logs to console. This plan wires together the full upgrade flow: backend resource validation + deduction, enriched DTO, and a new frontend upgrade card.

---

## Requirements

1. Clicking a tile that already has a building opens a **BuildingUpgradeCard** (not the "Available Buildings" menu).
2. The card shows: building type, icon, current level → next level, upgrade cost per resource, and current production → next production (economic buildings only).
3. The "Upgrade" button is **disabled** if the player cannot afford it (checked against fetched current resources).
4. Pressing "Upgrade" calls the backend, which validates ownership, snapshots resources, deducts the upgrade cost, increments level, and updates production delta.
5. After a successful upgrade the card closes, the on-map building sprite is refreshed (visual level change is a future enhancement; for now just reload building data), and a re-fetch updates the state.

---

## Architecture

### Backend changes

**`Building.java`** — Add `getUpgradeCostAsResourceMap()` returning `Map<Resource, Integer>` so `ResourcesService.deductResources()` (which takes `Map<Resource, Integer>`) can be called directly. `getUpgradeCost()` (returns `Map<String, Integer>` for JSON serialization) stays as-is.

**`BuildingDTO.java`** — Extend with two new fields:
- `upgradeCost: Map<String, Integer>` — from `building.getUpgradeCost()`
- `productionPerHour: Integer` — from `EconomicProduction.productionPerHour()` if applicable, else `null`

**`BuildingService.upgradeBuilding()`** — Insert resource deduction before incrementing level:
```
snapshotCurrentResources(villageId)
deductResources(villageId, building.getUpgradeCostAsResourceMap())
// then existing: upgrade() + save + updateProductionDelta
```
`deductResources` already throws `IllegalArgumentException` on insufficient funds — the controller already returns 400 on any exception.

### Frontend changes

**`buildings.js`** — Fix `upgradeBuilding(villageId, constructionSiteId)`: currently sends `buildingId` key but `UpgradeRequestDTO` expects `constructionSiteId`.

**`BuildingUpgradeCard.vue`** (new component) — Modal card with:
- Props: `building` (BuildingDTO), `villageId` (Number), `currentResources` (Object)
- Computed: `canAfford` (compares each cost entry vs `currentResources`)
- Emits: `upgrade` (triggers API call in parent), `close`
- Displays: icon, type name, level badge (`n → n+1`), cost grid, production info row, Upgrade + Close buttons

**`VillageNew.vue`** — Key changes:
1. Lift `existingBuildings` out of `onMounted` into a module-level reactive `ref` so tile/sprite handlers can read it.
2. Modify `addBuildingSprite`'s `pointerup` handler: instead of `console.log`, set `currentBuilding.value` to the matching entry from `existingBuildings` and set `showUpgradeCard.value = true`.
3. In `addSpriteTileEvent`'s `pointerup` handler: skip showing `BuildingMenu` if `existingBuildings` already has an entry for this `constructionSiteId` (the building sprite's own handler takes over).
4. Add `currentResources` fetched via `refreshStorage(villageId)` when the upgrade card opens.
5. Add `handleUpgrade(constructionSiteId)` async function: calls `upgradeBuilding(villageId, constructionSiteId)`, re-fetches buildings (`fetchBuildings`), closes card.
6. Add `<BuildingUpgradeCard>` to template, conditionally shown via `showUpgradeCard`.

---

## Critical Files

| File | Change |
|---|---|
| `src/main/java/…/building/buildings/Building.java` | Add `getUpgradeCostAsResourceMap()` |
| `src/main/java/…/building/dto/BuildingDTO.java` | Add `upgradeCost`, `productionPerHour` fields |
| `src/main/java/…/building/BuildingService.java` | Add snapshot + deduct before upgrade |
| `src/test/java/…/building/BuildingControllerTest.java` | Add upgrade endpoint tests |
| `frontend/src/util/api/buildings.js` | Fix `upgradeBuilding` body key |
| `frontend/src/components/BuildingUpgradeCard.vue` | New component (create) |
| `frontend/src/components/VillageNew.vue` | Reactive buildings, tile/sprite click logic, upgrade card wiring |

---

## Reuse

- `resourcesService.snapshotCurrentResources(villageId)` — already used in `constructBuilding`; same call pattern for upgrade
- `resourcesService.deductResources(villageId, cost)` — already used in `constructBuilding`; validates all then deducts atomically
- `resourcesService.updateProductionDelta(eco, villageId, delta)` — already in `upgradeBuilding`; no change needed
- `Building.getUpgradeCost()` — already serialised to `Map<String, Integer>`; reused in DTO
- `BuildingPresentationCard.vue` — existing cost-grid styling; `BuildingUpgradeCard` adopts the same resource-icon pattern
- `refreshStorage(villageId)` from `resources.js` — used to fetch current resource snapshot for affordability check

---

## Task Decomposition

### Step 1 — Backend: `Building.getUpgradeCostAsResourceMap()`
Add the method to `Building.java`. Returns the same amounts as `getUpgradeCost()` but keyed by `Resource` enum.

### Step 2 — Backend: Enrich `BuildingDTO`
Add `upgradeCost` (`Map<String, Integer>`) and `productionPerHour` (`Integer`, nullable) to the record and update `fromEntity()`.

### Step 3 — Backend: Resource deduction in `upgradeBuilding()`
In `BuildingService.upgradeBuilding()`, call `snapshotCurrentResources` then `deductResources` using the result of `getUpgradeCostAsResourceMap()` before the existing upgrade logic.

### Step 4 — Backend: Tests
In `BuildingControllerTest.java` add:
- `upgradeBuilding_success_returns200WithDTO()`
- `upgradeBuilding_insufficientResources_returns400()`
- `upgradeBuilding_noBuilding_returns400()`

### Step 5 — Frontend: Fix `upgradeBuilding` API call
In `buildings.js`, change body `{ villageId, buildingId }` → `{ villageId, constructionSiteId }`.

### Step 6 — Frontend: `BuildingUpgradeCard.vue`
Create new modal component.

### Step 7 — Frontend: Wire `VillageNew.vue`
Reactive buildings map, building sprite click → upgrade card, tile click guard, resource fetch, handleUpgrade, template addition.

---

## Verification

1. **Backend unit tests**: `mvn test -Dtest=BuildingControllerTest` — all upgrade tests green.
2. **Manual upgrade happy path**: Start Docker (`docker compose up`), log in, construct a building, click it → upgrade card appears, click Upgrade → building level increments to 2, village production increases by base rate.
3. **Insufficient resources**: Drain resources via repeated construction, then attempt upgrade → Upgrade button disabled (or backend returns 400).
4. **Empty site**: Click empty construction site → "Available Buildings" menu still appears (no regression).
