## Why

Upgrading any economic building (Farm, LumberMill, Brickyard, Forge) never increases the village's resource production rate. The building's level increments correctly and costs are deducted, but the per-hour rate shown in the resource bar stays flat no matter how many upgrades the player performs. Construction of a new building correctly increases production; only upgrades are broken.

The root cause is a Hibernate lazy-loading proxy. `ConstructionSite.building` is declared `@OneToOne(fetch = FetchType.LAZY)`. When the site is fetched for an upgrade, Hibernate returns a `Building$$HibernateProxy` — a subclass of `Building` only. The upgrade service then does `instanceof AbstractEconomicBuilding` on this proxy; since the proxy class does not extend `AbstractEconomicBuilding`, the check is always `false`. The code falls into the else-branch (`building.upgrade()` — level increments via proxy delegation), returns `delta = 0`, and never calls `updateProductionDelta`. Production is silently left unchanged.

## What Changes

- **`getBuildingToUpgrade`** in `BuildingService` — call `Hibernate.unproxy()` on the result so the actual entity class (`Farm`, `LumberMill`, etc.) is returned instead of the proxy. All downstream `instanceof` checks then work correctly.
- **Failing regression test** — a new `BuildingServiceTest` case that verifies production is updated after an upgrade (currently passes `delta = 0` silently).

No schema changes. No API changes. No frontend changes.

## Capabilities

### New Capabilities
*(none)*

### Modified Capabilities
- `building-upgrade-costs`: The existing requirement that upgrading a building increases village production now actually holds. This is a bug fix, not a requirement change — the spec already states correct behaviour; the implementation was wrong.

## Impact

- `BuildingService.getBuildingToUpgrade` — one-line fix
- `BuildingServiceTest` or equivalent — new test covering the production delta after upgrade
