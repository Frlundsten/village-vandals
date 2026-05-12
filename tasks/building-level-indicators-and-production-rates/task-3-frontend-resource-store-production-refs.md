# Task 3 — Update frontend resource store with production rate refs

In `stores/resources.js`:
- Add `foodPerHour`, `woodPerHour`, `bricksPerHour`, `ironPerHour` refs (default 0).
- Update `refresh()` to parse them from the new response shape.

Write a Vitest unit test for the store covering the new fields.
