<template>
  <div ref="pixiContainer" class="w-[10vw] h-[10vh]"></div>
  <BuildingMenu
    :tileInfo="currentTile"
    :villageId="villageId"
    v-if="showMenu"
    :style="{ position: 'absolute', left: '40%', top: '15vh' }"
    @building-type="handleBuildingSelection"
    @close-menu="showMenu = false"
  />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Application, Assets, Container, Graphics, Rectangle, Sprite } from 'pixi.js'
import mapUrl from '@/assets/maps/vv.json?url'
import mapTilesUrl from '@/assets/maps/map_tiles.json?url'
import BuildingMenu from '@/components/BuildingMenu.vue'
import { fetchBuildings } from '@/util/api/buildings.js'

const villageId = Number(localStorage.getItem('villageId'))

const pixiContainer = ref(null)
let app
let container
let mapDataRef = null

const showMenu = ref(false)

const currentTile = ref({})

let dragging = false
let dragStart = { x: 0, y: 0 }
let containerStart = { x: 0, y: 0 }
let dragTimer = null
const DRAG_DELAY = 500 // ms

function resizeTilemap() {
  if (!app || !container || !mapDataRef) return

  const { width: canvasWidth, height: canvasHeight } = app.renderer

  const bounds = container.getLocalBounds()
  const mapWidth = bounds.width
  const mapHeight = bounds.height

  // Fit the map to viewport
  const scaleX = canvasWidth / mapWidth
  const scaleY = canvasHeight / mapHeight
  const scale = Math.min(scaleX, scaleY, 1) // Prevent overscaling on large screens

  container.scale.set(scale)

  // Pivot center for easy panning
  container.pivot.set(bounds.x + mapWidth / 2, bounds.y + mapHeight / 2)
  container.position.set(canvasWidth / 2, canvasHeight / 2)
}

onMounted(async () => {
  try {
    const existingBuildings = await fetchBuildings(villageId)

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
    const loadedTextures = {}

    const tilesetJson = await Assets.load(mapTilesUrl)
    const tilePromises = tilesetJson.tileset.tile.map((tile) => {
      const id = tile._id
      const fullPath = `/assets/${tile.image._source}`
      if (!loadedTextures[fullPath]) {
        loadedTextures[fullPath] = Assets.load(fullPath)
      }
      return loadedTextures[fullPath].then((texture) => {
        tileImageTextures[id] = texture
      })
    })
    await Promise.all(tilePromises)

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

        setupSprite(sprite, col, row, tileWidth, tileHeight)

        if (isConstructionSiteTile(gid)) {
          constructionSiteId++
          existingBuildings.forEach((building) => {
            if (constructionSiteId === building.constructionSiteId) {
              addBuildingSprite(row, col, `/assets/Tiles/${building.type}.png`)
            }
          })
          setInteractiveSpriteTile(sprite, tileWidth, tileHeight)
          addSpriteTileEvent(tileSprites, sprite, row, col, gid, constructionSiteId)
        }
        container.addChild(sprite)
      }
    }
    const bounds = container.getLocalBounds()

    const dragLayer = new Graphics()
    dragLayer.beginFill(0x000000, 0)
    dragLayer.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
    dragLayer.endFill()

    dragLayer.interactive = true
    dragLayer.cursor = 'grab'

// Add behind tiles
    container.addChildAt(dragLayer, 0)

    dragLayer.on('pointerdown', (event) => {
      dragStart = event.data.global.clone()
      containerStart = { x: container.x, y: container.y }

      dragTimer = setTimeout(() => {
        dragging = true
        dragLayer.cursor = 'grabbing'
      }, DRAG_DELAY)
    })

    const stopDrag = () => {
      clearTimeout(dragTimer)
      dragging = false
      dragLayer.cursor = 'grab'
    }

    dragLayer.on('pointerup', stopDrag)
    dragLayer.on('pointerupoutside', stopDrag)

    dragLayer.on('pointermove', (event) => {
      if (!dragging) return

      const current = event.data.global
      container.x = containerStart.x + (current.x - dragStart.x)
      container.y = containerStart.y + (current.y - dragStart.y)
    })
  } catch (error) {
    console.error(error)
  }
  // enableDragging()
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
 * This data makes the tile interactive and processable.
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

  sprite.on('pointerup', async () => {
    showMenu.value = !showMenu.value
    console.log(sprite._tileInfo)
    currentTile.value = sprite._tileInfo
  })
}

function handleBuildingSelection(type) {
  const { row, col, constructionSiteId } = currentTile.value
  console.log(type)
  addBuildingSprite(row, col, `/assets/Tiles/${type}.png`, constructionSiteId)
}

/**
 * Add a texture on the tile (constructing a building and place on that tile for example)
 * @param row what row to place texture on
 * @param col what column to place texture on
 * @param texturePath what texture to add
 * @param yOffset if no offset, the new texture will be kind of off in placement. -95 happens to solve this.
 * @returns {Sprite}
 */
async function addBuildingSprite(row, col, texturePath, constructionSiteId, yOffset = -95) {
  const { tilewidth, tileheight } = mapDataRef

  const texture = await Assets.load(texturePath)
  const building = new Sprite(texture)

  building.anchor.set(0.5, 1) // bottom-center
  building.x = (col - row) * (tilewidth / 2)
  building.y = (col + row) * (tileheight / 2) + yOffset

  container.addChild(building) // Adds texture "on top"
  building.interactive = true
  building.on('pointerup', async () => {
    console.log(`clicked
    x: ${building.x}
    y: ${building.y}
    with constructionsiteId: ${constructionSiteId} `)
  })

  return building
}

function setupSprite(sprite, col, row, tileWidth, tileHeight) {
  sprite.anchor.set(0.5, 1)
  sprite.x = (col - row) * (tileWidth / 2)
  sprite.y = (col + row) * (tileHeight / 2)
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeTilemap)
  if (app) app.destroy(true, { children: true })
})
</script>
