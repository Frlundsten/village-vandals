import { onMounted, onUnmounted, ref } from 'vue'
import { fetchTrainingQueue } from '@/util/api/units.js'
import { useArmyStore } from '@/stores/army.js'

const TICK_INTERVAL_MS = 100

export function useTrainingQueue(villageId) {
  const queue = ref([])
  let intervalId = null
  const armyStore = useArmyStore()

  function hasActiveFor(buildingId) {
    return queue.value.some((order) => order.buildingId === buildingId)
  }

  async function refresh(vid = villageId) {
    try {
      const queueData = await fetchTrainingQueue(vid)
      queue.value = queueData.map(enrichOrder)
    } catch {
      // Degrade gracefully
    }
  }

  function setQueue(rawOrders) {
    queue.value = rawOrders.map(enrichOrder)
  }

  function enrichOrder(order) {
    return {
      ...order,
      remainingMs: Math.max(0, new Date(order.finishesAt).getTime() - Date.now()),
    }
  }

  function tick() {
    if (queue.value.length === 0) return

    const now = Date.now()
    queue.value = queue.value.map((order) => ({
      ...order,
      remainingMs: Math.max(0, new Date(order.finishesAt).getTime() - now),
    }))

    const firstOrder = queue.value.find((o) => o.queuePosition === 1)
    if (firstOrder && firstOrder.remainingMs <= 0) {
      queue.value = queue.value.filter((o) => o.remainingMs > 0)
      armyStore.refresh(villageId).catch(() => {})
    }
  }

  onMounted(async () => {
    await refresh(villageId)
    intervalId = setInterval(tick, TICK_INTERVAL_MS)
  })

  onUnmounted(() => {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  })

  return { queue, hasActiveFor, refresh, setQueue }
}
