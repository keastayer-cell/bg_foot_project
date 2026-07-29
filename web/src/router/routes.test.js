import { describe, expect, it } from 'vitest'

import { routes } from './routes'

describe('route definitions smoke test', () => {
  it('keeps route paths and names unique', () => {
    const paths = routes.map((route) => route.path)
    const names = routes.map((route) => route.name)

    expect(new Set(paths).size).toBe(paths.length)
    expect(new Set(names).size).toBe(names.length)
  })

  it('contains the key public routes', () => {
    expect(routes.map((route) => route.path)).toEqual(expect.arrayContaining([
      '/',
      '/league',
      '/players',
      '/teams',
      '/teams/:slug',
      '/matches/:id',
    ]))
  })

  it('does not expose the removed client-only match editor', () => {
    expect(routes.map((route) => route.path)).not.toContain('/create')
  })

  it('redirects legacy singular match URLs to the canonical route', () => {
    const legacyRoute = routes.find((route) => route.path === '/match/:id')
    const target = legacyRoute.redirect({
      params: { id: '92' },
      query: {
        from: 'team-profile',
        team: 'meteor-novinki',
      },
    })

    expect(target).toEqual({
      name: 'match',
      params: { id: '92' },
      state: {
        returnContext: 'team-profile',
        teamSlug: 'meteor-novinki',
      },
    })
  })

  it.each([
    ['/admin', 'requiresAdminPanel'],
    ['/team-rep-dashboard', 'requiresTeamRep'],
    ['/team-rep-transfers', 'requiresTransferManager'],
    ['/api-explorer', 'requiresSuperAdmin'],
  ])('protects %s with auth and %s', (path, roleFlag) => {
    const route = routes.find((item) => item.path === path)

    expect(route?.meta?.requiresAuth).toBe(true)
    expect(route?.meta?.[roleFlag]).toBe(true)
  })
})
