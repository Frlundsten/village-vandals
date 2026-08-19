## Context

`BuildingService` already supports constructing and upgrading buildings on a `ConstructionSite`. A site references its building via a `@OneToOne(cascade = CascadeType.ALL)` with the FK (`building_id`) on the `construction_site` table. Economic buildings contribute to the village's `ResourceProduction` (per-resource per-hour rates) at construction time. There is currently no path to remove a building.

This change adds demolition: a backend endpoint and service method that frees a site and reverses any production contribution. The existing patterns to follow are `constructBuilding`/`upgradeBuilding` (owner validation, resource snapshot, production delta via `ResourcesService.updateProductionDelta`).

## Goals / Non-Goals

**Goals:**
- Owner-authenticated `DELETE /building` endpoint taking `villageId` + `constructionSiteId`.
- Reverse an economic building's production contribution exactly (subtract its current `productionPerHour()`).
- Free the construction site so it can be built on again, and remove the orphaned `Building` row.
- Snapshot resources before changing production rates so accumulated resources are committed.

**Non-Goals:**
- No resource refund for demolition.
- No frontend changes.
- No demolition cost, cooldown, or confirmation flow.
- No schema migration.

## Decisions

**1. Reuse `ResourcesService.updateProductionDelta` with a negative delta.**
Construction calls `updateProduction(eco, villageId)` which adds `productionPerHour()`. To reverse it precisely, call `updateProductionDelta(eco, villageId, -eco.productionPerHour())`. This mirrors the existing add path and keeps the per-resource switch logic in one place. *Alternative considered:* a dedicated `removeProduction` method — rejected as redundant since `updateProductionDelta` already accepts an arbitrary signed delta.

**2. Snapshot before reversing production.**
Per the project's async-production rule, `snapshotCurrentResources(villageId)` is called first so resources accrued at the old (higher) rate are committed before the rate drops. Skipping this would retroactively under-credit the player.

**3. Null the FK first, then delete the orphaned `Building`.**
The FK lives on `construction_site` and there is no `orphanRemoval = true`. So merely calling `site.setBuilding(null)` and saving leaves an orphaned `building` row. The order is: `site.setBuilding(null)` → save the site (nulls `building_id`) → `buildingRepository.delete(building)`. Doing the delete first would violate the FK constraint. *Alternative considered:* adding `orphanRemoval = true` to the entity — rejected to avoid changing construct/upgrade behavior in this change.

**4. Reuse `validateOwner` and the existing "no building at site" guard.**
`deleteBuilding` resolves the site via `findByIdAndVillageId`, calls `validateOwner(site, username)`, then requires a non-null building (else throws). This matches `upgradeBuilding` exactly.

**5. Controller mirrors existing try/catch + `Message` response shape.**
`DELETE /building` returns `200 OK` with a `Message` on success and `400 Bad Request` on failure, consistent with `createBuilding`/`upgradeBuilding`.

## Risks / Trade-offs

- **Orphaned building row if delete order is wrong** → Enforced order (null FK + save, then delete) and covered by a service test asserting `buildingRepository.delete` is called.
- **Production rate could be reversed without snapshot, mis-crediting resources** → Test asserts `snapshotCurrentResources` is invoked before `updateProductionDelta`.
- **Non-economic buildings (Barrack) have no production to reverse** → `instanceof EconomicProduction` guard skips the production step, same as construction.
- **No refund means demolition is purely destructive** → Accepted; matches the proposal's explicit non-goal.
