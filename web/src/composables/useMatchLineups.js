import { computed, ref } from 'vue'

export function useMatchLineups({
  match,
  user,
  hasRole,
  api,
  onMatchUpdated,
  clearPageError,
}) {
  const lineupSaving = ref({})
  const lineupErrors = ref({})
  const lineupNotices = ref({})
  const addPlayerModalTeamId = ref(null)
  const selectedAvailablePlayerId = ref('')

  const lineupCards = computed(() => {
    if (!match.value) return []
    return [match.value.homeLineup, match.value.awayLineup].filter(Boolean)
  })

  const activeLineupForModal = computed(() => {
    return lineupCards.value.find(
      (lineup) => String(lineup.teamId) === String(addPlayerModalTeamId.value),
    ) || null
  })

  const activeLineupPlayerOptions = computed(() => {
    return (activeLineupForModal.value?.availablePlayers || []).map((player) => ({
      value: String(player.playerId),
      label: player.playerName || '',
      keywords: player.playerName || '',
      disabled: Boolean(player.suspended),
    }))
  })

  function canEditLineup(teamId) {
    if (!user.value || match.value?.protocol?.status === 'VERIFIED') return false
    if (hasRole('SUPER_ADMIN')) return true
    if (!hasRole('TEAM_REP')) return false
    return String(user.value.teamId || '') === String(teamId)
      && Boolean(user.value.teamScope?.canEditRoster)
  }

  function lineupByTeamId(teamId) {
    return lineupCards.value.find((lineup) => String(lineup.teamId) === String(teamId)) || null
  }

  async function refreshMatch() {
    if (!match.value?.id) return
    onMatchUpdated(await api.getMatch(match.value.id))
  }

  async function openAddPlayerModal(teamId) {
    if (!match.value) return

    clearPageError()
    await refreshMatch()
    const lineup = lineupByTeamId(teamId)
    if (!availableSelectableCount(lineup)) {
      lineupNotices.value = {
        ...lineupNotices.value,
        [teamId]: lineup?.availablePlayers?.length
          ? 'Все оставшиеся игроки этой команды сейчас дисквалифицированы на матч.'
          : 'Для этой команды сейчас нет доступных игроков для добавления.',
      }
      return
    }

    addPlayerModalTeamId.value = teamId
    selectedAvailablePlayerId.value = ''
    lineupNotices.value = { ...lineupNotices.value, [teamId]: '' }
    lineupErrors.value = { ...lineupErrors.value, [teamId]: '' }
  }

  function closeAddPlayerModal() {
    addPlayerModalTeamId.value = null
    selectedAvailablePlayerId.value = ''
  }

  async function addLineupPlayer() {
    if (!activeLineupForModal.value || !selectedAvailablePlayerId.value) return

    const currentIds = (activeLineupForModal.value.players || []).map((player) => player.playerId)
    await saveLineup(
      activeLineupForModal.value.teamId,
      [...currentIds, Number(selectedAvailablePlayerId.value)],
      'Игрок добавлен в заявку.',
    )
    if (!lineupErrors.value[activeLineupForModal.value.teamId]) {
      closeAddPlayerModal()
    }
  }

  async function removeLineupPlayer(teamId, playerId) {
    const lineup = lineupByTeamId(teamId)
    if (!lineup) return
    const playerIds = (lineup.players || [])
      .map((player) => player.playerId)
      .filter((id) => id !== playerId)
    await saveLineup(teamId, playerIds, 'Игрок убран из заявки.')
  }

  async function clearLineup(teamId) {
    lineupNotices.value = { ...lineupNotices.value, [teamId]: '' }
    lineupErrors.value = { ...lineupErrors.value, [teamId]: '' }
    await saveLineup(teamId, [], 'Заявка очищена.')
  }

  async function saveLineup(teamId, playerIds, successMessage) {
    if (!match.value) return

    lineupSaving.value = { ...lineupSaving.value, [teamId]: true }
    lineupErrors.value = { ...lineupErrors.value, [teamId]: '' }
    lineupNotices.value = { ...lineupNotices.value, [teamId]: '' }

    try {
      onMatchUpdated(await api.saveLineup(match.value.id, teamId, playerIds))
      lineupNotices.value = {
        ...lineupNotices.value,
        [teamId]: successMessage || 'Заявка сохранена.',
      }
    } catch (error) {
      lineupErrors.value = {
        ...lineupErrors.value,
        [teamId]: error.message || 'Не удалось сохранить заявку.',
      }
    } finally {
      lineupSaving.value = { ...lineupSaving.value, [teamId]: false }
    }
  }

  function availableSelectableCount(lineup) {
    return (lineup?.availablePlayers || []).filter((player) => !player.suspended).length
  }

  function suspendedAvailablePlayers(lineup) {
    return (lineup?.availablePlayers || []).filter((player) => player.suspended)
  }

  return {
    lineupSaving,
    lineupErrors,
    lineupNotices,
    addPlayerModalTeamId,
    selectedAvailablePlayerId,
    lineupCards,
    activeLineupForModal,
    activeLineupPlayerOptions,
    canEditLineup,
    lineupByTeamId,
    refreshMatch,
    openAddPlayerModal,
    closeAddPlayerModal,
    addLineupPlayer,
    removeLineupPlayer,
    clearLineup,
    saveLineup,
    availableSelectableCount,
    suspendedAvailablePlayers,
  }
}
