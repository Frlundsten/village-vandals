<template>
  <div class="flex items-center justify-center min-h-screen">
    <div
        class="flex items-center justify-center w-full min-h-screen bg-[url('../assets/jpeg/vv.png')] bg-cover bg-center bg-no-repeat"
    >
      <form
          class="md:w-full max-w-xs min-w-[224px] mx-auto fieldset bg-base-200 border-base-300 rounded-box w-xs border p-4"
          @submit.prevent="registerUser"
      >
        <label class="label">Username</label>
        <input class="input" v-model="username" placeholder="Username" required />

        <label class="label mt-2">Email</label>
        <input class="input" v-model="email" type="email" placeholder="Email" required />

        <label class="label mt-2">Password</label>
        <input class="input" v-model="password" type="password" placeholder="Password" required />

        <button class="btn btn-neutral mt-4 w-full" type="submit">Register</button>

        <router-link to="/login" class="btn btn-link mt-4 block text-center">
          Already have an account?
        </router-link>

        <p v-if="message" :class="{'text-green-600': success, 'text-red-600': !success}" class="mt-2">
          {{ message }}
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const email = ref('')
const password = ref('')
const message = ref('')
const success = ref(false)

async function registerUser() {
  message.value = ''
  success.value = false

  const userInfo = {
    username: username.value,
    email: email.value,
    password: password.value,
  }

  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/user/addNewUser`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userInfo),
    })

    if (response.ok) {
      message.value = 'User registered successfully! Redirecting to login...'
      success.value = true
      username.value = ''
      email.value = ''
      password.value = ''

      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      const errText = await response.text()
      console.log(errText)
      message.value = `Registration failed`
    }
  } catch (error) {
    message.value = `Error: ${error.message}`
  }
}
</script>
