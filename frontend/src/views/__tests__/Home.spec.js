import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import Home from '../Home.vue'
import { useArmyStore } from '@/stores/army.js'
import { useResourceStore } from '@/stores/resources.js'
import { useTrainingStore } from '@/stores/training.js'
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
  username: 'alice',
  villages: [{ id: 1, xCoordinate: 4, yCoordinate: 7 }],
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

describe('Home — header and session state', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.setItem('villageId', '1')
    localStorage.setItem('jwt_token', 'fake-token')
    apiModule.apiRequest.mockResolvedValue(mockVillageResponse)
    unitsApi.fetchRoster.mockResolvedValue([])
    unitsApi.fetchTrainingQueue.mockResolvedValue([])
  })

  afterEach(() => {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('villageId')
  })

  it('shows the authenticated username returned by /user', async () => {
    const wrapper = await mountHome('/')
    await flushPromises()

    expect(wrapper.text()).toContain('alice')
  })

  it('identifies the current village by its coordinates', async () => {
    const wrapper = await mountHome('/')
    await flushPromises()

    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('7')
  })

  it('falls back to the stored village id while the current village id is still 0', async () => {
    // /user has not resolved yet, so currentVillage.id is its initial 0 — `??` would keep
    // that 0 and route the player to /village/0.
    apiModule.apiRequest.mockReturnValue(new Promise(() => {}))
    localStorage.setItem('villageId', '99')

    const wrapper = await mountHome('/')
    await nextTick()

    const villageLink = wrapper.findAll('a').find((a) => a.text().includes('Village'))
    expect(villageLink.attributes('href')).toContain('99')
  })

  it('does not throw when no village id is known at all', async () => {
    apiModule.apiRequest.mockReturnValue(new Promise(() => {}))
    localStorage.removeItem('villageId')

    const wrapper = await mountHome('/')
    await nextTick()

    const villageLink = wrapper.findAll('a').find((a) => a.text().includes('Village'))
    await villageLink.trigger('click')
    await flushPromises()

    expect(wrapper.exists()).toBe(true)
  })

  it('renders unimplemented menu entries as unavailable instead of wiring a missing handler', async () => {
    const wrapper = await mountHome('/')
    await flushPromises()

    const reports = wrapper.findAll('li').find((li) => li.text().includes('Reports'))
    expect(reports.html()).toContain('menu-disabled')
    await expect(reports.find('a').trigger('click')).resolves.not.toThrow()
  })

  it('resets the army roster and resource totals on logout', async () => {
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 3, hp: 4, damage: 1 }])

    const wrapper = await mountHome('/')
    await flushPromises()

    const armyStore = useArmyStore()
    const resourceStore = useResourceStore()
    resourceStore.food = 500
    expect(armyStore.roster).toHaveLength(1)

    await wrapper.find('[data-testid="logout-button"]').trigger('click')
    await flushPromises()

    expect(armyStore.roster).toEqual([])
    expect(resourceStore.food).toBe(0)
  })

  it('a failing roster fetch does not stop the training store from hydrating', async () => {
    unitsApi.fetchRoster.mockRejectedValue(new Error('boom'))

    await mountHome('/')
    await flushPromises()

    expect(unitsApi.fetchTrainingQueue).toHaveBeenCalledWith(1)
  })
})

describe('Home — training store session wiring', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.setItem('villageId', '1')
    localStorage.setItem('jwt_token', 'fake-token')
    apiModule.apiRequest.mockResolvedValue(mockVillageResponse)
    unitsApi.fetchRoster.mockResolvedValue([])
    unitsApi.fetchTrainingQueue.mockResolvedValue([])
  })

  afterEach(() => {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('villageId')
  })

  it('hydrates the training store with the current village once it is loaded', async () => {
    await mountHome('/')
    await flushPromises()

    // Home is the authenticated layout, so hydrating here keeps the countdown
    // alive for the whole session regardless of the active route.
    expect(unitsApi.fetchTrainingQueue).toHaveBeenCalledWith(1)
  })

  it('stops the training store on logout so nothing leaks into the next session', async () => {
    const wrapper = await mountHome('/')
    await flushPromises()

    const trainingStore = useTrainingStore()
    trainingStore.setOrders([
      {
        id: 1,
        unitType: 'VANDAL',
        buildingId: 10,
        quantity: 1,
        queuePosition: 1,
        finishesAt: new Date(Date.now() + 60000).toISOString(),
      },
    ])
    expect(trainingStore.orders).toHaveLength(1)

    await wrapper.find('[data-testid="logout-button"]').trigger('click')
    await flushPromises()

    expect(trainingStore.orders).toEqual([])
  })
})
