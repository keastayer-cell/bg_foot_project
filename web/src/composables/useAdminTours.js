import { computed, reactive, ref, watch } from 'vue'

export function countHeadToHeadMeetings(matches, firstTeamId, secondTeamId) {
  const firstId = Number(firstTeamId || 0)
  const secondId = Number(secondTeamId || 0)
  if (firstId <= 0 || secondId <= 0) return 0

  return matches.filter((match) => {
    const homeId = Number(match.homeTeamId || 0)
    const awayId = Number(match.awayTeamId || 0)
    return (
      (homeId === firstId && awayId === secondId)
      || (homeId === secondId && awayId === firstId)
    )
  }).length
}

export function canDeleteTourMatch(match) {
  return String(match?.protocolStatus || 'SCHEDULED') === 'SCHEDULED'
}

export function tourMatchScoreLabel(match) {
  const homeScore = Number.isInteger(match?.homeScore) ? match.homeScore : null
  const awayScore = Number.isInteger(match?.awayScore) ? match.awayScore : null
  if (homeScore === null || awayScore === null) return ''
  return `Счет: ${homeScore}:${awayScore}`
}

export function tourMatchDeleteTitle(match) {
  if (canDeleteTourMatch(match)) return 'Удалить матч из тура'
  return 'Нельзя удалить матч, если по нему уже поданы составы или подтвержден протокол.'
}

export function matchProtocolStatusLabel(status) {
  switch (String(status || 'SCHEDULED')) {
    case 'LINEUPS_SUBMITTED':
      return 'Составы поданы'
    case 'LIVE':
      return 'Идет матч'
    case 'FINISHED':
      return 'Матч сыгран'
    case 'VERIFIED':
      return 'Протокол подтвержден'
    default:
      return 'Не сыгран'
  }
}

export function protocolStatusBadgeClass(status) {
  return {
    'is-scheduled': String(status || 'SCHEDULED') === 'SCHEDULED',
    'is-lineups': String(status || '') === 'LINEUPS_SUBMITTED',
    'is-live': String(status || '') === 'LIVE',
    'is-finished': String(status || '') === 'FINISHED',
    'is-verified': String(status || '') === 'VERIFIED',
  }
}

export function useAdminTours({
  request,
  seasons,
  clearMessages,
  errorMessage,
  successMessage,
  confirmAction = (message) => window.confirm(message),
}) {
  const seasonId = ref('')
  const selectedId = ref('')
  const teams = ref([])
  const tours = ref([])
  const matches = ref([])
  const seasonMatches = ref([])
  const matchForm = reactive({
    homeTeamId: '',
    awayTeamId: '',
    kickoffAt: '',
  })

  const selectedSeason = computed(() => {
    return seasons.value.find((season) => String(season.id) === String(seasonId.value)) || null
  })
  const selectedTour = computed(() => {
    return tours.value.find((tour) => String(tour.id) === String(selectedId.value)) || null
  })
  const canPublishSelectedTour = computed(() => {
    return Boolean(selectedTour.value) && !selectedTour.value.published && matches.value.length > 0
  })
  const availableAwayTeams = computed(() => {
    const homeTeamId = Number(matchForm.homeTeamId || 0)
    if (!selectedSeason.value || homeTeamId <= 0) return teams.value

    const allowedMeetings = Math.max(Number(selectedSeason.value.roundsCount || 1), 1)
    return teams.value.filter((team) => {
      const awayTeamId = Number(team.id || 0)
      if (awayTeamId <= 0 || awayTeamId === homeTeamId) return false
      return countHeadToHeadMeetings(seasonMatches.value, homeTeamId, awayTeamId) < allowedMeetings
    })
  })
  const matchLimitMessage = computed(() => {
    const homeTeamId = Number(matchForm.homeTeamId || 0)
    const awayTeamId = Number(matchForm.awayTeamId || 0)
    if (
      !selectedSeason.value
      || homeTeamId <= 0
      || awayTeamId <= 0
      || homeTeamId === awayTeamId
    ) {
      return ''
    }

    const allowedMeetings = Math.max(Number(selectedSeason.value.roundsCount || 1), 1)
    const existingMeetings = countHeadToHeadMeetings(seasonMatches.value, homeTeamId, awayTeamId)
    if (existingMeetings < allowedMeetings) return ''

    const homeTeam = teams.value.find((team) => Number(team.id) === homeTeamId)
    const awayTeam = teams.value.find((team) => Number(team.id) === awayTeamId)
    return `Пара ${homeTeam?.name || 'Команда 1'} - ${awayTeam?.name || 'Команда 2'} уже исчерпала лимит очных встреч для сезона: ${allowedMeetings} круг(а).`
  })

  function resetMatchForm() {
    matchForm.homeTeamId = ''
    matchForm.awayTeamId = ''
    matchForm.kickoffAt = ''
  }

  watch(availableAwayTeams, (availableTeams) => {
    if (!matchForm.awayTeamId) return
    const remainsAvailable = availableTeams.some(
      (team) => String(team.id) === String(matchForm.awayTeamId)
    )
    if (!remainsAvailable) matchForm.awayTeamId = ''
  })

  async function onSeasonChange() {
    clearMessages()
    selectedId.value = ''
    matches.value = []
    resetMatchForm()

    if (!seasonId.value) {
      tours.value = []
      teams.value = []
      seasonMatches.value = []
      return
    }
    await Promise.all([loadTours(), loadTeams()])
  }

  async function loadTours() {
    if (!seasonId.value) {
      tours.value = []
      seasonMatches.value = []
      return
    }

    try {
      const payload = await request(
        `/api/tours?season_id=${encodeURIComponent(seasonId.value)}&active_flag=1`,
        { method: 'GET' }
      )
      tours.value = Array.isArray(payload) ? payload : []
      await loadSeasonMatches()
    } catch (error) {
      tours.value = []
      seasonMatches.value = []
      errorMessage.value = error.message || 'Не удалось загрузить туры.'
    }
  }

  async function loadSeasonMatches() {
    if (!tours.value.length) {
      seasonMatches.value = []
      return
    }

    const payloads = await Promise.all(
      tours.value.map((tour) => request(`/api/tours/${tour.id}/matches?active_flag=1`, {
        method: 'GET',
      }))
    )
    seasonMatches.value = payloads.flatMap((payload) => Array.isArray(payload) ? payload : [])
  }

  async function loadTeams() {
    if (!seasonId.value) {
      teams.value = []
      return
    }

    try {
      const payload = await request(
        `/api/teams?active_flag=1&season_id=${encodeURIComponent(seasonId.value)}`,
        { method: 'GET' }
      )
      teams.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      teams.value = []
      errorMessage.value = error.message || 'Не удалось загрузить команды сезона для туров.'
    }
  }

  async function onTourChange() {
    clearMessages()
    resetMatchForm()
    if (!selectedId.value) {
      matches.value = []
      return
    }

    try {
      const payload = await request(`/api/tours/${selectedId.value}/matches?active_flag=1`, {
        method: 'GET',
      })
      matches.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      matches.value = []
      errorMessage.value = error.message || 'Не удалось загрузить матчи тура.'
    }
  }

  async function refresh() {
    if (!seasonId.value) return
    await Promise.all([loadTours(), loadTeams()])
    if (selectedId.value) await onTourChange()
  }

  async function publish() {
    clearMessages()
    if (!selectedId.value) {
      errorMessage.value = 'Сначала выберите тур.'
      return
    }
    if (!matches.value.length) {
      errorMessage.value = 'Нельзя публиковать пустой тур без матчей.'
      return
    }
    if (selectedTour.value?.published) {
      errorMessage.value = 'Этот тур уже опубликован.'
      return
    }

    try {
      await request(`/api/tours/${selectedId.value}/publish`, { method: 'PUT' })
      await Promise.all([loadTours(), onTourChange()])
      successMessage.value = 'Тур опубликован.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось опубликовать тур.'
    }
  }

  async function createMatch() {
    clearMessages()
    if (!selectedId.value) {
      errorMessage.value = 'Сначала выберите тур.'
      return
    }
    if (!matchForm.homeTeamId || !matchForm.awayTeamId) {
      errorMessage.value = 'Выберите обе команды матча.'
      return
    }
    if (String(matchForm.homeTeamId) === String(matchForm.awayTeamId)) {
      errorMessage.value = 'Команды матча должны отличаться.'
      return
    }
    if (!matchForm.kickoffAt) {
      errorMessage.value = 'Укажите дату и время матча.'
      return
    }
    if (matchLimitMessage.value) {
      errorMessage.value = matchLimitMessage.value
      return
    }

    try {
      await request(`/api/tours/${selectedId.value}/matches`, {
        method: 'POST',
        body: JSON.stringify({
          homeTeamId: Number(matchForm.homeTeamId),
          awayTeamId: Number(matchForm.awayTeamId),
          kickoffAt: new Date(matchForm.kickoffAt).toISOString(),
        }),
      })
      await loadSeasonMatches()
      await onTourChange()
      resetMatchForm()
      successMessage.value = 'Матч добавлен в тур.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось добавить матч.'
    }
  }

  async function deleteMatch(matchId) {
    clearMessages()
    if (!selectedId.value) {
      errorMessage.value = 'Сначала выберите тур.'
      return
    }

    const match = matches.value.find((item) => Number(item.id) === Number(matchId))
    if (match && !canDeleteTourMatch(match)) {
      errorMessage.value = tourMatchDeleteTitle(match)
      return
    }
    if (!confirmAction('Удалить матч из тура без возможности восстановления?')) return

    try {
      await request(`/api/tours/${selectedId.value}/matches/${matchId}`, { method: 'DELETE' })
      await loadSeasonMatches()
      await Promise.all([loadTours(), onTourChange()])
      successMessage.value = 'Матч удален из тура.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось удалить матч из тура.'
    }
  }

  return {
    availableAwayTeams,
    canDeleteTourMatch,
    canPublishSelectedTour,
    createMatch,
    deleteMatch,
    matchForm,
    matchLimitMessage,
    matchProtocolStatusLabel,
    matches,
    onSeasonChange,
    onTourChange,
    protocolStatusBadgeClass,
    publish,
    refresh,
    seasonId,
    seasonMatches,
    selectedId,
    selectedSeason,
    selectedTour,
    teams,
    tourMatchDeleteTitle,
    tourMatchScoreLabel,
    tours,
  }
}
