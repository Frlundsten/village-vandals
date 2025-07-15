<template>
  <div ref="pixiContainer" class="w-[10vw] h-[10vh]"></div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue';
import {Application, Assets, Container, Rectangle, Sprite} from 'pixi.js';
import mapUrl from '@/assets/maps/vv.json?url';
import mapTilesUrl from '@/assets/maps/map_tiles.json?url';

const pixiContainer = ref(null);
let app;
let container;
let mapDataRef = null;

function resizeTilemap() {
  if (!app || !container || !mapDataRef) return;

  const { width, height } = app.renderer;

  const isoWidth = (mapDataRef.width + mapDataRef.height) * (mapDataRef.tilewidth / 2);
  const isoHeight = (mapDataRef.width + mapDataRef.height) * (mapDataRef.tileheight / 2);

  const scaleX = width / isoWidth;
  const scaleY = height / isoHeight;
  const scale = Math.min(scaleX, scaleY) * 2 ; // multiply by 2 if you want bigger zoom

  container.scale.set(scale);

  // Center the isometric map, adjust for its origin offset (because your anchor points are set on tiles)
  container.x = width / 2;
  container.y = height / 2;

  // If you want, add a small offset up to move the map fully into view:
  container.y -= isoHeight * scale / 2.5; // tweak this as needed
}

onMounted(async () => {
  app = new Application();

  // Initialize the application
  await app.init({background: '#1099bb', resizeTo: window});

  // Append the application canvas to the document body
  pixiContainer.value.appendChild(app.canvas);

  // Create and add a container to the stage
  container = new Container();

  app.stage.addChild(container);

  // 1. Load main map JSON (vv.tmj)
  const mapData = await Assets.load(mapUrl);
  mapDataRef = mapData;

  // 2. Load tileset JSON and create GID -> texture mapping
  const tileImageTextures = {};

  for (const tileset of mapData.tilesets) {
    if (tileset.source) {
      // Load tileset JSON file
      const tilesetJson = await Assets.load(mapTilesUrl);

      // For each tile in tileset, load texture
      if (tilesetJson?.tileset?.tile && Array.isArray(tilesetJson.tileset.tile)) {
        for (const tile of tilesetJson.tileset.tile) {

          const id = tile._id;

          const imageSource = tile.image?._source; // Adjust if your JSON structure differs

          // Resolve full path of tile image
          const fullPath = `/assets/${imageSource}`;

          tileImageTextures[id] = await Assets.load(fullPath);
        }
      } else {
        console.warn('Tileset JSON missing tiles:', tilesetJson);
      }
    }
  }

  // 3. Render tile layers
  for (const layer of mapData.layers) {
    if (layer.type !== 'tilelayer') continue;

    const {width, data} = layer;
    const tileWidth = mapData.tilewidth;
    const tileHeight = mapData.tileheight;

    for (let i = 0; i < data.length; i++) {
      const gid = data[i];
      if (!gid || !tileImageTextures[gid - 1]) continue;

      const row = Math.floor(i / width);
      const col = i % width;

      const sprite = new Sprite(tileImageTextures[gid - 1]);

      sprite.anchor.set(0.5, 1);
      sprite.x = (col - row) * (tileWidth / 2);
      sprite.y = (col + row) * (tileHeight / 2);

      if ((gid - 1) === 58) {
        sprite.interactive = true;
        sprite.buttonMode = true;
        sprite.cursor = 'pointer';
        sprite.hitArea = new Rectangle(
            -tileWidth / 2,
            -tileHeight * 2,
            tileWidth,
            tileHeight
        );

        sprite.on('pointerdown', () => {
          console.log('Tile 58 clicked!');
        });
      }


      container.addChild(sprite);
    }
  }

  resizeTilemap();
  window.addEventListener('resize', resizeTilemap);

  // container.x = 1200;
  // container.y = -450;
  // container.scale.set(0.35);

});

// onBeforeUnmount(() => {
//   if (app) app.destroy(true, {children: true});
// });

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeTilemap);
  if (app) app.destroy(true, { children: true });
});
</script>

