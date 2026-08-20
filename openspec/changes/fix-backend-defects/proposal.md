## Why

A full read of the backend turned up a family of defects that are invisible from the UI but seriously affect correctness, game balance, and account safety. Three of them let one player act on another player's village, one hands out free resources via integer overflow, one lets a player skip the training timer entirely, and one means logging out never actually revokes anything.

The unifying theme is **missing server-side authorization**: village-scoped endpoints trust the `villageId` in the request. `GET /user/all` publishes every village id to every authenticated user (it backs the world map), so nothing about these holes is theoretical — the attacker already has the ids.

## What Changes

### Authorization (`/unit/**`, `POST /building`, `/resources/refresh`)
- Add a single `VillageOwnershipService.requireOwner(villageId, username)` backed by a `VillageRepository.existsByIdAndOwner_Username` projection — one query, no lazy-loading of the owner.
- `UnitController` gains `Principal` on all three endpoints; `UnitService.trainVandal` / `getTrainingQueue` / `getRoster` take the username and validate ownership before touching anything.
- `BuildingController.createBuilding` gains `Principal`; `BuildingService.constructBuilding` validates the site's owner exactly as `upgradeBuilding` and `deleteBuilding` already do.
- `ResourceController` gains `Principal` and validates before refreshing.
- `BuildingService.getAllBuildingsByVillageId` validates ownership of the **village** up front instead of only checking the first construction site *after* it has already decided to return data, and stops wrapping every failure in a bare `RuntimeException`.
- Add a `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping `AccessDeniedException` → `403`, `IllegalArgumentException` → `400`, `IllegalStateException` → `409`, so authorization failures are no longer reported as `500`.

### Training queue integrity
- **Reject absurd batch sizes.** `VANDAL_FOOD_COST * quantity` is `int` arithmetic: `quantity = 50_000_000` wraps the cost negative, the "can you afford it" guard passes, and the deduction *adds* ~1.8 billion food. `trainVandal` now rejects `quantity` outside `1..MAX_TRAINING_BATCH_SIZE`, and `ResourcesService.deductResources` rejects negative costs as defence in depth.
- **Never schedule an order into the past.** `computeNextFinishesAt` chains off the last pending order even when that order finished minutes ago (orders resolve lazily, only on read). The new order then completes the instant anyone reads the queue. `trainVandal` now resolves completed orders first, and the chain base becomes `max(now, lastPendingFinishesAt)`.
- **Train only at a barrack in that village.** `requireBarrack` currently accepts any Barrack id in the world; it now resolves the barrack through the village's construction sites.

### Resource production accounting
- `refreshAndPersist` truncates elapsed time to whole seconds but then advances `lastUpdate` all the way to `now`, silently discarding the remainder. Two actions inside the same second credit zero production while still consuming the time. `lastUpdate` now advances by exactly the number of seconds that were credited, so the remainder carries forward.
- Elapsed time is clamped at zero (a `lastUpdate` in the future must never subtract resources) and production arithmetic is done in `long` with a saturating add, so a long-idle high-rate village cannot overflow `int` into a negative balance.

### Session lifecycle
- `RefreshTokenRepository.deleteByUsername` is a **derived delete query executed with no active transaction**, which Spring Data rejects — so `POST /auth/logout` fails and refresh tokens are never revoked. `RefreshTokenService.revokeByUsername` becomes `@Transactional`.
- `POST /auth/refresh` currently throws a bare `RuntimeException` when the cookie is absent and lets `validateRefreshToken`'s exceptions escape, producing `500` for the ordinary expired-session case. It now returns `401` for missing, unknown, and expired refresh tokens, which is what the frontend's `apiRequest` already knows how to handle.

### Account provisioning
- `UserService.newUser` is not transactional: `villageService.starterVillage` commits `markOccupied` on a world tile, and any later failure leaves that tile permanently occupied with no village on it. `newUser` becomes `@Transactional`.

## Capabilities

### New Capabilities
- `village-ownership-authorization`: every village-scoped endpoint proves the authenticated caller owns the village before reading or mutating it, and authorization failures surface as `403`.
- `training-queue-integrity`: training orders are affordable, sanely bounded, scheduled no earlier than now, and placed only at a barrack belonging to the village.
- `session-lifecycle`: logout actually revokes refresh tokens, and refresh reports an unusable token as `401`.
- `user-account-provisioning`: creating an account and its starter village is atomic, so a failed registration never strands a claimed world tile.

### Modified Capabilities
- `resource-production`: adds requirements covering how elapsed time is credited and carried forward. Existing requirements about base rates and per-building contributions are unchanged.

## Impact

**Backend only. No frontend change is required** — every endpoint keeps its URL, parameters, and success payload. Failure responses change: statuses get more specific (`403` for an ownership violation, `409` for "no building at this site") and error bodies become the same `Message` JSON as success bodies, carrying the real reason instead of a generic `"Unable to construct building"`. `apiRequest` treats any non-2xx other than `401` as a thrown error and never reads the body, so nothing in the client breaks — and the now-useful reason is what a later frontend change can surface.

- New: `village/VillageOwnershipService.java`, `web/GlobalExceptionHandler.java`
- Modified: `UnitController`, `UnitService`, `BuildingController`, `BuildingService`, `ResourceController`, `ResourcesService`, `RefreshTokenService`, `AuthController`, `UserService`, `VillageRepository`, `ConstructionSiteRepository`, `GameDefaults`
- **No schema migration.** No new tables, columns, or Liquibase changesets.
- Tests: new `VillageOwnershipServiceTest`, `GlobalExceptionHandlerTest`; extended `UnitServiceTest`, `UnitControllerTest`, `BuildingControllerTest`, `ResourceControllerTest`, `ResourcesServiceTest`, `RefreshTokenServiceTest`, `AuthControllerTest`, `UserServiceTest`.

## Out of scope

- Frontend changes (tracked separately).
- Rate limiting, audit logging, and role-based authorization beyond ownership.
- A database-backed integration test harness (see `design.md` — the logout fix is guarded by a transactional-boundary assertion rather than a live-database test).
- Resource storage caps.
