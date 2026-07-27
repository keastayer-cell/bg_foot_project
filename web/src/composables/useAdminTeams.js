import { computed, reactive, ref } from 'vue'

export function normalizePositiveIdList(values) {
  return [...new Set((Array.isArray(values) ? values : [])
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0))]
}

export function useAdminTeams({
  request,
  teams,
  players,
  loadPlayers,
  clearMessages,
  errorMessage,
  successMessage,
  formatDateOnly,
  confirmAction = (message) => window.confirm(message),
}) {
  const form = reactive({
    name: '',
    shortName: '',
    city: '',
    logoDataUrl: '',
  })
  const editingId = ref(null)
  const subMode = ref('create')
  const editSelectId = ref('')
  const roster = ref([])
  const seasonOptions = ref([])
  const seasonPlayers = ref([])
  const rosterToAddIds = ref([])
  const selectedSeasonId = ref('')
  const seasonToAddIds = ref([])
  const seasonToRemoveIds = ref([])
  const rosterBusy = ref(false)
  const seasonBusy = ref(false)
  const isRosterVisible = ref(false)
  const saving = ref(false)

  const playersAvailableForRoster = computed(() => {
    const rosterIds = new Set(roster.value.map((player) => Number(player.id)))
    const currentTeamId = Number(editingId.value)

    return players.value.filter((player) => {
      const playerId = Number(player.id)
      const activeSeasonTeamId = Number(player.activeSeasonTeamId)
      if (rosterIds.has(playerId)) return false
      if (!Number.isFinite(activeSeasonTeamId) || activeSeasonTeamId <= 0) return true
      return activeSeasonTeamId === currentTeamId
    })
  })

  function playerOption(player) {
    const captionParts = []
    if (player.birthDate) captionParts.push(`ДР: ${formatDateOnly(player.birthDate)}`)
    if (player.residence) captionParts.push(player.residence)
    if (player.isGoalkeeper) captionParts.push('Вратарь')

    return {
      value: String(player.id),
      label: String(player.fullName || ''),
      caption: captionParts.join(' · '),
      keywords: String(player.fullName || ''),
    }
  }

  const rosterAddOptions = computed(() => playersAvailableForRoster.value.map(playerOption))
  const seasonSelectedPlayers = computed(() => seasonPlayers.value.filter((player) => Boolean(player?.selectedForSeason)))
  const seasonAvailablePlayers = computed(() => seasonPlayers.value.filter((player) => !player?.selectedForSeason))
  const selectedSeason = computed(() => {
    return seasonOptions.value.find((season) => String(season.id) === String(selectedSeasonId.value)) || null
  })
  const seasonMaxRosterSize = computed(() => {
    const normalized = Number(selectedSeason.value?.maxRosterSize)
    return Number.isFinite(normalized) && normalized > 0 ? normalized : null
  })
  const seasonRemainingSlots = computed(() => {
    if (!seasonMaxRosterSize.value) return null
    return Math.max(seasonMaxRosterSize.value - seasonSelectedPlayers.value.length, 0)
  })
  const isSeasonAtLimit = computed(() => seasonRemainingSlots.value === 0)
  const willSelectedPlayersExceedSeasonLimit = computed(() => {
    if (seasonRemainingSlots.value == null) return false
    return seasonToAddIds.value.length > seasonRemainingSlots.value
  })
  const seasonAddOptions = computed(() => seasonAvailablePlayers.value.map(playerOption))
  const seasonRemoveOptions = computed(() => seasonSelectedPlayers.value.map(playerOption))

  function resetForm() {
    form.name = ''
    form.shortName = ''
    form.city = ''
    form.logoDataUrl = ''
  }

  function resetContext() {
    roster.value = []
    seasonOptions.value = []
    seasonPlayers.value = []
    rosterToAddIds.value = []
    selectedSeasonId.value = ''
    seasonToAddIds.value = []
    seasonToRemoveIds.value = []
    rosterBusy.value = false
    seasonBusy.value = false
    isRosterVisible.value = false
  }

  async function loadRegistry() {
    try {
      const payload = await request('/api/teams?active_flag=1', { method: 'GET' })
      teams.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      teams.value = []
      errorMessage.value = error.message || 'Не удалось загрузить команды.'
    }
  }

  async function create() {
    clearMessages()
    if (!form.name || !form.shortName || !form.city) {
      errorMessage.value = 'Заполните все поля команды.'
      return
    }

    try {
      await request('/api/teams', {
        method: 'POST',
        body: JSON.stringify(form),
      })
      await loadRegistry()
      resetForm()
      successMessage.value = 'Команда создана.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось создать команду.'
    }
  }

  function startEdit(item) {
    editingId.value = item.id
    form.name = item.name
    form.shortName = item.shortName
    form.city = item.city
    form.logoDataUrl = item.logoDataUrl || ''
    isRosterVisible.value = false
    clearMessages()
  }

  function cancelEdit() {
    editingId.value = null
    resetForm()
    resetContext()
    clearMessages()
  }

  async function saveEdit() {
    clearMessages()
    if (!editingId.value) {
      errorMessage.value = 'Сначала выберите команду для редактирования.'
      return
    }
    if (!form.name || !form.shortName || !form.city) {
      errorMessage.value = 'Заполните все поля команды.'
      return
    }

    saving.value = true
    try {
      const updatedTeam = await request(`/api/teams/${editingId.value}`, {
        method: 'PUT',
        body: JSON.stringify(form),
      })
      await loadRegistry()

      const refreshedTeam = teams.value.find(
        (team) => String(team.id) === String(updatedTeam?.id || editingId.value)
      ) || updatedTeam
      if (refreshedTeam?.id) {
        editSelectId.value = String(refreshedTeam.id)
        startEdit(refreshedTeam)
        await refreshContext(refreshedTeam.id)
      }
      successMessage.value = 'Команда обновлена.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось обновить команду.'
    } finally {
      saving.value = false
    }
  }

  async function onSelectChange() {
    if (!editSelectId.value) {
      cancelEdit()
      return
    }
    const item = teams.value.find((team) => String(team.id) === editSelectId.value)
    if (item) {
      startEdit(item)
      await refreshContext(item.id)
    }
  }

  async function deactivate(teamId) {
    clearMessages()
    try {
      await request(`/api/teams/${teamId}`, { method: 'DELETE' })
      if (String(editingId.value || '') === String(teamId)) {
        cancelEdit()
        editSelectId.value = ''
      }
      await loadRegistry()
      successMessage.value = 'Команда деактивирована.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось удалить команду.'
    }
  }

  function onLogoSelected(event) {
    const file = event.target?.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      form.logoDataUrl = String(reader.result || '')
    }
    reader.readAsDataURL(file)
  }

  function toggleRosterVisibility() {
    isRosterVisible.value = !isRosterVisible.value
  }

  async function refreshContext(teamId = editingId.value) {
    const normalizedTeamId = Number(teamId)
    if (!Number.isFinite(normalizedTeamId) || normalizedTeamId <= 0) {
      resetContext()
      return
    }

    await loadRoster(normalizedTeamId)
    await loadSeasonOptions(normalizedTeamId)

    if (
      selectedSeasonId.value
      && !seasonOptions.value.some((season) => String(season.id) === String(selectedSeasonId.value))
    ) {
      selectedSeasonId.value = ''
      seasonPlayers.value = []
      seasonToAddIds.value = []
      seasonToRemoveIds.value = []
    }
    if (selectedSeasonId.value) {
      await loadSeasonPlayers(normalizedTeamId, selectedSeasonId.value)
    }
  }

  async function loadRoster(teamId) {
    rosterBusy.value = true
    try {
      const payload = await request(`/api/teams/${encodeURIComponent(teamId)}/players`, { method: 'GET' })
      roster.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      roster.value = []
      errorMessage.value = error.message || 'Не удалось загрузить состав команды.'
    } finally {
      rosterBusy.value = false
    }
  }

  async function loadSeasonOptions(teamId) {
    seasonBusy.value = true
    try {
      const payload = await request(`/api/teams/${encodeURIComponent(teamId)}/seasons`, { method: 'GET' })
      seasonOptions.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      seasonOptions.value = []
      errorMessage.value = error.message || 'Не удалось определить сезоны команды.'
    } finally {
      seasonBusy.value = false
    }
  }

  async function onSeasonChange() {
    clearMessages()
    seasonToAddIds.value = []
    seasonToRemoveIds.value = []
    if (!editingId.value || !selectedSeasonId.value) {
      seasonPlayers.value = []
      return
    }
    await loadSeasonPlayers(editingId.value, selectedSeasonId.value)
  }

  async function loadSeasonPlayers(teamId, seasonId) {
    seasonBusy.value = true
    try {
      const payload = await request(
        `/api/seasons/${encodeURIComponent(seasonId)}/teams/${encodeURIComponent(teamId)}/players`,
        { method: 'GET' }
      )
      seasonPlayers.value = Array.isArray(payload) ? payload : []
      seasonToAddIds.value = []
      seasonToRemoveIds.value = []
    } catch (error) {
      seasonPlayers.value = []
      seasonToAddIds.value = []
      seasonToRemoveIds.value = []
      errorMessage.value = error.message || 'Не удалось загрузить заявку команды на сезон.'
    } finally {
      seasonBusy.value = false
    }
  }

  async function addPlayersToRoster() {
    clearMessages()
    if (!editingId.value) {
      errorMessage.value = 'Сначала выберите команду.'
      return
    }

    const playerIds = normalizePositiveIdList(rosterToAddIds.value)
    if (!playerIds.length) {
      errorMessage.value = 'Выберите хотя бы одного игрока для добавления в состав.'
      return
    }

    rosterBusy.value = true
    try {
      for (const playerId of playerIds) {
        await request(
          `/api/teams/${encodeURIComponent(editingId.value)}/players/${encodeURIComponent(playerId)}`,
          { method: 'POST' }
        )
      }
      rosterToAddIds.value = []
      await loadPlayers()
      await refreshContext()
      successMessage.value = playerIds.length === 1
        ? 'Игрок добавлен в состав команды.'
        : `В состав команды добавлено игроков: ${playerIds.length}.`
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось добавить игрока в состав команды.'
    } finally {
      rosterBusy.value = false
    }
  }

  async function removePlayerFromRoster(playerId) {
    clearMessages()
    if (!editingId.value) {
      errorMessage.value = 'Сначала выберите команду.'
      return
    }
    if (!confirmAction('Убрать игрока из состава команды?')) return

    rosterBusy.value = true
    try {
      await request(
        `/api/teams/${encodeURIComponent(editingId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'DELETE' }
      )
      await loadPlayers()
      await refreshContext()
      successMessage.value = 'Игрок убран из состава команды.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось убрать игрока из состава команды.'
    } finally {
      rosterBusy.value = false
    }
  }

  async function addPlayersToSeason() {
    clearMessages()
    if (!editingId.value || !selectedSeasonId.value) {
      errorMessage.value = 'Сначала выберите команду и сезон.'
      return
    }

    const playerIds = normalizePositiveIdList(seasonToAddIds.value)
    if (!playerIds.length) {
      errorMessage.value = 'Выберите хотя бы одного игрока для добавления в заявку сезона.'
      return
    }
    if (seasonRemainingSlots.value != null && playerIds.length > seasonRemainingSlots.value) {
      errorMessage.value = `Нельзя превысить лимит заявки сезона: ${seasonMaxRosterSize.value}.`
      return
    }

    seasonBusy.value = true
    try {
      for (const playerId of playerIds) {
        await request(
          `/api/seasons/${encodeURIComponent(selectedSeasonId.value)}/teams/${encodeURIComponent(editingId.value)}/players/${encodeURIComponent(playerId)}`,
          { method: 'POST' }
        )
      }
      await loadSeasonPlayers(editingId.value, selectedSeasonId.value)
      successMessage.value = playerIds.length === 1
        ? 'Игрок добавлен в заявку сезона.'
        : `В заявку сезона добавлено игроков: ${playerIds.length}.`
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось добавить игроков в заявку сезона.'
    } finally {
      seasonBusy.value = false
    }
  }

  async function removePlayersFromSeason() {
    clearMessages()
    if (!editingId.value || !selectedSeasonId.value) {
      errorMessage.value = 'Сначала выберите команду и сезон.'
      return
    }

    const playerIds = normalizePositiveIdList(seasonToRemoveIds.value)
    if (!playerIds.length) {
      errorMessage.value = 'Выберите хотя бы одного игрока для удаления из заявки сезона.'
      return
    }

    seasonBusy.value = true
    try {
      for (const playerId of playerIds) {
        await request(
          `/api/seasons/${encodeURIComponent(selectedSeasonId.value)}/teams/${encodeURIComponent(editingId.value)}/players/${encodeURIComponent(playerId)}`,
          { method: 'DELETE' }
        )
      }
      await loadSeasonPlayers(editingId.value, selectedSeasonId.value)
      successMessage.value = playerIds.length === 1
        ? 'Игрок убран из заявки сезона.'
        : `Из заявки сезона убрано игроков: ${playerIds.length}.`
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось изменить заявку сезона.'
    } finally {
      seasonBusy.value = false
    }
  }

  return {
    addPlayersToRoster,
    addPlayersToSeason,
    cancelEdit,
    create,
    deactivate,
    editingId,
    editSelectId,
    form,
    isRosterVisible,
    isSeasonAtLimit,
    loadRegistry,
    onLogoSelected,
    onSeasonChange,
    onSelectChange,
    refreshContext,
    removePlayerFromRoster,
    removePlayersFromSeason,
    roster,
    rosterAddOptions,
    rosterBusy,
    rosterToAddIds,
    saveEdit,
    saving,
    seasonAddOptions,
    seasonAvailablePlayers,
    seasonBusy,
    seasonMaxRosterSize,
    seasonOptions,
    seasonPlayers,
    seasonRemainingSlots,
    seasonRemoveOptions,
    seasonSelectedPlayers,
    seasonToAddIds,
    seasonToRemoveIds,
    selectedSeasonId,
    subMode,
    toggleRosterVisibility,
    willSelectedPlayersExceedSeasonLimit,
  }
}
