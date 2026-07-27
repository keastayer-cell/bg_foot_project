import { computed } from 'vue'

const EMPTY_SUMMARY = {
  played: 0,
  wins: 0,
  draws: 0,
  losses: 0,
  goalsFor: 0,
  goalsAgainst: 0,
  points: 0,
}

export function useSeasonMatrix({ season, teams, tours, standings }) {
  const regularSeasonMatches = computed(() => {
    return tours.value
      .filter((tour) => String(tour.stageType || '').toUpperCase() !== 'PLAYOFF')
      .flatMap((tour) => (Array.isArray(tour.matches) ? tour.matches : []))
      .sort((left, right) => {
        const leftTime = new Date(left.kickoffAt || 0).getTime()
        const rightTime = new Date(right.kickoffAt || 0).getTime()
        return leftTime - rightTime || Number(left.id) - Number(right.id)
      })
  })

  const standingsMap = computed(() => {
    return new Map(standings.value.map((row) => [String(row.teamId), row]))
  })

  const teamPositionMap = computed(() => {
    return new Map(standings.value.map((row) => [String(row.teamId), Number(row.position || 0)]))
  })

  const matrixSummaryMap = computed(() => {
    const summary = new Map()

    function ensure(teamId) {
      const key = String(teamId)
      if (!summary.has(key)) {
        const standingsRow = standingsMap.value.get(key)
        summary.set(key, {
          played: Number(standingsRow?.matchesPlayed || 0),
          wins: 0,
          draws: 0,
          losses: 0,
          goalsFor: Number(standingsRow?.goalsFor || 0),
          goalsAgainst: Number(standingsRow?.goalsAgainst || 0),
          points: Number(standingsRow?.points || 0),
        })
      }
      return summary.get(key)
    }

    for (const team of teams.value) {
      ensure(team.id)
    }

    for (const match of regularSeasonMatches.value) {
      if (!Number.isInteger(match.homeScore) || !Number.isInteger(match.awayScore)) continue

      const home = ensure(match.homeTeamId)
      const away = ensure(match.awayTeamId)

      if (match.homeScore > match.awayScore) {
        home.wins += 1
        away.losses += 1
      } else if (match.homeScore < match.awayScore) {
        away.wins += 1
        home.losses += 1
      } else {
        home.draws += 1
        away.draws += 1
      }
    }

    return summary
  })

  const matrixTeams = computed(() => {
    const ordered = []
    const used = new Set()

    for (const row of standings.value) {
      ordered.push({
        id: row.teamId,
        name: row.teamName,
        positionLabel: row.position,
      })
      used.add(String(row.teamId))
    }

    teams.value
      .filter((team) => !used.has(String(team.id)))
      .slice()
      .sort((left, right) => String(left.name || '').localeCompare(
        String(right.name || ''),
        'ru',
        { sensitivity: 'base' },
      ))
      .forEach((team) => {
        ordered.push({
          id: team.id,
          name: team.name,
          positionLabel: ordered.length + 1,
        })
      })

    return ordered
  })

  const expectedMatchCount = computed(() => {
    const roundsCount = Number(season.value?.roundsCount || 1)
    return Number.isFinite(roundsCount) && roundsCount > 0 ? roundsCount : 1
  })

  const matrixRows = computed(() => {
    const matchMap = new Map()

    for (const match of regularSeasonMatches.value) {
      const key = [String(match.homeTeamId), String(match.awayTeamId)].sort().join(':')
      if (!matchMap.has(key)) matchMap.set(key, [])
      matchMap.get(key).push(match)
    }

    return matrixTeams.value.map((team) => ({
      team,
      summary: matrixSummaryMap.value.get(String(team.id)) || { ...EMPTY_SUMMARY },
      cells: matrixTeams.value.map((opponent) => {
        if (String(team.id) === String(opponent.id)) {
          return {
            opponentTeamId: opponent.id,
            opponentName: opponent.name,
            isSelf: true,
            results: [],
          }
        }

        const key = [String(team.id), String(opponent.id)].sort().join(':')
        const results = (matchMap.get(key) || []).map((match) => {
          const hasScore = Number.isInteger(match.homeScore) && Number.isInteger(match.awayScore)
          const label = hasScore
            ? String(match.homeTeamId) === String(team.id)
              ? `${match.homeScore}:${match.awayScore}`
              : `${match.awayScore}:${match.homeScore}`
            : '—'

          return {
            key: match.id,
            matchId: match.id,
            label,
            pending: !hasScore,
          }
        })

        while (results.length < expectedMatchCount.value) {
          results.push({
            key: `placeholder-${team.id}-${opponent.id}-${results.length}`,
            matchId: null,
            label: '—',
            pending: true,
          })
        }

        return {
          opponentTeamId: opponent.id,
          opponentName: opponent.name,
          isSelf: false,
          results,
        }
      }),
    }))
  })

  return {
    matrixRows,
    matrixTeams,
    teamPositionMap,
  }
}
