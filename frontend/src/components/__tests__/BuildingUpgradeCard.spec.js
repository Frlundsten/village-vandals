import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import BuildingUpgradeCard from '../BuildingUpgradeCard.vue'
import { useResourceStore } from '@/stores/resources.js'
import { useArmyStore } from '@/stores/army.js'
import * as unitsApi from '@/util/api/units.js'

vi.mock('@/util/api/units.js', () => ({
  trainUnit: vi.fn(),
  fetchTrainingQueue: vi.fn().mockResolvedValue([]),
  fetchRoster: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn().mockResolvedValue({ food: 0, wood: 0, bricks: 0, iron: 0 }),
}))

const barracks = { type: 'BARRACK', level: 1, buildingId: 10, constructionSiteId: 1 }
const farmBuilding = {
  type: 'FARM',
  level: 1,
  buildingId: 5,
  constructionSiteId: 2,
  upgradeCost: { food: 100, wood: 50 },
  productionPerHour: 1800,
}

function makeOrder(overrides = {}) {
  return {
    id: 1,
    unitType: 'VANDAL',
    buildingId: 10,
    finishesAt: new Date(Date.now() + 3000).toISOString(),
    quantity: 1,
    queuePosition: 1,
    ...overrides,
  }
}

describe('BuildingUpgradeCard — training queue section', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('renders queue section with countdown when orders exist', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([makeOrder()])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').text()).toMatch(/\d+\.\d+s/)
  })

  it('renders queue rows with a quantity badge and a single countdown sized to the batch duration', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      makeOrder({ id: 1, quantity: 12, queuePosition: 1, finishesAt: new Date(Date.now() + 60000).toISOString() }),
    ])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
    await flushPromises()

    const activeRow = wrapper.find('[data-testid="training-queue"]')
    expect(activeRow.text()).toContain('×12')
    expect(wrapper.findAll('[data-testid="countdown"]')).toHaveLength(1)
    const progress = wrapper.find('progress')
    expect(Number(progress.attributes('max'))).toBe(12 * 5000)
  })

  it('renders queued orders with "ready in Xs" text for queuePosition > 1', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      makeOrder({ id: 1, queuePosition: 1, finishesAt: new Date(Date.now() + 3000).toISOString() }),
      makeOrder({ id: 2, queuePosition: 2, finishesAt: new Date(Date.now() + 8000).toISOString() }),
    ])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
    await flushPromises()

    const queuedOrders = wrapper.findAll('[data-testid="queued-order"]')
    expect(queuedOrders).toHaveLength(1)
    expect(queuedOrders[0].text()).toContain('ready in')
  })

  it('queue panel renders and stays visible on mount when backend clock lags client clock', async () => {
    // Backend clock lags the client clock by 70s. finishesAt looks "in the past" against
    // raw Date.now(), but is ~5s in the future relative to serverTime.
    const now = Date.now()
    const serverTime = new Date(now - 70000).toISOString()
    const finishesAt = new Date(now - 70000 + 5000).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([makeOrder({ finishesAt, serverTime, queuePosition: 1 })])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(parseFloat(wrapper.find('[data-testid="countdown"]').text())).toBeGreaterThan(4)

    // Queue must not disappear on the next countdown tick
    vi.advanceTimersByTime(100)
    await nextTick()
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
  })

  it('does not render queue section when queue is empty', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(false)
  })

  it('shows upgrade cost section for non-Barrack buildings', async () => {
    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: farmBuilding, villageId: 1, currentResources: { food: 200, wood: 100 } },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Upgrade cost')
  })
})

describe('BuildingUpgradeCard — train action queue render', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    unitsApi.fetchTrainingQueue.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('queue section renders immediately after POST /train response without a second fetch', async () => {
    const finishesAt = new Date(Date.now() + 20000).toISOString()
    unitsApi.trainUnit.mockResolvedValue([
      { id: 42, unitType: 'VANDAL', buildingId: 10, finishesAt, quantity: 4, queuePosition: 1 },
    ])

    const store = useResourceStore()
    store.food = 200
    store.iron = 120

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 120 } },
    })
    await flushPromises()

    // Queue is empty before training
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(false)

    await wrapper.find('[data-testid="train-vandal-button"]').trigger('click')
    await flushPromises()

    // Queue renders immediately from POST response — no second fetchTrainingQueue needed
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').exists()).toBe(true)
    // fetchTrainingQueue was only called once (on mount), not again after training
    expect(unitsApi.fetchTrainingQueue).toHaveBeenCalledTimes(1)
  })

  it('queue stays visible when the mount-time fetch resolves AFTER Train is clicked', async () => {
    // Simulate a slow mount-time GET that is still in-flight when the user clicks Train.
    // Without protection, the GET response (empty queue) will overwrite the POST response
    // and erase the countdown the user just triggered.
    let resolveMountFetch
    unitsApi.fetchTrainingQueue.mockReturnValueOnce(
      new Promise((resolve) => { resolveMountFetch = resolve }),
    )

    const finishesAt = new Date(Date.now() + 20000).toISOString()
    unitsApi.trainUnit.mockResolvedValue([
      makeOrder({ finishesAt, quantity: 5, queuePosition: 1 }),
    ])

    const store = useResourceStore()
    store.food = 1000
    store.iron = 1000

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 1000, iron: 1000 } },
    })
    // Mount-time GET is still pending — do NOT flush promises here

    await wrapper.find('[data-testid="train-vandal-button"]').trigger('click')
    await flushPromises()

    // Queue shows from POST response
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)

    // Now the stale mount-time GET resolves with an empty queue (pre-dates the POST)
    resolveMountFetch([])
    await flushPromises()

    // Queue must still be visible — stale GET must not erase the countdown
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').exists()).toBe(true)
  })

  it('queue remains visible with a corrected countdown when backend clock lags client clock', async () => {
    // Backend clock lags the client clock by 70s. finishesAt looks "in the past" against
    // raw Date.now(), but is ~5s in the future relative to serverTime.
    const now = Date.now()
    const serverTime = new Date(now - 70000).toISOString()
    const finishesAt = new Date(now - 70000 + 5000).toISOString()

    unitsApi.trainUnit.mockResolvedValue([
      makeOrder({ finishesAt, serverTime, queuePosition: 1 }),
    ])

    const store = useResourceStore()
    store.food = 200
    store.iron = 120

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 120 } },
    })
    await flushPromises()

    await wrapper.find('[data-testid="train-vandal-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(parseFloat(wrapper.find('[data-testid="countdown"]').text())).toBeGreaterThan(4)

    // Queue must not disappear on the next countdown tick
    vi.advanceTimersByTime(100)
    await nextTick()
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
  })

  it('fetches the army roster when the training countdown reaches zero', async () => {
    const finishesAt = new Date(Date.now() + 50).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([makeOrder({ finishesAt })])
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 1, hp: 4, damage: 1 }])

    const wrapper = mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 100, iron: 100 } },
    })
    await flushPromises()

    // Advance timers past the finishesAt
    vi.advanceTimersByTime(200)
    await nextTick()

    // fetchRoster must be called with the village id to trigger lazy promotion
    // and update the shared army store so army tab + mini-panel refresh
    expect(unitsApi.fetchRoster).toHaveBeenCalledWith(1)
  })
})

describe('BuildingUpgradeCard — bulk training quantity', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    unitsApi.fetchTrainingQueue.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  function mountBarrackCard() {
    return mount(BuildingUpgradeCard, {
      props: { building: barracks, villageId: 1, currentResources: { food: 200, iron: 100 } },
    })
  }

  it('renders a quantity input defaulting to 1 with increment/decrement controls, clamped to [0, 999]', async () => {
    const wrapper = mountBarrackCard()
    await flushPromises()

    const input = wrapper.find('[data-testid="train-quantity-input"]')
    expect(input.exists()).toBe(true)
    expect(Number(input.element.value)).toBe(1)

    await wrapper.find('[data-testid="quantity-decrement"]').trigger('click')
    await wrapper.find('[data-testid="quantity-decrement"]').trigger('click')
    expect(Number(wrapper.find('[data-testid="train-quantity-input"]').element.value)).toBe(0)

    await input.setValue(5000)
    expect(Number(wrapper.find('[data-testid="train-quantity-input"]').element.value)).toBe(999)
  })

  it('scales the resource cost preview live with the chosen quantity', async () => {
    const wrapper = mountBarrackCard()
    await flushPromises()

    const input = wrapper.find('[data-testid="train-quantity-input"]')
    await input.setValue(10)

    expect(wrapper.find('[data-testid="train-food-cost"]').text()).toContain('500')
    expect(wrapper.find('[data-testid="train-iron-cost"]').text()).toContain('300')
  })

  it('disables the Train button when the full batch is unaffordable, without auto-correcting the quantity', async () => {
    const store = useResourceStore()
    store.food = 200
    store.iron = 100

    const wrapper = mountBarrackCard()
    await flushPromises()

    const input = wrapper.find('[data-testid="train-quantity-input"]')
    const button = wrapper.find('[data-testid="train-vandal-button"]')

    // 4 Vandals cost 200 food / 120 iron — affordable on food, not on iron
    await input.setValue(4)
    expect(button.attributes('disabled')).toBeDefined()
    expect(Number(input.element.value)).toBe(4)

    // 2 Vandals cost 100 food / 60 iron — affordable
    await input.setValue(2)
    expect(button.attributes('disabled')).toBeUndefined()
  })
})
