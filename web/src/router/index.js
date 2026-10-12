import { createRouter, createWebHistory } from 'vue-router'
import { requireRouteAccess } from './authGuard'
import { routes } from './routes'
import { cleanCompetitionUrl } from '../store/competitionContext'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(cleanCompetitionUrl)
router.beforeEach(requireRouteAccess)

export default router
