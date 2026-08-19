import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { useTrainingQueue } from '../useTrainingQueue.js'
import * as unitsApi from '@/util/api/units.js'

const mockArmyRefresh = vi.hoisted(() => vi.fn().mockResolvedValue(undefined))

vi.mock('@/util/api/units.js', () => ({
  fetchTrainingQueue: vi.fn(),
}))

vi.mock('@/stores/army.js', () => ({
  useArmyStore: () => ({ refresh: mockArmyRefresh }),
}))

function mountComposable(villageId) {
  let composableResult
  const TestComponent = defineComponent({
    setup() {
      composableResult = useTrainingQueue(villageId)
      return composableResult
    },
    template: '<div></div>',
  })
  const wrapper = mount(TestComponent)
  return { wrapper, getResult: () => composableResult }
}

describe('useTrainingQueue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
    mockArmyRefresh.mockClear()
  })

  it('restores countdown from server finishesAt timestamp on mount (not reset to 5s)', async () => {
    const finishesAt = new Date(Date.now() + 3000).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      { id: 1, unitType: 'VANDAL', buildingId: 10, finishesAt, queuePosition: 1 },
    ])

    const { getResult } = mountComposable(1)
    await flushPromises()

    const order = getResult().queue.value[0]
    expect(order.remainingMs).toBeGreaterThan(2800)
    expect(order.remainingMs).toBeLessThanOrEqual(3000)
  })

  it('hasActiveFor returns true when a pending order has the given buildingId', async () => {
    const finishesAt = new Date(Date.now() + 5000).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      { id: 3, unitType: 'VANDAL', buildingId: 42, finishesAt, queuePosition: 1 },
    ])

    const { getResult } = mountComposable(1)
    await flushPromises()

    expect(getResult().hasActiveFor(42)).toBe(true)
    expect(getResult().hasActiveFor(99)).toBe(false)
  })

  it('hasActiveFor remains true under clock skew until the corrected countdown elapses', async () => {
    // Backend clock lags the client clock by 70s. finishesAt looks "in the past" against
    // raw Date.now(), but is ~5s in the future relative to serverTime.
    const now = Date.now()
    const serverTime = new Date(now - 70000).toISOString()
    const finishesAt = new Date(now - 70000 + 5000).toISOString()

    unitsApi.fetchTrainingQueue.mockResolvedValue([
      { id: 1, unitType: 'VANDAL', buildingId: 42, finishesAt, serverTime, queuePosition: 1 },
    ])

    const { getResult } = mountComposable(1)
    await flushPromises()

    expect(getResult().hasActiveFor(42)).toBe(true)

    // One tick must not immediately drop the order due to clock skew
    vi.advanceTimersByTime(100)
    await nextTick()
    expect(getResult().hasActiveFor(42)).toBe(true)

    // Once the corrected duration elapses, the order is removed
    vi.advanceTimersByTime(5000)
    await nextTick()
    expect(getResult().hasActiveFor(42)).toBe(false)
  })

  it('hasActiveFor returns false when queue is empty', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([])

    const { getResult } = mountComposable(1)
    await flushPromises()

    expect(getResult().hasActiveFor(10)).toBe(false)
  })

  it('setQueue populates queue immediately with enriched orders (remainingMs from finishesAt)', () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([])

    const { getResult } = mountComposable(1)

    const finishesAt = new Date(Date.now() + 8000).toISOString()
    getResult().setQueue([
      { id: 7, unitType: 'VANDAL', buildingId: 10, finishesAt, quantity: 3, queuePosition: 1 },
    ])

    expect(getResult().queue.value).toHaveLength(1)
    expect(getResult().queue.value[0].id).toBe(7)
    expect(getResult().queue.value[0].remainingMs).toBeGreaterThan(7800)
    expect(getResult().queue.value[0].remainingMs).toBeLessThanOrEqual(8000)
  })

  it('tick removes expired first order without calling fetchRoster', async () => {
    const finishesAt = new Date(Date.now() + 50).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      { id: 5, unitType: 'VANDAL', buildingId: 10, finishesAt, queuePosition: 1 },
    ])

    const { getResult } = mountComposable(1)
    await flushPromises()

    vi.advanceTimersByTime(200)
    await nextTick()

    expect(getResult().queue.value).toHaveLength(0)
    // Only the initial mount fetch — no extra network call triggered by the timer
    expect(unitsApi.fetchTrainingQueue).toHaveBeenCalledTimes(1)
  })

  it('tick_callsArmyStoreRefresh_whenFirstOrderExpires', async () => {
    const finishesAt = new Date(Date.now() + 50).toISOString()
    unitsApi.fetchTrainingQueue.mockResolvedValue([
      { id: 5, unitType: 'VANDAL', buildingId: 10, finishesAt, queuePosition: 1 },
    ])

    const { getResult } = mountComposable(1)
    await flushPromises()

    vi.advanceTimersByTime(200)
    await nextTick()

    expect(mockArmyRefresh).toHaveBeenCalledWith(1)
    expect(mockArmyRefresh).toHaveBeenCalledTimes(1)
  })

  it('refresh overwrites queue unconditionally even after setQueue was called', async () => {
    let resolveRefresh
    const deferredFetch = new Promise((resolve) => {
      resolveRefresh = resolve
    })
    unitsApi.fetchTrainingQueue.mockReturnValueOnce(deferredFetch)

    const { getResult } = mountComposable(1)

    const freshFinishesAt = new Date(Date.now() + 25000).toISOString()
    getResult().setQueue([
      { id: 99, unitType: 'VANDAL', buildingId: 10, finishesAt: freshFinishesAt, quantity: 5, queuePosition: 1 },
    ])
    expect(getResult().queue.value).toHaveLength(1)

    // Mount-time refresh resolves with empty — should overwrite the setQueue result
    resolveRefresh([])
    await flushPromises()

    expect(getResult().queue.value).toHaveLength(0)
  })

  it('clears interval on unmount', async () => {
    unitsApi.fetchTrainingQueue.mockResolvedValue([])
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')

    const { wrapper } = mountComposable(1)
    await flushPromises()
    wrapper.unmount()

    expect(clearIntervalSpy).toHaveBeenCalled()
  })
})
