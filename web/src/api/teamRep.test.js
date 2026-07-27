import { describe, expect, it, vi } from 'vitest'
import { createTeamRepApi } from './teamRep'

describe('team representative api', () => {
  it('loads seasons and players through the same team scope', async () => {
    const request = vi.fn().mockResolvedValue([])
    const api = createTeamRepApi(request)
    const scopedPath = (path) => `${path}?teamId=42`

    await api.getDashboard(scopedPath)

    expect(request).toHaveBeenNthCalledWith(
      1,
      '/api/team-rep/seasons?teamId=42',
      { method: 'GET' },
    )
    expect(request).toHaveBeenNthCalledWith(
      2,
      '/api/team-rep/players?teamId=42',
      { method: 'GET' },
    )
  })

  it('serializes a supplemental application update', async () => {
    const request = vi.fn().mockResolvedValue({})
    const api = createTeamRepApi(request)

    await api.addSeasonPlayers((path) => path, 5, [7, 8])

    expect(request).toHaveBeenCalledWith('/api/team-rep/seasons/5/players', {
      method: 'POST',
      body: JSON.stringify({ playerIds: [7, 8] }),
    })
  })
})
