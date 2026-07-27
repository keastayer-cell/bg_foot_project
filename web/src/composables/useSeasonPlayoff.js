import { computed } from 'vue'

function detectRoundKey(tour) {
  const title = String(tour?.name || '').toLowerCase()

  if (/треть|3\s*мест|бронз/.test(title)) return 'THIRD_PLACE'
  if (/1\s*[\/\\-]\s*8|1\/8|восьм|round\s*of\s*16/.test(title)) return 'ROUND_OF_16'
  if (/1\s*[\/\\-]\s*4|1\/4|четверт|quarter/.test(title)) return 'QUARTERFINAL'
  if (/1\s*[\/\\-]\s*2|1\/2|полуфин|semi/.test(title)) return 'SEMIFINAL'
  if (/финал|final/.test(title)) return 'FINAL'
  return ''
}

function roundLabel(roundKey, fallbackName = '') {
  const labels = {
    ROUND_OF_16: '1/8 финала',
    QUARTERFINAL: '1/4 финала',
    SEMIFINAL: '1/2 финала',
    FINAL: 'Финал',
    THIRD_PLACE: 'Матч за 3 место',
  }
  return labels[roundKey] || fallbackName || 'Плей-офф'
}

function seededPlaceholderLabel(roundKey, seedNumber) {
  if (roundKey === 'ROUND_OF_16' || roundKey === 'QUARTERFINAL') {
    return `Команда (${seedNumber})`
  }
  if (roundKey === 'SEMIFINAL') return 'Победитель пары'
  if (roundKey === 'FINAL') return 'Победитель 1/2'
  if (roundKey === 'THIRD_PLACE') return 'Проигравший 1/2'
  return `Команда ${seedNumber}`
}

function compactMatchDateTime(value) {
  if (!value) return 'Дата не назначена'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Дата не назначена'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function matchStatusLabel(status) {
  if (status === 'LIVE') return 'Матч идет'
  if (status === 'FINISHED') return 'Матч завершен'
  if (status === 'VERIFIED') return 'Протокол подтвержден'
  if (status === 'LINEUPS_SUBMITTED') return 'Заявки поданы'
  return 'Матч запланирован'
}

function matchesWord(count) {
  const normalized = Math.abs(Number(count || 0))
  const lastTwo = normalized % 100
  const last = normalized % 10

  if (lastTwo >= 11 && lastTwo <= 14) return 'ей'
  if (last === 1) return ''
  if (last >= 2 && last <= 4) return 'а'
  return 'ей'
}

export function useSeasonPlayoff({ bracket, season, tours, teamPositionMap }) {
  const bracketTies = computed(() => {
    return Array.isArray(bracket.value?.ties) ? bracket.value.ties : []
  })

  const playoffTours = computed(() => {
    return [...tours.value]
      .filter((tour) => String(tour.stageType || '').toUpperCase() === 'PLAYOFF')
      .map((tour) => ({
        ...tour,
        matches: [...(Array.isArray(tour.matches) ? tour.matches : [])].sort((left, right) => {
          const leftTime = new Date(left.kickoffAt || 0).getTime()
          const rightTime = new Date(right.kickoffAt || 0).getTime()
          return leftTime - rightTime || Number(left.id) - Number(right.id)
        }),
      }))
      .sort((left, right) => {
        const orderDiff = Number(left.sortOrder || 0) - Number(right.sortOrder || 0)
        return orderDiff || Number(left.id) - Number(right.id)
      })
  })

  const roundBlueprint = computed(() => {
    const teamCount = Number(bracket.value?.teamCount || season.value?.playoffTeamCount || 0)
    const rounds = []

    if (teamCount >= 16) rounds.push({ key: 'ROUND_OF_16', label: '1/8 финала', expectedTieCount: 8 })
    if (teamCount >= 8) rounds.push({ key: 'QUARTERFINAL', label: '1/4 финала', expectedTieCount: 4 })
    if (teamCount >= 4) rounds.push({ key: 'SEMIFINAL', label: '1/2 финала', expectedTieCount: 2 })
    if (teamCount >= 2) rounds.push({ key: 'FINAL', label: 'Финал', expectedTieCount: 1 })

    if (Boolean(bracket.value?.thirdPlaceEnabled) && teamCount >= 4) {
      rounds.push({ key: 'THIRD_PLACE', label: 'Матч за 3 место', expectedTieCount: 1 })
    }

    return rounds
  })

  const bracketColumns = computed(() => {
    const blueprint = roundBlueprint.value
    const blueprintKeys = blueprint.map((round) => round.key)

    if (bracketTies.value.length) {
      const groupedTies = new Map()

      for (const tie of bracketTies.value) {
        const roundKey = String(tie.roundCode || '')
        if (!roundKey) continue

        const existingGroup = groupedTies.get(roundKey)
        if (existingGroup) {
          existingGroup.ties.push(tie)
          continue
        }

        groupedTies.set(roundKey, {
          key: roundKey,
          label: roundLabel(roundKey),
          expectedTieCount: blueprint.find((round) => round.key === roundKey)?.expectedTieCount || 1,
          ties: [tie],
          tours: [],
          matchCount: 0,
          order: Number(tie.roundOrder || 999),
        })
      }

      return [...groupedTies.values()].sort((left, right) => left.order - right.order)
    }

    const groups = new Map()
    let fallbackIndex = 0

    for (const tour of playoffTours.value) {
      let roundKey = detectRoundKey(tour)
      if (!roundKey) {
        roundKey = blueprintKeys[fallbackIndex] || `PLAYOFF_${fallbackIndex + 1}`
        fallbackIndex += 1
      } else if (roundKey !== 'THIRD_PLACE') {
        const explicitIndex = blueprintKeys.indexOf(roundKey)
        if (explicitIndex >= 0 && explicitIndex >= fallbackIndex) {
          fallbackIndex = explicitIndex + 1
        }
      }

      const blueprintRound = blueprint.find((round) => round.key === roundKey)
      const preparedTour = {
        ...tour,
        key: `${roundKey}-${tour.id}`,
      }
      const existingGroup = groups.get(roundKey)

      if (existingGroup) {
        existingGroup.tours.push(preparedTour)
        existingGroup.matchCount += preparedTour.matches.length
        continue
      }

      groups.set(roundKey, {
        key: roundKey,
        label: roundKey === 'THIRD_PLACE' ? 'Матч за 3 место' : roundLabel(roundKey, tour.name),
        expectedTieCount: blueprintRound?.expectedTieCount || Math.max(preparedTour.matches.length, 1),
        tours: [preparedTour],
        matchCount: preparedTour.matches.length,
        order: roundKey === 'THIRD_PLACE'
          ? 999
          : blueprintKeys.indexOf(roundKey) >= 0
            ? blueprintKeys.indexOf(roundKey)
            : blueprint.length + groups.size,
      })
    }

    const columns = blueprint.map((round, index) => {
      const existingGroup = groups.get(round.key)
      return {
        key: round.key,
        label: round.label,
        expectedTieCount: round.expectedTieCount,
        tours: existingGroup?.tours || [],
        matchCount: existingGroup?.matchCount || 0,
        order: index,
      }
    })

    if (groups.has('THIRD_PLACE')) columns.push(groups.get('THIRD_PLACE'))
    return columns.sort((left, right) => left.order - right.order)
  })

  function participantLabel(tie, side, roundKey) {
    const teamNameKey = side === 'home' ? 'homeTeamName' : 'awayTeamName'
    const teamIdKey = side === 'home' ? 'homeTeamId' : 'awayTeamId'
    const seedKey = side === 'home' ? 'homeSeed' : 'awaySeed'
    const sourceResultKey = side === 'home' ? 'homeSourceResult' : 'awaySourceResult'
    const sourceTieIdKey = side === 'home' ? 'homeSourceTieId' : 'awaySourceTieId'

    if (tie?.[teamNameKey]) {
      const position = teamPositionMap.value.get(String(tie?.[teamIdKey] || '')) || tie?.[seedKey] || null
      return position ? `${tie[teamNameKey]} (${position})` : tie[teamNameKey]
    }
    if (tie?.[seedKey]) return `Команда (${tie[seedKey]})`
    if (tie?.[sourceTieIdKey]) {
      if (String(tie[sourceResultKey] || '').toUpperCase() === 'LOSER') {
        return roundKey === 'THIRD_PLACE' ? 'Проигравший 1/2' : 'Проигравший пары'
      }
      return roundKey === 'FINAL' ? 'Победитель 1/2' : 'Победитель пары'
    }
    return seededPlaceholderLabel(roundKey, side === 'home' ? 1 : 2)
  }

  function tourHeading(tour, round) {
    if (tour.matches.length <= 1) return tour.name || round.label
    return tour.name && tour.name !== round.label
      ? tour.name
      : `${round.label} · ${tour.matches.length} матч${matchesWord(tour.matches.length)}`
  }

  function roundCards(round) {
    if (Array.isArray(round?.ties) && round.ties.length) {
      return round.ties.map((tie, index) => ({
        key: `${round.key}-tie-${tie.id || index + 1}`,
        matchId: null,
        badge: round.ties.length > 1 ? `Пара ${tie.slotOrder || index + 1}` : (tie.title || round.label),
        dateLabel: Number.isInteger(tie.aggregateHomeScore) && Number.isInteger(tie.aggregateAwayScore)
          ? 'Сыграно'
          : '',
        homeTeamName: participantLabel(tie, 'home', round.key),
        awayTeamName: participantLabel(tie, 'away', round.key),
        homeScoreLabel: Number.isInteger(tie.aggregateHomeScore) ? String(tie.aggregateHomeScore) : '—',
        awayScoreLabel: Number.isInteger(tie.aggregateAwayScore) ? String(tie.aggregateAwayScore) : '—',
        statusLabel: '',
        tourLabel: '',
      }))
    }

    if (!round?.tours?.length) {
      return Array.from({ length: Number(round?.expectedTieCount || 0) }, (_, index) => ({
        key: `${round.key}-placeholder-${index + 1}`,
        matchId: null,
        badge: `Пара ${index + 1}`,
        dateLabel: '',
        homeTeamName: seededPlaceholderLabel(round.key, index * 2 + 1),
        awayTeamName: seededPlaceholderLabel(round.key, index * 2 + 2),
        homeScoreLabel: '—',
        awayScoreLabel: '—',
        statusLabel: '',
        tourLabel: '',
      }))
    }

    return round.tours.flatMap((tour) => {
      if (!Array.isArray(tour.matches) || !tour.matches.length) {
        return [{
          key: `${tour.key}-empty`,
          matchId: null,
          badge: tourHeading(tour, round),
          dateLabel: '',
          homeTeamName: 'Команда A',
          awayTeamName: 'Команда B',
          homeScoreLabel: '—',
          awayScoreLabel: '—',
          statusLabel: '',
          tourLabel: tour.name || round.label,
        }]
      }

      return tour.matches.map((match, index) => ({
        key: `${tour.key}-match-${match.id}`,
        matchId: match.id,
        badge: tour.matches.length > 1 ? `Пара ${index + 1}` : tourHeading(tour, round),
        dateLabel: compactMatchDateTime(match.kickoffAt),
        homeTeamName: match.homeTeamName || 'Команда 1',
        awayTeamName: match.awayTeamName || 'Команда 2',
        homeScoreLabel: Number.isInteger(match.homeScore) ? String(match.homeScore) : '—',
        awayScoreLabel: Number.isInteger(match.awayScore) ? String(match.awayScore) : '—',
        statusLabel: matchStatusLabel(match.status),
        tourLabel: tour.matches.length > 1 ? (tour.name || round.label) : '',
      }))
    })
  }

  const competitiveColumns = computed(() => {
    return bracketColumns.value.filter((round) => round.key !== 'FINAL' && round.key !== 'THIRD_PLACE')
  })

  const leftColumns = computed(() => {
    return competitiveColumns.value.map((round) => {
      const cards = roundCards(round)
      return {
        ...round,
        cards: cards.slice(0, Math.ceil(cards.length / 2)),
      }
    })
  })

  const rightColumns = computed(() => {
    return [...competitiveColumns.value].reverse().map((round) => {
      const cards = roundCards(round)
      return {
        ...round,
        cards: cards.slice(Math.ceil(cards.length / 2)),
      }
    })
  })

  const centerCards = computed(() => {
    return bracketColumns.value
      .filter((round) => round.key === 'FINAL' || round.key === 'THIRD_PLACE')
      .flatMap((round) => roundCards(round).map((card) => ({
        ...card,
        roundKey: round.key,
        roundLabel: round.label,
      })))
  })

  return {
    bracketColumns,
    centerCards,
    leftColumns,
    rightColumns,
  }
}
