import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import World from '@/views/WorldView.vue'
import LoginView from '@/views/LoginView.vue'
import { useSessionStore } from '@/stores/pinia.js'
import RegisterView from '@/views/RegisterView.vue'
import VillageNew from '@/components/VillageNew.vue'

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
          meta: { requiresAuth: true }
        },
        {
          path: 'village/:villageId',
          name: 'Village',
          component: VillageNew,
          meta: { requiresAuth: true }
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
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
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

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  if (requiresAuth && !session.isAuthenticated) {
    return next('/login')
  }

  next()
})

export default router
