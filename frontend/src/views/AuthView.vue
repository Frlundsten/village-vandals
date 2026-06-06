<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/pinia.js'
import { apiRequest } from '@/util/api/api.js'
import { BASE_URL } from '@/util/util.js'

const REDIRECT_URI = `${import.meta.env.VITE_APP_URL}/auth`
const router = useRouter()
const session = useSessionStore()

onMounted(async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('code')

  if (!code) {
    router.push('/login')
    return
  }

  try {
    const response = await fetch(`${BASE_URL}/auth/callback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, redirectUri: REDIRECT_URI }),
    })

    if (!response.ok) {
      console.error('Token exchange failed', response.status)
      router.push('/login')
      return
    }

    const data = await response.json()
    session.setToken(data.accessToken, data.keycloakIdToken ?? null)

    // Fetch user info now so VillageNew gets the right villageId immediately
    // instead of racing against Home.vue's async load
    const userInfo = await apiRequest('/user')
    const villageId = userInfo.villages?.[0]?.id
    if (villageId) {
      localStorage.setItem('villageId', villageId)
      router.push({ name: 'Village', params: { villageId } })
    } else {
      router.push('/')
    }
  } catch (err) {
    console.error('Error exchanging code for token', err)
    router.push('/login')
  }
})
</script>

<template></template>
