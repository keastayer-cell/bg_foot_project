import { createRouter, createWebHistory } from 'vue-router'
import { requireRouteAccess } from './authGuard'
import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(requireRouteAccess)

export default router
