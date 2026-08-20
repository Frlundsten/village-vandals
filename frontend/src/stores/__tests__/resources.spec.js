import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useResourceStore } from '@/stores/resources.js'

vi.mock('@/util/api/resources.js', () => ({
  refreshStorage: vi.fn(),
}))

import { refreshStorage } from '@/util/api/resources.js'

/** A server payload with every rate at `rate` unless overridden. */
function payload(overrides = {}) {
  return {
    food: 100,
    wood: 100,
    bricks: 100,
    iron: 100,
    foodPerHour: 0,
    woodPerHour: 0,
    bricksPerHour: 0,
    ironPerHour: 0,
    ...overrides,
  }
}

describe('useResourceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('initialises all amounts and rates to 0', () => {
    const store = useResourceStore()
    expect(store.food).toBe(0)
    expect(store.wood).toBe(0)
    expect(store.bricks).toBe(0)
    expect(store.iron).toBe(0)
    expect(store.foodPerHour).toBe(0)
    expect(store.woodPerHour).toBe(0)
    expect(store.bricksPerHour).toBe(0)
    expect(store.ironPerHour).toBe(0)
  })

  it('refresh() updates amounts and production rates from API response', async () => {
    refreshStorage.mockResolvedValue({
      food: 100,
      wood: 200,
      bricks: 300,
      iron: 400,
      foodPerHour: 10,
      woodPerHour: 20,
      bricksPerHour: 30,
      ironPerHour: 40,
    })

    const store = useResourceStore()
    await store.refresh(1)

    expect(store.food).toBe(100)
    expect(store.wood).toBe(200)
    expect(store.bricks).toBe(300)
    expect(store.iron).toBe(400)
    expect(store.foodPerHour).toBe(10)
    expect(store.woodPerHour).toBe(20)
    expect(store.bricksPerHour).toBe(30)
    expect(store.ironPerHour).toBe(40)

    store.reset()
  })

  // --- local accrual between syncs ---

  it('advances amounts according to the per-hour rate as time passes', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ foodPerHour: 3600 })) // 1 food/second

    const store = useResourceStore()
    await store.refresh(1)
    expect(store.food).toBe(100)

    await vi.advanceTimersByTimeAsync(5000)

    expect(store.food).toBe(105)
    store.reset()
  })

  it('carries the fractional remainder so sub-unit rates are not lost', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ woodPerHour: 1800 })) // 0.5 wood/second

    const store = useResourceStore()
    await store.refresh(1)

    await vi.advanceTimersByTimeAsync(1000)
    expect(store.wood).toBe(100) // 0.5 accrued, nothing credited yet

    await vi.advanceTimersByTimeAsync(1000)
    expect(store.wood).toBe(101) // the carried 0.5 completes a unit

    store.reset()
  })

  it('credits the real elapsed time when ticks are delayed', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ ironPerHour: 3600 })) // 1 iron/second

    const store = useResourceStore()
    await store.refresh(1)

    // Simulate a throttled background tab: wall clock moves on, the interval does not fire.
    vi.setSystemTime(Date.now() + 60_000)
    await vi.advanceTimersByTimeAsync(1000)

    expect(store.iron).toBe(161) // 100 + 61 seconds of production, not 100 + 1
    store.reset()
  })

  it('leaves an amount untouched when its rate is zero', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ foodPerHour: 3600 }))

    const store = useResourceStore()
    await store.refresh(1)

    await vi.advanceTimersByTimeAsync(10_000)

    expect(store.bricks).toBe(100)
    store.reset()
  })

  it('refresh() overwrites a locally projected amount with the server value', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ foodPerHour: 3600 }))

    const store = useResourceStore()
    await store.refresh(1)
    await vi.advanceTimersByTimeAsync(10_000)
    expect(store.food).toBe(110)

    // The server is authoritative — e.g. the player just spent food.
    refreshStorage.mockResolvedValue(payload({ food: 40, foodPerHour: 3600 }))
    await store.refresh(1)

    expect(store.food).toBe(40)

    // and accrual restarts from the sync point rather than replaying the old elapsed time
    await vi.advanceTimersByTimeAsync(2000)
    expect(store.food).toBe(42)

    store.reset()
  })

  it('reset() zeroes everything and stops accruing', async () => {
    vi.useFakeTimers()
    refreshStorage.mockResolvedValue(payload({ foodPerHour: 3600 }))

    const store = useResourceStore()
    await store.refresh(1)

    store.reset()

    expect(store.food).toBe(0)
    expect(store.foodPerHour).toBe(0)

    await vi.advanceTimersByTimeAsync(10_000)
    expect(store.food).toBe(0)
  })
})
