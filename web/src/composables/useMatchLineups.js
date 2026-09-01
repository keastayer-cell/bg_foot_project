import { computed, ref } from 'vue'
import { stripTeamSuffix } from '../utils/matchPresentation'

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
  const selectedStarterPlayerIds = ref([])
  const selectedSubstitutePlayerIds = ref([])

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
    const lineup = activeLineupForModal.value
    const team = [match.value?.homeTeam, match.value?.awayTeam]
      .find((item) => String(item?.id) === String(lineup?.teamId))
    const playersById = new Map()
    for (const player of [...(lineup?.players || []), ...(lineup?.availablePlayers || [])]) {
      playersById.set(String(player.playerId), player)
    }
    return Array.from(playersById.values()).map((player) => ({
      value: String(player.playerId),
      label: stripTeamSuffix(player.playerName, team?.shortName, lineup?.teamName),
      keywords: player.playerName || '',
      disabled: Boolean(player.suspended),
    }))
  })

  const starterPlayerOptions = computed(() => {
    const substitutes = new Set(selectedSubstitutePlayerIds.value.map(String))
    return activeLineupPlayerOptions.value.map((option) => ({
      ...option,
      disabled: option.disabled || substitutes.has(option.value),
    }))
  })

  const substitutePlayerOptions = computed(() => {
    const starters = new Set(selectedStarterPlayerIds.value.map(String))
    return activeLineupPlayerOptions.value.map((option) => ({
      ...option,
      disabled: option.disabled || starters.has(option.value),
    }))
  })

  const requiredStarterCount = computed(() => Number(match.value?.playersOnField || 11))
  const starterCountIsValid = computed(() => selectedStarterPlayerIds.value.length === requiredStarterCount.value)

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
    addPlayerModalTeamId.value = teamId
    selectedStarterPlayerIds.value = (lineup?.players || [])
      .filter((player) => player.isStarter)
      .map((player) => String(player.playerId))
    selectedSubstitutePlayerIds.value = (lineup?.players || [])
      .filter((player) => !player.isStarter)
      .map((player) => String(player.playerId))
    lineupNotices.value = { ...lineupNotices.value, [teamId]: '' }
    lineupErrors.value = { ...lineupErrors.value, [teamId]: '' }
  }

  function closeAddPlayerModal() {
    addPlayerModalTeamId.value = null
    selectedStarterPlayerIds.value = []
    selectedSubstitutePlayerIds.value = []
  }

  async function saveLineupSelection() {
    const lineup = activeLineupForModal.value
    if (!lineup || !starterCountIsValid.value) return

    const starterIds = selectedStarterPlayerIds.value.map(Number).filter(Number.isFinite)
    const substituteIds = selectedSubstitutePlayerIds.value.map(Number).filter(Number.isFinite)
    const saved = await saveLineup(lineup.teamId, starterIds, substituteIds)
    if (saved) {
      closeAddPlayerModal()
    }
  }

  async function saveLineup(teamId, starterIds, substituteIds) {
    if (!match.value) return

    lineupSaving.value = { ...lineupSaving.value, [teamId]: true }
    lineupErrors.value = { ...lineupErrors.value, [teamId]: '' }
    lineupNotices.value = { ...lineupNotices.value, [teamId]: '' }

    try {
      onMatchUpdated(await api.saveLineup(match.value.id, teamId, starterIds, substituteIds))
      lineupNotices.value = {
        ...lineupNotices.value,
        [teamId]: 'Состав матча сохранён.',
      }
      return true
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
  }
}
