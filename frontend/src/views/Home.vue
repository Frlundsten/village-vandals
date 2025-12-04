<script setup>
import { RouterView, useRouter } from 'vue-router'

import { computed, onMounted, ref, watch } from 'vue'
import Avatar from '@/components/Avatar.vue'

import { useSessionStore } from '@/stores/pinia.js'
import { storeToRefs } from 'pinia'
import { refreshStorage } from '@/util/api/resources.js'

const session = useSessionStore()
const router = useRouter()

async function handleLogout() {
  await session.logout()
  await router.push('/login')
}

const player = ref({
  name: '',
})

const currentVillage = ref({ id: 0, name: '' })

const resources = ref({
  food: 0,
  wood: 0,
  bricks: 0,
  iron: 0,
})

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
  try {
    currentVillage.value.id = localStorage.getItem('villageId')
    currentVillage.value.name = localStorage.getItem('villageName')
    resources.value = await refreshStorage(currentVillage.value.id)
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    clearUserData()
    await router.push('/login')
  }
}

function clearUserData() {
  player.value = { name: '' }
  currentVillage.value = {id: 0, name: '' }
  resources.value = { food: 0, wood: 0, bricks: 0, iron: 0 }
}

function goTo(section) {
  console.log(`Navigate to ${section}`)
}

const safeVillageId = computed(() => {
  return currentVillage.value?.id ?? Number(localStorage.getItem('villageId'))
})

async function updateResourceUI() {
  resources.value = await refreshStorage(safeVillageId.value)
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
            class="badge badge-accent flex items-center gap-2 px-4 py-3 rounded-xl shadow-md font-semibold text-lg bg-gradient-to-tr from-yellow-300 to-yellow-400 text-black"
          >
            <span class="text-2xl">🌾</span> {{ resources.food }}
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Wood">
          <div
            class="badge badge-success flex items-center gap-2 px-4 py-3 rounded-xl shadow-md font-semibold text-lg bg-gradient-to-tr from-green-400 to-green-500 text-white"
          >
            <span class="text-2xl">🌲</span> {{ resources.wood }}
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Brick">
          <div
            class="badge badge-warning flex items-center gap-2 px-4 py-3 rounded-xl shadow-md font-semibold text-lg bg-gradient-to-tr from-amber-400 to-amber-500 text-black"
          >
            <span class="text-2xl">🧱</span> {{ resources.bricks }}
          </div>
        </div>

        <div class="tooltip tooltip-bottom" data-tip="Iron">
          <div
            class="badge badge-info flex items-center gap-2 px-4 py-3 rounded-xl shadow-md font-semibold text-lg bg-gradient-to-tr from-sky-400 to-sky-500 text-white"
          >
            <span class="text-2xl">⚒️</span> {{ resources.iron }}
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
