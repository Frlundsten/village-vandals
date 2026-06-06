## MODIFIED Requirements

### Requirement: Village production starts at zero
A newly created village SHALL have zero production rate for all resources. Production rates SHALL only increase when economic buildings are constructed or upgraded. The `ResourceProduction.withDefaults()` factory method SHALL return an instance with all rates set to zero.

#### Scenario: New village has zero production rates
- **WHEN** a new village is created
- **THEN** `woodPerHour`, `bricksPerHour`, `ironPerHour`, and `foodPerHour` SHALL all be `0`

#### Scenario: Building a level-1 LumberMill adds exactly one building's worth of production
- **WHEN** a level-1 LumberMill is constructed in a village that previously had no buildings
- **THEN** `woodPerHour` SHALL equal `DEFAULT_ECONOMICAL_PRODUCTION_RATE * 1` (18000)

#### Scenario: Production rate displayed matches building contributions only
- **WHEN** the resource endpoint is called for a village with one level-1 economic building
- **THEN** the `*PerHour` value for the produced resource SHALL equal `DEFAULT_ECONOMICAL_PRODUCTION_RATE` (18000)
- **THEN** the `*PerHour` values for all other resources SHALL equal `0`

## ADDED Requirements

### Requirement: Existing village production rates are corrected by migration
The database SHALL contain a Liquibase changeset that subtracts the erroneous 18000 offset from all `*_per_hour` columns in the `village` table, floored at zero, to remove the phantom starter-village production that was previously baked in by `withDefaults()`.

#### Scenario: Migration corrects an over-inflated production rate
- **WHEN** the Liquibase migration runs on a village row where `wood_per_hour = 36000` (18000 offset + one level-1 LumberMill)
- **THEN** `wood_per_hour` SHALL become `18000` after the migration
