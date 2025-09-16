<script setup>
import imgUrl from '../assets/jpeg/farmer.png'

defineProps(['upgradeCost', 'type', 'img']);
const icons = {
  bricks: "🧱",
  wood: "🌲",
  food: "🌾",
  iron: "⚒️"
}
</script>

<template>
  <div class="shadow-[0px_0px_0px_2px_rgba(0,0,0,0.9)]">
    <img class="p-2" :src="imgUrl"/>
    <h1 class="text-center">{{ type }}</h1>
    <div class="flex flex-wrap">
      <div v-for="(value, key) in upgradeCost" :key="key" class="text-center w-1/2 flex flex-col">
        <div class="text-2xl">{{ icons[key] || "❓" }}</div>
        <div class="text-lg font-medium ">{{ value }}</div>
      </div>
    </div>
  </div>
</template>