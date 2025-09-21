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
  <div class="shadow-[0px_0px_0px_2px_rgba(0,0,0,0.9)] p-4 flex flex-col items-center text-center">
    <img class="w-24 h-24 object-contain mb-2" :src="`assets/${type}.png`" />
    <h1 class="text-lg font-semibold mb-2">{{ type }}</h1>
    <div class="grid grid-cols-2 gap-1 w-full">
      <div
        v-for="(value, key) in upgradeCost"
        :key="key"
        class="flex flex-col items-center justify-center border p-2 rounded"
      >
        <div class="text-2xl">{{ icons[key] || '❓' }}</div>
        <div class="text-lg font-medium">{{ value }}</div>
      </div>
    </div>
  </div>
</template>
