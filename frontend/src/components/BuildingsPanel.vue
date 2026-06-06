<script setup>
import { onMounted, ref } from 'vue'
import { fetchBuildings, upgradeBuilding } from '@/util/api/buildings.js'
import { useResourceStore } from '@/stores/resources.js'

const resourceStore = useResourceStore()
const buildings = ref([])
const loading = ref(true)
const errors = ref({})

const villageId = Number(localStorage.getItem('villageId'))

async function loadBuildings() {
  loading.value = true
  try {
    buildings.value = await fetchBuildings(villageId)
  } finally {
    loading.value = false
  }
}

onMounted(loadBuildings)

function canAfford(building) {
  if (building.type === 'BARRACK') return false
  if (!building.upgradeCost) return false
  return Object.entries(building.upgradeCost).every(
    ([resource, cost]) => (resourceStore[resource] ?? 0) >= cost,
  )
}

async function handleUpgrade(building) {
  errors.value[building.constructionSiteId] = null
  try {
    await upgradeBuilding(villageId, building.constructionSiteId)
    await resourceStore.refresh(villageId)
    await loadBuildings()
  } catch (e) {
    errors.value[building.constructionSiteId] = e.message ?? 'Upgrade failed'
  }
}
</script>

<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold mb-6">Buildings</h2>

    <div v-if="loading" class="text-base-content/60">Loading...</div>

    <div v-else-if="buildings.length === 0" class="text-base-content/60 italic">
      No buildings yet — construct something from your village map.
    </div>

    <div v-else class="overflow-x-auto">
      <table class="table table-zebra w-full">
        <thead>
          <tr>
            <th class="w-20">Level</th>
            <th>Building</th>
            <th class="text-right w-36">Upgrade</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="building in buildings"
            :key="building.constructionSiteId"
            data-testid="building-row"
          >
            <td>
              <span class="badge badge-lg badge-outline font-bold">{{ building.level }}</span>
            </td>

            <td>
              <div class="flex items-center gap-3">
                <img
                  :src="`/assets/${building.type}.png`"
                  :alt="building.type"
                  class="w-10 h-10 object-contain"
                  @error="(e) => (e.target.style.display = 'none')"
                />
                <span class="font-semibold capitalize">
                  {{ building.type.toLowerCase().replace('_', ' ') }}
                </span>
              </div>
            </td>

            <td class="text-right">
              <div class="flex flex-col items-end gap-1">
                <button
                  data-testid="upgrade-btn"
                  class="btn btn-sm"
                  :class="canAfford(building) ? 'btn-success' : ''"
                  :disabled="!canAfford(building)"
                  @click="handleUpgrade(building)"
                >
                  ⬆ Upgrade
                </button>
                <span
                  v-if="errors[building.constructionSiteId]"
                  class="text-xs text-error"
                >
                  {{ errors[building.constructionSiteId] }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
