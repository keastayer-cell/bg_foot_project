import { computed, reactive, ref, watch } from 'vue'
import { createCompetitionsApi } from '../api/competitions'

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

export function matchScheduleStatusLabel(status) {
  switch (String(status || 'SCHEDULED')) {
    case 'RESCHEDULED': return 'Перенесён'
    case 'CANCELLED': return 'Отменён'
    case 'COMPLETED': return 'Завершён'
    case 'TECHNICAL_RESULT': return 'Технический результат'
    default: return 'Запланирован'
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
  const competitionsApi = createCompetitionsApi(request)
  const seasonId = ref('')
  const competitionId = ref('')
  const selectedId = ref('')
  const teams = ref([])
  const tours = ref([])
  const matches = ref([])
  const seasonMatches = ref([])
  const competitions = ref([])
  const venues = ref([])
  const scheduleEditingId = ref('')
  const scheduleSaving = ref(false)
  const scheduleForm = reactive({ status: 'SCHEDULED', kickoffAt: '', venueId: '', reason: '' })
  const cupDrawBusy = ref(false)
  const cupDrawOrder = ref([])
  const selectedCupTieId = ref('')
  const cupKickoffDates = ref([])
  const cupPenaltyForm = reactive({ home: null, away: null })
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
  const selectedCompetition = computed(() => {
    return competitions.value.find((competition) => String(competition.id) === String(competitionId.value)) || null
  })
  const selectedCup = computed(() => selectedCompetition.value?.type === 'CUP' ? selectedCompetition.value : null)
  const selectedCupTie = computed(() => {
    return selectedCup.value?.ties?.find((tie) => String(tie.id) === String(selectedCupTieId.value)) || null
  })
  const cupRounds = computed(() => {
    const groups = new Map()
    for (const tie of selectedCup.value?.ties || []) {
      if (!groups.has(tie.roundCode)) {
        groups.set(tie.roundCode, {
          code: tie.roundCode,
          label: String(tie.title || '').split(' · ')[0] || tie.roundCode,
          order: Number(tie.roundOrder || 0),
          ties: [],
        })
      }
      groups.get(tie.roundCode).ties.push(tie)
    }
    return [...groups.values()]
      .map((round) => ({ ...round, ties: [...round.ties].sort((a, b) => Number(a.slotOrder) - Number(b.slotOrder)) }))
      .sort((a, b) => a.order - b.order)
  })
  const canCreateCupMatches = computed(() => {
    const tie = selectedCupTie.value
    return Boolean(
      tie
      && selectedCup.value?.drawStatus === 'CONFIRMED'
      && tie.homeTeam
      && tie.awayTeam
      && !tie.matches?.length
      && cupKickoffDates.value.length === Number(tie.legCount || 0)
      && cupKickoffDates.value.every(Boolean)
    )
  })
  const needsCupTieWinner = computed(() => {
    const tie = selectedCupTie.value
    return Boolean(
      selectedCup.value?.penaltiesEnabled
      && tie
      && tie.matches?.length === tie.legCount
      && tie.matches.every((match) => match.protocolStatus === 'VERIFIED')
      && tie.aggregateHomeScore != null
      && tie.aggregateHomeScore === tie.aggregateAwayScore
      && !tie.winnerTeam
    )
  })
  const canSaveCupTieWinner = computed(() => {
    const home = Number(cupPenaltyForm.home)
    const away = Number(cupPenaltyForm.away)
    return needsCupTieWinner.value
      && Number.isInteger(home)
      && Number.isInteger(away)
      && home >= 0
      && away >= 0
      && home !== away
  })
  const canPublishSelectedTour = computed(() => {
    return Boolean(selectedTour.value) && !selectedTour.value.published && matches.value.length > 0
  })
  function canScheduleRegularPair(firstTeamId, secondTeamId) {
    if (!selectedSeason.value) return true
    const allowedMeetings = Math.max(Number(selectedSeason.value.roundsCount || 1), 1)
    return countHeadToHeadMeetings(
      seasonMatches.value,
      firstTeamId,
      secondTeamId
    ) < allowedMeetings
  }
  const availableHomeTeams = computed(() => {
    if (!selectedSeason.value) return teams.value
    return teams.value.filter((homeTeam) => teams.value.some((awayTeam) => (
      Number(homeTeam.id) !== Number(awayTeam.id)
      && canScheduleRegularPair(homeTeam.id, awayTeam.id)
    )))
  })
  const availableAwayTeams = computed(() => {
    const homeTeamId = Number(matchForm.homeTeamId || 0)
    if (homeTeamId <= 0) return []
    return teams.value.filter((team) => {
      const awayTeamId = Number(team.id || 0)
      if (awayTeamId <= 0 || awayTeamId === homeTeamId) return false
      return canScheduleRegularPair(homeTeamId, awayTeamId)
    })
  })
  const matchAvailabilityMessage = computed(() => {
    if (!selectedSeason.value) return ''
    if (teams.value.length < 2) {
      return 'Для создания матча в сезоне должно быть как минимум две команды.'
    }
    if (availableHomeTeams.value.length) return ''
    const allowedMeetings = Math.max(Number(selectedSeason.value.roundsCount || 1), 1)
    return `Добавить ещё один матч регулярного этапа нельзя: каждая пара команд уже включена в календарь. Лимит сезона — ${allowedMeetings} круг(а).`
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

  function toLocalDateTimeInput(value) {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const offset = date.getTimezoneOffset() * 60_000
    return new Date(date.getTime() - offset).toISOString().slice(0, 16)
  }

  function openScheduleEditor(match) {
    scheduleEditingId.value = String(match.id)
    scheduleForm.status = match.scheduleStatus || 'SCHEDULED'
    scheduleForm.kickoffAt = toLocalDateTimeInput(match.kickoffAt)
    scheduleForm.venueId = match.venueId ? String(match.venueId) : ''
    scheduleForm.reason = match.scheduleChangeReason || ''
  }

  function closeScheduleEditor() {
    scheduleEditingId.value = ''
    scheduleForm.status = 'SCHEDULED'
    scheduleForm.kickoffAt = ''
    scheduleForm.venueId = ''
    scheduleForm.reason = ''
  }

  async function saveSchedule(matchId) {
    clearMessages()
    if (!selectedId.value || !scheduleForm.kickoffAt) {
      errorMessage.value = 'Укажите дату и время матча.'
      return
    }
    if (['RESCHEDULED', 'CANCELLED'].includes(scheduleForm.status) && scheduleForm.reason.trim().length < 5) {
      errorMessage.value = 'Для переноса или отмены укажите причину.'
      return
    }
    scheduleSaving.value = true
    try {
      await request(`/api/tours/${selectedId.value}/matches/${matchId}/schedule`, {
        method: 'PUT',
        body: JSON.stringify({
          status: scheduleForm.status,
          kickoffAt: new Date(scheduleForm.kickoffAt).toISOString(),
          venueId: scheduleForm.venueId ? Number(scheduleForm.venueId) : null,
          reason: scheduleForm.reason.trim() || null,
        }),
      })
      await Promise.all([loadSeasonMatches(), onTourChange()])
      closeScheduleEditor()
      successMessage.value = 'Расписание матча обновлено.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось изменить расписание матча.'
    } finally {
      scheduleSaving.value = false
    }
  }

  async function loadVenues() {
    try {
      const payload = await request('/api/admin/league/venues?active_flag=1', { method: 'GET' })
      venues.value = Array.isArray(payload) ? payload : []
    } catch {
      venues.value = []
    }
  }

  watch(availableAwayTeams, (availableTeams) => {
    if (!matchForm.awayTeamId) return
    const remainsAvailable = availableTeams.some(
      (team) => String(team.id) === String(matchForm.awayTeamId)
    )
    if (!remainsAvailable) matchForm.awayTeamId = ''
  })

  watch(availableHomeTeams, (availableTeams) => {
    if (!matchForm.homeTeamId) return
    const remainsAvailable = availableTeams.some(
      (team) => String(team.id) === String(matchForm.homeTeamId)
    )
    if (!remainsAvailable) resetMatchForm()
  })

  async function onSeasonChange() {
    clearMessages()
    competitionId.value = ''
    selectedId.value = ''
    matches.value = []
    selectedCupTieId.value = ''
    cupKickoffDates.value = []
    cupPenaltyForm.home = null
    cupPenaltyForm.away = null
    resetMatchForm()

    if (!seasonId.value) {
      tours.value = []
      teams.value = []
      seasonMatches.value = []
      competitions.value = []
      cupDrawOrder.value = []
      return
    }
    await Promise.all([loadTours(), loadTeams(), loadCompetitions(), loadVenues()])
  }

  async function loadCompetitions() {
    if (!seasonId.value) {
      competitions.value = []
      competitionId.value = ''
      cupDrawOrder.value = []
      return
    }

    try {
      const payload = await competitionsApi.list(seasonId.value)
      competitions.value = Array.isArray(payload) ? payload : []
      const currentExists = competitions.value.some(
        (competition) => String(competition.id) === String(competitionId.value)
      )
      if (!currentExists) {
        const preferred = competitions.value.find((competition) => competition.type === 'CHAMPIONSHIP')
          || competitions.value[0]
        competitionId.value = preferred ? String(preferred.id) : ''
      }
      syncCupDrawOrder()
    } catch (error) {
      competitions.value = []
      competitionId.value = ''
      errorMessage.value = error.message || 'Не удалось загрузить соревнования сезона.'
    }
  }

  function onCompetitionChange() {
    clearMessages()
    selectedId.value = ''
    matches.value = []
    selectedCupTieId.value = ''
    cupKickoffDates.value = []
    syncCupDrawOrder()
    cupPenaltyForm.home = null
    cupPenaltyForm.away = null
    resetMatchForm()
  }

  function onCupTieChange() {
    clearMessages()
    const legCount = Number(selectedCupTie.value?.legCount || 0)
    cupKickoffDates.value = Array.from({ length: legCount }, () => '')
    cupPenaltyForm.home = selectedCupTie.value?.homePenaltyScore ?? null
    cupPenaltyForm.away = selectedCupTie.value?.awayPenaltyScore ?? null
  }

  function syncCupDrawOrder(cup = selectedCup.value) {
    if (!cup) {
      cupDrawOrder.value = []
      return
    }

    const ties = [...(cup.ties || [])]
    const firstRoundOrder = ties.length
      ? Math.min(...ties.map((tie) => Number(tie.roundOrder || 0)))
      : null
    const drawnIds = firstRoundOrder == null
      ? []
      : ties
          .filter((tie) => Number(tie.roundOrder || 0) === firstRoundOrder)
          .sort((left, right) => Number(left.slotOrder || 0) - Number(right.slotOrder || 0))
          .flatMap((tie) => [tie.homeTeam?.id, tie.awayTeam?.id])
          .filter(Boolean)
    const participantIds = [...(cup.teams || [])]
      .sort((left, right) => Number(left.seedNumber || 0) - Number(right.seedNumber || 0))
      .map((team) => team.id)
    cupDrawOrder.value = new Set(drawnIds).size === participantIds.length
      ? drawnIds
      : participantIds
  }

  function replaceCompetition(saved) {
    competitions.value = competitions.value.map((competition) => (
      String(competition.id) === String(saved.id) ? saved : competition
    ))
  }

  function moveCupDrawTeam(index, delta) {
    const nextIndex = index + delta
    if (nextIndex < 0 || nextIndex >= cupDrawOrder.value.length) return
    const next = [...cupDrawOrder.value]
    const currentTeam = next[index]
    next[index] = next[nextIndex]
    next[nextIndex] = currentTeam
    cupDrawOrder.value = next
  }

  async function performCupDraw(orderedTeamIds) {
    clearMessages()
    if (!selectedCup.value) return
    cupDrawBusy.value = true
    try {
      const saved = await competitionsApi.draw(
        seasonId.value,
        selectedCup.value.id,
        orderedTeamIds
      )
      replaceCompetition(saved)
      syncCupDrawOrder(saved)
      selectedCupTieId.value = ''
      successMessage.value = 'Черновик кубковой сетки сформирован.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось провести жеребьёвку.'
    } finally {
      cupDrawBusy.value = false
    }
  }

  async function drawCupRandom() {
    await performCupDraw([])
  }

  async function drawCupManual() {
    await performCupDraw(cupDrawOrder.value.map(Number))
  }

  async function confirmCupDraw() {
    clearMessages()
    if (!selectedCup.value) return
    cupDrawBusy.value = true
    try {
      const saved = await competitionsApi.confirmDraw(seasonId.value, selectedCup.value.id)
      replaceCompetition(saved)
      syncCupDrawOrder(saved)
      selectedCupTieId.value = ''
      successMessage.value = 'Кубковая сетка утверждена. Теперь можно назначать матчи пар.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось утвердить кубковую сетку.'
    } finally {
      cupDrawBusy.value = false
    }
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
    if (!seasonId.value || !tours.value.length) {
      seasonMatches.value = []
      return
    }

    const payload = await request(
      `/api/tours/matches?season_id=${encodeURIComponent(seasonId.value)}&active_flag=1`,
      { method: 'GET' }
    )
    const regularTourIds = new Set(
      tours.value
        .filter((tour) => String(tour.stageType || '').toUpperCase() === 'REGULAR')
        .map((tour) => String(tour.id))
    )
    seasonMatches.value = (Array.isArray(payload) ? payload : [])
      .filter((match) => regularTourIds.has(String(match.tourId)))
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
    closeScheduleEditor()
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
    await Promise.all([loadTours(), loadTeams(), loadCompetitions(), loadVenues()])
    if (selectedId.value) await onTourChange()
  }

  async function createCupMatches() {
    clearMessages()
    if (!selectedCup.value || !selectedCupTie.value) {
      errorMessage.value = 'Выберите кубковую пару.'
      return
    }
    if (!canCreateCupMatches.value) {
      errorMessage.value = 'Укажите дату и время каждого матча пары.'
      return
    }

    try {
      const saved = await competitionsApi.scheduleTie(
        seasonId.value,
        selectedCup.value.id,
        selectedCupTie.value.id,
        cupKickoffDates.value.map((value) => new Date(value).toISOString())
      )
      replaceCompetition(saved)
      cupKickoffDates.value = []
      await loadTours()
      successMessage.value = 'Матчи кубковой пары созданы.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось создать матчи кубковой пары.'
    }
  }

  async function saveCupTieWinner() {
    clearMessages()
    if (!selectedCup.value || !selectedCupTie.value || !canSaveCupTieWinner.value) {
      errorMessage.value = 'Укажите разный итог серии пенальти для обеих команд.'
      return
    }

    try {
      const saved = await competitionsApi.chooseTieWinner(
        seasonId.value,
        selectedCup.value.id,
        selectedCupTie.value.id,
        Number(cupPenaltyForm.home),
        Number(cupPenaltyForm.away)
      )
      replaceCompetition(saved)
      successMessage.value = 'Результат серии пенальти сохранён.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось сохранить результат серии пенальти.'
    }
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
      await Promise.all([loadTours(), onTourChange()])
      successMessage.value = 'Матч удален из тура.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось удалить матч из тура.'
    }
  }

  return {
    availableAwayTeams,
    availableHomeTeams,
    canCreateCupMatches,
    canSaveCupTieWinner,
    canDeleteTourMatch,
    canPublishSelectedTour,
    competitionId,
    competitions,
    createMatch,
    createCupMatches,
    confirmCupDraw,
    cupDrawBusy,
    cupDrawOrder,
    cupKickoffDates,
    cupPenaltyForm,
    cupRounds,
    deleteMatch,
    drawCupManual,
    drawCupRandom,
    matchForm,
    matchAvailabilityMessage,
    matchLimitMessage,
    matchProtocolStatusLabel,
    matchScheduleStatusLabel,
    matches,
    moveCupDrawTeam,
    needsCupTieWinner,
    onSeasonChange,
    onCompetitionChange,
    onCupTieChange,
    onTourChange,
    openScheduleEditor,
    closeScheduleEditor,
    protocolStatusBadgeClass,
    publish,
    refresh,
    saveCupTieWinner,
    saveSchedule,
    scheduleEditingId,
    scheduleForm,
    scheduleSaving,
    seasonId,
    seasonMatches,
    selectedId,
    selectedCompetition,
    selectedCup,
    selectedCupTie,
    selectedCupTieId,
    selectedSeason,
    selectedTour,
    teams,
    tourMatchDeleteTitle,
    tourMatchScoreLabel,
    tours,
    venues,
  }
}
