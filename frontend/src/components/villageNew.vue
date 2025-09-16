<template>
  <div ref="pixiContainer" class="w-[10vw] h-[10vh]"></div>
  <BuildingMenu
    :tileInfo="currentTile"
    :villageId="villageId"
    v-if="showMenu"
    :style="{ position: 'absolute', left: `${menuX}px`, top: `${menuY}px` }"
    @building-type="handleBuildingSelection"
  />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Application, Assets, Container, Rectangle, Sprite, Texture } from 'pixi.js'
import mapUrl from '@/assets/maps/vv.json?url'
import mapTilesUrl from '@/assets/maps/map_tiles.json?url'
import BuildingMenu from '@/components/BuildingMenu.vue'

const villageId = Number(localStorage.getItem('villageId'))

// Look into a better solution to handle villageId....
// defineProps({
//   villageId: {
//     type: String,
//     required: true
//   }
// })

const pixiContainer = ref(null)
let app
let container
let mapDataRef = null

const showMenu = ref(false)
let menuX = 0
let menuY = 0

const currentTile = ref({})

function resizeTilemap() {
  if (!app || !container || !mapDataRef) return

  const { width, height } = app.renderer

  const isoWidth = (mapDataRef.width + mapDataRef.height) * (mapDataRef.tilewidth / 2)
  const isoHeight = (mapDataRef.width + mapDataRef.height) * (mapDataRef.tileheight / 2)

  const scaleX = width / isoWidth
  const scaleY = height / isoHeight
  const scale = Math.min(scaleX, scaleY) * 2 // multiply by 2 if you want bigger zoom

  container.scale.set(scale)

  // Center the isometric map, adjust for its origin offset (because your anchor points are set on tiles)
  container.x = width / 2
  container.y = height / 2

  // If you want, add a small offset up to move the map fully into view:
  container.y -= (isoHeight * scale) / 2.5 // tweak this as needed
}

onMounted(async () => {
  const tileSprites = new Map()
  app = new Application()

  await app.init({ background: '#1099bb', resizeTo: window })

  pixiContainer.value.appendChild(app.canvas)

  container = new Container()

  app.stage.addChild(container)

  // Load main map JSON (vv.tmj)
  const mapData = await Assets.load(mapUrl)
  mapDataRef = mapData

  // Load tileset JSON and create GID -> texture mapping
  const tileImageTextures = {}

  for (const tileset of mapData.tilesets) {
    if (tileset.source) {
      // Load tileset JSON file
      const tilesetJson = await Assets.load(mapTilesUrl)

      // For each tile in tileset, load texture
      if (tilesetJson?.tileset?.tile && Array.isArray(tilesetJson.tileset.tile)) {
        for (const tile of tilesetJson.tileset.tile) {
          const id = tile._id

          const imageSource = tile.image?._source

          const fullPath = `/assets/${imageSource}`

          tileImageTextures[id] = await Assets.load(fullPath)
        }
      } else {
        console.warn('Tileset JSON missing tiles:', tilesetJson)
      }
    }
  }

  let constructionSiteId = 0

  // Render tile layers
  for (const layer of mapData.layers) {
    if (layer.type !== 'tilelayer') continue

    const { width, data } = layer
    const tileWidth = mapData.tilewidth
    const tileHeight = mapData.tileheight

    for (let i = 0; i < data.length; i++) {
      const gid = data[i]

      // Continue with next tile if no gid or texture on tile
      if (!gid || !tileImageTextures[gid - 1]) continue

      const row = Math.floor(i / width)
      const col = i % width

      const sprite = new Sprite(tileImageTextures[gid - 1])

      sprite.anchor.set(0.5, 1)
      sprite.x = (col - row) * (tileWidth / 2)
      sprite.y = (col + row) * (tileHeight / 2)

      if (isConstructionSiteTile(gid)) {
        constructionSiteId++
        setInteractiveSpriteTile(sprite, tileWidth, tileHeight)
        addSpriteTileEvent(tileSprites, sprite, row, col, gid, constructionSiteId)
      }
      container.addChild(sprite)
    }
  }

  resizeTilemap()
  window.addEventListener('resize', resizeTilemap)
})

/**
 * Returns a boolean if gid is the same as a construction site tile sprite
 * @param gid the gid to check
 * @returns {boolean}
 */
function isConstructionSiteTile(gid) {
  return gid - 1 === 58
}

/**
 * Make the tilesprite interactive
 * @param sprite the sprite to make interactive
 * @param tileWidth calculate hitbox
 * @param tileHeight calculate hitbox
 */
function setInteractiveSpriteTile(sprite, tileWidth, tileHeight) {
  sprite.interactive = true
  sprite.buttonMode = true
  sprite.cursor = 'pointer'
  sprite.hitArea = new Rectangle(-tileWidth / 2, -tileHeight * 2, tileWidth, tileHeight)
}

/**
 * This function adds data to a specific tile/sprite.
 * This data makes the tile interactive och processable.
 * @param tileSprites tilesprite map
 * @param sprite current sprite
 * @param row what row in the map that the tile is located at
 * @param col what column in the map that the tile is located at
 * @param gid gid of the tile. Identifier.
 * @param constructionSiteId custom id used when processing the request in the backend
 */
function addSpriteTileEvent(tileSprites, sprite, row, col, gid, constructionSiteId) {
  const key = `${row},${col}`
  sprite._tileInfo = { row, col, gid, constructionSiteId }
  tileSprites.set(key, sprite)

  sprite.on('pointerdown', async () => {
    showMenu.value = !showMenu.value
    menuX = event.clientX
    menuY = event.clientY
    console.log(sprite._tileInfo)
    currentTile.value = sprite._tileInfo
  })
}

function handleBuildingSelection(type) {
  const {row,col,constructionSiteId} = currentTile.value;
  console.log(type)
  addBuildingSprite(row, col, `/assets/Tiles/${type}.png`)
}

/**
 * Add a texture on the tile (constructing a building and place on that tile for example)
 * @param row what row to place texture on
 * @param col what column to place texture on
 * @param texturePath what texture to add
 * @param yOffset if no offset, the new texture will be kind of off in placement. -25 happends to solve this.
 * @returns {Sprite}
 */
async function addBuildingSprite(row, col, texturePath, yOffset = -95) {
  const { tilewidth, tileheight } = mapDataRef;

  const texture = await Assets.load(texturePath);
  const building = new Sprite(texture);

  building.anchor.set(0.5, 1); // bottom-center
  building.x = (col - row) * (tilewidth / 2);
  building.y = (col + row) * (tileheight / 2) + yOffset;

  container.addChild(building); // Adds texture "on top"

  return building;
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeTilemap)
  if (app) app.destroy(true, { children: true })
})
</script>
