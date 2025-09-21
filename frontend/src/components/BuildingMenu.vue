<script setup>
import {onMounted, ref} from "vue";
import BuildingPresentationCard from "@/components/BuildingPresentationCard.vue";
import { BASE_URL } from '@/util/util.js'

const availableBuildings = ref([]);

const { tileInfo, villageId } = defineProps({
  tileInfo: Object,
  villageId: Number
});

const emit = defineEmits(['buildingType', 'closeMenu'])

async function sendInfo(type, upgradeCost, tileInfo) {
  try {
    const response = await fetch(`${BASE_URL}/building`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        'Authorization': `Bearer ${localStorage.getItem("jwt_token")}`
      },
      body: JSON.stringify({
        type: type,
        constructionSiteId: tileInfo.constructionSiteId,
        villageId: villageId,
        upgradeCost: upgradeCost,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    emit('buildingType',type);

  } catch (error) {
    console.error("Failed to create building:", error);
  }
}

onMounted(async () => {
  try {
    const response = await fetch(`${BASE_URL}/building/available?villageId=1`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem("jwt_token")}`
      }
    });

    if (!response.ok) {
      throw new Error('Failed to fetch buildings');
    }

    availableBuildings.value = await response.json();

  } catch (e) {
    console.error('Error fetching buildings:', e);
  }
})
</script>

<template>
  <div class="card bg-base-100 shadow-[1px_2px_2px_rgba(0,0,0,0.9)]">
    <div class="card-body">
      <div class="card-actions">
        <div v-for="building in availableBuildings" :key="building.type">
          <BuildingPresentationCard
            @click="sendInfo(building.type, building.upgradeCost, tileInfo)"
            class="hover:cursor-pointer"
            :type="building.type"
            :upgradeCost="building.upgradeCost"
          />
        </div>
      </div>
    </div>
    <button @click="emit('closeMenu')" class="btn border-t-neutral-400 btn-success">Close</button>
  </div>
</template>