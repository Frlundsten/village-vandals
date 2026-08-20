## Context

The backend is a small Spring Boot 3.5 service. Village-scoped state (resources, buildings, units) is addressed by a numeric `villageId` supplied by the client. `BuildingService` already validates ownership on `upgradeBuilding` and `deleteBuilding` via a private `validateOwner(ConstructionSite, String)`, but that pattern was never extended to construction, resources, or units — those paths trust the request.

`GET /user/all` returns `(userId, username, villageId, x, y)` for every user to populate the world map, so every authenticated player already holds every other player's `villageId`. The missing checks are directly exploitable, not merely theoretical.

Two further defects are pure arithmetic/scheduling: unit training costs are computed in `int` and can overflow, and training orders are chained off a queue that is only resolved lazily on read.

## Goals / Non-Goals

**Goals:**
- One reusable ownership check, applied at every village-scoped entry point.
- Correct HTTP status codes for authorization and validation failures.
- Training orders that cannot be free, cannot be instant, and cannot be placed in a foreign barrack.
- Resource accounting that never discards or double-counts elapsed time.
- Logout that actually revokes; refresh that reports an unusable token as `401`.

**Non-Goals:**
- No frontend changes; no API shape changes.
- No new authorization model (roles, scopes, per-resource ACLs) — ownership only.
- No database integration-test harness.
- No storage caps, rate limits, or anti-automation measures.

## Decisions

**1. Ownership is checked in the service layer, with a dedicated collaborator.**
`VillageOwnershipService.requireOwner(long villageId, String username)` lives in the `village` package and throws `AccessDeniedException` when the village is not owned by the caller, `IllegalArgumentException` when it does not exist. Services take a `username` parameter and call it first.

Putting the check in the service (rather than the controller) means it is exercised by the existing Mockito service tests, cannot be bypassed by a future second caller, and keeps controllers thin. *Alternative considered:* `@PreAuthorize` with a SpEL bean call — rejected because it moves the rule into a string expression that the current test setup (`MockMvcBuilders.standaloneSetup`) does not evaluate at all, so it would silently not be covered.

**2. Ownership is resolved with an exists-projection, not by loading the owner.**
`Village.owner` is `@ManyToOne(fetch = LAZY)`, so `village.getOwner().getUsername()` needs an open session and drags in the `User` row plus, through `User.equals`/`hashCode`, its village collection. `boolean existsByIdAndOwner_Username(Long id, String username)` is a single `SELECT COUNT` with no entity materialised and no lazy-initialisation hazard.

`requireOwner` distinguishes "village does not exist" (`IllegalArgumentException` → `400`) from "village exists but is not yours" (`AccessDeniedException` → `403`) by checking existence separately. That does mean the response reveals whether a village id exists — acceptable here, since `GET /user/all` publishes exactly that already.

**3. A `@RestControllerAdvice` replaces per-endpoint status guessing.**
`AccessDeniedException` → `403`, `IllegalArgumentException` → `400`, `IllegalStateException` → `409`, anything else → `500`. Bodies reuse the existing `Message` record so the shape is unchanged. Controllers keep their current try/catch for now so this change stays reviewable; removing that duplication is a follow-up refactor.

*Note:* the standalone `MockMvcBuilders.standaloneSetup(...)` used across the controller tests does **not** pick up `@RestControllerAdvice` automatically — tests that assert status mapping must register it with `.setControllerAdvice(new GlobalExceptionHandler())`.

**4. Batch size is bounded rather than made overflow-proof.**
`VANDAL_FOOD_COST * quantity` overflows `int` for large quantities, wrapping the cost negative; `deductResources` then sees "have 100 < need -1794967296" as *false*, and `storage.set(current - negative)` credits about 1.8 billion food. Widening to `long` alone would still allow a single order for tens of millions of units, so the fix is an explicit bound: `1 <= quantity <= MAX_TRAINING_BATCH_SIZE` (50). `deductResources` additionally rejects any non-positive cost entry, so no future caller can re-open the same hole from a different direction. *Alternative considered:* clamping instead of rejecting — rejected because silently training fewer units than requested is worse than an error.

**5. The training chain base is the later of `now` and the last pending order's finish time.**
Orders are promoted to units only when someone reads the queue (`resolveCompletedOrders` runs on `GET /unit` and `GET /unit/training`). A player who trains, closes the client, and returns has a pending order whose `finishesAt` is in the past; chaining off it produces a new order that is already complete. Two changes together fix this: `trainVandal` calls `resolveCompletedOrders(villageId)` before reading the queue (so genuinely finished orders leave the queue), and the chain base is clamped to `now` (so anything that survives cannot pull the new order backwards).

**6. `lastUpdate` advances by the seconds actually credited.**
Today `refreshAndPersist` computes `Duration.between(lastUpdate, now).getSeconds()` — truncating — and then sets `lastUpdate = now`, throwing the sub-second remainder away. Every construct/upgrade/train/demolish snapshots resources, so back-to-back actions inside one second credit nothing while still consuming the time.

The fix is `lastUpdate = lastUpdate.plusSeconds(creditedSeconds)`. The remainder carries into the next call. This is exact for the current rate scheme because every rate is a multiple of 3600/hour (`DEFAULT_BASE_PRODUCTION_RATE = 3600`, `DEFAULT_ECONOMICAL_PRODUCTION_RATE = 18000`), so a whole second always yields a whole number of units. *If a future rate is not a multiple of 3600, sub-unit remainders would again be lost* — that would call for fractional storage, which is out of scope here and noted as a risk.

**7. Production arithmetic moves to `long` with a saturating add.**
`(int)(rate * (seconds / 3600.0))` overflows for a long-idle village at a high rate (around 500k/hour reaches `Integer.MAX_VALUE` in under six months), producing a *negative* balance. Computation is done in `long` and the result saturates at `Integer.MAX_VALUE` instead of wrapping.

**8. Logout is fixed at the service boundary, and guarded by a transactional-boundary assertion.**
`deleteByUsername` is a Spring Data *derived delete query*: it selects the matching entities and calls `EntityManager.remove` on each, which requires an active transaction. Nothing in `AuthController.logout` → `RefreshTokenService.revokeByUsername` opens one (`open-in-view` supplies an `EntityManager`, not a transaction), so the call fails and no token is ever revoked. `revokeByUsername` becomes `@Transactional`.

This project has no database test infrastructure — every backend test is Mockito or standalone MockMvc, and there is no H2 dependency. A mocked `RefreshTokenRepository` cannot reproduce the failure. Rather than add an embedded-database harness inside a bug-fix change (the entity model contains an H2-reserved column name, `map_tiles.row`, so it is not a drop-in), the requirement is pinned by a test asserting that `revokeByUsername` declares a write transaction. This is a weaker test than a live-database one and is recorded as a known gap; adding an integration harness is a separate piece of work.

**9. Registration becomes atomic.**
`VillageService.starterVillage` is `@Transactional` and marks a world tile occupied, then returns an *unsaved* `Village`. `UserService.newUser` is not transactional, so `starterVillage`'s transaction commits on its own; if `userRepository.save` then fails (duplicate username race, constraint violation), the tile stays occupied forever with no village on it. Annotating `newUser` with `@Transactional` makes the tile claim and the user/village insert commit or roll back together.

## Risks / Trade-offs

- **Status-code changes could surprise the frontend.** `apiRequest` treats any non-ok response other than `401` as a thrown error, so `400`/`403`/`409` all behave as they do today. Only `401` changes routing, and the only new `401` (`/auth/refresh`) is exactly the case that *should* route to login.
- **The logout fix is not covered by a live-database test.** Mitigated by the transactional-boundary assertion and recorded above as a known gap.
- **`lastUpdate` carry-forward assumes rates are multiples of 3600/hour.** True for every current rate; documented in `ResourcesService` so a future non-multiple rate is a deliberate decision rather than a silent leak.
- **Bounding batch size at 50 is a balance decision, not just a safety one.** Chosen because a batch already multiplies both cost and duration linearly, so 50 Vandals is 2500 food / 1500 iron / 250 s — well past any reasonable single click.
