<script setup>
import { computed } from 'vue'

const props = defineProps({
  building: Object,
  currentResources: Object,
})

const emit = defineEmits(['upgrade', 'close'])

const icons = {
  bricks: '🧱',
  wood: '🌲',
  food: '🌾',
  iron: '⚒️',
}

const canAfford = computed(() => {
  if (!props.currentResources || !props.building?.upgradeCost) return false
  return Object.entries(props.building.upgradeCost).every(
    ([resource, cost]) => (props.currentResources[resource] ?? 0) >= cost,
  )
})

const nextLevel = computed(() => (props.building?.level ?? 1) + 1)

const nextProduction = computed(() => {
  if (props.building?.productionPerHour == null) return null
  const ratePerLevel = props.building.productionPerHour / props.building.level
  return props.building.productionPerHour + ratePerLevel
})
</script>

<template>
  <div class="fixed inset-0 flex items-center justify-center z-50 p-2">
    <div class="card w-full max-w-sm bg-base-100 shadow-2xl rounded-xl overflow-hidden flex flex-col">
      <div class="card-body flex flex-col items-center gap-4">
        <img
          :src="`/assets/${building.type}.png`"
          :alt="building.type"
          class="w-28 h-28 object-contain"
        />

        <h2 class="text-2xl font-bold">{{ building.type }}</h2>

        <div class="badge badge-lg badge-outline text-base">
          Level {{ building.level }} → {{ nextLevel }}
        </div>

        <div v-if="nextProduction != null" class="text-sm text-base-content/70">
          Production: {{ building.productionPerHour }} → {{ nextProduction }}/hr
        </div>

        <div class="w-full">
          <p class="text-sm font-semibold mb-2 text-center">Upgrade cost</p>
          <div class="grid grid-cols-2 gap-2">
            <div
              v-for="(value, key) in building.upgradeCost"
              :key="key"
              class="flex flex-col items-center justify-center border rounded-lg p-2 bg-base-200 shadow-sm"
              :class="{
                'border-error text-error': (currentResources?.[key] ?? 0) < value,
                'border-success': (currentResources?.[key] ?? 0) >= value,
              }"
            >
              <div class="text-2xl">{{ icons[key] || '❓' }}</div>
              <div class="text-lg font-medium">{{ value }}</div>
              <div class="text-xs opacity-60">have {{ currentResources?.[key] ?? '?' }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-2 p-4">
        <button
          class="btn btn-primary flex-1"
          :disabled="!canAfford"
          @click="emit('upgrade', building.constructionSiteId)"
        >
          Upgrade
        </button>
        <button class="btn flex-1" @click="emit('close')">Close</button>
      </div>
    </div>
  </div>
</template>
