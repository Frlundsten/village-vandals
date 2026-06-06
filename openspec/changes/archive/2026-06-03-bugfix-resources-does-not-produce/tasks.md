## 1. Tests First (TDD)

- [x] 1.1 Update `ResourceProductionTest`: change `isZero()` assertion to `isEqualTo(DEFAULT_BASE_PRODUCTION_RATE)` — this test now fails because `withDefaults()` still returns 0
- [x] 1.2 Update `VillageTest`: change `isZero()` assertions in the two village-production tests to `isEqualTo(DEFAULT_BASE_PRODUCTION_RATE)` — these now fail

## 2. Fix the Root Cause

- [x] 2.1 Add `DEFAULT_BASE_PRODUCTION_RATE = 3600` constant to `GameDefaults.java`
- [x] 2.2 Change `ResourceProduction.withDefaults()` to return `new ResourceProduction(DEFAULT_BASE_PRODUCTION_RATE)`

## 3. Database Migration

- [x] 3.1 Add a Liquibase changeset in `changelog-master.yaml` that adds `3600` to each `*_per_hour` column in the `village` table for all existing rows

## 4. Verify

- [x] 4.1 Run `mvn test` — all tests green
