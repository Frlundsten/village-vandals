<template>
  <div class="flex items-center justify-center min-h-screen">
    <div
        class="flex items-center justify-center w-full min-h-screen bg-[url('../assets/jpeg/vv.png')] bg-cover bg-center bg-no-repeat">
      <form
          class="md:w-full max-w-xs min-w-[224px] mx-auto fieldset bg-base-200 border-base-300 rounded-box w-xs border p-4"
          @submit.prevent="login">
        <label class="label">Username</label>
        <input class="input" v-model="username" placeholder="Username"/>
        <label class="label">Password</label>
        <input class="input" v-model="password" placeholder="Password" type="password"/>
        <button class="btn btn-neutral mt-4" type="submit">Login</button>
        <router-link to="/register" class="btn btn-link mt-4">Register</router-link>
        <p v-if="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useSessionStore} from '../stores/pinia.js'

const username = ref('')
const password = ref('')
const error = ref(null)
const router = useRouter()

async function login() {
  error.value = null;
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/user/auth/generateToken`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        username: username.value,
        password: password.value,
      }),
    });

    if (!response.ok) {
      throw new Error('Login failed');
    }

    const token = await response.text();

    const session = useSessionStore()
    session.setToken(token);
    await router.push('/village')
  } catch (e) {
    error.value = 'Login failed, check credentials'
  }
}
</script>
