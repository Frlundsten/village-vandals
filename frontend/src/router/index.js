import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import World from '@/views/WorldView.vue'
import { useSessionStore } from '@/stores/pinia.js'
import VillageNew from '@/components/VillageNew.vue'
import BuildingsPanel from '@/components/BuildingsPanel.vue'
import LoginOrRegister from '@/views/LoginOrRegister.vue'
import AuthView from '@/views/AuthView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true },
      children: [
        {
          name: '-',
          path: '',
          redirect: { name: 'village' },
          meta: { requiresAuth: true },
        },
        {
          path: 'village/:villageId',
          name: 'Village',
          component: VillageNew,
          meta: { requiresAuth: true },
        },
        {
          path: 'map',
          name: 'world',
          component: World,
          meta: { requiresAuth: true },
        },
        {
          path: 'village',
          name: 'village',
          component: VillageNew,
          meta: { requiresAuth: true },
        },
        {
          path: 'buildings',
          name: 'Buildings',
          component: BuildingsPanel,
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: LoginOrRegister,
    },
    {
      path: '/auth',
      name: 'auth',
      component: AuthView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

router.beforeEach((to, from, next) => {
  const session = useSessionStore()

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (requiresAuth && !session.isAuthenticated) {
    return next('/login')
  }

  next()
})

export default router
