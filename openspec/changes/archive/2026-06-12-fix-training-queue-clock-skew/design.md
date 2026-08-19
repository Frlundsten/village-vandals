## Context

`TrainingOrderDTO` carries `finishesAt`, an absolute `Instant` computed by the backend as `Instant.now() + duration`. Two places on the frontend turn this into a countdown by computing `remainingMs = new Date(order.finishesAt).getTime() - Date.now()`:

- `BuildingUpgradeCard.vue` — `enrichOrder()` (initial render) and the `startCountdown()` 100ms tick.
- `useTrainingQueue.js` — `enrichOrder()` and `tick()`, which drives the pulsing training indicator on the village map via `hasActiveFor()`.

Both assume the backend's clock and the browser's clock agree. In this dev environment (Docker backend running inside WSL2), the WSL2 VM clock can lag the Windows host clock by tens of seconds after the host sleeps/resumes — but the same class of bug would affect any real player whose OS clock is wrong. When the backend clock is behind, `finishesAt` (e.g. "5 seconds from backend-now") is already "in the past" by browser-now, so `remainingMs` clamps to 0 on the very first tick, the order is dropped from the visible queue, and `armyStore.refresh()` fires before the backend has actually finished the order — so nothing visibly changes until a later, unrelated refresh (e.g. navigating tabs) catches the now-actually-completed order.

## Goals / Non-Goals

**Goals:**
- Training queue countdown (progress bar + numeric countdown) and the pulsing map indicator remain visible and accurate for the full server-determined duration, independent of client/server clock skew.
- The "countdown reaches zero → `armyStore.refresh()`" trigger fires at approximately the real completion time, so the army roster mini-panel updates without a tab switch.
- Minimal, localized change — no new endpoints, no DB schema changes.

**Non-Goals:**
- General-purpose NTP-style clock synchronization or sub-second accuracy. A best-effort offset derived from one HTTP round trip is sufficient for a UI countdown.
- Persisting the clock offset across page reloads or sharing it via a Pinia store — each composable/component instance computes its own offset from its own responses.
- Fixing the underlying WSL2/Docker clock drift itself (that's an environment issue; this change makes the app robust to it regardless).

## Decisions

### 1. Carry server time on `TrainingOrderDTO` rather than a new response wrapper
Add a `serverTime` (`Instant`) field to `TrainingOrderDTO`, populated with `Instant.now()` at the point `toOrderDTOs()` builds the response. Every order in a given response carries the same `serverTime` value.

**Alternative considered**: wrap responses as `{ serverTime, orders: [...] }`. Rejected because `POST /unit/train` and `GET /unit/training` currently return a bare `List<TrainingOrderDTO>`, and frontend code (`Array.isArray(updatedQueue)`) and tests depend on that array shape. Repeating `serverTime` per element is a few extra bytes but avoids a breaking response-shape change.

### 2. Frontend derives a per-response clock offset, applied via `Date.now() + offset`
In both `BuildingUpgradeCard.vue` and `useTrainingQueue.js`:
- After fetching/receiving a non-empty order list, compute `clockOffsetMs = new Date(orders[0].serverTime).getTime() - Date.now()`.
- Store `clockOffsetMs` in a local variable (component/composable scope, default `0`).
- Replace all `Date.now()` calls used for `remainingMs` math (in `enrichOrder`, `startCountdown`'s tick, and `useTrainingQueue`'s `tick`) with `Date.now() + clockOffsetMs`.

**Alternative considered**: a shared Pinia store or composable holding a global clock offset. Rejected as unnecessary complexity for this fix — each consumer already fetches training-queue data independently and can derive its own offset from its own responses; values will be consistent in practice since they're sampled close together.

### 3. Recompute the offset on every response that contains orders
Rather than computing once and freezing it, recompute `clockOffsetMs` each time a response with at least one order arrives (mount fetch, POST /train response). This naturally tracks any change in drift (e.g. WSL2 clock catching up mid-session) without extra mechanism.

## Risks / Trade-offs

- **Network latency skews the offset slightly** → `serverTime` is captured when the backend builds the response, but the client receives it after some RTT, so the derived offset is off by roughly one-way latency. Mitigation: acceptable for a UI countdown with 5s-per-unit granularity; RTTs in dev/prod are typically well under 1s.
- **Empty-queue responses carry no `serverTime`** → `GET /unit/training` can return `[]`. Mitigation: no orders means no countdown to correct, so the offset simply isn't updated in that case (previous offset, or `0`, remains).
- **Two independent offset estimates** (`BuildingUpgradeCard` vs `useTrainingQueue`) could diverge slightly if their requests land far apart in time. Mitigation: both are corrected against the same backend clock and sampled within the same user session/seconds of each other — any divergence is negligible relative to training durations.

## Migration Plan

No database changes. Backend and frontend deploy together as usual; `serverTime` is an additive field on an existing DTO so it is backward-compatible with any client that ignores it. No rollback considerations beyond the normal revert-the-PR path.
