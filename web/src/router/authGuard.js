import { useAuth } from '../store/auth'

export async function requireRouteAccess(to) {
  if (!to.meta.requiresAuth) return true

  const { isAuthenticated, hasRole, ensureSession } = useAuth()
  if (!isAuthenticated.value) {
    await ensureSession({ forceRefresh: true })
  }

  if (!isAuthenticated.value) {
    return '/'
  }

  if (to.meta.requiresSuperAdmin && !hasRole('SUPER_ADMIN')) return '/'
  if (to.meta.requiresAdminPanel && !hasRole('SUPER_ADMIN') && !hasRole('REFEREE')) return '/'
  if (to.meta.requiresTeamRep && !hasRole('TEAM_REP') && !hasRole('SUPER_ADMIN')) return '/'
  if (to.meta.requiresTransferManager && !hasRole('TEAM_REP') && !hasRole('SUPER_ADMIN') && !hasRole('REFEREE')) return '/'

  return true
}
