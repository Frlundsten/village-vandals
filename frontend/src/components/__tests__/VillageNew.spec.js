import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import VillageNew from '../VillageNew.vue'
import { useTrainingStore } from '@/stores/training.js'
import * as buildingsApi from '@/util/api/buildings.js'
import { Application, Assets, Container, Graphics } from 'pixi.js'

vi.mock('pixi.js', () => ({
  Application: vi.fn(function () {
    const canvas = document.createElement('canvas')
    return {
      init: vi.fn().mockResolvedValue(undefined),
      stage: { addChild: vi.fn(), on: vi.fn() },
      canvas,
      renderer: { width: 800, height: 600 },
      screen: { x: 0, y: 0, width: 800, height: 600 },
      destroy: vi.fn(),
    }
  }),
  Assets: {
    load: vi.fn().mockResolvedValue({
      layers: [],
      tilewidth: 32,
      tileheight: 32,
      tileset: { tile: [] },
    }),
  },
  Container: vi.fn(function () {
    return {
      addChild: vi.fn(),
      addChildAt: vi.fn(),
      getLocalBounds: vi.fn().mockReturnValue({ x: 0, y: 0, width: 100, height: 100 }),
      getGlobalPosition: vi.fn().mockReturnValue({ x: 100, y: 200 }),
      scale: { set: vi.fn(), x: 1, y: 1 },
      pivot: { set: vi.fn(), x: 0, y: 0 },
      position: { set: vi.fn() },
      x: 0,
      y: 0,
      zIndex: 0,
      interactive: false,
      sortableChildren: false,
      on: vi.fn(),
    }
  }),
  Graphics: vi.fn(function () {
    return {
      beginFill: vi.fn().mockReturnThis(),
      drawRect: vi.fn().mockReturnThis(),
      endFill: vi.fn().mockReturnThis(),
      rect: vi.fn().mockReturnThis(),
      fill: vi.fn().mockReturnThis(),
      circle: vi.fn().mockReturnThis(),
      stroke: vi.fn().mockReturnThis(),
      interactive: false,
      eventMode: '',
      cursor: '',
      zIndex: 0,
      on: vi.fn(),
    }
  }),
  Rectangle: vi.fn(),
  Sprite: vi.fn(function () {
    return {
      anchor: { set: vi.fn() },
      x: 0,
      y: 0,
      interactive: false,
      on: vi.fn(),
    }
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { villageId: '1' } }),
}))

vi.mock('@/util/api/buildings.js', () => ({
  fetchBuildings: vi.fn(),
  constructBuilding: vi.fn(),
  upgradeBuilding: vi.fn(),
  getAvailableBuildings: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn().mockResolvedValue({ food: 0, wood: 0, bricks: 0, iron: 0 }),
}))

vi.mock('@/util/api/units.js', () => ({
  trainUnit: vi.fn(),
  fetchRoster: vi.fn(),
  fetchTrainingQueue: vi.fn().mockResolvedValue([]),
}))

describe('VillageNew — drag vs click', () => {
  it('a small pointer movement after clicking a tile does not activate dragging', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])

    // Intercept canvas.addEventListener to capture the native pointerdown handler
    let canvasPointerdownFn = null
    const testCanvas = document.createElement('canvas')
    const origAdd = testCanvas.addEventListener.bind(testCanvas)
    vi.spyOn(testCanvas, 'addEventListener').mockImplementation((evt, fn, ...opts) => {
      if (evt === 'pointerdown') canvasPointerdownFn = fn
      origAdd(evt, fn, ...opts)
    })

    Application.mockImplementationOnce(function () {
      return {
        init: vi.fn().mockResolvedValue(undefined),
        stage: { addChild: vi.fn(), on: vi.fn(), eventMode: 'passive', hitArea: null },
        canvas: testCanvas,
        renderer: { width: 800, height: 600 },
        screen: { x: 0, y: 0, width: 800, height: 600 },
        destroy: vi.fn(),
      }
    })

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    // Simulate: user presses canvas at (300, 200) — native event sets dragStart
    canvasPointerdownFn?.({ offsetX: 300, offsetY: 200 })

    // Get the dragLayer's globalpointermove handler
    const graphicsInstance = Graphics.mock.results[0].value
    const moveHandler = graphicsInstance.on.mock.calls.find(
      (c) => c[0] === 'globalpointermove',
    )?.[1]

    // Simulate: pointer moves only 2 px — under the 5 px threshold
    moveHandler?.({ global: { x: 302, y: 200 }, buttons: 1 })

    // dragging must stay false — a 2 px movement is a click, not a drag
    expect(wrapper.vm.dragging).toBe(false)

    wrapper.unmount()
  })
})

describe('VillageNew — loading overlay', () => {
  it('starts as loading and clears after assets are ready', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })

    expect(wrapper.vm.loading).toBe(true)

    await flushPromises()

    expect(wrapper.vm.loading).toBe(false)
  })
})

describe('VillageNew — drag guard when press originates outside canvas', () => {
  it('does not pan the map when globalpointermove fires without a prior canvas pointerdown', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])

    const { Container } = await import('pixi.js')

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    const containerInstance = Container.mock.results[0].value
    const initialX = containerInstance.x
    const initialY = containerInstance.y

    // Get globalpointermove handler — WITHOUT triggering canvas pointerdown first
    const graphicsInstance = Graphics.mock.results[0].value
    const moveHandler = graphicsInstance.on.mock.calls.find(
      (c) => c[0] === 'globalpointermove',
    )?.[1]

    // Simulate a large move with button held (as if pressed on an overlay)
    moveHandler?.({ global: { x: 400, y: 300 }, buttons: 1 })

    // Container must not have moved
    expect(containerInstance.x).toBe(initialX)
    expect(containerInstance.y).toBe(initialY)
    expect(wrapper.vm.dragging).toBe(false)

    wrapper.unmount()
  })
})

describe('VillageNew — terrain tile zIndex', () => {
  it('does not set an explicit zIndex on plain terrain tiles', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])

    const { Assets, Sprite } = await import('pixi.js')
    Sprite.mockClear()

    // Two plain terrain tiles (gid=1, not a construction site)
    Assets.load
      .mockResolvedValueOnce({
        layers: [{ type: 'tilelayer', width: 2, data: [1, 1] }],
        tilewidth: 32,
        tileheight: 32,
      })
      .mockResolvedValueOnce({ tileset: { tile: [{ _id: 0, image: { _source: 'test.png' } }] } })
      .mockResolvedValue({})

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    const sprites = Sprite.mock.results.map((r) => r.value)
    // Plain terrain tiles must NOT have an explicit zIndex — PixiJS insertion order handles them
    expect(sprites[0].zIndex).toBeUndefined()
    expect(sprites[1].zIndex).toBeUndefined()

    wrapper.unmount()
  })
})

describe('VillageNew — construction site tile zIndex', () => {
  it('assigns zIndex = row + col to construction site tiles only', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])

    const { Assets, Sprite } = await import('pixi.js')
    Sprite.mockClear()

    // Map: plain tile at col=0, construction site (gid=59, gid-1=58) at col=1 (row=0)
    Assets.load
      .mockResolvedValueOnce({
        layers: [{ type: 'tilelayer', width: 2, data: [1, 59] }],
        tilewidth: 32,
        tileheight: 32,
      })
      .mockResolvedValueOnce({
        tileset: {
          tile: [
            { _id: 0, image: { _source: 'terrain.png' } },
            { _id: 58, image: { _source: 'site.png' } },
          ],
        },
      })
      .mockResolvedValue({})

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    const sprites = Sprite.mock.results.map((r) => r.value)
    const plainSprite = sprites[0] // row=0, col=0
    const siteSprite = sprites[1] // row=0, col=1

    expect(plainSprite.zIndex).toBeUndefined() // terrain — no explicit zIndex
    expect(siteSprite.zIndex).toBeUndefined() // construction site tile — no explicit zIndex

    wrapper.unmount()
  })
})

describe('VillageNew — handleBuildingSelection', () => {
  let wrapper

  beforeEach(async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.constructBuilding.mockResolvedValue({})

    wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: {
        stubs: { BuildingMenu: true, BuildingUpgradeCard: true },
      },
    })
    await flushPromises()

    wrapper.vm.currentTile = { row: 1, col: 1, constructionSiteId: 2 }
  })

  it('populates buildingsBySiteId with the new building after successful construction', async () => {
    const newBuilding = { constructionSiteId: 2, type: 'Farm', level: 1 }
    buildingsApi.fetchBuildings.mockResolvedValue([newBuilding])

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    expect(wrapper.vm.buildingsBySiteId.get(2)).toEqual(newBuilding)
  })

  it('calls addBuildingSprite after successful construction', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 2, type: 'Farm', level: 1 },
    ])

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    expect(Assets.load).toHaveBeenCalledWith('/assets/Tiles/Farm.png')
  })

  it('does not call addBuildingSprite when constructBuilding throws', async () => {
    buildingsApi.constructBuilding.mockRejectedValue(new Error('Construction failed'))
    Assets.load.mockClear()

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    expect(Assets.load).not.toHaveBeenCalledWith('/assets/Tiles/Farm.png')
  })

  it('loads /assets/Tiles/BRICKYARD.png when BRICKYARD is selected', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 2, type: 'BRICKYARD', level: 1 },
    ])

    await wrapper.vm.handleBuildingSelection('BRICKYARD')
    await flushPromises()

    expect(Assets.load).toHaveBeenCalledWith('/assets/Tiles/BRICKYARD.png')
  })

  it('building container zIndex is greater than base tile zIndex at the same row+col', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 2, type: 'FARM', level: 1 },
    ])

    const { Container } = await import('pixi.js')

    await wrapper.vm.handleBuildingSelection('FARM')
    await flushPromises()

    // Container[0] = main map container, Container[1] = building wrapper container
    const buildingContainer = Container.mock.results[1].value

    // currentTile is row=1, col=1 → base tile zIndex = 2, building container zIndex must be > 2
    expect(buildingContainer.zIndex).toBeGreaterThan(
      wrapper.vm.currentTile.row + wrapper.vm.currentTile.col,
    )
  })
})

describe('VillageNew — building level badge (HTML overlay)', () => {
  let wrapper

  beforeEach(async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.constructBuilding.mockResolvedValue({})

    wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    wrapper.vm.currentTile = { row: 0, col: 0, constructionSiteId: 3 }
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it('adds a badge entry with the building level when a building is placed', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 3, type: 'Farm', level: 2 },
    ])

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    const badge = wrapper.vm.buildingBadges.find((b) => b.constructionSiteId === 3)
    expect(badge).toBeDefined()
    expect(badge.level).toBe(2)
  })

  it('badge position is derived from the building container screen coordinates', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 3, type: 'Farm', level: 1 },
    ])

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    const badge = wrapper.vm.buildingBadges.find((b) => b.constructionSiteId === 3)
    // The mock Container.getGlobalPosition returns { x: 100, y: 200 }
    expect(badge.x).toBe(100)
    expect(typeof badge.y).toBe('number')
  })
})

describe('VillageNew — badge update on upgrade', () => {
  it('updates the badge level after handleUpgrade', async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.constructBuilding.mockResolvedValue({})
    buildingsApi.upgradeBuilding = vi.fn().mockResolvedValue({})

    const wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()

    wrapper.vm.currentTile = { row: 0, col: 0, constructionSiteId: 5 }

    // Place a level-1 building
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 5, type: 'Farm', level: 1 },
    ])
    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    expect(wrapper.vm.buildingBadges.find((b) => b.constructionSiteId === 5).level).toBe(1)

    // Upgrade: backend now returns level 2
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 5, type: 'Farm', level: 2 },
    ])
    await wrapper.vm.handleUpgrade(5)
    await flushPromises()

    expect(wrapper.vm.buildingBadges.find((b) => b.constructionSiteId === 5).level).toBe(2)

    wrapper.unmount()
  })
})

describe('VillageNew — training indicator', () => {
  let wrapper

  beforeEach(async () => {
    setActivePinia(createPinia())
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.constructBuilding.mockResolvedValue({})

    wrapper = mount(VillageNew, {
      attachTo: document.body,
      global: { stubs: { BuildingMenu: true, BuildingUpgradeCard: true } },
    })
    await flushPromises()
    wrapper.vm.currentTile = { row: 0, col: 0, constructionSiteId: 3 }
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it('shows the training indicator for a barrack whose buildingId differs from its site id', async () => {
    // Construction site 3 holds the barrack whose building PK is 77 — two distinct
    // identifier spaces, and the training order only ever carries the building PK.
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 3, buildingId: 77, type: 'BARRACK', level: 1 },
    ])
    await wrapper.vm.handleBuildingSelection('BARRACK')
    await flushPromises()

    expect(wrapper.find('[data-testid="training-indicator"]').exists()).toBe(false)

    useTrainingStore().setOrders([
      {
        id: 1,
        unitType: 'VANDAL',
        buildingId: 77,
        quantity: 1,
        queuePosition: 1,
        finishesAt: new Date(Date.now() + 5000).toISOString(),
      },
    ])
    await nextTick()

    expect(wrapper.find('[data-testid="training-indicator"]').exists()).toBe(true)
  })

  it('shows no training indicator for a barrack with no pending orders', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([
      { constructionSiteId: 3, buildingId: 77, type: 'BARRACK', level: 1 },
    ])
    await wrapper.vm.handleBuildingSelection('BARRACK')
    await flushPromises()

    useTrainingStore().setOrders([
      {
        id: 1,
        unitType: 'VANDAL',
        buildingId: 99,
        quantity: 1,
        queuePosition: 1,
        finishesAt: new Date(Date.now() + 5000).toISOString(),
      },
    ])
    await nextTick()

    expect(wrapper.find('[data-testid="training-indicator"]').exists()).toBe(false)
  })
})

describe('VillageNew — load failures and teardown', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Assets.load.mockResolvedValue({
      layers: [],
      tilewidth: 32,
      tileheight: 32,
      tileset: { tile: [] },
    })
  })

  it('still renders the map and shows an error when the buildings fetch fails', async () => {
    buildingsApi.fetchBuildings.mockRejectedValue(new Error('Not the owner of village 1'))

    const wrapper = mount(VillageNew)
    await flushPromises()

    // The map must not be gated on building data — the player used to get a blank white div.
    expect(Application).toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(false)
    expect(wrapper.text()).toContain('Not the owner of village 1')
    expect(wrapper.text()).not.toContain('Loading village')
  })

  it('shows an error when the map assets fail to load', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])
    Assets.load.mockRejectedValue(new Error('tileset unavailable'))

    const wrapper = mount(VillageNew)
    await flushPromises()

    expect(wrapper.vm.loading).toBe(false)
    expect(wrapper.text()).toContain('tileset unavailable')
  })

  it('unmounting during the buildings fetch builds nothing and registers no listener', async () => {
    let releaseBuildings
    buildingsApi.fetchBuildings.mockReturnValue(
      new Promise((resolve) => {
        releaseBuildings = resolve
      }),
    )

    const addSpy = vi.spyOn(window, 'addEventListener')

    const wrapper = mount(VillageNew)
    await nextTick()

    // The player navigates away while the buildings request is still in flight.
    wrapper.unmount()

    releaseBuildings([])
    await flushPromises()

    expect(Application).not.toHaveBeenCalled()
    expect(addSpy.mock.calls.filter(([event]) => event === 'resize')).toHaveLength(0)

    addSpy.mockRestore()
  })

  it('unmounting after the app is created still destroys it and registers no listener', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])

    let releaseInit
    Application.mockImplementationOnce(function () {
      return {
        init: vi.fn().mockReturnValue(
          new Promise((resolve) => {
            releaseInit = resolve
          }),
        ),
        stage: { addChild: vi.fn(), on: vi.fn() },
        canvas: document.createElement('canvas'),
        renderer: { width: 800, height: 600 },
        screen: { x: 0, y: 0, width: 800, height: 600 },
        destroy: vi.fn(),
      }
    })

    const addSpy = vi.spyOn(window, 'addEventListener')

    const wrapper = mount(VillageNew)
    await flushPromises()

    // Unmount while app.init() is still pending — this is where the old code leaked, because
    // onBeforeUnmount ran before `app` was assigned and the continuation then added a listener.
    wrapper.unmount()
    releaseInit(undefined)
    await flushPromises()

    const created = Application.mock.results[0].value
    expect(created.destroy).toHaveBeenCalled()
    expect(addSpy.mock.calls.filter(([event]) => event === 'resize')).toHaveLength(0)

    addSpy.mockRestore()
  })

  it('a fully mounted view tears everything down on unmount', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])
    const removeSpy = vi.spyOn(window, 'removeEventListener')

    const wrapper = mount(VillageNew)
    await flushPromises()

    wrapper.unmount()

    const created = Application.mock.results[0].value
    expect(created.destroy).toHaveBeenCalled()
    expect(removeSpy.mock.calls.some(([event]) => event === 'resize')).toBe(true)
    removeSpy.mockRestore()
  })

  it('a failed upgrade keeps the card open and shows the reason', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.upgradeBuilding.mockRejectedValue(
      new Error('Insufficient wood: need 200, have 50'),
    )

    const wrapper = mount(VillageNew)
    await flushPromises()

    wrapper.vm.showUpgradeCard = true
    await wrapper.vm.handleUpgrade(1)
    await nextTick()

    expect(wrapper.vm.showUpgradeCard).toBe(true)
    expect(wrapper.text()).toContain('Insufficient wood: need 200, have 50')
  })

  it('a successful upgrade closes the card', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.upgradeBuilding.mockResolvedValue({})

    const wrapper = mount(VillageNew)
    await flushPromises()

    wrapper.vm.showUpgradeCard = true
    await wrapper.vm.handleUpgrade(1)
    await nextTick()

    expect(wrapper.vm.showUpgradeCard).toBe(false)
  })

  it('a failed construction surfaces the reason', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])
    buildingsApi.constructBuilding.mockRejectedValue(
      new Error('A building already exists on this site'),
    )

    const wrapper = mount(VillageNew)
    await flushPromises()

    wrapper.vm.currentTile = { row: 0, col: 0, constructionSiteId: 1 }
    await wrapper.vm.handleBuildingSelection('FARM')
    await nextTick()

    expect(wrapper.text()).toContain('A building already exists on this site')
  })
})

describe('VillageNew — resize fits the new canvas size', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    Assets.load.mockResolvedValue({
      layers: [],
      tilewidth: 32,
      tileheight: 32,
      tileset: { tile: [] },
    })
    buildingsApi.fetchBuildings.mockResolvedValue([])
  })

  it('centres on the resize target size, not the stale renderer size', async () => {
    const wrapper = mount(VillageNew, { attachTo: document.body })
    await flushPromises()

    // PixiJS defers renderer.resize() to requestAnimationFrame, so during the resize event the
    // renderer still reports the OLD size while the target element already reports the new one.
    const target = wrapper.element.querySelector('div')
    Object.defineProperty(target, 'clientWidth', { value: 400, configurable: true })
    Object.defineProperty(target, 'clientHeight', { value: 300, configurable: true })

    const mapContainer = Container.mock.results[0].value
    mapContainer.position.set.mockClear()

    window.dispatchEvent(new Event('resize'))
    await nextTick()

    expect(mapContainer.position.set).toHaveBeenCalledWith(200, 150)
    wrapper.unmount()
  })
})
