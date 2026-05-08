<script setup>
import { onMounted, ref } from 'vue'
import BuildingPresentationCard from '@/components/BuildingPresentationCard.vue'
import { getAvailableBuildings } from '@/util/api/buildings.js'

const availableBuildings = ref([])

const { tileInfo, villageId, currentResources } = defineProps({
  tileInfo: Object,
  villageId: Number,
  currentResources: Object,
})

const emit = defineEmits(['buildingType', 'closeMenu'])

function sendInfo(type, tileInfo) {
  emit('buildingType', type)
  emit('closeMenu')
}

onMounted(async () => {
  try {
    availableBuildings.value = await getAvailableBuildings(villageId)
  } catch (e) {
    console.error('Error fetching buildings:', e)
  }
})

defineExpose({ sendInfo })
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
          <BuildingPresentationCard
            v-for="building in availableBuildings"
            :key="building.type"
            @click="sendInfo(building.type, tileInfo)"
            :type="building.type"
            :constructionCost="building.constructionCost"
            :currentResources="currentResources"
          />
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
