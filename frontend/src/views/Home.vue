<script setup>
import { RouterView, useRouter, useRoute } from 'vue-router'

import { computed, onMounted, ref, watch } from 'vue'
import Avatar from '@/components/Avatar.vue'

import { useSessionStore } from '@/stores/pinia.js'
import { useResourceStore } from '@/stores/resources.js'
import { useArmyStore } from '@/stores/army.js'
import { useTrainingStore } from '@/stores/training.js'
import { storeToRefs } from 'pinia'
import { apiRequest } from '@/util/api/api.js'

const session = useSessionStore()
const router = useRouter()
const route = useRoute()

const armyStore = useArmyStore()
const trainingStore = useTrainingStore()

async function handleLogout() {
  const keycloakIdToken = localStorage.getItem('keycloak_id_token')

  try {
    await apiRequest('/auth/logout', { method: 'POST' })
  } catch {
    // Proceed with local logout even if backend call fails
  }

  // Every per-account store has to be torn down, or the previous player's units and
  // resource totals are still on screen for the first frames of the next session.
  trainingStore.stop()
  armyStore.reset()
  resourceStore.reset()
  session.clearSession()

  if (keycloakIdToken) {
    const params = new URLSearchParams({
      post_logout_redirect_uri: `${import.meta.env.VITE_APP_URL}/login`,
      id_token_hint: keycloakIdToken,
      client_id: 'backend-service',
    })
    window.location.href = `${import.meta.env.VITE_KEYCLOAK_BASE_URL}/realms/villagevandals/protocol/openid-connect/logout?${params}`
  } else {
    router.push('/login')
  }
}

const player = ref({
  name: '',
})

// VillageDTO carries no name, so the village is identified by its map coordinates.
const currentVillage = ref({ id: 0, x: null, y: null })

const resourceStore = useResourceStore()
const { food, wood, bricks, iron, foodPerHour, woodPerHour, bricksPerHour, ironPerHour } =
  storeToRefs(resourceStore)

const { isAuthenticated } = storeToRefs(session)

onMounted(() => {
  if (isAuthenticated.value) {
    loadUserData()
  }
})

watch(isAuthenticated, (loggedIn) => {
  if (loggedIn) {
    loadUserData()
  } else {
    clearUserData()
  }
})

async function loadUserData() {
  if (!isAuthenticated.value) return

  try {
    const userData = await apiRequest('/user')
    const village = userData.villages?.[0]
    if (!village) return

    player.value = { name: userData.username ?? '' }
    currentVillage.value = {
      id: village.id,
      x: village.xCoordinate ?? null,
      y: village.yCoordinate ?? null,
    }
    localStorage.setItem('villageId', village.id)

    // Independent loads: one failing must not blank the header or skip the others.
    // Hydrating the training store here rather than in the village view keeps the countdown
    // alive for the whole session, since Home stays mounted across route changes.
    const results = await Promise.allSettled([
      resourceStore.refresh(village.id),
      armyStore.refresh(village.id),
      trainingStore.hydrate(village.id),
    ])
    for (const result of results) {
      if (result.status === 'rejected') console.error('Village data load failed:', result.reason)
    }
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    clearUserData()
  }
}

function clearUserData() {
  player.value = { name: '' }
  currentVillage.value = { id: 0, x: null, y: null }
}

const showArmyPanel = computed(() => armyStore.roster.length > 0 && route.path !== '/army')

// `||` not `??`: the initial id is 0, and `??` only falls through on null/undefined, so the
// stored fallback never fired and the player could navigate to /village/0.
const safeVillageId = computed(() => {
  return currentVillage.value?.id || Number(localStorage.getItem('villageId')) || 0
})

async function updateResourceUI() {
  if (!safeVillageId.value) return
  try {
    await resourceStore.refresh(safeVillageId.value)
  } catch (error) {
    console.error('Failed to refresh resources:', error)
  }
}
</script>

<template>
  <div class="h-screen flex flex-col bg-base-200">
    <header
      class="flex flex-col sm:flex-row bg-gradient-to-r from-base-300 to-base-200 text-base-content shadow-lg px-6 py-4 border-b border-base-300"
    >
      <div class="flex items-center gap-4">
        <Avatar />
        <div class="flex flex-col">
          <span class="text-lg font-bold text-primary">
            {{ player.name }}
          </span>
          <span class="text-sm opacity-80"
            >🏰
            <span class="text-secondary">
              <template v-if="currentVillage.x !== null"
                >({{ currentVillage.x }}|{{ currentVillage.y }})</template
              >
            </span></span
          >
        </div>
      </div>

      <div class="flex flex-wrap gap-6 items-center mx-auto mt-5 sm:mt-0">
        <div class="tooltip tooltip-bottom" data-tip="Food">
          <div
            class="flex flex-col items-center px-4 py-2 rounded-xl shadow-md bg-gradient-to-tr from-yellow-300 to-yellow-400 text-black"
          >
            <div class="flex items-center gap-2 font-semibold text-lg">
              <span class="text-2xl">🌾</span> {{ food }}
            </div>
            <span class="text-xs font-medium opacity-75">+{{ foodPerHour }}/hr</span>
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Wood">
          <div
            class="flex flex-col items-center px-4 py-2 rounded-xl shadow-md bg-gradient-to-tr from-green-400 to-green-500 text-white"
          >
            <div class="flex items-center gap-2 font-semibold text-lg">
              <span class="text-2xl">🌲</span> {{ wood }}
            </div>
            <span class="text-xs font-medium opacity-75">+{{ woodPerHour }}/hr</span>
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Brick">
          <div
            class="flex flex-col items-center px-4 py-2 rounded-xl shadow-md bg-gradient-to-tr from-amber-400 to-amber-500 text-black"
          >
            <div class="flex items-center gap-2 font-semibold text-lg">
              <span class="text-2xl">🧱</span> {{ bricks }}
            </div>
            <span class="text-xs font-medium opacity-75">+{{ bricksPerHour }}/hr</span>
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Iron">
          <div
            class="flex flex-col items-center px-4 py-2 rounded-xl shadow-md bg-gradient-to-tr from-sky-400 to-sky-500 text-white"
          >
            <div class="flex items-center gap-2 font-semibold text-lg">
              <span class="text-2xl">⚒️</span> {{ iron }}
            </div>
            <span class="text-xs font-medium opacity-75">+{{ ironPerHour }}/hr</span>
          </div>
        </div>
      </div>
    </header>

    <div class="flex flex-1 overflow-hidden">
      <aside class="w-[12%] min-w-[120px] bg-base-100 p-4 shadow-inner overflow-y-auto">
        <ul class="menu rounded-box bg-base-200 w-full">
          <li>
            <RouterLink
              @click="updateResourceUI"
              :to="{ name: 'Village', params: { villageId: safeVillageId } }"
              >🏘️️ Village</RouterLink
            >
          </li>
          <li><RouterLink to="/buildings">🏗️ Buildings</RouterLink></li>
          <li><RouterLink to="/army">🛡️ Army</RouterLink></li>
          <li>
            <RouterLink to="/map">🗺️ World Map</RouterLink>
          </li>
          <!-- Not implemented yet. Rendered inert rather than wired to a handler that
               does not exist — clicking these used to throw. -->
          <li class="menu-disabled" title="Coming soon"><a>📜 Reports</a></li>
          <li class="menu-disabled" title="Coming soon"><a>✉️ Messages</a></li>
        </ul>
        <button
          v-if="isAuthenticated"
          data-testid="logout-button"
          @click="handleLogout"
          class="btn btn-md w-full mt-4"
        >
          Logout
        </button>

        <!-- Army mini-panel: compact unit summary below Logout -->
        <div v-if="showArmyPanel" class="mt-3" data-testid="army-mini-panel">
          <div
            v-for="unit in armyStore.roster"
            :key="unit.unitType"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-base-200 mb-1 text-sm"
          >
            <span>⚔️</span>
            <span class="flex-1 font-medium">{{ unit.unitType }}</span>
            <span class="badge badge-sm">× {{ unit.count }}</span>
          </div>
        </div>
      </aside>

      <main class="flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>
