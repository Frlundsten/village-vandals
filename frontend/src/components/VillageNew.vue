<template>
  <div class="w-full h-full relative">
    <div ref="pixiContainer" class="w-full h-full"></div>

    <div
      v-if="loading"
      class="absolute inset-0 flex items-center justify-center bg-black/50 text-white text-xl font-semibold z-10"
    >
      Loading village…
    </div>

    <!-- Level badges rendered as HTML so they stay at a fixed CSS size regardless of map zoom -->
    <template v-for="badge in buildingBadges" :key="badge.constructionSiteId">
      <div
        class="absolute z-20 pointer-events-none select-none -translate-x-1/2"
        :style="{ left: badge.x + 'px', top: badge.y + 'px' }"
      >
        <div
          class="flex items-center gap-1 px-2.5 py-0.5 rounded-full border-2 border-amber-400 bg-neutral/90 shadow-[0_2px_8px_rgba(0,0,0,0.6)] font-bold text-white text-sm whitespace-nowrap"
        >
          <span class="text-amber-400 text-xs font-normal leading-none">Lv</span>{{ badge.level }}
        </div>
      </div>
      <!-- Pulsating training indicator: shown when this barrack has an active training order -->
      <div
        v-if="trainingQueue.hasActiveFor(badge.constructionSiteId)"
        class="absolute z-10 pointer-events-none animate-pulse ring-4 ring-amber-400 rounded-sm"
        :style="{ left: badge.x - 32 + 'px', top: badge.y - 48 + 'px', width: '64px', height: '64px' }"
      ></div>
    </template>

    <BuildingMenu
      :tileInfo="currentTile"
      :villageId="villageId"
      :currentResources="resourceStore"
      v-if="showMenu"
      :style="{ position: 'absolute', left: '40%', top: '15vh' }"
      @building-type="handleBuildingSelection"
      @close-menu="showMenu = false"
    />
    <BuildingUpgradeCard
      v-if="showUpgradeCard && currentBuilding"
      :building="currentBuilding"
      :villageId="villageId"
      :currentResources="resourceStore"
      @upgrade="handleUpgrade"
      @close="showUpgradeCard = false"
    />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Application, Assets, Container, Graphics, Rectangle, Sprite } from 'pixi.js'
import mapUrl from '@/assets/maps/vv.json?url'
import mapTilesUrl from '@/assets/maps/map_tiles.json?url'
import BuildingMenu from '@/components/BuildingMenu.vue'
import BuildingUpgradeCard from '@/components/BuildingUpgradeCard.vue'
import { constructBuilding, fetchBuildings, upgradeBuilding } from '@/util/api/buildings.js'
import { useResourceStore } from '@/stores/resources.js'
import { clampMapPosition } from '@/util/clampMapPosition.js'
import { useTrainingQueue } from '@/composables/useTrainingQueue.js'

const route = useRoute()
const villageId = Number(route.params.villageId) || Number(localStorage.getItem('villageId'))

const pixiContainer = ref(null)
let app
let container
let mapDataRef = null

const resourceStore = useResourceStore()
const trainingQueue = useTrainingQueue(villageId)

const loading = ref(true)
const showMenu = ref(false)
const showUpgradeCard = ref(false)
const currentTile = ref({})
const currentBuilding = ref(null)

// Reactive map of constructionSiteId -> BuildingDTO for all placed buildings
const buildingsBySiteId = ref(new Map())

// HTML overlay badges — updated whenever the map moves or buildings change
const buildingBadges = ref([])
// PixiJS Container references keyed by constructionSiteId for position tracking
const buildingContainersBySiteId = new Map()

const dragging = ref(false)
let dragStart = { x: 0, y: 0 }
let containerStart = { x: 0, y: 0 }
let dragInitiatedOnCanvas = false
const DRAG_THRESHOLD = 5
let canvasCleanup = null

function updateBadgePositions() {
  if (!app) return
  buildingBadges.value = Array.from(buildingContainersBySiteId.entries()).map(([siteId, bc]) => {
    const gp = bc.getGlobalPosition()
    return {
      constructionSiteId: siteId,
      level: buildingsBySiteId.value.get(siteId)?.level ?? 1,
      x: Math.round(gp.x),
      y: Math.round(gp.y),
    }
  })
}

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

  updateBadgePositions()
}

onMounted(async () => {
  try {
    const existingBuildings = await fetchBuildings(villageId)
    existingBuildings.forEach((b) => buildingsBySiteId.value.set(b.constructionSiteId, b))

    const tileSprites = new Map()
    app = new Application()

    await app.init({ background: '#1099bb', resizeTo: pixiContainer.value })

    pixiContainer.value.appendChild(app.canvas)

    container = new Container()
    container.sortableChildren = true
    app.stage.addChild(container)

    // Use native canvas events so PixiJS hit-testing is not bypassed by a
    // stage hitArea that would otherwise make the stage catch all pointer events
    // before tile sprites are checked.
    const pointerDownFn = (event) => {
      dragInitiatedOnCanvas = true
      dragStart = { x: event.offsetX, y: event.offsetY }
      containerStart = { x: container.x, y: container.y }
      dragging.value = false
    }
    const pointerUpFn = () => {
      dragInitiatedOnCanvas = false
      dragging.value = false
    }
    const wheelFn = (event) => {
      event.preventDefault()
      const zoomFactor = event.deltaY < 0 ? 1.1 : 0.9
      const currentScale = container.scale.x
      const newScale = Math.min(3, Math.max(0.3, currentScale * zoomFactor))
      const px = event.offsetX
      const py = event.offsetY
      container.x = px + (container.x - px) * (newScale / currentScale)
      container.y = py + (container.y - py) * (newScale / currentScale)
      container.scale.set(newScale)
      clampMapPosition(container, app.renderer.width, app.renderer.height)
      updateBadgePositions()
    }
    app.canvas.addEventListener('pointerdown', pointerDownFn)
    app.canvas.addEventListener('pointerup', pointerUpFn)
    app.canvas.addEventListener('wheel', wheelFn, { passive: false })
    canvasCleanup = () => {
      app.canvas.removeEventListener('pointerdown', pointerDownFn)
      app.canvas.removeEventListener('pointerup', pointerUpFn)
      app.canvas.removeEventListener('wheel', wheelFn)
    }

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
    const buildingSpritePromises = []

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
              buildingSpritePromises.push(
                addBuildingSprite(
                  row,
                  col,
                  `/assets/Tiles/${building.type}.png`,
                  building.constructionSiteId,
                  building.level ?? 1,
                ),
              )
            }
          })
          setInteractiveSpriteTile(sprite, tileWidth, tileHeight)
          addSpriteTileEvent(tileSprites, sprite, row, col, gid, constructionSiteId)
        }
        container.addChild(sprite)
      }
    }

    await Promise.all(buildingSpritePromises)

    const bounds = container.getLocalBounds()

    const dragLayer = new Graphics()
    dragLayer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
    dragLayer.fill({ color: 0x000000, alpha: 0 })

    dragLayer.eventMode = 'static'
    dragLayer.cursor = 'grab'
    dragLayer.zIndex = -1

    container.addChildAt(dragLayer, 0)

    dragLayer.on('globalpointermove', (event) => {
      if (!dragInitiatedOnCanvas) return
      if (event.buttons === 0) {
        dragging.value = false
        return
      }

      const dx = event.global.x - dragStart.x
      const dy = event.global.y - dragStart.y
      if (!dragging.value && Math.hypot(dx, dy) > DRAG_THRESHOLD) {
        dragging.value = true
        dragLayer.cursor = 'grabbing'
      }
      if (!dragging.value) return

      container.x = containerStart.x + dx
      container.y = containerStart.y + dy
      clampMapPosition(container, app.renderer.width, app.renderer.height)
      updateBadgePositions()
    })

    loading.value = false
  } catch (error) {
    console.error(error)
    loading.value = false
  }
  resizeTilemap()
  window.addEventListener('resize', resizeTilemap)
})

function isConstructionSiteTile(gid) {
  return gid - 1 === 58
}

function setInteractiveSpriteTile(sprite, tileWidth, tileHeight) {
  sprite.eventMode = 'static'
  sprite.cursor = 'pointer'
  sprite.hitArea = new Rectangle(-tileWidth / 2, -tileHeight * 2, tileWidth, tileHeight)
  sprite.on('pointerover', () => {
    sprite.tint = 0xddddff
  })
  sprite.on('pointerout', () => {
    sprite.tint = 0xffffff
  })
}

function addSpriteTileEvent(tileSprites, sprite, row, col, gid, constructionSiteId) {
  const key = `${row},${col}`
  sprite._tileInfo = { row, col, gid, constructionSiteId }
  tileSprites.set(key, sprite)

  sprite.on('pointerup', async () => {
    if (dragging.value) return
    if (buildingsBySiteId.value.has(constructionSiteId)) return

    showMenu.value = !showMenu.value
    currentTile.value = sprite._tileInfo
    try {
      await resourceStore.refresh(villageId)
    } catch (e) {
      console.error('Failed to fetch resources', e)
    }
  })
}

async function handleBuildingSelection(type) {
  const { row, col, constructionSiteId } = currentTile.value
  try {
    await constructBuilding(type, constructionSiteId, villageId)
    const updated = await fetchBuildings(villageId)
    updated.forEach((b) => buildingsBySiteId.value.set(b.constructionSiteId, b))
  } catch (e) {
    console.error('Failed to construct building:', e)
    return
  }
  const level = buildingsBySiteId.value.get(constructionSiteId)?.level ?? 1
  await addBuildingSprite(row, col, `/assets/Tiles/${type}.png`, constructionSiteId, level)
  updateBadgePositions()
  try {
    await resourceStore.refresh(villageId)
  } catch (e) {
    console.error('Failed to refresh resources after construction', e)
  }
}

async function addBuildingSprite(
  row,
  col,
  texturePath,
  constructionSiteId,
  level = 1,
  yOffset = -95,
) {
  const { tilewidth, tileheight } = mapDataRef

  const texture = await Assets.load(texturePath)
  const sprite = new Sprite(texture)
  sprite.anchor.set(0.5, 1) // bottom-center

  const buildingContainer = new Container()
  buildingContainer.x = (col - row) * (tilewidth / 2)
  buildingContainer.y = (col + row) * (tileheight / 2) + yOffset
  buildingContainer.zIndex = row + col + 0.5
  buildingContainer.addChild(sprite)

  buildingContainersBySiteId.set(constructionSiteId, buildingContainer)

  container.addChild(buildingContainer)
  buildingContainer.interactive = true
  buildingContainer.on('pointerup', async () => {
    const buildingData = buildingsBySiteId.value.get(constructionSiteId)
    if (!buildingData) return
    currentBuilding.value = buildingData
    showUpgradeCard.value = true
    showMenu.value = false
    try {
      await resourceStore.refresh(villageId)
    } catch (e) {
      console.error('Failed to fetch resources', e)
    }
  })

  return buildingContainer
}

async function handleUpgrade(constructionSiteId) {
  try {
    await upgradeBuilding(villageId, constructionSiteId)
    const updated = await fetchBuildings(villageId)
    buildingsBySiteId.value = new Map(updated.map((b) => [b.constructionSiteId, b]))
    currentBuilding.value = buildingsBySiteId.value.get(constructionSiteId) ?? null

    const badge = buildingBadges.value.find((b) => b.constructionSiteId === constructionSiteId)
    if (badge) badge.level = currentBuilding.value?.level ?? 1

    await resourceStore.refresh(villageId)
  } catch (e) {
    console.error('Upgrade failed', e)
  } finally {
    showUpgradeCard.value = false
  }
}

function setupSprite(sprite, col, row, tileWidth, tileHeight) {
  sprite.anchor.set(0.5, 1)
  sprite.x = (col - row) * (tileWidth / 2)
  sprite.y = (col + row) * (tileHeight / 2)
}

onBeforeUnmount(() => {
  canvasCleanup?.()
  window.removeEventListener('resize', resizeTilemap)
  if (app) app.destroy(true, { children: true })
})

defineExpose({
  handleBuildingSelection,
  buildingsBySiteId,
  currentTile,
  loading,
  dragging,
  buildingBadges,
})
</script>
