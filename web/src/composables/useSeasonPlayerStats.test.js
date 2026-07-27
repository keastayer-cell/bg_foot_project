import { ref } from 'vue'
import { describe, expect, it } from 'vitest'

import { useSeasonPlayerStats } from './useSeasonPlayerStats'

const PLAYERS = [
  { playerId: 1, fullName: 'Борис', goals: 2, yellowCards: 1, redCards: 0 },
  { playerId: 2, fullName: 'Антон', goals: 2, yellowCards: 0, redCards: 1 },
  { playerId: 3, fullName: 'Виктор', goals: 0, yellowCards: 3, redCards: 0 },
  { playerId: 4, fullName: 'Глеб', goals: 1, yellowCards: 0, redCards: 0 },
]

describe('useSeasonPlayerStats', () => {
  it('filters and sorts scorers by goals and then by name', () => {
    const { scorersStats } = useSeasonPlayerStats(ref(PLAYERS), ref('scorers'))

    expect(scorersStats.value.map((player) => player.playerId)).toEqual([2, 1, 4])
  })

  it('sorts discipline rows by red cards and then yellow cards', () => {
    const { disciplineStats } = useSeasonPlayerStats(ref(PLAYERS), ref('discipline'))

    expect(disciplineStats.value.map((player) => player.playerId)).toEqual([2, 3, 1])
  })

  it('reacts to mode changes and applies the row limit', () => {
    const mode = ref('scorers')
    const { topStatsRows } = useSeasonPlayerStats(ref(PLAYERS), mode, 2)

    expect(topStatsRows.value.map((player) => player.playerId)).toEqual([2, 1])

    mode.value = 'discipline'
    expect(topStatsRows.value.map((player) => player.playerId)).toEqual([2, 3])
  })

  it('provides mode-specific empty text', () => {
    const mode = ref('scorers')
    const { statsEmptyText } = useSeasonPlayerStats(ref([]), mode)

    expect(statsEmptyText.value).toContain('нет голов')

    mode.value = 'discipline'
    expect(statsEmptyText.value).toContain('нет желтых или красных карточек')
  })
})
