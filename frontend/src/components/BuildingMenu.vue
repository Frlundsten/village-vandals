<script setup>
import { onMounted, ref } from 'vue'
import BuildingPresentationCard from '@/components/BuildingPresentationCard.vue'
import { constructBuilding, getAvailableBuildings } from '@/util/api/buildings.js'

const availableBuildings = ref([])

const { tileInfo, villageId } = defineProps({
  tileInfo: Object,
  villageId: Number,
})

const emit = defineEmits(['buildingType', 'closeMenu'])

async function sendInfo(type, upgradeCost, tileInfo) {
  emit('buildingType', type)
  emit('closeMenu')
  try {
    const response = await constructBuilding(
      type,
      tileInfo.constructionSiteId,
      villageId,
      upgradeCost,
    )
  } catch (error) {
    console.error('Failed to create building:', error)
  }
}

onMounted(async () => {
  try {
    availableBuildings.value = await getAvailableBuildings(villageId)
  } catch (e) {
    console.error('Error fetching buildings:', e)
  }
})
</script>
<template>
  <!-- Overlay for fullscreen modal style -->
  <div class="fixed inset-0 flex items-center justify-center z-50 p-2">
    <div
      class="card w-full max-w-3xl h-5/6 bg-base-100 shadow-2xl rounded-xl overflow-y-auto flex flex-col"
    >
      <!-- Header -->
      <div class="card-body flex-1 flex flex-col">
        <h2 class="text-2xl font-bold text-center mb-4">Available Buildings</h2>

        <!-- Building grid -->
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 flex-1">
          <div
            v-for="building in availableBuildings"
            :key="building.type"
            class="hover:scale-105 transition-transform duration-200"
          >
            <BuildingPresentationCard
              @click="sendInfo(building.type, building.upgradeCost, tileInfo)"
              :type="building.type"
              :upgradeCost="building.upgradeCost"
            />
          </div>
        </div>
      </div>

      <!-- Close button -->
      <button
        @click="emit('closeMenu')"
        class="btn btn-success w-full mt-4 rounded-b-xl"
      >
        Close
      </button>
    </div>
  </div>
</template>
