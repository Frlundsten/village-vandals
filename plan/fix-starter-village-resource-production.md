# Spec: Fix resource production for new villages

## Requirements
When a user registers and their starter village is created, all four resources (food, wood, bricks, iron) must begin accumulating immediately — no buildings required. The village must have a non-zero base production rate from the moment it is created.

## Root Cause
`Village` initialises `ResourceProduction` with all rates at 0. Production only becomes non-zero when a building is explicitly constructed (`BuildingService.constructBuilding` → `ResourcesService.updateProduction`). A brand-new village with no buildings therefore produces nothing.

## Acceptance Criteria
- [ ] A freshly registered village has `foodPerHour`, `woodPerHour`, `bricksPerHour`, and `ironPerHour` all equal to `DEFAULT_ECONOMICAL_PRODUCTION_RATE` (18 000) from the moment of creation.
- [ ] Calling `/resources/refresh` on a new village after any elapsed time returns values above 100.
- [ ] Existing villages are unaffected (their production rates are already set by their buildings).

## Architecture Impact
- `ResourceProduction` — add a constructor that sets all four rates to `DEFAULT_ECONOMICAL_PRODUCTION_RATE`.
- `Village` — use the new constructor when initialising the embedded `production` object.
- No schema changes, no new endpoints, no frontend changes.

## Out of Scope
- Auto-placing starter buildings.
- Per-resource differentiation of starting rates.
- Changing how building construction updates production.
