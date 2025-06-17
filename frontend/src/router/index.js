import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import World from "@/views/WorldView.vue";
import Village from "@/components/Village.vue";
import LoginView from "@/views/LoginView.vue";
import {useSessionStore} from "@/stores/pinia.js";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/map',
      name: 'world',
      component: World,
      meta: { requiresAuth: true }
    },
    {
      path: '/village',
      name: 'village',
      component: Village,
      meta: { requiresAuth: true }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  const session = useSessionStore()

  await session.checkSession()

  if (to.meta.requiresAuth && !session.isAuthenticated) {
    return next('/login')
  }

  next()
})


export default router
