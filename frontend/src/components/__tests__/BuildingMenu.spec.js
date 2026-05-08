import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BuildingMenu from '../BuildingMenu.vue'
import * as buildingsApi from '@/util/api/buildings.js'

vi.mock('@/util/api/buildings.js', () => ({
  getAvailableBuildings: vi.fn().mockResolvedValue([]),
  constructBuilding: vi.fn(),
  fetchBuildings: vi.fn(),
  upgradeBuilding: vi.fn(),
}))

describe('BuildingMenu — sendInfo', () => {
  it('emits buildingType and closeMenu without calling the buildings API', async () => {
    setActivePinia(createPinia())

    const wrapper = mount(BuildingMenu, {
      props: {
        tileInfo: { row: 0, col: 0, constructionSiteId: 1 },
        villageId: 1,
        currentResources: { food: 100, wood: 100, bricks: 100, iron: 100 },
      },
      global: {
        stubs: { BuildingPresentationCard: true },
      },
    })

    await flushPromises()

    wrapper.vm.sendInfo('Farm', { row: 0, col: 0 })

    expect(wrapper.emitted('buildingType')).toEqual([['Farm']])
    expect(wrapper.emitted('closeMenu')).toHaveLength(1)
    expect(buildingsApi.constructBuilding).not.toHaveBeenCalled()
  })
})
