import { defineStore } from 'pinia'
import { ref } from 'vue'
import { refreshStorage } from '@/util/api/resources.js'

const SECONDS_PER_HOUR = 3600
const TICK_INTERVAL_MS = 1000
const KEYS = ['food', 'wood', 'bricks', 'iron']

/**
 * Current resource amounts and per-hour production rates.
 *
 * Between server syncs the amounts advance locally, mirroring the backend's rule: credit whole
 * units for the elapsed time and carry the fractional remainder into the next tick. Accrual is
 * measured from the real elapsed time rather than assumed to be one interval per tick, so a
 * throttled background tab does not lose production.
 *
 * The projection is only ever optimistic about *gains*. Every action re-syncs from the server,
 * and the server re-checks affordability before any deduction, so a drifted client cannot spend
 * resources it does not have.
 */
export const useResourceStore = defineStore('resources', () => {
  const food = ref(0)
  const wood = ref(0)
  const bricks = ref(0)
  const iron = ref(0)
  const foodPerHour = ref(0)
  const woodPerHour = ref(0)
  const bricksPerHour = ref(0)
  const ironPerHour = ref(0)

  const amounts = { food, wood, bricks, iron }
  const rates = {
    food: foodPerHour,
    wood: woodPerHour,
    bricks: bricksPerHour,
    iron: ironPerHour,
  }

  // Sub-unit production waiting to become a whole unit.
  let carry = { food: 0, wood: 0, bricks: 0, iron: 0 }
  let lastAccrualAt = Date.now()
  let tickId = null

  function accrue() {
    const now = Date.now()
    const elapsedSeconds = Math.max(0, (now - lastAccrualAt) / 1000)
    lastAccrualAt = now
    if (elapsedSeconds === 0) return

    let producing = false
    for (const key of KEYS) {
      const rate = rates[key].value
      if (rate <= 0) continue
      producing = true

      carry[key] += (rate * elapsedSeconds) / SECONDS_PER_HOUR
      const whole = Math.floor(carry[key])
      if (whole > 0) {
        amounts[key].value += whole
        carry[key] -= whole
      }
    }

    // Nothing is being produced — stop burning a timer until the next sync says otherwise.
    if (!producing) stopTicking()
  }

  function startTicking() {
    if (tickId !== null) return
    if (!KEYS.some((key) => rates[key].value > 0)) return
    lastAccrualAt = Date.now()
    tickId = setInterval(accrue, TICK_INTERVAL_MS)
  }

  function stopTicking() {
    if (tickId !== null) {
      clearInterval(tickId)
      tickId = null
    }
  }

  /** Syncs from the server. The response is authoritative — it replaces any local projection. */
  async function refresh(villageId) {
    const data = await refreshStorage(villageId)

    food.value = data.food
    wood.value = data.wood
    bricks.value = data.bricks
    iron.value = data.iron
    foodPerHour.value = data.foodPerHour ?? 0
    woodPerHour.value = data.woodPerHour ?? 0
    bricksPerHour.value = data.bricksPerHour ?? 0
    ironPerHour.value = data.ironPerHour ?? 0

    carry = { food: 0, wood: 0, bricks: 0, iron: 0 }
    lastAccrualAt = Date.now()
    startTicking()
  }

  /** Tear down on logout so one account's totals never show up in the next session. */
  function reset() {
    stopTicking()
    for (const key of KEYS) {
      amounts[key].value = 0
      rates[key].value = 0
      carry[key] = 0
    }
  }

  return {
    food,
    wood,
    bricks,
    iron,
    foodPerHour,
    woodPerHour,
    bricksPerHour,
    ironPerHour,
    refresh,
    reset,
  }
})
