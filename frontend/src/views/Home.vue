<script setup>
import { RouterView, useRouter } from 'vue-router'

import { computed, onMounted, ref, watch } from 'vue'
import Avatar from '@/components/Avatar.vue'

import { useSessionStore } from '@/stores/pinia.js'
import { useResourceStore } from '@/stores/resources.js'
import { storeToRefs } from 'pinia'
import { apiRequest } from '@/util/api/api.js'

const session = useSessionStore()
const router = useRouter()

async function handleLogout() {
  const keycloakIdToken = localStorage.getItem('keycloak_id_token')

  try {
    await apiRequest('/auth/logout', { method: 'POST' })
  } catch {
    // Proceed with local logout even if backend call fails
  }

  session.clearSession()

  if (keycloakIdToken) {
    const params = new URLSearchParams({
      post_logout_redirect_uri: 'http://localhost:5173/login',
      id_token_hint: keycloakIdToken,
      client_id: 'backend-service',
    })
    window.location.href = `http://localhost:8080/realms/villagevandals/protocol/openid-connect/logout?${params}`
  } else {
    router.push('/login')
  }
}

const player = ref({
  name: '',
})

const currentVillage = ref({ id: 0, name: '' })

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

    currentVillage.value.id = village.id
    localStorage.setItem('villageId', village.id)

    await resourceStore.refresh(village.id)
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    clearUserData()
  }
}

function clearUserData() {
  player.value = { name: '' }
  currentVillage.value = { id: 0, name: '' }
}

function goTo(section) {
  console.log(`Navigate to ${section}`)
}

const safeVillageId = computed(() => {
  return currentVillage.value?.id ?? Number(localStorage.getItem('villageId'))
})

async function updateResourceUI() {
  await resourceStore.refresh(safeVillageId.value)
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
            >🏰 <span class="text-secondary">{{ currentVillage.name }}</span></span
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
          <li><a @click="goTo('buildings')">🏗️ Buildings</a></li>
          <li><a @click="goTo('army')">🛡️ Army</a></li>
          <li>
            <RouterLink to="/map">🗺️ World Map</RouterLink>
          </li>
          <li><a @click="goTo('reports')">📜 Reports</a></li>
          <li><a @click="goTo('messages')">✉️ Messages</a></li>
        </ul>
        <button v-if="isAuthenticated" @click="handleLogout" class="btn btn-md w-full mt-4">
          Logout
        </button>
      </aside>

      <main class="flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>
