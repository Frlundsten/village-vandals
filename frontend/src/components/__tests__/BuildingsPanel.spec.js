import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BuildingsPanel from '../BuildingsPanel.vue'
import * as buildingsApi from '@/util/api/buildings.js'
import * as resourcesApi from '@/util/api/resources.js'

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn().mockResolvedValue({
    food: 0,
    wood: 0,
    bricks: 0,
    iron: 0,
    foodPerHour: 0,
    woodPerHour: 0,
    bricksPerHour: 0,
    ironPerHour: 0,
  }),
}))

vi.mock('@/util/api/buildings.js', () => ({
  fetchBuildings: vi.fn(),
  upgradeBuilding: vi.fn(),
  constructBuilding: vi.fn(),
  getAvailableBuildings: vi.fn(),
}))

const LUMBERMILL = {
  constructionSiteId: 1,
  buildingId: 10,
  type: 'LUMBERMILL',
  level: 1,
  upgradeCost: { wood: 200, bricks: 200, food: 200, iron: 200 },
  productionPerHour: 18000,
}

const FARM = {
  constructionSiteId: 2,
  buildingId: 11,
  type: 'FARM',
  level: 1,
  upgradeCost: { wood: 200, bricks: 200, food: 200, iron: 200 },
  productionPerHour: 18000,
}

describe('BuildingsPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.setItem('villageId', '42')
    buildingsApi.fetchBuildings.mockResolvedValue([LUMBERMILL, FARM])
  })

  it('renders a row for each building', async () => {
    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    expect(wrapper.findAll('[data-testid="building-row"]')).toHaveLength(2)
  })

  it('shows an error rather than the empty state when the buildings fetch fails', async () => {
    buildingsApi.fetchBuildings.mockRejectedValue(new Error('Not the owner of village 42'))
    const unhandled = vi.fn()
    process.on('unhandledRejection', unhandled)

    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    expect(wrapper.text()).toContain('Not the owner of village 42')
    expect(wrapper.text()).not.toContain('No buildings yet')
    expect(unhandled).not.toHaveBeenCalled()
    process.off('unhandledRejection', unhandled)
  })

  it('still shows the empty state when the server returns no buildings', async () => {
    buildingsApi.fetchBuildings.mockResolvedValue([])

    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    expect(wrapper.text()).toContain('No buildings yet')
  })

  it('disables upgrade button when resources are insufficient', async () => {
    // resource store defaults to 0 for all — upgrade costs 200 each
    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    const buttons = wrapper.findAll('[data-testid="upgrade-btn"]')
    expect(buttons[0].attributes('disabled')).toBeDefined()
  })

  it('enables upgrade button when resources are sufficient', async () => {
    // The panel syncs resources on mount, so affordability comes from the server payload.
    resourcesApi.refreshStorage.mockResolvedValue({
      food: 500,
      wood: 500,
      bricks: 500,
      iron: 500,
      foodPerHour: 0,
      woodPerHour: 0,
      bricksPerHour: 0,
      ironPerHour: 0,
    })

    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    const buttons = wrapper.findAll('[data-testid="upgrade-btn"]')
    expect(buttons[0].attributes('disabled')).toBeUndefined()
  })
})
