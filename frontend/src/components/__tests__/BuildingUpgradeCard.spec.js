import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import BuildingUpgradeCard from '../BuildingUpgradeCard.vue'
import { MAX_TRAINING_BATCH_SIZE, MIN_TRAINING_BATCH_SIZE } from '@/util/gameConfig.js'
import { useResourceStore } from '@/stores/resources.js'
import { useTrainingStore } from '@/stores/training.js'
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

/** Seed the shared training store directly, as a train response would. */
function seedQueue(orders) {
  const store = useTrainingStore()
  store.setOrders(orders)
  return store
}

/** Seed the store the way Home.vue does, so it also caches the village id. */
async function hydrateQueue(orders, villageId = 1) {
  unitsApi.fetchTrainingQueue.mockResolvedValueOnce(orders)
  const store = useTrainingStore()
  await store.hydrate(villageId)
  return store
}

function mountCard(building = barracks, currentResources = { food: 200, iron: 100 }) {
  return mount(BuildingUpgradeCard, {
    props: { building, villageId: 1, currentResources },
  })
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
    seedQueue([makeOrder()])

    const wrapper = mountCard()
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').text()).toMatch(/\d+\.\d+s/)
  })

  it('renders queue rows with a quantity badge and a single countdown sized to the batch duration', async () => {
    seedQueue([
      makeOrder({
        id: 1,
        quantity: 12,
        queuePosition: 1,
        finishesAt: new Date(Date.now() + 60000).toISOString(),
      }),
    ])

    const wrapper = mountCard()
    await flushPromises()

    const activeRow = wrapper.find('[data-testid="training-queue"]')
    expect(activeRow.text()).toContain('×12')
    expect(wrapper.findAll('[data-testid="countdown"]')).toHaveLength(1)
    const progress = wrapper.find('progress')
    expect(Number(progress.attributes('max'))).toBe(12 * 5000)
  })

  it('renders later orders as queued rows with "ready in Xs" text', async () => {
    seedQueue([
      makeOrder({ id: 1, queuePosition: 1, finishesAt: new Date(Date.now() + 3000).toISOString() }),
      makeOrder({ id: 2, queuePosition: 2, finishesAt: new Date(Date.now() + 8000).toISOString() }),
    ])

    const wrapper = mountCard()
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
    seedQueue([makeOrder({ finishesAt, serverTime, queuePosition: 1 })])

    const wrapper = mountCard()
    await flushPromises()

    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(parseFloat(wrapper.find('[data-testid="countdown"]').text())).toBeGreaterThan(4)

    // Queue must not disappear on the next countdown tick
    vi.advanceTimersByTime(100)
    await nextTick()
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
  })

  it('does not render queue section when queue is empty', async () => {
    const wrapper = mountCard()
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
    // The card never fetches the queue itself — the store owns it
    expect(unitsApi.fetchTrainingQueue).not.toHaveBeenCalled()
  })

  it('renders the queue from the store on open, without fetching the training queue', async () => {
    seedQueue([makeOrder({ quantity: 3 })])

    const wrapper = mountCard()
    await flushPromises()

    // The store is already hydrated, so the card shows the live queue straight away
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="countdown"]').exists()).toBe(true)
    expect(unitsApi.fetchTrainingQueue).not.toHaveBeenCalled()
  })

  it('shows only the orders belonging to this building', async () => {
    seedQueue([
      makeOrder({ id: 1, buildingId: 10, finishesAt: new Date(Date.now() + 3000).toISOString() }),
      makeOrder({ id: 2, buildingId: 99, finishesAt: new Date(Date.now() + 8000).toISOString() }),
    ])

    const wrapper = mountCard()
    await flushPromises()

    // Only barracks 10's order — the other barrack's order must not appear here
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="queued-order"]')).toHaveLength(0)
  })

  it('hands the POST /train response to the training store', async () => {
    const finishesAt = new Date(Date.now() + 20000).toISOString()
    unitsApi.trainUnit.mockResolvedValue([
      { id: 42, unitType: 'VANDAL', buildingId: 10, finishesAt, quantity: 4, queuePosition: 1 },
    ])

    const resources = useResourceStore()
    resources.food = 1000
    resources.iron = 1000
    const trainingStore = useTrainingStore()

    const wrapper = mountCard()
    await flushPromises()

    await wrapper.find('[data-testid="train-vandal-button"]').trigger('click')
    await flushPromises()

    expect(trainingStore.orders.map((o) => o.id)).toEqual([42])
  })

  it('countdown keeps running after the card is unmounted and still refreshes the roster', async () => {
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 1, hp: 4, damage: 1 }])
    const trainingStore = await hydrateQueue(
      [makeOrder({ finishesAt: new Date(Date.now() + 5000).toISOString() })],
      1,
    )

    const wrapper = mountCard()
    await flushPromises()
    expect(wrapper.find('[data-testid="training-queue"]').exists()).toBe(true)

    // The player closes the building card while training is still running
    wrapper.unmount()

    vi.advanceTimersByTime(5100)
    await flushPromises()

    expect(trainingStore.orders).toHaveLength(0)
    expect(unitsApi.fetchRoster).toHaveBeenCalledWith(1)
  })

  it('queue remains visible with a corrected countdown when backend clock lags client clock', async () => {
    // Backend clock lags the client clock by 70s. finishesAt looks "in the past" against
    // raw Date.now(), but is ~5s in the future relative to serverTime.
    const now = Date.now()
    const serverTime = new Date(now - 70000).toISOString()
    const finishesAt = new Date(now - 70000 + 5000).toISOString()

    unitsApi.trainUnit.mockResolvedValue([makeOrder({ finishesAt, serverTime, queuePosition: 1 })])

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
    unitsApi.fetchRoster.mockResolvedValue([{ unitType: 'VANDAL', count: 1, hp: 4, damage: 1 }])
    await hydrateQueue([makeOrder({ finishesAt })], 1)

    const wrapper = mountCard(barracks, { food: 100, iron: 100 })
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

  it('clamps the quantity to the bounds the server accepts', async () => {
    // The server rejects anything outside [1, MAX_TRAINING_BATCH_SIZE]; the control used to
    // allow [0, 999], so the player could compose a request that could only fail.
    const wrapper = mountBarrackCard()
    await flushPromises()

    const input = wrapper.find('[data-testid="train-quantity-input"]')
    expect(input.exists()).toBe(true)
    expect(Number(input.element.value)).toBe(MIN_TRAINING_BATCH_SIZE)

    await wrapper.find('[data-testid="quantity-decrement"]').trigger('click')
    await wrapper.find('[data-testid="quantity-decrement"]').trigger('click')
    expect(Number(wrapper.find('[data-testid="train-quantity-input"]').element.value)).toBe(
      MIN_TRAINING_BATCH_SIZE,
    )

    await input.setValue(5000)
    expect(Number(wrapper.find('[data-testid="train-quantity-input"]').element.value)).toBe(
      MAX_TRAINING_BATCH_SIZE,
    )
  })

  it('exposes the server bounds on the quantity input itself', async () => {
    const wrapper = mountBarrackCard()
    await flushPromises()

    const input = wrapper.find('[data-testid="train-quantity-input"]')
    expect(input.attributes('min')).toBe(String(MIN_TRAINING_BATCH_SIZE))
    expect(input.attributes('max')).toBe(String(MAX_TRAINING_BATCH_SIZE))
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
