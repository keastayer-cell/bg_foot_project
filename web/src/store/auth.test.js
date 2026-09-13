import { describe, expect, it } from 'vitest'
import { normalizeUserAccess } from './auth'

describe('normalizeUserAccess', () => {
  it('does not invent a team when the backend returned no active scope', () => {
    const result = normalizeUserAccess(
      { id: 17, email: 'rep@example.test', name: 'Представитель', roles: ['TEAM_REP'] },
      { roles: ['TEAM_REP'], teamScopes: [] },
    )

    expect(result.teamId).toBeNull()
    expect(result.teamName).toBe('')
    expect(result.teamScope).toBeNull()
    expect(result.teamScopes).toEqual([])
  })

  it('uses the real team and permissions from the access profile', () => {
    const result = normalizeUserAccess(
      { id: 17, email: 'rep@example.test', name: 'Представитель' },
      {
        roles: ['TEAM_REP'],
        roleAnnotations: [{ code: 'TEAM_REP', title: 'Представитель команды' }],
        teamScopes: [{
          teamId: 42,
          teamName: 'Атлетик Богородск',
          canEditRoster: true,
          canEditApplication: false,
          validFrom: '2026-09-01T10:00:00Z',
        }],
      },
    )

    expect(result.teamId).toBe(42)
    expect(result.teamName).toBe('Атлетик Богородск')
    expect(result.teamScope).toMatchObject({
      teamId: 42,
      canEditRoster: true,
      canEditApplication: false,
    })
    expect(result.teamScopes).toHaveLength(1)
  })

  it('ignores malformed team scopes', () => {
    const result = normalizeUserAccess(
      { id: 17, roles: ['TEAM_REP'] },
      {
        roles: ['TEAM_REP'],
        teamScopes: [
          { teamId: null, teamName: 'Без ID' },
          { teamId: 7, teamName: '' },
        ],
      },
    )

    expect(result.teamId).toBeNull()
    expect(result.teamScopes).toEqual([])
  })
})
