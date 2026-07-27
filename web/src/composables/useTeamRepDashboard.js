import { computed, onMounted, ref, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../store/auth'
import { createTeamRepApi } from '../api/teamRep'
import { useTeamRepPlayerForm } from './useTeamRepPlayerForm'
import { useIncomingTransfers } from './useIncomingTransfers'
import {
  applicationReviewNoteClass,
  applicationStatusChipClass,
  canEditApplicationSummary,
  formatApplicationStatus,
  formatDateOnly,
  formatDateTime,
  formatPlayerOptionLabel,
  formatSeasonStatus,
} from '../utils/teamRepPresentation'

export function useTeamRepDashboard() {
  const { user, isAuthenticated, hasRole, loadCurrentUser, authorizedApiRequest } = useAuth()
  const teamRepApi = createTeamRepApi(authorizedApiRequest)
  const route = useRoute()
  const router = useRouter()

  const dashboardLoading = ref(false)
  const seasonLoading = ref(false)
  const pageError = ref('')
  const pageSuccess = ref('')
  const seasonError = ref('')
  const seasonSuccess = ref('')
  const adminTeams = ref([])
  const teamSeasons = ref([])
  const teamPlayers = ref([])
  const seasonView = ref(null)
  const selectedAdminTeamId = ref('')
  const selectedSeasonId = ref('')
  const selectedAvailablePlayerIds = ref([])
  const addPlayerModalOpen = ref(false)
  const showSelectedSeasonPlayersOnly = ref(false)
  const isTeamRosterVisible = ref(false)

  const {
    playerSaving,
    playerModalOpen,
    editingPlayerId,
    playerModalError,
    playerForm,
    openCreatePlayerModal,
    openEditPlayerModal,
    closePlayerModal,
    savePlayer,
    onPhotoSelected,
  } = useTeamRepPlayerForm({
    api: teamRepApi,
    onSaved: async (wasEditing) => {
      pageSuccess.value = wasEditing ? 'Игрок обновлён.' : 'Игрок создан и добавлен в состав команды.'
      await loadDashboard()
      if (seasonView.value) {
        await loadSeasonView(seasonView.value.seasonId)
      }
    },
  })
  const {
    incomingTransfersLoading,
    incomingTransfersModalOpen,
    incomingDecisionLoadingId,
    incomingTransfersSummary,
    incomingDecisionComments,
    loadIncomingTransfersNotifications,
    openIncomingTransfersModal,
    closeIncomingTransfersModal,
    changeIncomingTransfersPage,
    processIncomingTransfer,
  } = useIncomingTransfers({
    api: teamRepApi,
    pageError,
    pageSuccess,
    onProcessed: async () => {
      await loadDashboard()
      if (selectedSeasonId.value) {
        await loadSeasonView(selectedSeasonId.value)
      }
    },
  })
  let suspendSeasonSelectionWatch = false

  const isSuperAdminEditor = computed(() => hasRole('SUPER_ADMIN'))
  const canOpenDashboard = computed(() => isAuthenticated.value && (hasRole('TEAM_REP') || hasRole('SUPER_ADMIN')))
  const canOpenTransfers = computed(() => hasRole('TEAM_REP'))
  const canManagePlayers = computed(() => hasRole('TEAM_REP'))

  const activeTeamName = computed(() => {
    if (!isSuperAdminEditor.value) {
      return user.value?.teamName || 'Не назначена'
    }
    return adminTeams.value.find((team) => String(team.id) === String(selectedAdminTeamId.value))?.name || 'Не выбрана'
  })

  const profile = computed(() => ({
    id: user.value?.id || 0,
    name: user.value?.name || 'Неизвестный пользователь',
    email: user.value?.email || '-',
    teamName: activeTeamName.value,
  }))

  const selectedSeasonSummary = computed(() => {
    const summary = teamSeasons.value.find((season) => String(season.id) === String(selectedSeasonId.value)) || null
    if (!summary) {
      return null
    }
    if (!seasonView.value || String(seasonView.value.seasonId) !== String(selectedSeasonId.value)) {
      return summary
    }

    const liveSelectedPlayersCount = Array.isArray(seasonView.value.players)
      ? seasonView.value.players.filter((player) => player.selectedForSeason).length
      : summary.selectedPlayersCount

    return {
      ...summary,
      applicationDeadline: seasonView.value.applicationDeadline ?? summary.applicationDeadline,
      status: seasonView.value.status ?? summary.status,
      maxRosterSize: seasonView.value.maxRosterSize ?? summary.maxRosterSize,
      transferWindowStartDate: seasonView.value.transferWindowStartDate ?? summary.transferWindowStartDate,
      transferWindowEndDate: seasonView.value.transferWindowEndDate ?? summary.transferWindowEndDate,
      applicationOpen: Boolean(seasonView.value.applicationOpen),
      selectedPlayersCount: liveSelectedPlayersCount,
      applicationStatus: seasonView.value.applicationStatus ?? summary.applicationStatus,
      applicationSubmittedAt: seasonView.value.applicationSubmittedAt ?? summary.applicationSubmittedAt,
      applicationDecisionAt: seasonView.value.applicationDecisionAt ?? summary.applicationDecisionAt,
      applicationDecisionComment: seasonView.value.applicationDecisionComment ?? summary.applicationDecisionComment,
      applicationSubmittable: Boolean(seasonView.value.applicationSubmittable),
    }
  })

  const canEditSelectedSeasonApplication = computed(() => {
    if (!selectedSeasonSummary.value) {
      return false
    }
    const status = String(selectedSeasonSummary.value.applicationStatus || 'DRAFT')
    return selectedSeasonSummary.value.applicationOpen && (status === 'DRAFT' || status === 'RETURNED' || status === 'APPROVED')
  })

  const displayedTeamPlayers = computed(() => {
    const rosterPlayers = Array.isArray(teamPlayers.value) ? teamPlayers.value : []
    const hasLiveSeasonView = seasonView.value && String(seasonView.value.seasonId) === String(selectedSeasonId.value)

    if (!hasLiveSeasonView) {
      if (!showSelectedSeasonPlayersOnly.value || !selectedSeasonId.value) {
        return rosterPlayers
      }
      return rosterPlayers.filter(player => playerHasSelectedSeason(player))
    }

    const mergedPlayers = new Map()

    for (const player of rosterPlayers) {
      mergedPlayers.set(String(player.id), { ...player })
    }

    for (const player of seasonView.value.players || []) {
      const key = String(player.id)
      mergedPlayers.set(key, {
        ...(mergedPlayers.get(key) || {}),
        ...player,
      })
    }

    const players = Array.from(mergedPlayers.values()).sort((left, right) =>
      String(left.fullName || '').localeCompare(String(right.fullName || ''), 'ru', { sensitivity: 'base' })
    )

    if (!showSelectedSeasonPlayersOnly.value || !selectedSeasonId.value) {
      return players
    }

    return players.filter((player) => player.selectedForSeason || playerHasSelectedSeason(player))
  })

  const seasonSelectablePlayers = computed(() => {
    if (!seasonView.value) {
      return []
    }

    const combined = new Map()

    for (const player of seasonView.value.players || []) {
      if (!player.selectedForSeason) {
        combined.set(String(player.id), player)
      }
    }

    for (const player of seasonView.value.availablePlayers || []) {
      combined.set(String(player.id), player)
    }

    return Array.from(combined.values()).sort((left, right) =>
      String(left.fullName || '').localeCompare(String(right.fullName || ''), 'ru', { sensitivity: 'base' })
    )
  })

  const seasonSelectablePlayerOptions = computed(() => {
    return seasonSelectablePlayers.value.map((player) => ({
      value: String(player.id),
      label: formatPlayerOptionLabel(player),
      keywords: `${player.fullName || ''}`,
    }))
  })

  watchEffect(() => {
    if (canOpenDashboard.value) {
      return
    }

    closeSeasonModals()
    closePlayerModal()
    closeIncomingTransfersModal()
    router.replace('/')
  })

  onMounted(async () => {
    await loadCurrentUser().catch(() => null)
    if (!canOpenDashboard.value) {
      return
    }
    if (isSuperAdminEditor.value) {
      await loadAdminTeams()
      selectedAdminTeamId.value = String(route.query.teamId || '')
      return
    }
    if (route.query.seasonId) {
      selectedSeasonId.value = String(route.query.seasonId)
    }
    await loadDashboard()
  })

  watch(selectedSeasonId, async (seasonId) => {
    if (suspendSeasonSelectionWatch) {
      return
    }
    syncDashboardQuery(selectedAdminTeamId.value, seasonId)
    if (!seasonId) {
      seasonView.value = null
      seasonError.value = ''
      seasonSuccess.value = ''
      showSelectedSeasonPlayersOnly.value = false
      return
    }

    await loadSeasonView(seasonId)
  })

  watch(selectedAdminTeamId, async (teamId) => {
    if (!isSuperAdminEditor.value) {
      return
    }

    const preserveSeasonId = String(route.query.teamId || '') === String(teamId || '')
      ? String(route.query.seasonId || '')
      : ''
    suspendSeasonSelectionWatch = true
    clearDashboardState()
    suspendSeasonSelectionWatch = false
    syncDashboardQuery(teamId, preserveSeasonId)
    if (!teamId) {
      return
    }

    await loadDashboard()
  })

  watch(() => route.query.teamId, (teamId) => {
    if (!isSuperAdminEditor.value) {
      return
    }
    const normalized = String(teamId || '')
    if (normalized !== String(selectedAdminTeamId.value || '')) {
      selectedAdminTeamId.value = normalized
    }
  })

  watch(() => route.query.seasonId, (seasonId) => {
    const normalized = String(seasonId || '')
    if (normalized !== String(selectedSeasonId.value || '')) {
      selectedSeasonId.value = normalized
    }
  })

  async function loadAdminTeams() {
    try {
      const payload = await teamRepApi.getActiveTeams()
      adminTeams.value = (Array.isArray(payload) ? payload : [])
        .map((item) => ({
          id: item.id,
          name: item.name || 'Без названия',
        }))
        .sort((left, right) => String(left.name).localeCompare(String(right.name), 'ru', { sensitivity: 'base' }))
    } catch (error) {
      pageError.value = error.message || 'Не удалось загрузить список команд.'
      adminTeams.value = []
    }
  }

  function teamScopedPath(path) {
    if (!isSuperAdminEditor.value || !selectedAdminTeamId.value) {
      return path
    }
    const separator = path.includes('?') ? '&' : '?'
    return `${path}${separator}teamId=${encodeURIComponent(selectedAdminTeamId.value)}`
  }

  function syncDashboardQuery(teamId, seasonId) {
    if (!isSuperAdminEditor.value) {
      return
    }
    const nextTeamId = String(teamId || '')
    const nextSeasonId = String(seasonId || '')
    const currentTeamId = String(route.query.teamId || '')
    const currentSeasonId = String(route.query.seasonId || '')
    if (nextTeamId === currentTeamId && nextSeasonId === currentSeasonId) {
      return
    }
    router.replace({
      query: {
        ...route.query,
        teamId: nextTeamId || undefined,
        seasonId: nextSeasonId || undefined,
      },
    })
  }

  function clearDashboardState() {
    teamSeasons.value = []
    teamPlayers.value = []
    seasonView.value = null
    selectedSeasonId.value = ''
    selectedAvailablePlayerIds.value = []
    seasonError.value = ''
    seasonSuccess.value = ''
    showSelectedSeasonPlayersOnly.value = false
  }

  async function loadDashboard() {
    dashboardLoading.value = true
    pageError.value = ''

    if (isSuperAdminEditor.value && !selectedAdminTeamId.value) {
      clearDashboardState()
      dashboardLoading.value = false
      return
    }

    try {
      const [seasonsPayload, playersPayload] = await teamRepApi.getDashboard(teamScopedPath)
      teamSeasons.value = Array.isArray(seasonsPayload) ? seasonsPayload : []
      teamPlayers.value = Array.isArray(playersPayload) ? playersPayload : []
      if (canOpenTransfers.value) {
        await loadIncomingTransfersNotifications(0)
      }
      const requestedSeasonId = String(route.query.seasonId || '')
      if (requestedSeasonId && teamSeasons.value.some((season) => String(season.id) === requestedSeasonId)) {
        selectedSeasonId.value = requestedSeasonId
      }
      if (selectedSeasonId.value) {
        const stillExists = teamSeasons.value.some((season) => String(season.id) === String(selectedSeasonId.value))
        if (!stillExists) {
          selectedSeasonId.value = ''
          seasonView.value = null
        }
      }
    } catch (error) {
      pageError.value = error.message || 'Не удалось загрузить кабинет представителя.'
    } finally {
      dashboardLoading.value = false
    }
  }

  async function loadSeasonView(seasonId) {
    seasonLoading.value = true
    seasonError.value = ''
    seasonSuccess.value = ''
    selectedAvailablePlayerIds.value = []

    try {
      seasonView.value = await teamRepApi.getSeasonPlayers(teamScopedPath, seasonId)
    } catch (error) {
      seasonError.value = error.message || 'Не удалось открыть заявку сезона.'
    } finally {
      seasonLoading.value = false
    }
  }

  async function openAddPlayerModal(seasonId) {
    seasonError.value = ''
    seasonSuccess.value = ''
    const summary = teamSeasons.value.find((season) => String(season.id) === String(seasonId)) || null
    if (summary && !canEditApplicationSummary(summary)) {
      seasonError.value = summary.applicationDeadline
        ? `Дедлайн добавления игроков истек ${formatDateOnly(summary.applicationDeadline)}.`
        : 'Добавление игроков в заявку этого сезона закрыто.'
      return
    }
    addPlayerModalOpen.value = true

    if (String(seasonView.value?.seasonId || '') === String(seasonId)) {
      return
    }

    await loadSeasonView(seasonId)
  }

  function closeSeasonModals() {
    addPlayerModalOpen.value = false
    selectedAvailablePlayerIds.value = []
    seasonError.value = ''
    seasonSuccess.value = ''
  }

  function toggleSelectedSeasonPlayersFilter() {
    if (!selectedSeasonId.value) {
      seasonError.value = 'Сначала выберите сезон.'
      return
    }

    seasonError.value = ''
    showSelectedSeasonPlayersOnly.value = !showSelectedSeasonPlayersOnly.value
  }

  function toggleTeamRosterVisibility() {
    isTeamRosterVisible.value = !isTeamRosterVisible.value
  }

  async function removeFromSeason(playerId) {
    await mutateSeasonPlayer(playerId, 'DELETE', 'Игрок убран из заявки сезона.')
  }

  async function addAvailablePlayersToSeason() {
    if (!seasonView.value || !selectedAvailablePlayerIds.value.length) {
      seasonError.value = 'Выберите хотя бы одного игрока из списка.'
      return
    }
    if (!canEditApplicationSummary(seasonView.value)) {
      seasonError.value = seasonView.value.applicationDeadline
        ? `Дедлайн добавления игроков истек ${formatDateOnly(seasonView.value.applicationDeadline)}.`
        : 'Добавление игроков в заявку этого сезона закрыто.'
      return
    }

    seasonLoading.value = true
    seasonError.value = ''
    seasonSuccess.value = ''

    try {
      seasonView.value = await teamRepApi.addSeasonPlayers(
        teamScopedPath,
        seasonView.value.seasonId,
        selectedAvailablePlayerIds.value.map((id) => Number(id)),
      )
      selectedAvailablePlayerIds.value = []
      seasonSuccess.value = 'Выбранные игроки добавлены в заявку.'
      addPlayerModalOpen.value = false
      await loadDashboard()
    } catch (error) {
      seasonError.value = error.message || 'Не удалось изменить заявку сезона.'
    } finally {
      seasonLoading.value = false
    }
  }

  async function submitSeasonApplication() {
    if (!selectedSeasonId.value) {
      seasonError.value = 'Сначала выберите сезон.'
      return
    }

    seasonLoading.value = true
    seasonError.value = ''
    seasonSuccess.value = ''

    try {
      seasonView.value = await teamRepApi.submitSeasonApplication(
        teamScopedPath,
        selectedSeasonId.value,
      )
      seasonSuccess.value = 'Сезонная заявка отправлена на проверку.'
      await loadDashboard()
    } catch (error) {
      seasonError.value = error.message || 'Не удалось отправить сезонную заявку.'
    } finally {
      seasonLoading.value = false
    }
  }

  async function mutateSeasonPlayer(playerId, method, successMessage) {
    if (!seasonView.value) {
      return
    }

    seasonLoading.value = true
    seasonError.value = ''
    seasonSuccess.value = ''

    try {
      seasonView.value = await teamRepApi.mutateSeasonPlayer(
        teamScopedPath,
        seasonView.value.seasonId,
        playerId,
        method,
      )
      selectedAvailablePlayerIds.value = []
      seasonSuccess.value = successMessage
      await loadDashboard()
      if (method === 'POST') {
        addPlayerModalOpen.value = false
      }
    } catch (error) {
      seasonError.value = error.message || 'Не удалось изменить заявку сезона.'
    } finally {
      seasonLoading.value = false
    }
  }

  function playerHasSelectedSeason(player) {
    if (seasonView.value && String(seasonView.value.seasonId) === String(selectedSeasonId.value)) {
      return Array.isArray(seasonView.value.players)
        && seasonView.value.players.some((item) => String(item.id) === String(player?.id) && item.selectedForSeason)
    }

    return Array.isArray(player?.seasons)
      && player.seasons.some((season) => String(season.id) === String(selectedSeasonId.value))
  }

  async function removeFromSelectedSeason(playerId) {
    if (!selectedSeasonId.value) {
      pageError.value = 'Сначала выберите сезон.'
      return
    }

    await removeFromSeason(playerId)
  }

  async function removeFromTeam(playerId) {
    pageError.value = ''
    pageSuccess.value = ''

    const teamId = user.value?.teamId
    if (!teamId) {
      pageError.value = 'Не удалось определить текущую команду пользователя.'
      return
    }

    try {
      await teamRepApi.removeTeamPlayer(teamId, playerId)
      pageSuccess.value = 'Игрок отвязан от текущей команды.'
      await loadDashboard()
      if (selectedSeasonId.value) {
        await loadSeasonView(selectedSeasonId.value)
      }
    } catch (error) {
      pageError.value = error.message || 'Не удалось удалить игрока из команды.'
    }
  }

  return {
    route,
    router,
    dashboardLoading,
    seasonLoading,
    playerSaving,
    pageError,
    pageSuccess,
    seasonError,
    seasonSuccess,
    incomingTransfersLoading,
    incomingTransfersModalOpen,
    incomingDecisionLoadingId,
    incomingTransfersSummary,
    incomingDecisionComments,
    adminTeams,
    teamSeasons,
    teamPlayers,
    seasonView,
    selectedAdminTeamId,
    selectedSeasonId,
    selectedAvailablePlayerIds,
    addPlayerModalOpen,
    showSelectedSeasonPlayersOnly,
    isTeamRosterVisible,
    playerModalOpen,
    editingPlayerId,
    playerModalError,
    playerForm,
    suspendSeasonSelectionWatch,
    isSuperAdminEditor,
    canOpenDashboard,
    canOpenTransfers,
    canManagePlayers,
    activeTeamName,
    profile,
    selectedSeasonSummary,
    canEditSelectedSeasonApplication,
    displayedTeamPlayers,
    seasonSelectablePlayers,
    seasonSelectablePlayerOptions,
    loadAdminTeams,
    teamScopedPath,
    syncDashboardQuery,
    clearDashboardState,
    loadDashboard,
    loadIncomingTransfersNotifications,
    openIncomingTransfersModal,
    closeIncomingTransfersModal,
    changeIncomingTransfersPage,
    processIncomingTransfer,
    loadSeasonView,
    openAddPlayerModal,
    closeSeasonModals,
    toggleSelectedSeasonPlayersFilter,
    toggleTeamRosterVisibility,
    removeFromSeason,
    addAvailablePlayersToSeason,
    submitSeasonApplication,
    mutateSeasonPlayer,
    playerHasSelectedSeason,
    removeFromSelectedSeason,
    removeFromTeam,
    openCreatePlayerModal,
    openEditPlayerModal,
    closePlayerModal,
    savePlayer,
    onPhotoSelected,
    formatDateOnly,
    formatDateTime,
    formatPlayerOptionLabel,
    canEditApplicationSummary,
    formatSeasonStatus,
    formatApplicationStatus,
    applicationStatusChipClass,
    applicationReviewNoteClass,
  }
}
