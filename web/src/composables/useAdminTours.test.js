import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import {
  canDeleteTourMatch,
  countHeadToHeadMeetings,
  matchScheduleStatusLabel,
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

  it('shows schedule states independently from the protocol', () => {
    expect(matchScheduleStatusLabel('SCHEDULED')).toBe('Запланирован')
    expect(matchScheduleStatusLabel('RESCHEDULED')).toBe('Перенесён')
    expect(matchScheduleStatusLabel('CANCELLED')).toBe('Отменён')
    expect(matchScheduleStatusLabel('TECHNICAL_RESULT')).toBe('Технический результат')
  })

  it('removes teams that reached the season meeting limit', () => {
    const { result } = createTours()
    result.seasonId.value = '3'
    result.teams.value = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }]
    result.matchForm.homeTeamId = '1'
    result.seasonMatches.value = [{ homeTeamId: 2, awayTeamId: 1 }]

    expect(result.availableAwayTeams.value.map((team) => team.id)).toEqual([3])
  })

  it('removes a home team when it has no available opponent', () => {
    const { result } = createTours()
    result.seasonId.value = '3'
    result.teams.value = [
      { id: 1, name: 'A' },
      { id: 2, name: 'B' },
      { id: 3, name: 'C' },
      { id: 4, name: 'D' },
    ]
    result.seasonMatches.value = [
      { homeTeamId: 1, awayTeamId: 2 },
      { homeTeamId: 1, awayTeamId: 3 },
      { homeTeamId: 1, awayTeamId: 4 },
    ]

    expect(result.availableHomeTeams.value.map((team) => team.id)).toEqual([2, 3, 4])
  })

  it('explains when every season matchup is already scheduled', () => {
    const { result } = createTours()
    result.seasonId.value = '3'
    result.teams.value = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }]
    result.seasonMatches.value = [
      { homeTeamId: 1, awayTeamId: 2 },
      { homeTeamId: 1, awayTeamId: 3 },
      { homeTeamId: 2, awayTeamId: 3 },
    ]

    expect(result.availableHomeTeams.value).toEqual([])
    expect(result.matchAvailabilityMessage.value).toContain('каждая пара команд уже включена в календарь')
  })

  it('loads season matches with one request and ignores non-regular tours', async () => {
    const request = vi.fn(async (url) => {
      if (url.startsWith('/api/tours?')) {
        return [
          { id: 4, stageType: 'REGULAR' },
          { id: 5, stageType: 'PLAYOFF' },
        ]
      }
      if (url.startsWith('/api/tours/matches?')) {
        return [
          { id: 10, tourId: 4, homeTeamId: 1, awayTeamId: 2 },
          { id: 11, tourId: 5, homeTeamId: 1, awayTeamId: 2 },
        ]
      }
      return []
    })
    const { result } = createTours(request)
    result.seasonId.value = '3'

    await result.onSeasonChange()

    expect(result.seasonMatches.value.map((match) => match.id)).toEqual([10])
    expect(request).toHaveBeenCalledWith(
      '/api/tours/matches?season_id=3&active_flag=1',
      { method: 'GET' }
    )
    expect(request.mock.calls.filter(([url]) => /^\/api\/tours\/\d+\/matches/.test(url))).toHaveLength(0)
  })

  it('creates all matches of a selected cup tie from the tours panel', async () => {
    const request = vi.fn(async (url) => {
      if (url.includes('/ties/9/matches')) {
        return {
          id: 7,
          type: 'CUP',
          drawStatus: 'CONFIRMED',
          ties: [{ id: 9, legCount: 2, homeTeam: { id: 1 }, awayTeam: { id: 2 }, matches: [{ id: 31 }, { id: 32 }] }],
        }
      }
      return []
    })
    const { result, successMessage } = createTours(request)
    result.seasonId.value = '3'
    result.competitions.value = [{
      id: 7,
      type: 'CUP',
      drawStatus: 'CONFIRMED',
      ties: [{ id: 9, legCount: 2, homeTeam: { id: 1 }, awayTeam: { id: 2 }, matches: [] }],
    }]
    result.competitionId.value = '7'
    result.selectedCupTieId.value = '9'
    result.onCupTieChange()
    const firstKickoff = '2026-09-10T18:00'
    const secondKickoff = '2026-09-17T18:00'
    result.cupKickoffDates.value[0] = firstKickoff
    result.cupKickoffDates.value[1] = secondKickoff

    await result.createCupMatches()

    expect(request).toHaveBeenCalledWith('/api/seasons/3/competitions/7/ties/9/matches', {
      method: 'POST',
      body: JSON.stringify({
        kickoffDates: [new Date(firstKickoff).toISOString(), new Date(secondKickoff).toISOString()],
      }),
    })
    expect(result.selectedCupTie.value.matches).toHaveLength(2)
    expect(successMessage.value).toBe('Матчи кубковой пары созданы.')
  })

  it('draws a cup from the tours panel using the selected team order', async () => {
    const drawnCup = {
      id: 7,
      type: 'CUP',
      drawStatus: 'DRAFT',
      teams: [
        { id: 1, seedNumber: 1 },
        { id: 2, seedNumber: 2 },
        { id: 3, seedNumber: 3 },
        { id: 4, seedNumber: 4 },
      ],
      ties: [
        { id: 10, roundOrder: 1, slotOrder: 1, homeTeam: { id: 4 }, awayTeam: { id: 2 } },
        { id: 11, roundOrder: 1, slotOrder: 2, homeTeam: { id: 1 }, awayTeam: { id: 3 } },
      ],
    }
    const request = vi.fn(async (url) => url.endsWith('/draw') ? drawnCup : [])
    const { result, successMessage } = createTours(request)
    result.seasonId.value = '3'
    result.competitions.value = [{
      ...drawnCup,
      drawStatus: 'NOT_DRAWN',
      ties: [],
    }]
    result.competitionId.value = '7'
    result.onCompetitionChange()
    result.cupDrawOrder.value = [4, 2, 1, 3]

    await result.drawCupManual()

    expect(request).toHaveBeenCalledWith('/api/seasons/3/competitions/7/draw', {
      method: 'POST',
      body: JSON.stringify({ orderedTeamIds: [4, 2, 1, 3] }),
    })
    expect(result.selectedCup.value.drawStatus).toBe('DRAFT')
    expect(result.cupDrawOrder.value).toEqual([4, 2, 1, 3])
    expect(successMessage.value).toBe('Черновик кубковой сетки сформирован.')
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

  it('saves a rescheduled match with a mandatory reason', async () => {
    const request = vi.fn(async () => [])
    const { result, successMessage } = createTours(request)
    result.seasonId.value = '3'
    result.selectedId.value = '4'
    result.tours.value = [{ id: 4, stageType: 'REGULAR' }]
    result.openScheduleEditor({ id: 9, scheduleStatus: 'SCHEDULED', kickoffAt: '2026-09-12T15:00:00Z' })
    result.scheduleForm.status = 'RESCHEDULED'
    result.scheduleForm.kickoffAt = '2026-09-13T18:30'
    result.scheduleForm.venueId = '2'
    result.scheduleForm.reason = 'Перенос по просьбе команд'

    await result.saveSchedule(9)

    expect(request).toHaveBeenCalledWith('/api/tours/4/matches/9/schedule', {
      method: 'PUT',
      body: JSON.stringify({
        status: 'RESCHEDULED',
        kickoffAt: new Date('2026-09-13T18:30').toISOString(),
        venueId: 2,
        reason: 'Перенос по просьбе команд',
      }),
    })
    expect(successMessage.value).toBe('Расписание матча обновлено.')
    expect(result.scheduleEditingId.value).toBe('')
  })
})
