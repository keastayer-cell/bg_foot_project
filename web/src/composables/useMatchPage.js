import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../store/auth'
import { createMatchesApi } from '../api/matches'
import { useMatchLineups } from './useMatchLineups'
import { stripTeamSuffix } from '../utils/matchPresentation'

export function useMatchPage() {
  const route = useRoute()
  const navigationState = window.history.state || {}
  const { optionalAuthApiRequest, optionalAuthApiRequestRaw, authorizedApiRequest, user, hasRole } = useAuth()
  const matchesApi = createMatchesApi({
    optionalRequest: optionalAuthApiRequest,
    optionalRawRequest: optionalAuthApiRequestRaw,
    authorizedRequest: authorizedApiRequest,
  })

  const match = ref(null)
  const loading = ref(false)
  const pageError = ref('')
  const protocolSaving = ref(false)
  const protocolError = ref('')
  const protocolNotice = ref('')
  const protocolDownloadError = ref('')
  const downloadingProtocolPdf = ref(false)
  const protocolDraft = ref(createEmptyProtocolDraft())
  const {
    lineupSaving,
    lineupErrors,
    lineupNotices,
    addPlayerModalTeamId,
    selectedStarterPlayerIds,
    selectedSubstitutePlayerIds,
    lineupCards,
    activeLineupForModal,
    activeLineupPlayerOptions,
    starterPlayerOptions,
    substitutePlayerOptions,
    requiredStarterCount,
    starterCountIsValid,
    canEditLineup,
    lineupByTeamId,
    refreshMatch,
    openAddPlayerModal,
    closeAddPlayerModal,
    saveLineupSelection,
    saveLineup,
    availableSelectableCount,
    suspendedAvailablePlayers,
  } = useMatchLineups({
    match,
    user,
    hasRole,
    api: matchesApi,
    clearPageError: () => {
      pageError.value = ''
    },
    onMatchUpdated: (payload) => {
      match.value = payload
      syncProtocolDraft(payload)
    },
  })

  const returnContext = computed(() => {
    return String(navigationState.returnContext || route.query.from || '')
  })

  const backLinkTarget = computed(() => {
    if (returnContext.value === 'playoff') {
      return {
        name: 'tours',
        state: {
          ...(navigationState.seasonId || route.query.season
            ? { seasonId: String(navigationState.seasonId || route.query.season) }
            : {}),
          view: 'playoff',
        },
      }
    }
    if (returnContext.value === 'tours') {
      return {
        name: 'tours',
        state: {
          ...(navigationState.seasonId || route.query.season
            ? { seasonId: String(navigationState.seasonId || route.query.season) }
            : {}),
          ...(navigationState.tourId || route.query.tour
            ? { tourId: String(navigationState.tourId || route.query.tour) }
            : {}),
          view: 'table',
        },
      }
    }
    if (returnContext.value === 'matrix') {
      return {
        name: 'tours',
        state: {
          ...(navigationState.seasonId
            ? { seasonId: String(navigationState.seasonId) }
            : {}),
          view: 'matrix',
        },
      }
    }
    const teamRouteValue = navigationState.teamSlug || route.query.team || route.query.teamId
    if (returnContext.value === 'team-profile' && teamRouteValue) {
      return {
        name: 'team-profile',
        params: { slug: String(teamRouteValue) },
        state: {
          ...(navigationState.teamId ? { teamId: navigationState.teamId } : {}),
          teamSlug: String(teamRouteValue),
          ...(navigationState.seasonId ? { seasonId: String(navigationState.seasonId) } : {}),
        },
      }
    }
    return '/'
  })

  const backLinkLabel = computed(() => {
    if (returnContext.value === 'playoff') {
      return 'Вернуться в сетку плей-офф'
    }
    if (returnContext.value === 'tours') {
      return 'Вернуться к выбранному туру'
    }
    if (returnContext.value === 'matrix') {
      return 'Вернуться в шахматку'
    }
    if (returnContext.value === 'team-profile') {
      return 'Вернуться в профиль команды'
    }
    return 'Вернуться на главную'
  })

  const backLinkArrowLabel = computed(() => {
    if (returnContext.value === 'playoff') {
      return '← В сетку плей-офф'
    }
    if (returnContext.value === 'tours') {
      return '← К выбранному туру'
    }
    if (returnContext.value === 'matrix') {
      return '← В шахматку'
    }
    if (returnContext.value === 'team-profile') {
      return '← В профиль команды'
    }
    return '← На главную'
  })

  const savedStatsMap = computed(() => buildSavedStatsMap(match.value?.protocol?.events || []))

  const hasSubmittedLineups = computed(() => {
    return lineupCards.value.length === 2 && lineupCards.value.every((lineup) => Boolean(lineup.submittedAt))
  })

  const canBypassLineupsForProtocol = computed(() => hasRole('SUPER_ADMIN'))

  const isVerifiedProtocol = computed(() => {
    return match.value?.protocol?.status === 'VERIFIED'
  })

  const canDownloadProtocol = computed(() => isVerifiedProtocol.value)

  const canReopenVerifiedProtocol = computed(() => {
    return canEditProtocol() && isVerifiedProtocol.value
  })

  const showProtocolEditor = computed(() => {
    return canEditProtocol() && !isVerifiedProtocol.value
  })

  const isTechnicalDefeatDraft = computed(() => {
    return Boolean(protocolDraft.value.homeTechnicalDefeat || protocolDraft.value.awayTechnicalDefeat)
  })

  const protocolScoreHint = computed(() => {
    if (!hasSubmittedLineups.value && canBypassLineupsForProtocol.value) {
      return 'Тестовый режим SUPER_ADMIN: счет можно сохранить без голов по игрокам и без поданных заявок.'
    }
    if (isTechnicalDefeatDraft.value) {
      return 'При тех. поражении счет выставляется автоматически: 0:3.'
    }
    return 'Если тех.поражение не включено, сумма голов по игрокам должна совпадать со счетом матча.'
  })

  watch(
    () => route.params.id,
    async (matchId) => {
      if (!matchId) {
        match.value = null
        return
      }
      await loadMatch(matchId)
    },
    { immediate: true }
  )

  async function loadMatch(matchId) {
    loading.value = true
    pageError.value = ''
    protocolDownloadError.value = ''

    try {
      const payload = await matchesApi.getMatch(matchId)
      match.value = payload
      lineupErrors.value = {}
      syncProtocolDraft(payload)
    } catch (error) {
      match.value = null
      resetProtocolDraft()
      pageError.value = error.message || 'Не удалось загрузить матч.'
    } finally {
      loading.value = false
    }
  }

  function canEditProtocol() {
    return hasRole('SUPER_ADMIN') || hasRole('REFEREE')
  }

  const protocolEditorRoleLabel = computed(() => {
    if (hasRole('SUPER_ADMIN')) return 'SUPER_ADMIN'
    if (hasRole('REFEREE')) return 'REFEREE'
    return ''
  })

  const availableRefereeOptions = computed(() => {
    return (match.value?.availableReferees || []).map((referee) => ({
      value: String(referee.id),
      label: refereeOptionLabel(referee),
    }))
  })

  const protocolRefereeCards = computed(() => {
    const protocol = match.value?.protocol || {}
    return [
      buildProtocolRefereeCard('chief', 'Главный арбитр', protocol.chiefReferee),
      buildProtocolRefereeCard('assistant-1', 'Помощник 1', protocol.assistantRefereeOne),
      buildProtocolRefereeCard('assistant-2', 'Помощник 2', protocol.assistantRefereeTwo),
    ]
  })

  async function downloadProtocolPdf() {
    if (!match.value || !canDownloadProtocol.value) return

    downloadingProtocolPdf.value = true
    protocolDownloadError.value = ''

    try {
      const response = await matchesApi.downloadProtocol(match.value.id)
      const pdfBlob = await response.blob()
      const disposition = response.headers.get('content-disposition') || ''
      const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
      const fileName = fileNameMatch ? decodeURIComponent(fileNameMatch[1]) : 'match-protocol.pdf'
      const objectUrl = URL.createObjectURL(pdfBlob)

      try {
        const link = document.createElement('a')
        link.href = objectUrl
        link.download = fileName
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        link.remove()
      } finally {
        URL.revokeObjectURL(objectUrl)
      }
    } catch (error) {
      protocolDownloadError.value = error.message || 'Не удалось скачать PDF протокола.'
    } finally {
      downloadingProtocolPdf.value = false
    }
  }

  function createEmptyProtocolDraft() {
    return {
      homeScore: 0,
      awayScore: 0,
      homeTechnicalDefeat: false,
      awayTechnicalDefeat: false,
      chiefRefereeId: '',
      assistantRefereeOneId: '',
      assistantRefereeTwoId: '',
      playerStats: [],
    }
  }

  function syncProtocolDraft(payload) {
    const protocol = payload?.protocol || {}
    const nextPlayerStats = []
    const savedMap = buildSavedStatsMap(protocol.events || [])

    for (const lineup of [payload?.homeLineup, payload?.awayLineup].filter(Boolean)) {
      for (const player of lineup.players || []) {
        const key = statKey(lineup.teamId, player.playerId)
        const saved = savedMap.get(key) || emptyStats()
        nextPlayerStats.push({
          teamId: lineup.teamId,
          teamName: lineup.teamName,
          playerId: player.playerId,
          playerName: player.playerName,
          sortOrder: player.sortOrder,
          goals: saved.goals,
          yellowCards: saved.yellowCards,
          redCards: saved.redCards,
        })
      }
    }

    protocolDraft.value = {
      homeScore: Number.isInteger(protocol.homeScore) ? protocol.homeScore : 0,
      awayScore: Number.isInteger(protocol.awayScore) ? protocol.awayScore : 0,
      homeTechnicalDefeat: Boolean(protocol.homeTechnicalDefeat),
      awayTechnicalDefeat: Boolean(protocol.awayTechnicalDefeat),
      chiefRefereeId: protocol.chiefReferee?.id ? String(protocol.chiefReferee.id) : '',
      assistantRefereeOneId: protocol.assistantRefereeOne?.id ? String(protocol.assistantRefereeOne.id) : '',
      assistantRefereeTwoId: protocol.assistantRefereeTwo?.id ? String(protocol.assistantRefereeTwo.id) : '',
      playerStats: nextPlayerStats,
    }
    protocolError.value = ''
    protocolNotice.value = ''
  }

  function resetProtocolDraft() {
    if (match.value) {
      syncProtocolDraft(match.value)
      return
    }
    protocolDraft.value = createEmptyProtocolDraft()
    protocolError.value = ''
    protocolNotice.value = ''
  }

  async function saveProtocol(asVerified) {
    if (!match.value) return
    if (!hasSubmittedLineups.value && !canBypassLineupsForProtocol.value) {
      protocolError.value = 'Сначала нужно подать обе заявки на матч.'
      return
    }

    protocolSaving.value = true
    protocolError.value = ''
    protocolNotice.value = ''

    try {
      const payload = buildProtocolPayload(asVerified)
      const response = await matchesApi.saveProtocol(match.value.id, payload)
      match.value = response
      syncProtocolDraft(response)
      protocolNotice.value = asVerified ? 'Протокол подтвержден.' : 'Протокол сохранен.'
    } catch (error) {
      protocolError.value = error.message || 'Не удалось сохранить протокол матча.'
    } finally {
      protocolSaving.value = false
    }
  }

  async function reopenVerifiedProtocol() {
    if (!match.value?.id) return

    protocolSaving.value = true
    protocolError.value = ''
    protocolNotice.value = ''

    try {
      const response = await matchesApi.reopenProtocol(match.value.id)
      match.value = response
      syncProtocolDraft(response)
      protocolNotice.value = 'Протокол выведен из подтвержденного статуса. Теперь его можно редактировать.'
    } catch (error) {
      protocolError.value = error.message || 'Не удалось перевести протокол обратно в редактирование.'
    } finally {
      protocolSaving.value = false
    }
  }

  function buildProtocolPayload(asVerified) {
    const homeTechnicalDefeat = Boolean(protocolDraft.value.homeTechnicalDefeat)
    const awayTechnicalDefeat = Boolean(protocolDraft.value.awayTechnicalDefeat)
    const normalizedStats = protocolDraft.value.playerStats.map((playerStat) => ({
      teamId: playerStat.teamId,
      playerId: playerStat.playerId,
      goals: normalizeNonNegative(playerStat.goals),
      yellowCards: normalizeNonNegative(playerStat.yellowCards),
      redCards: normalizeNonNegative(playerStat.redCards),
    }))
    const hasRecordedPlayerStats = normalizedStats.some((playerStat) => hasAnyProtocolStats(playerStat))

    let homeScore = normalizeNonNegative(protocolDraft.value.homeScore)
    let awayScore = normalizeNonNegative(protocolDraft.value.awayScore)

    if (homeTechnicalDefeat) {
      homeScore = 0
      awayScore = 3
    }
    if (awayTechnicalDefeat) {
      homeScore = 3
      awayScore = 0
    }

    if (!homeTechnicalDefeat && !awayTechnicalDefeat) {
      const canSaveScoreOnly = canBypassLineupsForProtocol.value && !hasRecordedPlayerStats
      const homeGoals = sumGoals(match.value.homeTeam.id)
      const awayGoals = sumGoals(match.value.awayTeam.id)
      if (!canSaveScoreOnly && (homeGoals !== homeScore || awayGoals !== awayScore)) {
        throw new Error('Сумма голов по игрокам должна совпадать со счетом матча.')
      }
    }

    return {
      status: asVerified ? 'VERIFIED' : 'FINISHED',
      homeScore,
      awayScore,
      homeTechnicalDefeat,
      awayTechnicalDefeat,
      bestPlayerId: null,
      chiefRefereeId: protocolDraft.value.chiefRefereeId ? Number(protocolDraft.value.chiefRefereeId) : null,
      assistantRefereeOneId: protocolDraft.value.assistantRefereeOneId ? Number(protocolDraft.value.assistantRefereeOneId) : null,
      assistantRefereeTwoId: protocolDraft.value.assistantRefereeTwoId ? Number(protocolDraft.value.assistantRefereeTwoId) : null,
      notes: null,
      startedAt: null,
      finishedAt: null,
      playerStats: normalizedStats,
    }
  }

  function toggleTechnicalDefeat(side, checked) {
    const nextDraft = {
      ...protocolDraft.value,
      homeTechnicalDefeat: side === 'home' ? checked : checked ? false : protocolDraft.value.homeTechnicalDefeat,
      awayTechnicalDefeat: side === 'away' ? checked : checked ? false : protocolDraft.value.awayTechnicalDefeat,
      playerStats: protocolDraft.value.playerStats.map((playerStat) => ({
        ...playerStat,
        goals: 0,
        yellowCards: 0,
        redCards: 0,
      })),
    }

    if (checked) {
      if (side === 'home') {
        nextDraft.homeScore = 0
        nextDraft.awayScore = 3
      } else {
        nextDraft.homeScore = 3
        nextDraft.awayScore = 0
      }
    } else if (!nextDraft.homeTechnicalDefeat && !nextDraft.awayTechnicalDefeat) {
      nextDraft.homeScore = 0
      nextDraft.awayScore = 0
    }

    protocolDraft.value = nextDraft
  }

  function hasAnyProtocolStats(playerStat) {
    return Number(playerStat?.goals || 0) > 0
      || Number(playerStat?.yellowCards || 0) > 0
      || Number(playerStat?.redCards || 0) > 0
  }

  function findOrCreateDraftPlayerStat(lineup, player) {
    const existing = protocolDraft.value.playerStats.find(
      (item) => item.teamId === lineup.teamId && item.playerId === player.playerId
    )
    if (existing) return existing

    const created = {
      teamId: lineup.teamId,
      teamName: lineup.teamName,
      playerId: player.playerId,
      playerName: player.playerName,
      sortOrder: player.sortOrder,
      goals: 0,
      yellowCards: 0,
      redCards: 0,
    }
    protocolDraft.value.playerStats.push(created)
    return created
  }

  function buildSavedStatsMap(events) {
    const map = new Map()

    for (const event of events || []) {
      if (!event?.teamId || !event?.playerId) continue
      const key = statKey(event.teamId, event.playerId)
      const current = map.get(key) || emptyStats()
      if (event.eventType === 'GOAL' || event.eventType === 'PENALTY_GOAL') current.goals += 1
      if (event.eventType === 'YELLOW_CARD') current.yellowCards += 1
      if (event.eventType === 'RED_CARD' || event.eventType === 'SECOND_YELLOW_RED') current.redCards += 1
      map.set(key, current)
    }

    return map
  }

  function savedStatsFor(teamId, playerId) {
    return savedStatsMap.value.get(statKey(teamId, playerId)) || emptyStats()
  }

  function sumGoals(teamId) {
    return protocolDraft.value.playerStats
      .filter((item) => item.teamId === teamId)
      .reduce((total, item) => total + normalizeNonNegative(item.goals), 0)
  }

  function normalizeNonNegative(value) {
    const parsed = Number(value)
    if (!Number.isFinite(parsed) || parsed < 0) return 0
    return Math.floor(parsed)
  }

  function statKey(teamId, playerId) {
    return `${teamId}:${playerId}`
  }

  function emptyStats() {
    return { goals: 0, yellowCards: 0, redCards: 0 }
  }

  function formatPlayerOptionLabel(player) {
    if (!player) return ''
    return `${player.playerName || ''}`
  }

  function formatDateTime(value) {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
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

  function matchScoreLabel(protocol) {
    const homeScore = Number.isInteger(protocol?.homeScore) ? protocol.homeScore : 0
    const awayScore = Number.isInteger(protocol?.awayScore) ? protocol.awayScore : 0
    return `${homeScore} : ${awayScore}`
  }

  function protocolResultLabel(protocol) {
    if (!match.value || !protocol) return ''
    if (protocol.homeTechnicalDefeat || protocol.awayTechnicalDefeat) return 'Тех. пор.'
    return ''
  }

  function lineupSubmittedLabel(lineup) {
    if (!lineup?.submittedAt) return 'Не подана'
    return `Подана ${formatDateTime(lineup.submittedAt)}`
  }

  function lineupPlayerDisplayName(lineup, player) {
    const team = [match.value?.homeTeam, match.value?.awayTeam]
      .find((item) => String(item?.id) === String(lineup?.teamId))
    return stripTeamSuffix(player?.playerName, team?.shortName, lineup?.teamName)
  }

  function startingLineupPlayers(lineup) {
    return (lineup?.players || []).filter((player) => player.isStarter)
  }

  function substituteLineupPlayers(lineup) {
    return (lineup?.players || []).filter((player) => !player.isStarter)
  }

  function lineupPlayerGroups(lineup) {
    const starters = startingLineupPlayers(lineup)
    const substitutes = substituteLineupPlayers(lineup)
    return [
      {
        key: 'starters',
        title: 'Основной состав',
        countLabel: `${starters.length} / ${Number(match.value?.playersOnField || 11)}`,
        players: starters,
      },
      {
        key: 'substitutes',
        title: 'Запасные',
        countLabel: String(substitutes.length),
        players: substitutes,
      },
    ]
  }

  function refereeOptionLabel(referee) {
    if (!referee) return ''
    const city = referee.city ? `, ${referee.city}` : ''
    return `${referee.fullName || ''}${city}`
  }

  function buildProtocolRefereeCard(key, label, referee) {
    return {
      key,
      label,
      name: referee?.fullName || 'Не назначен',
      meta: referee?.city || '',
    }
  }


  return {
    route,
    match,
    loading,
    pageError,
    lineupSaving,
    lineupErrors,
    lineupNotices,
    addPlayerModalTeamId,
    selectedStarterPlayerIds,
    selectedSubstitutePlayerIds,
    protocolSaving,
    protocolError,
    protocolNotice,
    protocolDownloadError,
    downloadingProtocolPdf,
    protocolDraft,
    backLinkTarget,
    backLinkLabel,
    backLinkArrowLabel,
    lineupCards,
    activeLineupForModal,
    activeLineupPlayerOptions,
    starterPlayerOptions,
    substitutePlayerOptions,
    requiredStarterCount,
    starterCountIsValid,
    savedStatsMap,
    hasSubmittedLineups,
    canBypassLineupsForProtocol,
    isVerifiedProtocol,
    canDownloadProtocol,
    canReopenVerifiedProtocol,
    showProtocolEditor,
    isTechnicalDefeatDraft,
    protocolScoreHint,
    loadMatch,
    canEditProtocol,
    protocolEditorRoleLabel,
    availableRefereeOptions,
    protocolRefereeCards,
    downloadProtocolPdf,
    canEditLineup,
    lineupByTeamId,
    refreshMatch,
    openAddPlayerModal,
    closeAddPlayerModal,
    saveLineupSelection,
    saveLineup,
    createEmptyProtocolDraft,
    syncProtocolDraft,
    resetProtocolDraft,
    saveProtocol,
    reopenVerifiedProtocol,
    buildProtocolPayload,
    toggleTechnicalDefeat,
    hasAnyProtocolStats,
    findOrCreateDraftPlayerStat,
    buildSavedStatsMap,
    savedStatsFor,
    sumGoals,
    normalizeNonNegative,
    statKey,
    emptyStats,
    formatPlayerOptionLabel,
    availableSelectableCount,
    suspendedAvailablePlayers,
    formatDateTime,
    matchStatusLabel,
    matchScoreLabel,
    protocolResultLabel,
    lineupSubmittedLabel,
    lineupPlayerDisplayName,
    startingLineupPlayers,
    substituteLineupPlayers,
    lineupPlayerGroups,
    refereeOptionLabel,
    buildProtocolRefereeCard,
  }
}
