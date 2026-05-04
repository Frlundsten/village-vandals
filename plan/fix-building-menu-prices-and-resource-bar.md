# Spec: Fix building menu prices and resource bar stale display

## Root Causes

**Bug 1 — Building menu shows 200 for all costs**
`/building/available` returns raw `Building` entities. Jackson serializes `getUpgradeCost()`
which returns `DEFAULT_STARTING_RESOURCE_COUNT (100) × nextLevel (2) = 200` for every resource
on every building. The frontend reads `building.upgradeCost` — it should show the construction
cost (`getConstructionCost()`).

**Bug 2 — Resource bar stays stale after upgrade/construction**
`Home.vue` owns a `resources` ref that is only refreshed on mount or when the user clicks the
"Village" nav link. `VillageNew.vue` lives inside `<RouterView>` and has no way to trigger
`Home.vue`'s `updateResourceUI()`. After a build or upgrade the header bar never reflects the
deduction.

## Acceptance Criteria
- [ ] Building menu cards show the correct construction cost (Farm: Wood 60 / Food 40,
      LumberMill: Food 50 / Bricks 60, etc.).
- [ ] After constructing a building the resource bar at the top immediately reflects the
      deducted amounts.
- [ ] After upgrading a building the resource bar at the top immediately reflects the
      deducted amounts.

## Architecture Impact
- **Backend**: Add `AvailableBuildingDTO(String type, Map<String, Integer> constructionCost)`.
  Change `BuildingController.getAvailableBuildings` to return it, mapping `getConstructionCost()`
  with lowercase string keys. No schema changes.
- **Frontend**:
  - Move `resources` into a new Pinia store (`useResourceStore`) so `Home.vue` and
    `VillageNew.vue` share the same reactive state.
  - `Home.vue` reads resources from the store instead of a local ref.
  - `VillageNew.vue` writes to the store after construction and upgrade.
  - `BuildingMenu.vue` / `BuildingPresentationCard.vue` read `constructionCost` instead of
    `upgradeCost`.

## Out of Scope
- Polling / live resource ticking on the header bar.
- Any backend change to upgrade cost logic.
