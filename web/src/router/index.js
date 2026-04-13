import { createRouter, createWebHistory } from 'vue-router'
import Tours from '../pages/Tours.vue'
import Match from '../pages/Match.vue'
import Players from '../pages/Players.vue'
import Teams from '../pages/Teams.vue'
import Admin from '../pages/Admin.vue'
import ApiExplorer from '../pages/ApiExplorer.vue'
import ApiExplorerTest from '../pages/ApiExplorerTest.vue'
import CreateMatch from '../pages/CreateMatch.vue'
import TeamRepDashboard from '../pages/TeamRepDashboard.vue'
import { useAuth } from '../store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Tours },
    { path: '/players', component: Players },
    { path: '/teams', component: Teams },
    { path: '/create', component: CreateMatch, meta: { requiresAuth: true } },
    { path: '/admin', component: Admin, meta: { requiresAuth: true, requiresSuperAdmin: true } },
    { path: '/team-rep-dashboard', component: TeamRepDashboard, meta: { requiresAuth: true, requiresTeamRep: true } },
    { path: '/api-explorer', component: ApiExplorer },
    { path: '/api-explorer/test/:endpointKey', component: ApiExplorerTest },
    { path: '/match/:id', component: Match }
  ]
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth) return true

  const { isAuthenticated, hasRole } = useAuth()
  if (!isAuthenticated.value) {
    return '/'
  }

  if (to.meta.requiresSuperAdmin && !hasRole('SUPER_ADMIN')) return '/'
  if (to.meta.requiresTeamRep && !hasRole('TEAM_REP')) return '/'

  return true
})

export default router
