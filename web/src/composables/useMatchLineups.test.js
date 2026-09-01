import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useMatchLineups } from './useMatchLineups'

describe('useMatchLineups', () => {
  it('keeps starters and substitutes separate when saving a lineup', async () => {
    const initialMatch = {
      id: 7,
      playersOnField: 2,
      protocol: { status: 'SCHEDULED' },
      homeTeam: { id: 10, shortName: 'Метеор' },
      homeLineup: {
        teamId: 10,
        teamName: 'Метеор Новинки',
        players: [
          { playerId: 1, playerName: 'Первый (Метеор)', isStarter: true },
          { playerId: 2, playerName: 'Второй (Метеор)', isStarter: true },
          { playerId: 3, playerName: 'Третий (Метеор)', isStarter: false },
        ],
        availablePlayers: [
          { playerId: 4, playerName: 'Четвёртый (Метеор)', suspended: false },
        ],
      },
      awayLineup: null,
    }
    const match = ref(initialMatch)
    const api = {
      getMatch: vi.fn().mockResolvedValue(initialMatch),
      saveLineup: vi.fn().mockResolvedValue(initialMatch),
    }
    const lineups = useMatchLineups({
      match,
      user: ref({}),
      hasRole: (role) => role === 'SUPER_ADMIN',
      api,
      onMatchUpdated: (value) => { match.value = value },
      clearPageError: vi.fn(),
    })

    await lineups.openAddPlayerModal(10)

    expect(lineups.selectedStarterPlayerIds.value).toEqual(['1', '2'])
    expect(lineups.selectedSubstitutePlayerIds.value).toEqual(['3'])
    expect(lineups.starterPlayerOptions.value.find((option) => option.value === '1').label).toBe('Первый')

    lineups.selectedStarterPlayerIds.value = ['1', '4']
    lineups.selectedSubstitutePlayerIds.value = ['3']
    await lineups.saveLineupSelection()

    expect(api.saveLineup).toHaveBeenCalledWith(7, 10, [1, 4], [3])
  })
})
