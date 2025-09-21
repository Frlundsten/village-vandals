<template>
  <div class="flex items-center justify-center min-h-screen">
    <div
      class="flex items-center justify-center w-full min-h-screen bg-[url('../assets/jpeg/vv.png')] bg-cover bg-center bg-no-repeat"
    >
      <form
        class="md:w-full max-w-xs min-w-[224px] mx-auto fieldset bg-base-200 border-base-300 rounded-box w-xs border p-4"
        @submit.prevent="login"
      >
        <label class="label">Username</label>
        <input class="input" v-model="username" placeholder="Username" />
        <label class="label">Password</label>
        <input class="input" v-model="password" placeholder="Password" type="password" />
        <button class="btn btn-neutral mt-4" type="submit">Login</button>
        <router-link to="/register" class="btn btn-link mt-4">Register</router-link>
        <p v-if="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '../stores/pinia.js'
import { BASE_URL } from '@/util/util.js'

const username = ref('')
const password = ref('')
const error = ref(null)
const router = useRouter()

async function login() {
  error.value = null
  try {
    const response = await fetch(`${BASE_URL}/user/auth/generateToken`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        username: username.value,
        password: password.value,
      }),
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const token = await response.text()

    const session = useSessionStore()
    session.setToken(token)
  } catch (e) {
    error.value = 'Login failed, check credentials'
    await router.push('/login')
  }

  try {
    const response = await fetch(`${BASE_URL}/user`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('jwt_token')}`,
      },
    })
    if (!response.ok) throw new Error('Failed to fetch user info')

    const resp = await response.json()

    const village = resp.villages[0] || null
    const userName = resp.username

    if (resp && village) {
      localStorage.setItem('villageId', village.id)
      localStorage.setItem('username', userName)
      localStorage.setItem('villageName', `${userName}'s village`)
    } else {
      console.warn('No village found for user')
      throw new Error('No village found for user')
    }
  } catch (error) {
    console.error('Failed to fetch user info:', error)
    await router.push('/login')
  }

  await router.push('/village')
}
</script>
