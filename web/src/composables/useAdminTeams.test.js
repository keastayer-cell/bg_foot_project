import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { normalizePositiveIdList, useAdminTeams } from './useAdminTeams'

function createTeams(request = vi.fn(async () => [])) {
  const errorMessage = ref('')
  const successMessage = ref('')
  const teams = ref([])
  const players = ref([
    { id: 1, fullName: 'Первый Игрок' },
    { id: 2, fullName: 'Второй Игрок', activeSeasonTeamId: 10 },
  ])
  const result = useAdminTeams({
    request,
    teams,
    players,
    loadPlayers: vi.fn(async () => {}),
    clearMessages: () => {
      errorMessage.value = ''
      successMessage.value = ''
    },
    errorMessage,
    successMessage,
    formatDateOnly: (value) => value,
    confirmAction: () => true,
  })
  return { errorMessage, result }
}

describe('useAdminTeams', () => {
  it('normalizes, deduplicates and filters player ids', () => {
    expect(normalizePositiveIdList(['2', 2, 0, -1, 'bad', 7])).toEqual([2, 7])
  })

  it('calculates season capacity and blocks an oversized batch', async () => {
    const request = vi.fn(async () => [])
    const { errorMessage, result } = createTeams(request)
    result.editingId.value = 10
    result.selectedSeasonId.value = '5'
    result.seasonOptions.value = [{ id: 5, maxRosterSize: 3 }]
    result.seasonPlayers.value = [
      { id: 1, selectedForSeason: true },
      { id: 2, selectedForSeason: true },
      { id: 3, selectedForSeason: false },
    ]
    result.seasonToAddIds.value = ['3', '4']

    expect(result.seasonRemainingSlots.value).toBe(1)
    expect(result.willSelectedPlayersExceedSeasonLimit.value).toBe(true)

    await result.addPlayersToSeason()

    expect(errorMessage.value).toBe('Нельзя превысить лимит заявки сезона: 3.')
    expect(request).not.toHaveBeenCalled()
  })

  it('creates one roster request per unique selected player', async () => {
    const request = vi.fn(async (url) => {
      if (url.endsWith('/players') || url.endsWith('/seasons')) return []
      return {}
    })
    const { result } = createTeams(request)
    result.editingId.value = 10
    result.rosterToAddIds.value = ['2', 2, '7']

    await result.addPlayersToRoster()

    expect(request).toHaveBeenCalledWith('/api/teams/10/players/2', { method: 'POST' })
    expect(request).toHaveBeenCalledWith('/api/teams/10/players/7', { method: 'POST' })
  })
})
