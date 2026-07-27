import { ref } from 'vue'
import { describe, expect, it } from 'vitest'

import { useSeasonMatrix } from './useSeasonMatrix'

function createMatrix() {
  return useSeasonMatrix({
    season: ref({ roundsCount: 2 }),
    teams: ref([
      { id: 1, name: 'Альфа' },
      { id: 2, name: 'Бета' },
    ]),
    standings: ref([
      { teamId: 1, teamName: 'Альфа', position: 1, matchesPlayed: 1, goalsFor: 2, goalsAgainst: 1, points: 3 },
      { teamId: 2, teamName: 'Бета', position: 2, matchesPlayed: 1, goalsFor: 1, goalsAgainst: 2, points: 0 },
    ]),
    tours: ref([
      {
        stageType: 'REGULAR',
        matches: [
          { id: 10, homeTeamId: 1, awayTeamId: 2, homeScore: 2, awayScore: 1 },
        ],
      },
    ]),
  })
}

describe('useSeasonMatrix', () => {
  it('keeps standings order and positions', () => {
    const { matrixTeams, teamPositionMap } = createMatrix()

    expect(matrixTeams.value.map((team) => team.id)).toEqual([1, 2])
    expect(teamPositionMap.value.get('2')).toBe(2)
  })

  it('builds mirrored score cells and pads missing rounds', () => {
    const { matrixRows } = createMatrix()
    const alphaAgainstBeta = matrixRows.value[0].cells[1]
    const betaAgainstAlpha = matrixRows.value[1].cells[0]

    expect(alphaAgainstBeta.results.map((result) => result.label)).toEqual(['2:1', '—'])
    expect(betaAgainstAlpha.results.map((result) => result.label)).toEqual(['1:2', '—'])
  })

  it('derives win and loss summaries from completed matches', () => {
    const { matrixRows } = createMatrix()

    expect(matrixRows.value[0].summary.wins).toBe(1)
    expect(matrixRows.value[1].summary.losses).toBe(1)
  })
})
