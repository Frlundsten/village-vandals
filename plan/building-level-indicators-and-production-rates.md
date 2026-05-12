# Spec: Building Level Indicators & Resource Production Rate Display

## Requirements

### Building Level Indicators (PixiJS)
1. Each placed building sprite shows a small level badge rendered via PixiJS.
2. The badge displays the numeric level (e.g. `3`) inside a styled pill at the bottom of the building sprite.
3. After an upgrade completes the badge number increments live — no full map re-render.
4. Level 1 buildings also show the badge.

### Production Rate Under Resources (Header)
1. Each resource badge in the header shows `+X/hr` below the current amount.
2. Production rates reflect the village's actual `ResourceProduction` values from the backend.
3. Rates update after a building is constructed or upgraded.

## Acceptance Criteria
- All placed buildings render a level badge on page load.
- Completing an upgrade increments the badge number immediately.
- Header resource badges show `+X/hr` with the correct rate from the backend.
- Production rate is part of the `/resources/refresh` response (no extra round-trip).

## Architecture Impact

### Backend — `ResourceController`
- Extend `GET /resources/refresh` response to include production rates alongside amounts.
- New response shape: `{ food, wood, bricks, iron, foodPerHour, woodPerHour, bricksPerHour, ironPerHour }`.
- Introduce a `ResourceStorageResponse` record in the `resource` package instead of the raw `Map<String, Integer>`.

### Frontend
| File | Change |
|---|---|
| `VillageNew.vue` | Wrap building `Sprite` in a `Container`; add PixiJS `Text` level badge as child. Store `Map<constructionSiteId, Text>` to update badge text after upgrade. |
| `stores/resources.js` | Add `foodPerHour`, `woodPerHour`, `bricksPerHour`, `ironPerHour` refs; update `refresh()` to parse them. |
| `views/Home.vue` | Bind `+{X}/hr` sub-label under each resource count using the new store refs. |

## Out of Scope
- Badge color tiers by level range (bronze/silver/gold).
- Animated transitions on upgrade.
- A live ticking resource counter in the header.
- Any changes to the world-map view.
