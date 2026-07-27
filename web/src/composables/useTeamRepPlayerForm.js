import { reactive, ref } from 'vue'

export function useTeamRepPlayerForm({ api, onSaved }) {
  const playerSaving = ref(false)
  const playerModalOpen = ref(false)
  const editingPlayerId = ref(null)
  const playerModalError = ref('')
  const playerForm = reactive({
    fullName: '',
    birthDate: '',
    residence: '',
    isGoalkeeper: false,
    photoDataUrl: '',
  })

  function openCreatePlayerModal() {
    editingPlayerId.value = null
    playerModalError.value = ''
    playerForm.fullName = ''
    playerForm.birthDate = ''
    playerForm.residence = ''
    playerForm.isGoalkeeper = false
    playerForm.photoDataUrl = ''
    playerModalOpen.value = true
  }

  function openEditPlayerModal(player) {
    editingPlayerId.value = player.id
    playerModalError.value = ''
    playerForm.fullName = player.fullName || ''
    playerForm.birthDate = player.birthDate || ''
    playerForm.residence = player.residence || ''
    playerForm.isGoalkeeper = Boolean(player.isGoalkeeper)
    playerForm.photoDataUrl = player.photoDataUrl || ''
    playerModalOpen.value = true
  }

  function closePlayerModal() {
    playerModalOpen.value = false
    playerModalError.value = ''
  }

  async function savePlayer() {
    playerModalError.value = ''
    playerSaving.value = true

    try {
      const wasEditing = Boolean(editingPlayerId.value)
      await api.savePlayer(editingPlayerId.value, {
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate || null,
        residence: playerForm.residence || null,
        isGoalkeeper: Boolean(playerForm.isGoalkeeper),
        photoDataUrl: playerForm.photoDataUrl || null,
      })
      closePlayerModal()
      await onSaved?.(wasEditing)
    } catch (error) {
      playerModalError.value = error.message || 'Не удалось сохранить игрока.'
    } finally {
      playerSaving.value = false
    }
  }

  function onPhotoSelected(event) {
    const file = event.target.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      playerForm.photoDataUrl = String(reader.result || '')
    }
    reader.readAsDataURL(file)
  }

  return {
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
  }
}
