import { computed, reactive, ref, watch } from 'vue'

export function useAdminPlayers({ request, clearMessages, errorMessage, successMessage }) {
  const playersList = ref([])
  const editingPlayerId = ref(null)
  const playerSubMode = ref('create')
  const playerEditSelectId = ref('')
  const playerForm = reactive({
    fullName: '',
    birthDate: '',
    residence: '',
    isGoalkeeper: false,
    photoDataUrl: '',
  })

  const playerEditOptions = computed(() => {
    return playersList.value.map((player) => ({
      value: String(player.id),
      label: player.fullName || '',
      keywords: player.fullName || '',
    }))
  })

  function resetPlayerForm() {
    playerForm.fullName = ''
    playerForm.birthDate = ''
    playerForm.residence = ''
    playerForm.isGoalkeeper = false
    playerForm.photoDataUrl = ''
  }

  function startEditPlayer(item) {
    editingPlayerId.value = item.id
    playerForm.fullName = item.fullName
    playerForm.birthDate = item.birthDate
    playerForm.residence = item.residence
    playerForm.isGoalkeeper = Boolean(item.isGoalkeeper)
    playerForm.photoDataUrl = item.photoDataUrl || ''
    clearMessages()
  }

  function cancelEditPlayer() {
    editingPlayerId.value = null
    resetPlayerForm()
    clearMessages()
  }

  function selectPlayerForEditing() {
    if (!playerEditSelectId.value) {
      cancelEditPlayer()
      return
    }
    const player = playersList.value.find((item) => String(item.id) === playerEditSelectId.value)
    if (player) startEditPlayer(player)
  }

  watch(playerEditSelectId, () => {
    if (playerSubMode.value === 'edit') selectPlayerForEditing()
  })

  async function loadPlayerRegistry() {
    try {
      const payload = await request('/api/players?active_flag=1&pagenum=0&pagesize=500', {
        method: 'GET',
      })
      playersList.value = Array.isArray(payload?.content) ? payload.content : []
    } catch (error) {
      playersList.value = []
      errorMessage.value = error.message || 'Не удалось загрузить игроков.'
    }
  }

  function playerPayload() {
    return {
      fullName: playerForm.fullName,
      birthDate: playerForm.birthDate,
      residence: playerForm.residence,
      isGoalkeeper: Boolean(playerForm.isGoalkeeper),
      photoDataUrl: playerForm.photoDataUrl,
    }
  }

  async function createPlayer() {
    clearMessages()
    if (!playerForm.fullName || !playerForm.birthDate || !playerForm.residence) {
      errorMessage.value = 'Заполните все поля игрока.'
      return
    }

    try {
      await request('/api/players', {
        method: 'POST',
        body: JSON.stringify(playerPayload()),
      })
      await loadPlayerRegistry()
      resetPlayerForm()
      successMessage.value = 'Игрок создан.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось создать игрока.'
    }
  }

  async function saveEditPlayer() {
    clearMessages()
    if (!playerForm.fullName) {
      errorMessage.value = 'Укажите ФИО игрока.'
      return
    }

    try {
      await request(`/api/players/${editingPlayerId.value}`, {
        method: 'PUT',
        body: JSON.stringify(playerPayload()),
      })
      await loadPlayerRegistry()
      cancelEditPlayer()
      successMessage.value = 'Игрок обновлен.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось обновить игрока.'
    }
  }

  async function deactivatePlayer(playerId) {
    clearMessages()
    try {
      await request(`/api/players/${playerId}`, { method: 'DELETE' })
      if (String(editingPlayerId.value || '') === String(playerId)) {
        cancelEditPlayer()
        playerEditSelectId.value = ''
      }
      await loadPlayerRegistry()
      successMessage.value = 'Игрок деактивирован.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось удалить игрока.'
    }
  }

  function onPlayerPhotoSelected(event) {
    const file = event.target?.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      playerForm.photoDataUrl = String(reader.result || '')
    }
    reader.readAsDataURL(file)
  }

  return {
    cancelEditPlayer,
    createPlayer,
    deactivatePlayer,
    editingPlayerId,
    loadPlayerRegistry,
    onPlayerPhotoSelected,
    playerEditOptions,
    playerEditSelectId,
    playerForm,
    playersList,
    playerSubMode,
    saveEditPlayer,
  }
}
