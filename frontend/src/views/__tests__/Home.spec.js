import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import Home from '../Home.vue'
import { useArmyStore } from '@/stores/army.js'
import * as unitsApi from '@/util/api/units.js'
import * as apiModule from '@/util/api/api.js'

vi.mock('@/util/api/units.js', () => ({
  fetchRoster: vi.fn(),
  fetchTrainingQueue: vi.fn().mockResolvedValue([]),
  trainUnit: vi.fn(),
}))

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn().mockResolvedValue({ food: 0, wood: 0, bricks: 0, iron: 0 }),
}))

vi.mock('@/util/api/api.js', () => ({
  apiRequest: vi.fn(),
}))

vi.mock('@/components/Avatar.vue', () => ({
  default: { template: '<div></div>' },
}))

const mockVillageResponse = {
  villages: [{ id: 1, name: 'Test Village' }],
}

function makeRouter(currentPath = '/') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/',
        component: { template: '<div><RouterView /></div>' },
        children: [
          { path: '', redirect: '/village' },
          { path: 'village/:villageId?', name: 'Village', component: { template: '<div></div>' } },
          { path: 'army', name: 'army', component: { template: '<div>army</div>' } },
          { path: 'map', name: 'world', component: { template: '<div>map</div>' } },
        ],
      },
      { path: '/army', component: { template: '<div>army</div>' } },
      { path: '/login', component: { template: '<div></div>' } },
    ],
  })
  router.push(currentPath)
  return router
}

async function mountHome(currentPath = '/') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = makeRouter(currentPath)
  await router.isReady()

  const wrapper = mount(Home, {
    global: {
      plugins: [pinia, router],
      stubs: { RouterView: { template: '<div></div>' } },
    },
  })
  return wrapper
}

describe('Home — army mini-panel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.setItem('villageId', '1')
    localStorage.setItem('jwt_token', 'fake-token')
    apiModule.apiRequest.mockResolvedValue(mockVillageResponse)
  })

  afterEach(() => {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('villageId')
  })

  it('mini-panel is hidden when roster is empty', async () => {
    unitsApi.fetchRoster.mockResolvedValue([])

    const wrapper = await mountHome('/')
    await flushPromises()

    expect(wrapper.find('[data-testid="army-mini-panel"]').exists()).toBe(false)
  })

  it('mini-panel is hidden when on /army route', async () => {
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 3, hp: 4, damage: 1 }])

    const wrapper = await mountHome('/army')
    await flushPromises()

    expect(wrapper.find('[data-testid="army-mini-panel"]').exists()).toBe(false)
  })

  it('mini-panel is visible when roster has units and not on /army', async () => {
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 3, hp: 4, damage: 1 }])

    const wrapper = await mountHome('/')
    await flushPromises()

    const panel = wrapper.find('[data-testid="army-mini-panel"]')
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain('VANDAL')
    expect(panel.text()).toContain('× 3')
  })

  it('mini-panel appears with updated count when army store is refreshed after training completes', async () => {
    // Verifies that when training finishes and the army store is refreshed from
    // elsewhere (e.g. the barrack countdown), Home.vue reacts without any user action.
    unitsApi.fetchRoster.mockResolvedValue([])

    const wrapper = await mountHome('/')
    await flushPromises()

    // No units yet — panel hidden
    expect(wrapper.find('[data-testid="army-mini-panel"]').exists()).toBe(false)

    // Training completes — army store updated by the countdown timer
    const armyStore = useArmyStore()
    armyStore.roster = [{ unitType: 'VANDAL', count: 5, hp: 4, damage: 1 }]
    await nextTick()

    const panel = wrapper.find('[data-testid="army-mini-panel"]')
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain('VANDAL')
    expect(panel.text()).toContain('× 5')
  })
})
