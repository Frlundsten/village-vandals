# Task 2 — Implement zIndex-based sorting in VillageNew.vue

Four changes to VillageNew.vue:
1. `container.sortableChildren = true` after creating the Container
2. `setupSprite`: add `sprite.zIndex = row + col`
3. `addBuildingSprite`: add `building.zIndex = row + col + 0.5`
4. `dragLayer`: add `dragLayer.zIndex = -1`
