## ADDED Requirements

### Requirement: Upgrading a single economic building increases the village production rate
When an economic building is upgraded, the village's production rate for the resource that building produces SHALL increase by exactly `DEFAULT_ECONOMICAL_PRODUCTION_RATE`. This update SHALL persist to the database within the same transaction as the level increment, and SHALL be reflected in the next call to `GET /resources/refresh`.

#### Scenario: Upgrading a Farm from level 1 to level 2 increases food production
- **WHEN** a village has one Farm at level 1 and `upgradeBuilding` is called for that Farm
- **THEN** `foodPerHour` SHALL increase by `DEFAULT_ECONOMICAL_PRODUCTION_RATE`
- **THEN** `woodPerHour`, `bricksPerHour`, and `ironPerHour` SHALL be unchanged

#### Scenario: Production increases stack across multiple upgrades
- **WHEN** a Farm is upgraded from level 1 to level 2, then again from level 2 to level 3
- **THEN** `foodPerHour` SHALL have increased by `DEFAULT_ECONOMICAL_PRODUCTION_RATE * 2` relative to after construction

#### Scenario: Upgrading a non-economic building (Barrack) does not change any production rate
- **WHEN** a Barrack is upgraded
- **THEN** `foodPerHour`, `woodPerHour`, `bricksPerHour`, and `ironPerHour` SHALL all be unchanged
