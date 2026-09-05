import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchTrainingQueue } from '@/util/api/units.js'
import { useArmyStore } from '@/stores/army.js'

const TICK_INTERVAL_MS = 100

/**
 * Owns the village's training queue and the single countdown that drives it.
 *
 * The countdown lives here rather than in a component so it keeps running when the
 * building card is closed or the player navigates away — orders complete and the army
 * roster refreshes regardless of what is mounted.
 */
export const useTrainingStore = defineStore('training', () => {
  const orders = ref([])
  const armyStore = useArmyStore()

  let villageId = null
  let intervalId = null
  // Difference between the backend clock and this client's clock, derived from serverTime.
  let clockOffsetMs = 0

  function correctedNow() {
    return Date.now() + clockOffsetMs
  }

  function updateClockOffset(rawOrders) {
    if (rawOrders.length > 0 && rawOrders[0].serverTime) {
      clockOffsetMs = new Date(rawOrders[0].serverTime).getTime() - Date.now()
    }
  }

  function withRemaining(order, now) {
    return {
      ...order,
      remainingMs: Math.max(0, new Date(order.finishesAt).getTime() - now),
    }
  }

  /** Positions must be sequential from 1, or the active order can no longer be found. */
  function renumber(list) {
    return [...list]
      .sort((a, b) => new Date(a.finishesAt).getTime() - new Date(b.finishesAt).getTime())
      .map((order, index) => ({ ...order, queuePosition: index + 1 }))
  }

  function tick() {
    const now = correctedNow()
    const updated = orders.value.map((order) => withRemaining(order, now))
    const remaining = updated.filter((order) => order.remainingMs > 0)
    const completed = updated.length - remaining.length

    orders.value = renumber(remaining)

    if (completed > 0 && villageId !== null) {
      // Fire-and-forget: the backend promotes completed orders to units on read.
      armyStore.refresh(villageId).catch(() => {})
    }

    if (orders.value.length === 0) {
      stopCountdown()
    }
  }

  function startCountdown() {
    if (intervalId !== null || orders.value.length === 0) return
    intervalId = setInterval(tick, TICK_INTERVAL_MS)
  }

  function stopCountdown() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  /** Seed the queue from the server. Called once the current village is known. */
  async function hydrate(vid) {
    villageId = vid
    try {
      const queue = await fetchTrainingQueue(vid)
      setOrders(queue)
    } catch {
      // Degrade gracefully — keep whatever the store already holds
    }
  }

  /** Replace the queue, e.g. with the response from POST /unit/train. */
  function setOrders(rawOrders) {
    updateClockOffset(rawOrders)
    const now = correctedNow()
    orders.value = renumber(rawOrders.map((order) => withRemaining(order, now)))
    startCountdown()
  }

  function ordersForBuilding(buildingId) {
    return orders.value.filter((order) => order.buildingId === buildingId)
  }

  function hasActiveFor(buildingId) {
    return orders.value.some((order) => order.buildingId === buildingId)
  }

  /** Tear down on logout so nothing leaks into the next session. */
  function stop() {
    stopCountdown()
    orders.value = []
    villageId = null
    // The offset was derived from the previous session's server clock; keeping it would skew
    // remaining times for any later response that carries no serverTime of its own.
    clockOffsetMs = 0
  }

  return { orders, hydrate, setOrders, ordersForBuilding, hasActiveFor, stop }
})
