import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BuildingsPanel from '../BuildingsPanel.vue'
import * as buildingsApi from '@/util/api/buildings.js'
import { useResourceStore } from '@/stores/resources.js'

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

  it('disables upgrade button when resources are insufficient', async () => {
    // resource store defaults to 0 for all — upgrade costs 200 each
    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    const buttons = wrapper.findAll('[data-testid="upgrade-btn"]')
    expect(buttons[0].attributes('disabled')).toBeDefined()
  })

  it('enables upgrade button when resources are sufficient', async () => {
    const resourceStore = useResourceStore()
    resourceStore.wood = 500
    resourceStore.bricks = 500
    resourceStore.food = 500
    resourceStore.iron = 500

    const wrapper = mount(BuildingsPanel)
    await flushPromises()

    const buttons = wrapper.findAll('[data-testid="upgrade-btn"]')
    expect(buttons[0].attributes('disabled')).toBeUndefined()
  })
})
