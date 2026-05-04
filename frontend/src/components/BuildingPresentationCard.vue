<script setup>
import { computed } from 'vue'

const props = defineProps({
  constructionCost: Object,
  type: String,
  currentResources: Object,
})

const icons = {
  bricks: '🧱',
  wood: '🌲',
  food: '🌾',
  iron: '⚒️',
}

const labels = {
  bricks: 'Bricks',
  wood: 'Wood',
  food: 'Food',
  iron: 'Iron',
}

const canAfford = computed(() => {
  if (!props.currentResources || !props.constructionCost) return true
  return Object.entries(props.constructionCost).every(
    ([resource, cost]) => (props.currentResources[resource] ?? 0) >= cost,
  )
})
</script>

<template>
  <div
    class="relative flex flex-col items-center text-center bg-base-200 rounded-xl shadow-lg p-4 transition-all duration-200"
    :class="
      canAfford
        ? 'cursor-pointer hover:shadow-2xl hover:scale-105'
        : 'opacity-50 cursor-not-allowed pointer-events-none'
    "
  >
    <img :src="`/assets/${type}.png`" :alt="type" class="w-20 h-20 object-contain mb-2" />

    <h3 class="text-base font-bold capitalize mb-1">{{ type }}</h3>

    <div v-if="!canAfford" class="badge badge-error badge-sm mb-2">Can't afford</div>

    <div class="grid grid-cols-2 gap-1.5 w-full mt-1">
      <div
        v-for="(value, key) in constructionCost"
        :key="key"
        class="flex flex-col items-center justify-center border rounded-lg p-1.5 bg-base-100 text-xs"
        :class="
          !currentResources
            ? 'border-base-300'
            : (currentResources[key] ?? 0) >= value
              ? 'border-success text-success'
              : 'border-error text-error'
        "
      >
        <span class="text-xl">{{ icons[key] || '❓' }}</span>
        <span class="font-semibold text-sm">{{ labels[key] || key }}</span>
        <span class="font-bold">{{ value }}</span>
        <span v-if="currentResources" class="opacity-60 text-xs">
          have {{ Math.floor(currentResources[key] ?? 0) }}
        </span>
      </div>
    </div>
  </div>
</template>
