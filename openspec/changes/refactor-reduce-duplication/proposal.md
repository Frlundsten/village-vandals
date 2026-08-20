## Why

With `fix-backend-defects` and `fix-frontend-defects` landed, the codebase has 260 passing tests but still carries the duplication that let several of those defects exist in the first place. Three separate resolutions of "which village am I looking at" caused two of the frontend bugs. Four separate affordability implementations, one of which defaults the opposite way to the other three, decide which buttons the player can press. Two `BuildingControllerTest` classes with the same name test the same endpoints.

This change is **behaviour-preserving**. It adds no capability and changes no requirement — the full suite is the acceptance criterion, and it must stay green without any test's *assertions* being weakened.

## What Changes

### Backend — collapse duplicated building definitions
- `productionPerHour()` is implemented identically (`DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel()`) in `Farm`, `Forge`, `Brickyard`, and `LumberMill`. It moves to `AbstractEconomicBuilding`; subclasses declare only the resource they produce.
- All five buildings build their upgrade cost map by repeating the same four-entry `Map.of` with `nextLevel()` multipliers. A protected `upgradeCost(wood, bricks, food, iron)` helper on `Building` does the multiplication once; subclasses declare only their four multipliers.
- `Building.getUpgradeCost()` builds a `Map.of` from four lookups that would throw `NullPointerException` if any subclass ever omitted a resource. It now builds the map defensively.

### Backend — delete dead code
`EconomicBuilding` (deprecated, implemented by nothing), `UserRegistrationDTO` (no local registration since Keycloak-only auth), `JwtService.generateToken(UserDetails)` (no callers), and `VillageController` (a stub returning the string `"Your village"`, which no client calls).

### Backend — correctness-adjacent tidying
- `UserRepository extends JpaRepository<User, Long>` while `User`'s `@Id` is a `UUID`. Nothing calls `findById` today, so it has never failed — the first caller would. Corrected to `JpaRepository<User, UUID>`.
- `User.equals`/`hashCode` include the lazy `villages` collection, so comparing or hashing a `User` outside a session can throw `LazyInitializationException` and inside one silently loads every village. Narrowed to the identifier.
- `ConstructionSiteRepository.findByIdAndVillageId` does not query by id — its JPQL matches `villageSiteId`. Renamed `findByVillageSiteIdAndVillageId`, which is what every caller actually means.
- `ResourceStorage.get` unboxes `Map::get` directly and would throw `NullPointerException` for an absent resource. Uses a default of zero.
- `UnitService.getRoster` scans the whole unit list again for every distinct unit type to find a representative. Single pass.
- `UnitController` still wraps failures in its own try/catch, returning a bare string body, while `BuildingController` now leaves that to `GlobalExceptionHandler`. Made consistent.
- The two `BuildingControllerTest` classes are given distinct names and one package: `BuildingControllerTest` (standalone MockMvc) and `BuildingEndpointSliceTest` (`@WebMvcTest`, which is where the auto-discovery of the exception handler is verified).

### Frontend — one resolution for the current village
`util/villageId.js` exports `resolveVillageId(routeValue)`. Home's `safeVillageId`, `VillageNew`'s route/localStorage fallback, and the bare `Number(localStorage.getItem('villageId'))` in `BuildingsPanel` and `ArmyView` all route through it. Three different fallback semantics — one of which broke on `0` — become one.

### Frontend — one affordability rule
`util/affordability.js` exports `canAfford(cost, resources)`. `BuildingsPanel`, `BuildingUpgradeCard`, and `BuildingPresentationCard` use it. `BuildingPresentationCard` previously returned `true` when data was missing while the other three returned `false`; the safe default (`false`) wins.

### Frontend — drop the redundant `currentResources` prop
`VillageNew` passes the Pinia resource store *itself* as a prop to `BuildingMenu` and `BuildingUpgradeCard`, which forward it to `BuildingPresentationCard`. The store is a singleton every component can import, and the indirection produced a real inconsistency: `BuildingUpgradeCard` reads the prop for upgrade costs and the imported store for training costs. The prop is removed and every component reads the store.

## Capabilities

### New Capabilities
<!-- None. This change is behaviour-preserving; no requirement is added or altered. -->

### Modified Capabilities
<!-- None. The existing specs continue to describe the behaviour exactly as before. -->

## Impact

- **Backend deleted:** `EconomicBuilding.java`, `UserRegistrationDTO.java`, `VillageController.java`, `JwtService.generateToken(UserDetails)`
- **Backend modified:** `Building`, `AbstractEconomicBuilding`, `Farm`, `Forge`, `Brickyard`, `LumberMill`, `Barrack`, `UserRepository`, `User`, `ConstructionSiteRepository`, `BuildingService`, `ResourceStorage`, `UnitService`, `UnitController`
- **Frontend new:** `util/villageId.js`, `util/affordability.js` (with tests)
- **Frontend modified:** `Home.vue`, `VillageNew.vue`, `BuildingsPanel.vue`, `BuildingMenu.vue`, `BuildingUpgradeCard.vue`, `BuildingPresentationCard.vue`, `ArmyView.vue`
- **No API change**, no schema change, no new dependency.

## Out of scope

- Splitting `VillageNew.vue` into composables. It is the largest single file and the right eventual move, but it touches PixiJS lifecycle code that this cycle has already changed once; it deserves its own change with its own tests.
- Generating `frontend/src/util/gameConfig.js` from `GameDefaults.java`.
- Introducing a toast/notification system to replace the per-surface inline errors.
