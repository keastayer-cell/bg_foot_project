import { useAuth } from '../store/auth'

export async function requireRouteAccess(to) {
  if (!to.meta.requiresAuth) return true

  const { isAuthenticated, hasRole, hasTeamAccess, ensureSession } = useAuth()
  if (!isAuthenticated.value) {
    await ensureSession({ forceRefresh: true })
  }

  if (!isAuthenticated.value) {
    return '/'
  }

  if (to.meta.requiresSuperAdmin && !hasRole('SUPER_ADMIN')) return '/'
  if (to.meta.requiresAdminPanel && !hasRole('SUPER_ADMIN') && !hasRole('REFEREE')) return '/'
  if (to.meta.requiresTeamRep && !hasRole('SUPER_ADMIN')) {
    if (!hasRole('TEAM_REP') || !hasTeamAccess()) return '/'
  }
  if (to.meta.requiresTransferManager && !hasRole('SUPER_ADMIN') && !hasRole('REFEREE')) {
    if (!hasRole('TEAM_REP') || !hasTeamAccess('canEditApplication')) return '/'
  }

  return true
}
