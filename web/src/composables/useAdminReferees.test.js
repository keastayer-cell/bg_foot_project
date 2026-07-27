import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useAdminReferees } from './useAdminReferees'

function createReferees(request = vi.fn()) {
  const errorMessage = ref('')
  const successMessage = ref('')
  const referees = useAdminReferees({
    request,
    clearMessages: () => {
      errorMessage.value = ''
      successMessage.value = ''
    },
    errorMessage,
    successMessage,
  })
  return { ...referees, errorMessage, successMessage }
}

describe('useAdminReferees', () => {
  it('validates a referee name before creating', async () => {
    const request = vi.fn()
    const { createReferee, errorMessage } = createReferees(request)

    await createReferee()

    expect(request).not.toHaveBeenCalled()
    expect(errorMessage.value).toBe('Укажите ФИО судьи.')
  })

  it('loads and selects a referee for editing', async () => {
    const request = vi.fn().mockResolvedValue([{ id: 3, fullName: 'Судья', city: 'Богородск' }])
    const {
      editingRefereeId,
      loadRefereeRegistry,
      onRefereeSelectChange,
      refereeEditSelectId,
      refereeForm,
    } = createReferees(request)

    await loadRefereeRegistry()
    refereeEditSelectId.value = '3'
    onRefereeSelectChange()

    expect(editingRefereeId.value).toBe(3)
    expect(refereeForm.fullName).toBe('Судья')
  })
})
