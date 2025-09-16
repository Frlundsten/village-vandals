<script setup>

import {onMounted, ref} from "vue";

const buildings = ref({})

defineProps({
  villageId: {
    type: String,
    required: true
  }
})

onMounted(async () => {
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/buildings?villageId=${villageId}`, {
      method: 'GET',
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error('Failed to fetch buildings');
    }

    buildings.value = await response.json();
  } catch (e) {
    console.error('Error fetching buildings:', e);
  }
})

</script>

<template>

</template>