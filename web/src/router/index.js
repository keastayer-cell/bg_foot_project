import { createRouter, createWebHistory } from 'vue-router'
import Tours from '../pages/Tours.vue'
import Match from '../pages/Match.vue'
import Players from '../pages/Players.vue'
import Transfers from '../pages/Transfers.vue'
import Teams from '../pages/Teams.vue'
import TeamProfile from '../pages/TeamProfile.vue'
import Admin from '../pages/Admin.vue'
import ApiExplorer from '../pages/ApiExplorer.vue'
import ApiExplorerTest from '../pages/ApiExplorerTest.vue'
import CreateMatch from '../pages/CreateMatch.vue'
import ResetPasswordPage from '../pages/ResetPasswordPage.vue'
import TeamRepDashboard from '../pages/TeamRepDashboard.vue'
import TeamRepTransfers from '../pages/TeamRepTransfers.vue'
import SeasonApplicationsReview from '../pages/SeasonApplicationsReview.vue'
import LeagueOverview from '../pages/LeagueOverview.vue'
import { requireRouteAccess } from './authGuard'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Tours },
    { path: '/league', component: LeagueOverview },
    { path: '/transfers', component: Transfers },
    { path: '/players', component: Players },
    { path: '/teams', component: Teams },
    { path: '/teams/:id', component: TeamProfile },
    { path: '/create', component: CreateMatch, meta: { requiresAuth: true } },
    { path: '/admin', component: Admin, meta: { requiresAuth: true, requiresAdminPanel: true } },
    { path: '/team-rep-dashboard', component: TeamRepDashboard, meta: { requiresAuth: true, requiresTeamRep: true } },
    { path: '/team-rep-transfers', component: TeamRepTransfers, meta: { requiresAuth: true, requiresTransferManager: true } },
    { path: '/season-applications-review', component: SeasonApplicationsReview, meta: { requiresAuth: true, requiresAdminPanel: true } },
    { path: '/reset-password', component: ResetPasswordPage },
    { path: '/api-explorer', component: ApiExplorer, meta: { requiresAuth: true, requiresSuperAdmin: true } },
    { path: '/api-explorer/test/:endpointKey', component: ApiExplorerTest, meta: { requiresAuth: true, requiresSuperAdmin: true } },
    { path: '/match/:id', component: Match }
  ]
})

router.beforeEach(requireRouteAccess)

export default router
