import { defineStore } from 'pinia'
import { ref } from 'vue'
import { refreshStorage } from '@/util/api/resources.js'

export const useResourceStore = defineStore('resources', () => {
  const food = ref(0)
  const wood = ref(0)
  const bricks = ref(0)
  const iron = ref(0)

  async function refresh(villageId) {
    const data = await refreshStorage(villageId)
    food.value = data.food
    wood.value = data.wood
    bricks.value = data.bricks
    iron.value = data.iron
  }

  return { food, wood, bricks, iron, refresh }
})
