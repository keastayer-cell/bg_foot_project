import { computed } from 'vue'

function compareNames(left, right) {
  return String(left?.fullName || '').localeCompare(
    String(right?.fullName || ''),
    'ru',
    { sensitivity: 'base' },
  )
}

export function useSeasonPlayerStats(playerStats, statsMode, limit = 10) {
  const scorersStats = computed(() => {
    return [...playerStats.value]
      .filter((item) => Number(item?.goals || 0) > 0)
      .sort((left, right) => {
        const goalsDiff = Number(right?.goals || 0) - Number(left?.goals || 0)
        return goalsDiff || compareNames(left, right)
      })
  })

  const disciplineStats = computed(() => {
    return [...playerStats.value]
      .filter((item) => Number(item?.yellowCards || 0) > 0 || Number(item?.redCards || 0) > 0)
      .sort((left, right) => {
        const redDiff = Number(right?.redCards || 0) - Number(left?.redCards || 0)
        if (redDiff) return redDiff

        const yellowDiff = Number(right?.yellowCards || 0) - Number(left?.yellowCards || 0)
        return yellowDiff || compareNames(left, right)
      })
  })

  const activeStatsRows = computed(() => {
    return statsMode.value === 'discipline' ? disciplineStats.value : scorersStats.value
  })

  const topStatsRows = computed(() => activeStatsRows.value.slice(0, limit))

  const statsEmptyText = computed(() => {
    if (statsMode.value === 'discipline') {
      return 'В подтвержденных матчах выбранного сезона пока нет желтых или красных карточек.'
    }
    return 'В подтвержденных матчах выбранного сезона пока нет голов.'
  })

  return {
    scorersStats,
    disciplineStats,
    activeStatsRows,
    topStatsRows,
    statsEmptyText,
  }
}
