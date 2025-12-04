<script setup>
defineProps(['upgradeCost', 'type'])
const icons = {
  bricks: '🧱',
  wood: '🌲',
  food: '🌾',
  iron: '⚒️',
}
</script>

<template>
  <div
    class="shadow-lg p-4 flex flex-col items-center text-center bg-base-200 rounded-xl hover:shadow-2xl transition-shadow duration-300 cursor-pointer"
  >
    <img :src="`/assets/${type}.png`" :alt="type" class="w-24 h-24 object-contain mb-2" />

    <h3 class="text-lg sm:text-xl md:text-2xl font-semibold mb-2">{{ type }}</h3>

    <div class="grid grid-cols-2 gap-2 w-full">
      <div
        v-for="(value, key) in upgradeCost"
        :key="key"
        class="flex flex-col items-center justify-center border rounded-lg p-2 bg-base-100 shadow-sm"
      >
        <div class="text-2xl sm:text-3xl">{{ icons[key] || '❓' }}</div>
        <div class="text-lg sm:text-xl font-medium">{{ value }}</div>
      </div>
    </div>
  </div>
</template>
