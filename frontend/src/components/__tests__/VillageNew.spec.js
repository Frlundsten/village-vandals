import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import VillageNew from '../VillageNew.vue'
import * as buildingsApi from '@/util/api/buildings.js'
import { Application, Assets, Graphics } from 'pixi.js'

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
      scale: { set: vi.fn() },
      pivot: { set: vi.fn() },
      position: { set: vi.fn() },
      x: 0,
      y: 0,
    }
  }),
  Graphics: vi.fn(function () {
    return {
      beginFill: vi.fn().mockReturnThis(),
      drawRect: vi.fn().mockReturnThis(),
      endFill: vi.fn().mockReturnThis(),
      interactive: false,
      cursor: '',
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
    const moveHandler = graphicsInstance.on.mock.calls
      .find((c) => c[0] === 'globalpointermove')?.[1]

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
    const newBuilding = { constructionSiteId: 2, type: 'Farm' }
    buildingsApi.fetchBuildings.mockResolvedValue([newBuilding])

    await wrapper.vm.handleBuildingSelection('Farm')
    await flushPromises()

    expect(wrapper.vm.buildingsBySiteId.get(2)).toEqual(newBuilding)
  })

  it('calls addBuildingSprite after successful construction', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([{ constructionSiteId: 2, type: 'Farm' }])

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
})
