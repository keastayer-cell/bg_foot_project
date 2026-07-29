import { computed, ref } from 'vue'
import { describe, expect, it } from 'vitest'

import { useSeasonPlayoff } from './useSeasonPlayoff'

function createPlayoff(overrides = {}) {
  return useSeasonPlayoff({
    bracket: ref(overrides.bracket || { teamCount: 4, thirdPlaceEnabled: true, ties: [] }),
    season: ref({ playoffTeamCount: 4 }),
    tours: ref(overrides.tours || []),
    teamPositionMap: computed(() => new Map([['10', 1], ['20', 2]])),
  })
}

describe('useSeasonPlayoff', () => {
  it('builds placeholder sides, final and third-place cards', () => {
    const { leftColumns, rightColumns, centerCards } = createPlayoff()

    expect(leftColumns.value[0].cards).toHaveLength(1)
    expect(rightColumns.value[0].cards).toHaveLength(1)
    expect(centerCards.value.map((card) => card.roundKey)).toEqual(['FINAL', 'THIRD_PLACE'])
  })

  it('maps aggregate ties and includes standings positions', () => {
    const { centerCards } = createPlayoff({
      bracket: {
        teamCount: 4,
        ties: [{
          id: 7,
          roundCode: 'FINAL',
          roundOrder: 2,
          homeTeamId: 10,
          homeTeamName: 'Альфа',
          awayTeamId: 20,
          awayTeamName: 'Бета',
          aggregateHomeScore: 3,
          aggregateAwayScore: 2,
          matchIds: [77],
        }],
      },
    })

    expect(centerCards.value[0]).toEqual(expect.objectContaining({
      homeTeamName: 'Альфа (1)',
      awayTeamName: 'Бета (2)',
      homeScoreLabel: '3',
      awayScoreLabel: '2',
      dateLabel: 'Сыграно',
      matchId: 77,
    }))
  })

  it('maps playoff tour matches to navigable cards', () => {
    const { centerCards } = createPlayoff({
      tours: [{
        id: 5,
        name: 'Финал',
        stageType: 'PLAYOFF',
        sortOrder: 2,
        matches: [{
          id: 99,
          homeTeamName: 'Альфа',
          awayTeamName: 'Бета',
          homeScore: 1,
          awayScore: 0,
          status: 'VERIFIED',
        }],
      }],
    })

    expect(centerCards.value[0]).toEqual(expect.objectContaining({
      matchId: 99,
      statusLabel: 'Протокол подтвержден',
    }))
  })
})
