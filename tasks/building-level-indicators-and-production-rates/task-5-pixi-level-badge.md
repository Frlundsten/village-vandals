# Task 5 — Add PixiJS level badge to building sprites in VillageNew.vue

Modify `addBuildingSprite()`:
- Accept building level as a parameter.
- Wrap the Sprite in a Container.
- Add a PixiJS Text label styled as a pill badge, positioned at the bottom of the sprite.
- Return the Container and store a `Map<constructionSiteId, Text>` for badge references.

Write a Vitest test verifying the badge is created with the correct level text.
