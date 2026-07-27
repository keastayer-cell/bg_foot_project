import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useAdminPlayers } from './useAdminPlayers'

function createPlayers(request = vi.fn()) {
  const errorMessage = ref('')
  const successMessage = ref('')
  const players = useAdminPlayers({
    request,
    clearMessages: () => {
      errorMessage.value = ''
      successMessage.value = ''
    },
    errorMessage,
    successMessage,
  })
  return { ...players, errorMessage, successMessage }
}

describe('useAdminPlayers', () => {
  it('validates required fields before creating a player', async () => {
    const request = vi.fn()
    const { createPlayer, errorMessage } = createPlayers(request)

    await createPlayer()

    expect(request).not.toHaveBeenCalled()
    expect(errorMessage.value).toBe('Заполните все поля игрока.')
  })

  it('creates a player and refreshes the registry', async () => {
    const request = vi.fn()
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({ content: [{ id: 1, fullName: 'Игрок' }] })
    const { createPlayer, playerForm, playersList, successMessage } = createPlayers(request)
    Object.assign(playerForm, {
      fullName: 'Игрок',
      birthDate: '2000-01-01',
      residence: 'Богородск',
    })

    await createPlayer()

    expect(request).toHaveBeenNthCalledWith(1, '/api/players', expect.objectContaining({ method: 'POST' }))
    expect(playersList.value).toHaveLength(1)
    expect(successMessage.value).toBe('Игрок создан.')
  })
})
