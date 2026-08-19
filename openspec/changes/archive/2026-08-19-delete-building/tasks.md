## 1. Service: failing tests first (TDD)

- [x] 1.1 Add `BuildingService` test: owner demolishes an economic building → asserts `snapshotCurrentResources` is called, then `updateProductionDelta` with `-productionPerHour()`, the site's building is cleared, and `buildingRepository.delete(building)` is invoked
- [x] 1.2 Add test: demolishing a non-economic building (Barrack) → `updateProductionDelta` is NOT called, site still cleared and building deleted
- [x] 1.3 Add test: non-owner request → throws and no clear/delete/production change occurs
- [x] 1.4 Add test: site has no building → throws and no production/persistence change occurs
- [x] 1.5 Add test: unknown site (`findByIdAndVillageId` empty) → throws

## 2. Service: implement `deleteBuilding`

- [x] 2.1 Add `deleteBuilding(long villageId, long constructionSiteId, String username)` to `BuildingService`: resolve site via `findByIdAndVillageId`, `validateOwner`, require non-null building (reuse the existing "no building" guard pattern)
- [x] 2.2 Call `snapshotCurrentResources(villageId)`, then if building `instanceof EconomicProduction eco` call `updateProductionDelta(eco, villageId, -eco.productionPerHour())`
- [x] 2.3 Clear the site (`site.setBuilding(null)`), save the site to null the FK, then `buildingRepository.delete(building)`; annotate `@Transactional`
- [x] 2.4 Run the service tests and make them pass

## 3. Controller: endpoint + tests

- [x] 3.1 Add `BuildingController` test (MockMvc or unit): `DELETE /building` with `villageId` + `constructionSiteId` as owner → `200 OK` with success message; service failure → `400 Bad Request`
- [x] 3.2 Add `@DeleteMapping` to `BuildingController` accepting `villageId` and `constructionSiteId` (request params) + `Principal`, delegating to `buildingService.deleteBuilding(...)`, mirroring the existing try/catch + `Message` response shape
- [x] 3.3 Run the controller tests and make them pass

## 4. Verify

- [x] 4.1 Run the full backend test suite (`mvn test`) and confirm green
