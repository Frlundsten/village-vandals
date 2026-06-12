import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRoster } from '@/util/api/units.js'

export const useArmyStore = defineStore('army', () => {
  const roster = ref([])

  async function refresh(villageId) {
    try {
      roster.value = await fetchRoster(villageId)
    } catch {
      // Degrade gracefully
    }
  }

  return { roster, refresh }
})
