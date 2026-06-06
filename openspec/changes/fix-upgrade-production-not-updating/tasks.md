## 1. Failing Test First

- [x] 1.1 Write a failing test in `BuildingServiceTest` (or a new `BuildingUpgradeProductionTest`): mock a `ConstructionSite` whose `getBuilding()` returns a real `Farm` instance at level 1, call `upgradeBuilding`, and assert that `updateProductionDelta` was called with a non-zero delta (or assert the resulting `foodPerHour` increased)
- [x] 1.2 Confirm the test fails before the fix is applied — `mvn test -Dtest=BuildingUpgradeProductionTest`

## 2. Fix

- [x] 2.1 Change `ConstructionSite.building` from `FetchType.LAZY` to `FetchType.EAGER` — Hibernate loads the real entity directly, no proxy created, instanceof checks work
- [x] 2.2 Confirm no import conflicts — FetchType is standard JPA

## 3. Tests Pass

- [x] 3.1 Run the new test — it should pass now: `mvn test -Dtest=BuildingUpgradeProductionTest`
- [x] 3.2 Run the full test suite to confirm no regressions: `mvn test`

## 4. Verify

- [ ] 4.1 Start the app and upgrade a Farm — confirm the food r/h in the resource bar increases after each upgrade
