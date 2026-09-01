import { describe, expect, it, vi } from 'vitest'
import { createMatchesApi } from './matches'

describe('matches api', () => {
  it('encodes identifiers and serializes lineup updates', async () => {
    const authorizedRequest = vi.fn().mockResolvedValue({ id: 1 })
    const api = createMatchesApi({
      optionalRequest: vi.fn(),
      optionalRawRequest: vi.fn(),
      authorizedRequest,
    })

    await api.saveLineup('match/1', 'team 2', [10, 11], [12])

    expect(authorizedRequest).toHaveBeenCalledWith(
      '/api/matches/match%2F1/lineups/team%202',
      {
        method: 'PUT',
        body: JSON.stringify({ starterPlayerIds: [10, 11], substitutePlayerIds: [12] }),
      },
    )
  })

  it('uses the raw request for protocol downloads', async () => {
    const optionalRawRequest = vi.fn().mockResolvedValue(new Response())
    const api = createMatchesApi({
      optionalRequest: vi.fn(),
      optionalRawRequest,
      authorizedRequest: vi.fn(),
    })

    await api.downloadProtocol(7)

    expect(optionalRawRequest).toHaveBeenCalledWith(
      '/api/matches/7/protocol/pdf',
      { method: 'GET' },
    )
  })
})
