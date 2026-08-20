import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useTrainingStore } from '@/stores/training.js'
import { fetchTrainingQueue } from '@/util/api/units.js'

const mockArmyRefresh = vi.hoisted(() => vi.fn().mockResolvedValue(undefined))

vi.mock('@/util/api/units.js', () => ({
  fetchTrainingQueue: vi.fn(),
}))

vi.mock('@/stores/army.js', () => ({
  useArmyStore: () => ({ refresh: mockArmyRefresh }),
}))

/** Build a raw TrainingOrderDTO as the backend would return it. */
function order({ id, buildingId = 10, inMs, queuePosition, quantity = 1, serverTime }) {
  return {
    id,
    unitType: 'VANDAL',
    buildingId,
    finishesAt: new Date(Date.now() + inMs).toISOString(),
    quantity,
    queuePosition,
    ...(serverTime ? { serverTime } : {}),
  }
}

/**
 * A store hydrated for a village, as it always is in production — Home.vue hydrates
 * once the current village is known, before any training can be started.
 */
async function hydratedStore(villageId = 1, initial = []) {
  fetchTrainingQueue.mockResolvedValueOnce(initial)
  const store = useTrainingStore()
  await store.hydrate(villageId)
  return store
}

describe('useTrainingStore', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockArmyRefresh.mockClear()
    mockArmyRefresh.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('hydrate', () => {
    it('hydrate_seedsOrdersAndStartsCountdown_fromTrainingQueue', async () => {
      const store = await hydratedStore(7, [
        order({ id: 1, inMs: 3000, queuePosition: 1 }),
        order({ id: 2, inMs: 6000, queuePosition: 2 }),
      ])

      expect(fetchTrainingQueue).toHaveBeenCalledWith(7)
      expect(store.orders).toHaveLength(2)
      expect(store.orders[0].remainingMs).toBe(3000)
      expect(store.orders[1].remainingMs).toBe(6000)

      // Countdown is running: remaining time decreases without any component mounted
      vi.advanceTimersByTime(1000)
      expect(store.orders[0].remainingMs).toBe(2000)
    })

    it('hydrate_leavesOrdersUnchanged_whenFetchRejects', async () => {
      const store = await hydratedStore(1, [order({ id: 1, inMs: 5000, queuePosition: 1 })])

      fetchTrainingQueue.mockRejectedValueOnce(new Error('network'))
      await store.hydrate(1)

      expect(store.orders).toHaveLength(1)
      expect(store.orders[0].id).toBe(1)
    })

    it('hydrate_leavesQueueEmptyAndStartsNoInterval_whenNoPendingOrders', async () => {
      const setIntervalSpy = vi.spyOn(globalThis, 'setInterval')

      const store = await hydratedStore(1, [])

      expect(store.orders).toEqual([])
      expect(setIntervalSpy).not.toHaveBeenCalled()
    })
  })

  describe('countdown lifecycle', () => {
    it('countdown_completesOrderAndRefreshesRoster_withNoComponentMounted', async () => {
      const store = await hydratedStore(1)
      store.setOrders([order({ id: 1, inMs: 5000, queuePosition: 1 })])

      vi.advanceTimersByTime(5100)

      expect(store.orders).toHaveLength(0)
      expect(mockArmyRefresh).toHaveBeenCalledTimes(1)
    })

    it('countdown_refreshesRosterWithVillageId_fromHydrate', async () => {
      await hydratedStore(42, [order({ id: 1, inMs: 500, queuePosition: 1 })])

      vi.advanceTimersByTime(600)

      expect(mockArmyRefresh).toHaveBeenCalledWith(42)
    })

    it('countdown_clearsInterval_whenQueueEmpties', async () => {
      const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')
      const store = await hydratedStore(1, [order({ id: 1, inMs: 300, queuePosition: 1 })])

      vi.advanceTimersByTime(400)
      expect(store.orders).toHaveLength(0)
      expect(clearIntervalSpy).toHaveBeenCalled()

      // No further ticks: refresh is not called again as time keeps passing
      mockArmyRefresh.mockClear()
      vi.advanceTimersByTime(5000)
      expect(mockArmyRefresh).not.toHaveBeenCalled()
    })

    it('startCountdown_doesNotCreateSecondInterval_whenAlreadyRunning', async () => {
      const store = await hydratedStore(1)
      const setIntervalSpy = vi.spyOn(globalThis, 'setInterval')

      store.setOrders([order({ id: 1, inMs: 5000, queuePosition: 1 })])
      store.setOrders([
        order({ id: 1, inMs: 5000, queuePosition: 1 }),
        order({ id: 2, inMs: 9000, queuePosition: 2 }),
      ])

      expect(setIntervalSpy).toHaveBeenCalledTimes(1)
    })
  })

  describe('queue position renumbering', () => {
    it('tick_renumbersQueuePositionsFromOne_afterFirstOrderCompletes', async () => {
      const store = await hydratedStore(1, [
        order({ id: 1, inMs: 1000, queuePosition: 1 }),
        order({ id: 2, inMs: 2000, queuePosition: 2 }),
        order({ id: 3, inMs: 3000, queuePosition: 3 }),
      ])

      vi.advanceTimersByTime(1100)

      expect(store.orders.map((o) => o.id)).toEqual([2, 3])
      expect(store.orders.map((o) => o.queuePosition)).toEqual([1, 2])
    })

    it('tick_refreshesRosterForEveryOrder_notOnlyTheFirst', async () => {
      const store = await hydratedStore(1, [
        order({ id: 1, inMs: 1000, queuePosition: 1 }),
        order({ id: 2, inMs: 2000, queuePosition: 2 }),
        order({ id: 3, inMs: 3000, queuePosition: 3 }),
      ])

      vi.advanceTimersByTime(1100)
      expect(mockArmyRefresh).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(1000)
      expect(mockArmyRefresh).toHaveBeenCalledTimes(2)

      vi.advanceTimersByTime(1000)
      expect(mockArmyRefresh).toHaveBeenCalledTimes(3)
      expect(store.orders).toHaveLength(0)
    })

    it('tick_removesBothOrdersAndRenumbers_whenTwoExpireInSameTick', async () => {
      const store = await hydratedStore(1, [
        order({ id: 1, inMs: 50, queuePosition: 1 }),
        order({ id: 2, inMs: 80, queuePosition: 2 }),
        order({ id: 3, inMs: 5000, queuePosition: 3 }),
      ])

      vi.advanceTimersByTime(100)

      expect(store.orders.map((o) => o.id)).toEqual([3])
      expect(store.orders[0].queuePosition).toBe(1)
    })
  })

  describe('per-building lookups', () => {
    it('ordersForBuilding_returnsOnlyOrdersMatchingBuildingId', async () => {
      const store = await hydratedStore(1, [
        order({ id: 1, buildingId: 10, inMs: 5000, queuePosition: 1 }),
        order({ id: 2, buildingId: 20, inMs: 9000, queuePosition: 2 }),
        order({ id: 3, buildingId: 10, inMs: 12000, queuePosition: 3 }),
      ])

      expect(store.ordersForBuilding(10).map((o) => o.id)).toEqual([1, 3])
      expect(store.ordersForBuilding(20).map((o) => o.id)).toEqual([2])
      expect(store.ordersForBuilding(99)).toEqual([])
    })

    it('hasActiveFor_matchesOnBuildingId_notConstructionSiteId', async () => {
      // buildingId 42 deliberately differs from any plausible construction site id
      const store = await hydratedStore(1, [
        order({ id: 1, buildingId: 42, inMs: 5000, queuePosition: 1 }),
      ])

      expect(store.hasActiveFor(42)).toBe(true)
      expect(store.hasActiveFor(3)).toBe(false)
    })

    it('hasActiveFor_returnsFalse_whenQueueIsEmpty', async () => {
      const store = await hydratedStore(1)
      expect(store.hasActiveFor(10)).toBe(false)
    })
  })

  describe('clock offset', () => {
    it('hydrate_derivesClockOffsetFromServerTime_whenBackendClockLags', async () => {
      // Backend is 70s behind the client: finishesAt looks past against raw Date.now()
      const now = Date.now()
      const store = await hydratedStore(1, [
        {
          id: 1,
          unitType: 'VANDAL',
          buildingId: 10,
          quantity: 1,
          queuePosition: 1,
          serverTime: new Date(now - 70000).toISOString(),
          finishesAt: new Date(now - 70000 + 5000).toISOString(),
        },
      ])

      expect(store.orders[0].remainingMs).toBe(5000)

      vi.advanceTimersByTime(100)
      expect(store.orders).toHaveLength(1)

      vi.advanceTimersByTime(5000)
      expect(store.orders).toHaveLength(0)
    })

    it('setOrders_derivesClockOffsetFromServerTime_whenBackendClockLeads', async () => {
      const store = await hydratedStore(1)
      const now = Date.now()

      store.setOrders([
        {
          id: 1,
          unitType: 'VANDAL',
          buildingId: 10,
          quantity: 1,
          queuePosition: 1,
          serverTime: new Date(now + 70000).toISOString(),
          finishesAt: new Date(now + 70000 + 5000).toISOString(),
        },
      ])

      expect(store.orders[0].remainingMs).toBe(5000)
    })

    it('hydrate_leavesOffsetUnchanged_whenResponseIsEmpty', async () => {
      const now = Date.now()
      const store = await hydratedStore(1, [
        {
          id: 1,
          unitType: 'VANDAL',
          buildingId: 10,
          quantity: 1,
          queuePosition: 1,
          serverTime: new Date(now - 70000).toISOString(),
          finishesAt: new Date(now - 70000 + 5000).toISOString(),
        },
      ])

      // Empty response must not reset the offset back to zero
      fetchTrainingQueue.mockResolvedValueOnce([])
      await store.hydrate(1)

      store.setOrders([
        {
          id: 2,
          unitType: 'VANDAL',
          buildingId: 10,
          quantity: 1,
          queuePosition: 1,
          finishesAt: new Date(Date.now() - 70000 + 8000).toISOString(),
        },
      ])

      expect(store.orders[0].remainingMs).toBe(8000)
    })
  })

  describe('stop', () => {
    it('stop_clearsIntervalAndResetsOrders', async () => {
      const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval')
      const store = await hydratedStore(1, [order({ id: 1, inMs: 5000, queuePosition: 1 })])

      store.stop()

      expect(clearIntervalSpy).toHaveBeenCalled()
      expect(store.orders).toEqual([])
    })

    it('stop_clearsCachedVillageId_soLaterCompletionsDoNotRefreshTheOldVillage', async () => {
      const store = await hydratedStore(42, [order({ id: 1, inMs: 5000, queuePosition: 1 })])

      store.stop()
      mockArmyRefresh.mockClear()

      store.setOrders([order({ id: 2, inMs: 300, queuePosition: 1 })])
      vi.advanceTimersByTime(400)

      expect(store.orders).toHaveLength(0)
      expect(mockArmyRefresh).not.toHaveBeenCalled()
    })

    it('stop_clearsClockOffset_soAStaleOffsetDoesNotLeakIntoTheNextSession', async () => {
      // Hydrate with a server clock 70s ahead of this client's.
      const store = await hydratedStore(1, [
        {
          id: 1,
          unitType: 'VANDAL',
          buildingId: 10,
          quantity: 1,
          queuePosition: 1,
          serverTime: new Date(Date.now() + 70000).toISOString(),
          finishesAt: new Date(Date.now() + 70000 + 5000).toISOString(),
        },
      ])
      expect(store.orders[0].remainingMs).toBe(5000)

      store.stop()

      // New orders with no serverTime must be measured against the local clock.
      store.setOrders([order({ id: 2, inMs: 5000, queuePosition: 1 })])

      expect(store.orders[0].remainingMs).toBe(5000)
    })
  })

  describe('error handling', () => {
    it('tick_swallowsRosterRefreshRejection_andKeepsRemainingOrders', async () => {
      const store = await hydratedStore(1, [
        order({ id: 1, inMs: 300, queuePosition: 1 }),
        order({ id: 2, inMs: 9000, queuePosition: 2 }),
      ])
      mockArmyRefresh.mockRejectedValue(new Error('network'))

      expect(() => vi.advanceTimersByTime(400)).not.toThrow()
      expect(store.orders.map((o) => o.id)).toEqual([2])
      expect(store.orders[0].queuePosition).toBe(1)
    })
  })
})
