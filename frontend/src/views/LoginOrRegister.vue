<script setup>
const KEYCLOAK_BASE = import.meta.env.VITE_KEYCLOAK_BASE_URL
const REALM = 'villagevandals'
const CLIENT_ID = 'backend-service'
const REDIRECT_URI = encodeURIComponent(`${import.meta.env.VITE_APP_URL}/auth`)

const authUrl =
  `${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/auth` +
  `?client_id=${CLIENT_ID}` +
  `&response_type=code` +
  `&scope=openid` +
  `&redirect_uri=${REDIRECT_URI}`

function login() {
  window.location.href = authUrl
}

function register() {
  window.location.href = `${authUrl}&kc_action=register`
}
</script>

<template>
  <div class="flex items-center justify-center min-h-screen">
    <div
      class="flex items-center justify-center w-full min-h-screen bg-[url('../assets/jpeg/vv.png')] bg-cover bg-center bg-no-repeat"
    >
      <button @click="login" class="btn primary">Login</button>

      <button @click="register" class="btn secondary">Register</button>
    </div>
  </div>
</template>

<style scoped></style>
