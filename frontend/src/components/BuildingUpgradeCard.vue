<script setup>
import { computed, ref } from 'vue'
import { useResourceStore } from '@/stores/resources.js'
import { useTrainingStore } from '@/stores/training.js'
import { trainUnit } from '@/util/api/units.js'
import {
  MAX_TRAINING_BATCH_SIZE,
  MIN_TRAINING_BATCH_SIZE,
  TRAINING_DURATION_MS,
  VANDAL_DAMAGE,
  VANDAL_FOOD_COST,
  VANDAL_HP,
  VANDAL_IRON_COST,
} from '@/util/gameConfig.js'

const props = defineProps({
  building: Object,
  currentResources: Object,
  villageId: Number,
})

const emit = defineEmits(['upgrade', 'close'])

const resourceStore = useResourceStore()
const trainingStore = useTrainingStore()

const icons = {
  bricks: '🧱',
  wood: '🌲',
  food: '🌾',
  iron: '⚒️',
}

const isBarrack = computed(() => props.building?.type === 'BARRACK')

// Bounds come from the shared config so they cannot drift from what the server accepts.
const trainQuantity = ref(MIN_TRAINING_BATCH_SIZE)

function clampQuantity(value) {
  const parsed = Number.isFinite(value) ? Math.trunc(value) : MIN_TRAINING_BATCH_SIZE
  return Math.min(MAX_TRAINING_BATCH_SIZE, Math.max(MIN_TRAINING_BATCH_SIZE, parsed))
}

function setTrainQuantity(value) {
  trainQuantity.value = clampQuantity(value)
}

function incrementTrainQuantity() {
  setTrainQuantity(trainQuantity.value + 1)
}

function decrementTrainQuantity() {
  setTrainQuantity(trainQuantity.value - 1)
}

const canAfford = computed(() => {
  if (!props.currentResources || !props.building?.upgradeCost) return false
  return Object.entries(props.building.upgradeCost).every(
    ([resource, cost]) => (props.currentResources[resource] ?? 0) >= cost,
  )
})

const canAffordBatch = computed(() => {
  if (trainQuantity.value < 1) return false
  return (
    resourceStore.food >= VANDAL_FOOD_COST * trainQuantity.value &&
    resourceStore.iron >= VANDAL_IRON_COST * trainQuantity.value
  )
})

const nextLevel = computed(() => (props.building?.level ?? 1) + 1)

const nextProduction = computed(() => {
  if (props.building?.productionPerHour == null) return null
  const ratePerLevel = props.building.productionPerHour / props.building.level
  return props.building.productionPerHour + ratePerLevel
})

// The queue and its countdown are owned by useTrainingStore, so training keeps
// running when this card is closed. The card is a pure view over that state.
const trainingOrders = computed(() =>
  props.building?.buildingId == null
    ? []
    : trainingStore.ordersForBuilding(props.building.buildingId),
)

const trainError = ref(null)
const training = ref(false)

// Orders arrive sorted by finishesAt, so this building's next completion is first.
const activeOrder = computed(() => trainingOrders.value[0] ?? null)
const pendingOrders = computed(() => trainingOrders.value.slice(1))

const activeOrderDurationMs = computed(() => {
  if (!activeOrder.value) return TRAINING_DURATION_MS
  return activeOrder.value.quantity * TRAINING_DURATION_MS
})

const elapsedMs = computed(() => {
  if (!activeOrder.value) return 0
  return Math.max(0, activeOrderDurationMs.value - activeOrder.value.remainingMs)
})

const activeCountdown = computed(() => {
  if (!activeOrder.value) return ''
  return (activeOrder.value.remainingMs / 1000).toFixed(1) + 's'
})

async function handleTrainVandal() {
  trainError.value = null
  training.value = true
  try {
    const updatedQueue = await trainUnit(
      props.villageId,
      props.building.buildingId,
      trainQuantity.value,
    )
    if (Array.isArray(updatedQueue) && updatedQueue.length > 0) {
      trainingStore.setOrders(updatedQueue)
    }
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

            <div class="flex items-center justify-center gap-2">
              <button
                class="btn btn-sm btn-circle"
                data-testid="quantity-decrement"
                @click="decrementTrainQuantity"
              >
                ▼
              </button>
              <input
                type="number"
                class="input input-sm input-bordered w-20 text-center"
                data-testid="train-quantity-input"
                :min="MIN_TRAINING_BATCH_SIZE"
                :max="MAX_TRAINING_BATCH_SIZE"
                :value="trainQuantity"
                @change="setTrainQuantity(Number($event.target.value))"
              />
              <button
                class="btn btn-sm btn-circle"
                data-testid="quantity-increment"
                @click="incrementTrainQuantity"
              >
                ▲
              </button>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <div
                class="flex flex-col items-center justify-center border rounded-lg p-2 shadow-sm"
                data-testid="train-food-cost"
                :class="{
                  'border-error text-error': resourceStore.food < VANDAL_FOOD_COST * trainQuantity,
                  'border-success': resourceStore.food >= VANDAL_FOOD_COST * trainQuantity,
                }"
              >
                <div class="text-2xl">{{ icons.food }}</div>
                <div class="text-lg font-medium">{{ VANDAL_FOOD_COST * trainQuantity }}</div>
                <div class="text-xs opacity-60">have {{ resourceStore.food }}</div>
              </div>
              <div
                class="flex flex-col items-center justify-center border rounded-lg p-2 shadow-sm"
                data-testid="train-iron-cost"
                :class="{
                  'border-error text-error': resourceStore.iron < VANDAL_IRON_COST * trainQuantity,
                  'border-success': resourceStore.iron >= VANDAL_IRON_COST * trainQuantity,
                }"
              >
                <div class="text-2xl">{{ icons.iron }}</div>
                <div class="text-lg font-medium">{{ VANDAL_IRON_COST * trainQuantity }}</div>
                <div class="text-xs opacity-60">have {{ resourceStore.iron }}</div>
              </div>
            </div>

            <p v-if="trainError" class="text-error text-xs text-center">{{ trainError }}</p>

            <button
              class="btn btn-secondary w-full"
              data-testid="train-vandal-button"
              :disabled="!canAffordBatch || training"
              @click="handleTrainVandal"
            >
              {{ training ? 'Training...' : 'Train Vandal' }}
            </button>
          </div>

          <!-- Training queue section -->
          <div
            v-if="trainingOrders.length > 0"
            class="w-full flex flex-col gap-2"
            data-testid="training-queue"
          >
            <p class="text-xs font-semibold text-base-content/60 uppercase tracking-wider">
              Training Queue
            </p>

            <!-- Active order: progress bar + countdown -->
            <div v-if="activeOrder" class="flex flex-col gap-1">
              <div class="flex items-center justify-between text-sm">
                <span class="font-medium"
                  >⚔️ {{ activeOrder.unitType }}
                  <span class="badge badge-sm badge-outline ml-1">×{{ activeOrder.quantity }}</span>
                </span>
                <span class="text-warning font-mono font-bold" data-testid="countdown">{{
                  activeCountdown
                }}</span>
              </div>
              <progress
                class="progress progress-warning w-full"
                :value="elapsedMs"
                :max="activeOrderDurationMs"
              ></progress>
            </div>

            <!-- Queued orders -->
            <div
              v-for="order in pendingOrders"
              :key="order.id"
              class="flex items-center justify-between px-2 py-1 rounded bg-base-300 text-sm text-base-content/70"
              data-testid="queued-order"
            >
              <span
                >⚔️ {{ order.unitType }}
                <span class="badge badge-sm badge-outline ml-1">×{{ order.quantity }}</span>
              </span>
              <span class="text-xs">ready in {{ (order.remainingMs / 1000).toFixed(1) }}s</span>
            </div>
          </div>
        </template>

        <!-- Upgrade cost section (non-barracks only) -->
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
