## 1. Failing Tests First (TDD)

- [x] 1.1 Write a failing test asserting `ResourceProduction.withDefaults()` returns all-zero rates
- [x] 1.2 Write a failing test asserting a new `Village`'s production rates are all zero
- [x] 1.3 Write a failing test asserting a village with one level-1 LumberMill shows exactly `18000` wood/hr and `0` for other resources

## 2. Fix the Root Cause

- [x] 2.1 Change `ResourceProduction.withDefaults()` to return `new ResourceProduction(0)` instead of `new ResourceProduction(DEFAULT_ECONOMICAL_PRODUCTION_RATE)`

## 3. Database Migration

- [x] 3.1 Add a Liquibase changeset in `changelog-master.yaml` that subtracts 18000 from each `*_per_hour` column in `village`, floored at 0 using `GREATEST`

## 4. Fix Broken Existing Tests

- [x] 4.1 Update any existing tests that assert production rates based on the old 18000 default (e.g. `VillageTest`, `ResourcesServiceTest`, `BuildingControllerTest` assertions)

## 5. Verify

- [x] 5.1 Run `mvn test` — all tests green
