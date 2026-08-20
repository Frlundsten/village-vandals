import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRoster } from '@/util/api/units.js'

/**
 * The village's unit roster.
 *
 * `refresh` deliberately propagates failures. Swallowing them here made ArmyView's error branch
 * unreachable, so a failed fetch was shown to the player as "No units yet". Callers that must not
 * throw — the training countdown's fire-and-forget refresh — suppress it at their own call site.
 */
export const useArmyStore = defineStore('army', () => {
  const roster = ref([])

  async function refresh(villageId) {
    // Assign only on success, so a failure leaves the previous roster intact.
    roster.value = await fetchRoster(villageId)
  }

  /** Tear down on logout so one account's units never show up in the next session. */
  function reset() {
    roster.value = []
  }

  return { roster, refresh, reset }
})
