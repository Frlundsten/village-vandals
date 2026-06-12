<script setup>
import { onMounted, ref } from 'vue'
import { useArmyStore } from '@/stores/army.js'

const UNIT_ICONS = {
  VANDAL: '⚔️',
}

const armyStore = useArmyStore()
const loading = ref(true)
const error = ref(null)

function iconFor(unitType) {
  return UNIT_ICONS[unitType] ?? '🗡️'
}

onMounted(async () => {
  const villageId = Number(localStorage.getItem('villageId'))
  try {
    await armyStore.refresh(villageId)
  } catch (err) {
    error.value = err.message ?? 'Failed to load army'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="p-6 max-w-2xl mx-auto">
    <h1 class="text-3xl font-bold mb-6">🛡️ Army</h1>

    <div v-if="loading" class="flex justify-center py-12">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <div v-else-if="error" class="alert alert-error">
      <span>{{ error }}</span>
    </div>

    <template v-else-if="armyStore.roster.length > 0">
      <div class="grid gap-4">
        <div
          v-for="unit in armyStore.roster"
          :key="unit.unitType"
          class="card bg-base-200 shadow-md rounded-xl"
        >
          <div class="card-body flex-row items-center gap-4 py-4">
            <div class="text-5xl">{{ iconFor(unit.unitType) }}</div>
            <div class="flex-1">
              <h2 class="card-title text-lg capitalize">{{ unit.unitType }}</h2>
              <p class="text-base-content/60 text-sm">HP {{ unit.hp }} | DMG {{ unit.damage }}</p>
            </div>
            <div class="badge badge-lg badge-primary font-bold text-lg px-4 py-3">
              × {{ unit.count }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="text-center py-16 text-base-content/50">
      <p class="text-lg">No units yet — train some Vandals from the Barrack!</p>
    </div>
  </div>
</template>
