## 1. Ownership check — failing tests first

- [x] 1.1 Add `VillageOwnershipServiceTest`: `requireOwner` returns normally when `existsByIdAndOwner_Username` is true; throws `AccessDeniedException` when the village exists but is owned by someone else; throws `IllegalArgumentException` when the village does not exist
- [x] 1.2 Add `GlobalExceptionHandlerTest`: `AccessDeniedException` maps to `403`, `IllegalArgumentException` to `400`, `IllegalStateException` to `409`

## 2. Ownership check — implementation

- [x] 2.1 Add `boolean existsByIdAndOwner_Username(Long id, String username)` and `boolean existsById(Long id)` usage to `VillageRepository`
- [x] 2.2 Add `village/VillageOwnershipService` with `requireOwner(long villageId, String username)`
- [x] 2.3 Add `web/GlobalExceptionHandler` (`@RestControllerAdvice`) mapping `AccessDeniedException`/`IllegalArgumentException`/`IllegalStateException`, returning the existing `Message` body shape
- [x] 2.4 Run `mvn test -Dtest=VillageOwnershipServiceTest+GlobalExceptionHandlerTest` and confirm green

## 3. Unit endpoints — authorize the caller

- [x] 3.1 Add failing `UnitServiceTest` cases: `trainVandal`, `getTrainingQueue`, and `getRoster` each call `requireOwner` and abort before any snapshot/deduction/read when it throws
- [x] 3.2 Add failing `UnitControllerTest` cases: all three endpoints pass `principal.getName()` through to the service
- [x] 3.3 Add `username` parameters to `UnitService.trainVandal`/`getTrainingQueue`/`getRoster` and call `villageOwnershipService.requireOwner` first
- [x] 3.4 Add `Principal` to all three `UnitController` endpoints and pass the name through
- [x] 3.5 Run `mvn test -Dtest=UnitServiceTest+UnitControllerTest` and confirm green

## 4. Building construction and listing — authorize the caller

- [x] 4.1 Add a failing `BuildingControllerTest` case: `POST /building` from a non-owner is rejected and the service is called with the principal's name
- [x] 4.2 Add a failing test: `getAllBuildingsByVillageId` for a village the caller does not own is rejected even when the village has no buildings
- [x] 4.3 Add `username` to `BuildingService.constructBuilding` and require village ownership before deducting anything (the private `validateOwner` site-traversal was replaced by the shared `requireOwner`, so all three write paths use one rule)
- [x] 4.4 Rework `getAllBuildingsByVillageId` to call `requireOwner(villageId, username)` first, drop the `RuntimeException` wrapper, and drop the `LOG.error` on the ordinary empty case
- [x] 4.5 Add `Principal` to `BuildingController.createBuilding` and pass the name through
- [x] 4.6 Run the building test classes and confirm green

## 5. Resource endpoint — authorize the caller

- [x] 5.1 Add a failing `ResourceControllerTest` case: a non-owner request is rejected and neither `refreshAndPersist` nor `getProduction` runs
- [x] 5.2 Add `Principal` to `ResourceController` and call `requireOwner` before refreshing
- [x] 5.3 Run `mvn test -Dtest=ResourceControllerTest` and confirm green

## 6. Training queue integrity — failing tests first

- [x] 6.1 Add `UnitServiceTest` case: `quantity` above `MAX_TRAINING_BATCH_SIZE` is rejected with no snapshot, no deduction, and no saved order
- [x] 6.2 Add a case using an overflow-sized `quantity`, asserting rejection rather than a negative cost reaching `deductResources`
- [x] 6.3 Add a case: `quantity` exactly `MAX_TRAINING_BATCH_SIZE` is accepted
- [x] 6.4 Add a case: a pending order whose `finishesAt` is already in the past does not produce a new order finishing in the past
- [x] 6.5 Add a case: `trainVandal` resolves completed orders before reading the pending queue
- [x] 6.6 Add a case: a Barrack that belongs to a different village is rejected
- [x] 6.7 Add `ResourcesServiceTest` case: `deductResources` rejects a negative cost and leaves balances untouched

## 7. Training queue integrity — implementation

- [x] 7.1 Add `MAX_TRAINING_BATCH_SIZE` to `GameDefaults` and validate the range at the top of `trainVandal`
- [x] 7.2 Reject negative cost entries in `ResourcesService.deductResources`
- [x] 7.3 Call `resolveCompletedOrders(villageId)` at the start of `trainVandal`, before reading the pending queue
- [x] 7.4 Clamp the chain base in `computeNextFinishesAt` to the later of `now` and the last pending order's `finishesAt`
- [x] 7.5 Add `Optional<ConstructionSite> findByVillage_IdAndBuilding_Id(Long villageId, Long buildingId)` to `ConstructionSiteRepository` and use it in `requireBarrackInVillage(villageId, buildingId)`; `UnitService` no longer needs `BuildingRepository`
- [x] 7.6 Run `mvn test -Dtest=UnitServiceTest+ResourcesServiceTest` and confirm green

## 8. Resource production accounting — failing tests first

- [x] 8.1 Add `ResourcesServiceTest` case: a snapshot with a sub-second elapsed time credits nothing and leaves `lastUpdate` unchanged
- [x] 8.2 Add a case: a snapshot with 1.5 s elapsed credits one second and advances `lastUpdate` by exactly one second, leaving 0.5 s pending
- [x] 8.3 Add a case: a `lastUpdate` in the future credits nothing and never reduces stored amounts
- [x] 8.4 Add a case: an extremely long idle period saturates at `Integer.MAX_VALUE` instead of going negative

## 9. Resource production accounting — implementation

- [x] 9.1 Change `calculateStorage` to derive whole elapsed seconds, clamped at zero, and return them alongside the computed amounts
- [x] 9.2 In `refreshAndPersist`, advance `lastUpdate` by the credited seconds rather than setting it to `now`
- [x] 9.3 Compute production in `long` and saturate the stored amount at `Integer.MAX_VALUE`
- [x] 9.4 Document the "rates are multiples of 3600/hour" assumption on `ResourcesService`
- [x] 9.5 Run `mvn test -Dtest=ResourcesServiceTest` and confirm green

## 10. Session lifecycle

- [x] 10.1 Add a failing `RefreshTokenServiceTest` case asserting `revokeByUsername` declares a read-write transaction
- [x] 10.2 Add failing `AuthControllerTest` cases: `/auth/refresh` returns `401` for no cookies, for cookies without `refreshToken`, for an unknown token, and for an expired token; and still returns `200` with rotation for a valid token
- [x] 10.3 Annotate `RefreshTokenService.revokeByUsername` with `@Transactional`
- [x] 10.4 Rework `AuthController.refresh` to return `401` instead of throwing for missing/unknown/expired tokens
- [x] 10.5 Run `mvn test -Dtest=RefreshTokenServiceTest+AuthControllerTest` and confirm green

## 11. Account provisioning

- [x] 11.1 Add a failing `UserServiceTest` case asserting `newUser` declares a read-write transaction so a claimed tile rolls back with a failed save
- [x] 11.2 Annotate `UserService.newUser` with `@Transactional`
- [x] 11.3 Run `mvn test -Dtest=UserServiceTest` and confirm green

## 12. Verify

- [x] 12.1 Run the full backend suite (`mvn test`) and confirm green
- [x] 12.2 Re-read the diff against the delta specs and confirm every requirement has at least one covering test

## 13. Follow-ups recorded during implementation

- [ ] 13.1 The logout and registration fixes are pinned by transactional-boundary assertions, not a live-database test — the project has no embedded-database harness (`map_tiles.row` is an H2 reserved word, so H2 is not a drop-in). Adding one is separate work.
- [ ] 13.2 `src/test/java/.../controller/building/BuildingControllerTest.java` and `src/test/java/.../building/BuildingControllerTest.java` cover the same endpoints. Kept both for now (one standalone, one full MVC slice); consolidate in the refactor.
