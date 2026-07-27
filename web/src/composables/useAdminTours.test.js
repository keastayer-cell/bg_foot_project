import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import {
  canDeleteTourMatch,
  countHeadToHeadMeetings,
  useAdminTours,
} from './useAdminTours'

function createTours(request = vi.fn(async () => [])) {
  const errorMessage = ref('')
  const successMessage = ref('')
  const result = useAdminTours({
    request,
    seasons: ref([{ id: 3, name: '2026/27', roundsCount: 1 }]),
    clearMessages: () => {
      errorMessage.value = ''
      successMessage.value = ''
    },
    errorMessage,
    successMessage,
    confirmAction: () => true,
  })
  return { errorMessage, result, successMessage }
}

describe('useAdminTours', () => {
  it('counts meetings regardless of home and away order', () => {
    const matches = [
      { homeTeamId: 1, awayTeamId: 2 },
      { homeTeamId: 2, awayTeamId: 1 },
      { homeTeamId: 1, awayTeamId: 3 },
    ]
    expect(countHeadToHeadMeetings(matches, 1, 2)).toBe(2)
  })

  it('removes teams that reached the season meeting limit', () => {
    const { result } = createTours()
    result.seasonId.value = '3'
    result.teams.value = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }]
    result.matchForm.homeTeamId = '1'
    result.seasonMatches.value = [{ homeTeamId: 2, awayTeamId: 1 }]

    expect(result.availableAwayTeams.value.map((team) => team.id)).toEqual([3])
  })

  it('publishes only a non-empty unpublished tour', async () => {
    const request = vi.fn(async (url) => {
      if (url.includes('/matches?')) return [{ id: 9 }]
      if (url.startsWith('/api/tours?')) return [{ id: 4, published: true }]
      return {}
    })
    const { result, successMessage } = createTours(request)
    result.seasonId.value = '3'
    result.selectedId.value = '4'
    result.tours.value = [{ id: 4, published: false }]
    result.matches.value = [{ id: 9 }]

    await result.publish()

    expect(request).toHaveBeenCalledWith('/api/tours/4/publish', { method: 'PUT' })
    expect(successMessage.value).toBe('Тур опубликован.')
  })

  it('does not delete a match after lineups were submitted', async () => {
    const request = vi.fn()
    const { errorMessage, result } = createTours(request)
    result.selectedId.value = '4'
    result.matches.value = [{ id: 9, protocolStatus: 'LINEUPS_SUBMITTED' }]

    expect(canDeleteTourMatch(result.matches.value[0])).toBe(false)
    await result.deleteMatch(9)

    expect(errorMessage.value).toContain('Нельзя удалить матч')
    expect(request).not.toHaveBeenCalled()
  })
})
