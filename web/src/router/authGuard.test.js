import { beforeEach, describe, expect, it, vi } from 'vitest'

const auth = vi.hoisted(() => ({
  isAuthenticated: { value: false },
  hasRole: vi.fn(),
  ensureSession: vi.fn(),
}))

vi.mock('../store/auth', () => ({
  useAuth: () => auth,
}))

import { requireRouteAccess } from './authGuard'

describe('requireRouteAccess', () => {
  beforeEach(() => {
    auth.isAuthenticated.value = false
    auth.hasRole.mockReset()
    auth.ensureSession.mockReset()
  })

  it('allows a public route without refreshing the session', async () => {
    await expect(requireRouteAccess({ meta: {} })).resolves.toBe(true)
    expect(auth.ensureSession).not.toHaveBeenCalled()
  })

  it('refreshes the session and redirects an unauthenticated user', async () => {
    await expect(requireRouteAccess({ meta: { requiresAuth: true } })).resolves.toBe('/')
    expect(auth.ensureSession).toHaveBeenCalledWith({ forceRefresh: true })
  })

  it('allows access when session refresh authenticates the user', async () => {
    auth.ensureSession.mockImplementation(async () => {
      auth.isAuthenticated.value = true
    })

    await expect(requireRouteAccess({ meta: { requiresAuth: true } })).resolves.toBe(true)
  })

  it.each([
    [{ requiresSuperAdmin: true }, ['SUPER_ADMIN']],
    [{ requiresAdminPanel: true }, ['REFEREE']],
    [{ requiresTeamRep: true }, ['TEAM_REP']],
    [{ requiresTransferManager: true }, ['REFEREE']],
  ])('allows an authenticated user with an accepted role for %o', async (accessMeta, roles) => {
    auth.isAuthenticated.value = true
    auth.hasRole.mockImplementation((role) => roles.includes(role))

    await expect(
      requireRouteAccess({ meta: { requiresAuth: true, ...accessMeta } }),
    ).resolves.toBe(true)
  })

  it.each([
    { requiresSuperAdmin: true },
    { requiresAdminPanel: true },
    { requiresTeamRep: true },
    { requiresTransferManager: true },
  ])('redirects an authenticated user without an accepted role for %o', async (accessMeta) => {
    auth.isAuthenticated.value = true
    auth.hasRole.mockReturnValue(false)

    await expect(
      requireRouteAccess({ meta: { requiresAuth: true, ...accessMeta } }),
    ).resolves.toBe('/')
  })
})
