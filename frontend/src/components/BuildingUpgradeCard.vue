<script setup>
import { computed, ref } from 'vue'
import { useResourceStore } from '@/stores/resources.js'
import { trainUnit } from '@/util/api/units.js'

const props = defineProps({
  building: Object,
  currentResources: Object,
  villageId: Number,
})

const emit = defineEmits(['upgrade', 'close'])

const resourceStore = useResourceStore()

const icons = {
  bricks: '🧱',
  wood: '🌲',
  food: '🌾',
  iron: '⚒️',
}

const isBarrack = computed(() => props.building?.type === 'BARRACK')

const VANDAL_FOOD_COST = 50
const VANDAL_IRON_COST = 30
const VANDAL_HP = 4
const VANDAL_DAMAGE = 1

const canAfford = computed(() => {
  if (!props.currentResources || !props.building?.upgradeCost) return false
  return Object.entries(props.building.upgradeCost).every(
    ([resource, cost]) => (props.currentResources[resource] ?? 0) >= cost,
  )
})

const canAffordVandal = computed(() => {
  return resourceStore.food >= VANDAL_FOOD_COST && resourceStore.iron >= VANDAL_IRON_COST
})

const nextLevel = computed(() => (props.building?.level ?? 1) + 1)

const nextProduction = computed(() => {
  if (props.building?.productionPerHour == null) return null
  const ratePerLevel = props.building.productionPerHour / props.building.level
  return props.building.productionPerHour + ratePerLevel
})

const trainError = ref(null)
const training = ref(false)

async function handleTrainVandal() {
  trainError.value = null
  training.value = true
  try {
    await trainUnit(props.villageId, props.building.buildingId)
    await resourceStore.refresh(props.villageId)
  } catch (err) {
    trainError.value = err.message ?? 'Failed to train Vandal'
  } finally {
    training.value = false
  }
}
</script>

<template>
  <div class="fixed inset-0 flex items-center justify-center z-50 p-2">
    <div
      class="card w-full max-w-sm bg-base-100 shadow-2xl rounded-xl overflow-hidden flex flex-col"
    >
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

        <!-- Barrack training panel -->
        <template v-if="isBarrack">
          <div class="divider w-full">Train Units</div>

          <div class="w-full border rounded-lg p-3 bg-base-200 flex flex-col gap-2">
            <p class="text-sm font-semibold text-center">Vandal</p>
            <p class="text-xs text-center text-base-content/70">
              HP {{ VANDAL_HP }} | DMG {{ VANDAL_DAMAGE }}
            </p>

            <div class="grid grid-cols-2 gap-2">
              <div
                class="flex flex-col items-center justify-center border rounded-lg p-2 shadow-sm"
                :class="{
                  'border-error text-error': resourceStore.food < VANDAL_FOOD_COST,
                  'border-success': resourceStore.food >= VANDAL_FOOD_COST,
                }"
              >
                <div class="text-2xl">{{ icons.food }}</div>
                <div class="text-lg font-medium">{{ VANDAL_FOOD_COST }}</div>
                <div class="text-xs opacity-60">have {{ resourceStore.food }}</div>
              </div>
              <div
                class="flex flex-col items-center justify-center border rounded-lg p-2 shadow-sm"
                :class="{
                  'border-error text-error': resourceStore.iron < VANDAL_IRON_COST,
                  'border-success': resourceStore.iron >= VANDAL_IRON_COST,
                }"
              >
                <div class="text-2xl">{{ icons.iron }}</div>
                <div class="text-lg font-medium">{{ VANDAL_IRON_COST }}</div>
                <div class="text-xs opacity-60">have {{ resourceStore.iron }}</div>
              </div>
            </div>

            <p v-if="trainError" class="text-error text-xs text-center">{{ trainError }}</p>

            <button
              class="btn btn-secondary w-full"
              :disabled="!canAffordVandal || training"
              @click="handleTrainVandal"
            >
              {{ training ? 'Training...' : 'Train Vandal' }}
            </button>
          </div>
        </template>

        <!-- Upgrade cost section (non-barracks or always shown) -->
        <template v-if="!isBarrack">
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
        </template>
      </div>

      <div class="flex gap-2 p-4">
        <button
          v-if="!isBarrack"
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
