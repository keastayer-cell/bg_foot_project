import { reactive, ref } from 'vue'

export function useAdminReferees({ request, clearMessages, errorMessage, successMessage }) {
  const refereesList = ref([])
  const editingRefereeId = ref(null)
  const refereeSubMode = ref('create')
  const refereeEditSelectId = ref('')
  const refereeForm = reactive({
    fullName: '',
    city: '',
    birthDate: '',
    photoDataUrl: '',
  })

  function resetRefereeForm() {
    refereeForm.fullName = ''
    refereeForm.city = ''
    refereeForm.birthDate = ''
    refereeForm.photoDataUrl = ''
  }

  function startEditReferee(item) {
    editingRefereeId.value = item.id
    refereeForm.fullName = item.fullName
    refereeForm.city = item.city || ''
    refereeForm.birthDate = item.birthDate || ''
    refereeForm.photoDataUrl = item.photoDataUrl || ''
    clearMessages()
  }

  function cancelEditReferee() {
    editingRefereeId.value = null
    resetRefereeForm()
    clearMessages()
  }

  function onRefereeSelectChange() {
    if (!refereeEditSelectId.value) {
      cancelEditReferee()
      return
    }
    const referee = refereesList.value.find((item) => String(item.id) === refereeEditSelectId.value)
    if (referee) startEditReferee(referee)
  }

  async function loadRefereeRegistry() {
    try {
      const payload = await request('/api/referees?active_flag=1', { method: 'GET' })
      refereesList.value = Array.isArray(payload) ? payload : []
    } catch (error) {
      refereesList.value = []
      errorMessage.value = error.message || 'Не удалось загрузить судей.'
    }
  }

  function refereePayload() {
    return {
      fullName: refereeForm.fullName,
      city: refereeForm.city,
      birthDate: refereeForm.birthDate || null,
      photoDataUrl: refereeForm.photoDataUrl,
    }
  }

  async function createReferee() {
    clearMessages()
    if (!refereeForm.fullName) {
      errorMessage.value = 'Укажите ФИО судьи.'
      return
    }

    try {
      await request('/api/referees', {
        method: 'POST',
        body: JSON.stringify(refereePayload()),
      })
      await loadRefereeRegistry()
      resetRefereeForm()
      successMessage.value = 'Судья создан.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось создать судью.'
    }
  }

  async function saveEditReferee() {
    clearMessages()
    if (!refereeForm.fullName) {
      errorMessage.value = 'Укажите ФИО судьи.'
      return
    }

    try {
      await request(`/api/referees/${editingRefereeId.value}`, {
        method: 'PUT',
        body: JSON.stringify(refereePayload()),
      })
      await loadRefereeRegistry()
      cancelEditReferee()
      successMessage.value = 'Судья обновлен.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось обновить судью.'
    }
  }

  async function deactivateReferee(refereeId) {
    clearMessages()
    try {
      await request(`/api/referees/${refereeId}`, { method: 'DELETE' })
      if (String(editingRefereeId.value || '') === String(refereeId)) {
        cancelEditReferee()
        refereeEditSelectId.value = ''
      }
      await loadRefereeRegistry()
      successMessage.value = 'Судья деактивирован.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось удалить судью.'
    }
  }

  function onRefereePhotoSelected(event) {
    const file = event.target?.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      refereeForm.photoDataUrl = String(reader.result || '')
    }
    reader.readAsDataURL(file)
  }

  return {
    cancelEditReferee,
    createReferee,
    deactivateReferee,
    editingRefereeId,
    loadRefereeRegistry,
    onRefereePhotoSelected,
    onRefereeSelectChange,
    refereeEditSelectId,
    refereeForm,
    refereesList,
    refereeSubMode,
    saveEditReferee,
  }
}
