import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useAdminAccess } from './useAdminAccess'

function createAccess(request = vi.fn()) {
  const errorMessage = ref('')
  const successMessage = ref('')
  const access = useAdminAccess({
    activeTab: ref('roles'),
    request,
    clearMessages: () => {
      errorMessage.value = ''
      successMessage.value = ''
    },
    errorMessage,
    successMessage,
  })
  return { ...access, errorMessage, successMessage }
}

describe('useAdminAccess', () => {
  it('requires a user query before role search', () => {
    const request = vi.fn()
    const { errorMessage, findUserForRoles } = createAccess(request)

    findUserForRoles()

    expect(request).not.toHaveBeenCalled()
    expect(errorMessage.value).toBe('Введите email или выберите пользователя.')
  })

  it('opening selected users preserves the list for another selection', async () => {
    const users = [{ id: 1, userId: 1, email: 'one@test.ru', roles: ['TEAM_REP'] }, { id: 2, userId: 2, email: 'two@test.ru', roles: ['TEAM_REP'] }]
    const request = vi.fn(async path => path.includes('/users?') ? { content: users } : { ...users.find(user => path.endsWith('/' + user.id)), teamScopes: [] })
    const access = createAccess(request)
    await access.loadRoleUsers()
    access.rolesSelectedEmail.value = 'one@test.ru'
    access.findUserForRoles()
    access.rolesSelectedEmail.value = 'two@test.ru'
    access.findUserForRoles()
    expect(access.filteredUsersForSelect.value).toHaveLength(2)
    expect(access.rolesFoundUser.value.email).toBe('two@test.ru')
    expect(request.mock.calls.filter(([path]) => path.includes('/users?'))).toHaveLength(1)
    await access.loadRepresentativeUsers()
    access.repSelectedEmail.value = 'one@test.ru'
    access.findRepresentative()
    await vi.waitFor(() => expect(access.repFoundUser.value?.email).toBe('one@test.ru'))
    access.repSelectedEmail.value = 'two@test.ru'
    access.findRepresentative()
    await vi.waitFor(() => expect(access.repFoundUser.value?.email).toBe('two@test.ru'))
    expect(access.filteredRepresentativeUsersForSelect.value).toHaveLength(2)
    expect(request.mock.calls.filter(([path]) => path.includes('/users?'))).toHaveLength(2)
  })

  it('rejects assigning an existing role', async () => {
    const request = vi.fn().mockResolvedValue({
      content: [{ id: 1, email: 'admin@test.ru', roles: ['USER'] }],
    })
    const {
      assignRoleCode,
      assignRoleToFound,
      errorMessage,
      findUserForRoles,
      rolesFoundUser,
      rolesSearch,
    } = createAccess(request)
    rolesSearch.value = 'admin@test.ru'
    findUserForRoles()
    await vi.waitFor(() => expect(rolesFoundUser.value?.email).toBe('admin@test.ru'))
    assignRoleCode.value = 'USER'

    await assignRoleToFound()

    expect(request).toHaveBeenCalledTimes(1)
    expect(errorMessage.value).toBe('Такая роль уже назначена.')
  })

  it('captures a reset link before clearing the previous result', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { clipboard: { writeText } })
    const access = createAccess()
    access.passwordResetResult.value = { resetPath: '/reset-password?token=test' }

    await access.copyPasswordResetLink()

    expect(writeText).toHaveBeenCalledWith('/reset-password?token=test')
    vi.unstubAllGlobals()
  })
})
