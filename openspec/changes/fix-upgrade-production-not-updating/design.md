## Context

`ConstructionSite.building` is declared `@OneToOne(fetch = FetchType.LAZY)`. When `BuildingService.upgradeBuilding` loads the site via `findByIdAndVillageId` (a JPQL query), Hibernate returns the site with the building as a lazy proxy: an instance of `Building$$HibernateProxy` that extends `Building` but not any subclass.

The upgrade path then checks:

```java
// upgradeAndGetProductionDelta
if (building instanceof AbstractEconomicBuilding eco) { ... }

// upgradeBuilding
if (building instanceof EconomicProduction eco) { ... }
```

Both return `false` for the proxy. `delta = 0` is returned; `updateProductionDelta` is never called. The building's level IS correctly incremented (the proxy delegates `upgrade()` → `level++` successfully), which is why the level badge updates but the production rate does not.

Construction is unaffected because `new Farm()` / `new LumberMill()` / etc. are real instances — no proxy involved.

## Goals / Non-Goals

**Goals:**
- `upgradeBuilding` receives the actual entity class so all `instanceof` checks work
- No regression in construction, resource deduction, or any other building flow
- Fix is confined to `BuildingService` — no entity, controller, or DB changes

**Non-Goals:**
- Refactoring all `instanceof` usage in the codebase — this change fixes the immediate bug
- Changing the fetch strategy globally — lazy loading is acceptable; the problem is specific to the upgrade path

## Decisions

### Decision 1: `FetchType.EAGER` on `ConstructionSite.building`

**Why:** Changing the fetch type to EAGER means Hibernate loads the actual entity (`Farm`, `LumberMill`, etc.) as part of the site query — no proxy is created, and all `instanceof` checks work correctly. It is a one-annotation change with no Hibernate-specific API on the call site.

`ConstructionSite.building` is already needed every time a site is touched in the upgrade and construction flows. There is no scenario where loading the site without needing the building would benefit from LAZY here.

**Alternatives considered:**

| Option | Why rejected |
|--------|-------------|
| `Hibernate.unproxy()` in `getBuildingToUpgrade` | Attempts to unwrap the proxy at call time. Adds a Hibernate-specific API call; also proved unreliable in unit tests with Mockito-simulated proxies, making the bug harder to reproduce in tests. |
| `JOIN FETCH cs.building` in `findByIdAndVillageId` | Eager only for that one query; other code paths still get a proxy. More fragile. |
| Polymorphic method on `Building` (e.g., `abstract int upgradeProductionDelta()`) | Correct long-term OO design, but bigger refactor than needed for a targeted bug fix. |

### Decision 2: No change to `BuildingService`

**Why:** The fix belongs at the data-access layer (fetch type), not in the service. The service's `instanceof` checks are the correct way to branch on building type — fixing the proxy means the checks work as written.

## Risks / Trade-offs

- **Hibernate.unproxy is Hibernate-specific** — ties the service to a Hibernate API (`org.hibernate.Hibernate`). This is acceptable given the project already depends on Spring Data JPA / Hibernate and has no ORM-portability requirement.
- **Proxy initialisation cost** — `unproxy` triggers a SELECT for the building row if not already initialised. In the upgrade flow, the building is always needed, so this is not a new query in practice — it just moves when it happens (from the first field access to the explicit unproxy call).

## Migration Plan

1. Add `Hibernate.unproxy()` call in `getBuildingToUpgrade`.
2. Write a failing integration/unit test that calls `upgradeBuilding` and asserts `foodPerHour` (or equivalent) increased on the village.
3. Run all tests — green.
4. Deploy — no DB migration, no API contract change.

**Rollback:** Revert the one-line change. No data migration needed.

## Open Questions

None — root cause is confirmed, fix is straightforward.
