import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from 'vue'

export function useAdminAccess({
  activeTab,
  request,
  clearMessages,
  errorMessage,
  successMessage,
}) {
  const roleUsersList = ref([])
  const repUsersList = ref([])
  const rolesSearch = ref('')
  const rolesSelectedEmail = ref('')
  const rolesFoundEmail = ref('')
  const replaceRoleTarget = ref('')
  const replaceRoleNewCode = ref('USER')
  const assignRoleCode = ref('USER')
  const passwordResetResult = ref(null)
  const repSearch = ref('')
  const repSelectedEmail = ref('')
  const repFoundEmail = ref('')
  const repSelectedTeamId = ref('')
  const repUserAccess = ref(null)
  let rolesSearchTimer = null
  let repSearchTimer = null

  const roleUserByEmail = computed(() => new Map(
    roleUsersList.value.map((user) => [String(user.email || '').toLowerCase(), user]),
  ))
  const repUserByEmail = computed(() => new Map(
    repUsersList.value.map((user) => [String(user.email || '').toLowerCase(), user]),
  ))
  const filteredUsersForSelect = computed(() => {
    const query = rolesSearch.value.toLowerCase()
    return query
      ? roleUsersList.value.filter((user) => String(user.email || '').toLowerCase().includes(query))
      : roleUsersList.value
  })
  const rolesFoundUser = computed(() => {
    return rolesFoundEmail.value ? roleUserByEmail.value.get(rolesFoundEmail.value) || null : null
  })
  const filteredRepresentativeUsersForSelect = computed(() => {
    const query = repSearch.value.toLowerCase()
    return query
      ? repUsersList.value.filter((user) => String(user.email || '').toLowerCase().includes(query))
      : repUsersList.value
  })
  const repFoundUser = computed(() => {
    return repFoundEmail.value ? repUserByEmail.value.get(repFoundEmail.value) || null : null
  })
  const repTeamScopes = computed(() => {
    return Array.isArray(repUserAccess.value?.teamScopes) ? repUserAccess.value.teamScopes : []
  })
  const repCurrentTeamScope = computed(() => repTeamScopes.value[0] || null)
  const repHasMultipleTeamScopes = computed(() => repTeamScopes.value.length > 1)
  const repPrimaryActionLabel = computed(() => {
    return repCurrentTeamScope.value ? 'Изменить команду' : 'Назначить команду'
  })
  const absolutePasswordResetLink = computed(() => {
    const resetPath = String(passwordResetResult.value?.resetPath || '').trim()
    if (!resetPath) return ''
    if (typeof window === 'undefined' || !window.location?.origin) return resetPath
    return `${window.location.origin}${resetPath}`
  })

  async function loadRoleUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
    try {
      const search = new URLSearchParams({
        pagenum: String(Math.max(0, pagenum)),
        pagesize: String(Math.min(Math.max(1, pagesize), 100)),
      })
      if (String(name || '').trim()) search.set('name', String(name).trim())
      if (String(email || '').trim()) search.set('email', String(email).trim())

      const payload = await request(`/api/admin/access/users?${search.toString()}`, { method: 'GET' })
      roleUsersList.value = Array.isArray(payload?.content) ? payload.content : []
      if (rolesSelectedEmail.value && !roleUserByEmail.value.has(rolesSelectedEmail.value.toLowerCase())) {
        rolesSelectedEmail.value = ''
      }
      if (rolesFoundEmail.value && !roleUserByEmail.value.has(rolesFoundEmail.value.toLowerCase())) {
        rolesFoundEmail.value = ''
      }
    } catch (error) {
      roleUsersList.value = []
      errorMessage.value = error.message || 'Не удалось загрузить список пользователей.'
    }
  }

  async function loadRepresentativeUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
    try {
      const search = new URLSearchParams({
        pagenum: String(Math.max(0, pagenum)),
        pagesize: String(Math.min(Math.max(1, pagesize), 100)),
        role: 'TEAM_REP',
      })
      if (String(name || '').trim()) search.set('name', String(name).trim())
      if (String(email || '').trim()) search.set('email', String(email).trim())

      const payload = await request(`/api/admin/access/users?${search.toString()}`, { method: 'GET' })
      repUsersList.value = Array.isArray(payload?.content) ? payload.content : []

      const selectedEmail = repSelectedEmail.value.toLowerCase()
      if (selectedEmail && !repUserByEmail.value.has(selectedEmail)) {
        repSelectedEmail.value = ''
        repFoundEmail.value = ''
        repUserAccess.value = null
        repSelectedTeamId.value = ''
      }
    } catch (error) {
      repUsersList.value = []
      errorMessage.value = error.message || 'Не удалось загрузить представителей команд.'
    }
  }

  async function refreshRepresentativeAccessById(userId) {
    const payload = await request(`/api/admin/access/users/${userId}`, { method: 'GET' })
    repUserAccess.value = payload
    const normalizedEmail = String(payload?.email || '').toLowerCase()
    repFoundEmail.value = normalizedEmail
    repSelectedEmail.value = normalizedEmail
    repSelectedTeamId.value = payload?.teamScopes?.[0]?.teamId ? String(payload.teamScopes[0].teamId) : ''

    const index = repUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
    if (index >= 0) {
      repUsersList.value[index] = {
        ...repUsersList.value[index],
        id: payload.userId,
        email: payload.email,
        name: payload.name,
        roles: Array.isArray(payload.roles) ? payload.roles : [],
      }
    }
  }

  async function refreshRepresentativeAccessByEmail(email) {
    const user = repUserByEmail.value.get(String(email || '').toLowerCase())
    if (!user) {
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
      return
    }
    await refreshRepresentativeAccessById(user.id)
  }

  function findUserForRoles() {
    clearMessages()
    const emailFilter = String(rolesSearch.value || rolesSelectedEmail.value || '').trim()
    if (!emailFilter) {
      errorMessage.value = 'Введите email или выберите пользователя.'
      return
    }

    void loadRoleUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(() => {
      const selected = rolesSelectedEmail.value.trim().toLowerCase()
      if (selected) {
        rolesFoundEmail.value = selected
        replaceRoleTarget.value = ''
      } else if (roleUsersList.value.length === 1) {
        const email = String(roleUsersList.value[0].email || '').toLowerCase()
        rolesSelectedEmail.value = email
        rolesFoundEmail.value = email
        replaceRoleTarget.value = ''
      } else if (!roleUsersList.value.length) {
        errorMessage.value = 'Пользователь не найден.'
        rolesFoundEmail.value = ''
      } else {
        errorMessage.value = 'Выберите пользователя из найденного списка.'
      }
    })
  }

  function findRepresentative() {
    clearMessages()
    const emailFilter = String(repSearch.value || repSelectedEmail.value || '').trim()
    if (!emailFilter) {
      errorMessage.value = 'Введите email или выберите представителя.'
      return
    }

    void loadRepresentativeUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(async () => {
      const selected = repSelectedEmail.value.trim().toLowerCase()
      if (selected) {
        await refreshRepresentativeAccessByEmail(selected)
      } else if (repUsersList.value.length === 1) {
        await refreshRepresentativeAccessByEmail(String(repUsersList.value[0].email || '').toLowerCase())
      } else if (!repUsersList.value.length) {
        errorMessage.value = 'Представитель не найден.'
        repFoundEmail.value = ''
        repUserAccess.value = null
        repSelectedTeamId.value = ''
      } else {
        errorMessage.value = 'Выберите представителя из найденного списка.'
      }
    })
  }

  async function refreshFoundUserAccess() {
    const user = rolesFoundUser.value
    if (!user) return null

    const payload = await request(`/api/admin/access/users/${user.id}`, { method: 'GET' })
    const normalizedEmail = String(payload?.email || '').toLowerCase()
    const index = roleUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
    if (index >= 0) {
      roleUsersList.value[index] = {
        ...roleUsersList.value[index],
        id: payload.userId,
        email: payload.email,
        name: payload.name,
        roles: Array.isArray(payload.roles) ? payload.roles : [],
        mustChangePassword: Boolean(payload.mustChangePassword),
      }
    }
    return payload
  }

  async function syncRepresentativeUsers(payload) {
    const normalizedEmail = String(payload?.email || '').toLowerCase()
    const roles = Array.isArray(payload?.roles) ? payload.roles : []
    const hasTeamRepRole = roles.includes('TEAM_REP')
    const currentSearch = repSearch.value.trim()
    const isCurrentTarget =
      repSelectedEmail.value.toLowerCase() === normalizedEmail ||
      repFoundEmail.value.toLowerCase() === normalizedEmail

    if (activeTab.value === 'representatives' || hasTeamRepRole || isCurrentTarget) {
      await loadRepresentativeUsers({
        email: currentSearch,
        pagenum: 0,
        pagesize: currentSearch ? 50 : 20,
      })
    }
    if (!hasTeamRepRole && isCurrentTarget) {
      repSelectedEmail.value = ''
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
    } else if (hasTeamRepRole && currentSearch && normalizedEmail.includes(currentSearch.toLowerCase())) {
      repSelectedEmail.value = normalizedEmail
    }
  }

  function startReplaceRole(role) {
    replaceRoleTarget.value = role
    replaceRoleNewCode.value = 'USER'
  }

  async function confirmReplaceRole() {
    clearMessages()
    const user = rolesFoundUser.value
    if (!user) return
    try {
      await request(`/api/admin/access/users/${user.id}/roles/${replaceRoleTarget.value}`, { method: 'DELETE' })
      await request(`/api/admin/access/users/${user.id}/roles/${replaceRoleNewCode.value}`, { method: 'POST' })
      await syncRepresentativeUsers(await refreshFoundUserAccess())
      replaceRoleTarget.value = ''
      successMessage.value = 'Роль заменена.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось заменить роль.'
    }
  }

  async function removeRoleFromFound(role) {
    clearMessages()
    const user = rolesFoundUser.value
    if (!user) return
    try {
      await request(`/api/admin/access/users/${user.id}/roles/${role}`, { method: 'DELETE' })
      await syncRepresentativeUsers(await refreshFoundUserAccess())
      successMessage.value = 'Роль снята.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось снять роль.'
    }
  }

  async function assignRoleToFound() {
    clearMessages()
    const user = rolesFoundUser.value
    if (!user) return
    if (user.roles.includes(assignRoleCode.value)) {
      errorMessage.value = 'Такая роль уже назначена.'
      return
    }
    try {
      await request(`/api/admin/access/users/${user.id}/roles/${assignRoleCode.value}`, { method: 'POST' })
      await syncRepresentativeUsers(await refreshFoundUserAccess())
      successMessage.value = 'Роль добавлена.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось добавить роль.'
    }
  }

  async function resetPasswordForFoundUser() {
    clearMessages()
    const user = rolesFoundUser.value
    if (!user) return
    try {
      passwordResetResult.value = await request(`/api/admin/access/users/${user.id}/reset-password`, {
        method: 'POST',
      })
      await refreshFoundUserAccess()
      successMessage.value = 'Одноразовая ссылка для установки нового пароля создана.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось сбросить пароль пользователя.'
    }
  }

  async function copyPasswordResetLink() {
    const link = absolutePasswordResetLink.value
    clearMessages()
    if (!link) {
      errorMessage.value = 'Ссылка для сброса пароля недоступна.'
      return
    }
    try {
      await navigator.clipboard.writeText(link)
      successMessage.value = 'Ссылка скопирована в буфер обмена.'
    } catch {
      errorMessage.value = 'Не удалось скопировать ссылку. Скопируйте ее вручную.'
    }
  }

  async function saveRepresentativeTeam() {
    clearMessages()
    const user = repFoundUser.value
    if (!user) {
      errorMessage.value = 'Сначала выберите представителя.'
      return
    }
    const nextTeamId = Number(repSelectedTeamId.value)
    if (!Number.isFinite(nextTeamId) || nextTeamId <= 0) {
      errorMessage.value = 'Выберите команду.'
      return
    }
    const currentScopes = repTeamScopes.value
    if (currentScopes.length === 1 && Number(currentScopes[0].teamId) === nextTeamId) {
      errorMessage.value = 'Эта команда уже назначена представителю.'
      return
    }

    try {
      for (const scope of currentScopes) {
        await request(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, { method: 'DELETE' })
      }
      await request(`/api/admin/access/users/${user.id}/team-scopes`, {
        method: 'POST',
        body: JSON.stringify({
          teamId: nextTeamId,
          canEditRoster: true,
          canEditApplication: true,
        }),
      })
      await refreshRepresentativeAccessById(user.id)
      successMessage.value = currentScopes.length
        ? 'Команда представителя обновлена.'
        : 'Команда представителю назначена.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось назначить команду представителю.'
    }
  }

  async function unassignRepresentativeTeam() {
    clearMessages()
    const user = repFoundUser.value
    if (!user) {
      errorMessage.value = 'Сначала выберите представителя.'
      return
    }
    if (!repTeamScopes.value.length) {
      errorMessage.value = 'У представителя нет активной привязки к команде.'
      return
    }
    try {
      for (const scope of repTeamScopes.value) {
        await request(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, { method: 'DELETE' })
      }
      await refreshRepresentativeAccessById(user.id)
      repSelectedTeamId.value = ''
      successMessage.value = 'Команда откреплена от представителя.'
    } catch (error) {
      errorMessage.value = error.message || 'Не удалось открепить команду.'
    }
  }

  watch(rolesSearch, (rawValue) => {
    clearTimeout(rolesSearchTimer)
    if (activeTab.value !== 'roles') return
    const email = String(rawValue || '').trim()
    if (!email) {
      void loadRoleUsers()
      rolesSelectedEmail.value = ''
      rolesFoundEmail.value = ''
      return
    }
    rolesSearchTimer = setTimeout(async () => {
      await loadRoleUsers({ email, pagenum: 0, pagesize: 50 })
      if (roleUsersList.value.length === 1) {
        const foundEmail = String(roleUsersList.value[0].email || '').toLowerCase()
        rolesSelectedEmail.value = foundEmail
        rolesFoundEmail.value = foundEmail
        replaceRoleTarget.value = ''
      }
    }, 5000)
  })

  watch(repSearch, (rawValue) => {
    clearTimeout(repSearchTimer)
    if (activeTab.value !== 'representatives') return
    const email = String(rawValue || '').trim()
    if (!email) {
      void loadRepresentativeUsers()
      repSelectedEmail.value = ''
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
      return
    }
    repSearchTimer = setTimeout(async () => {
      await loadRepresentativeUsers({ email, pagenum: 0, pagesize: 50 })
      if (repUsersList.value.length === 1) {
        await refreshRepresentativeAccessByEmail(String(repUsersList.value[0].email || '').toLowerCase())
      }
    }, 5000)
  })

  watch(repSelectedEmail, (value) => {
    const email = String(value || '').trim().toLowerCase()
    if (!email) {
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
      return
    }
    void refreshRepresentativeAccessByEmail(email)
  })

  if (getCurrentInstance()) {
    onBeforeUnmount(() => {
      clearTimeout(rolesSearchTimer)
      clearTimeout(repSearchTimer)
    })
  }

  return {
    absolutePasswordResetLink,
    assignRoleCode,
    assignRoleToFound,
    confirmReplaceRole,
    copyPasswordResetLink,
    filteredRepresentativeUsersForSelect,
    filteredUsersForSelect,
    findRepresentative,
    findUserForRoles,
    loadRepresentativeUsers,
    loadRoleUsers,
    passwordResetResult,
    removeRoleFromFound,
    repCurrentTeamScope,
    repFoundUser,
    repHasMultipleTeamScopes,
    repPrimaryActionLabel,
    repSearch,
    repSelectedEmail,
    repSelectedTeamId,
    repUsersList,
    replaceRoleNewCode,
    replaceRoleTarget,
    resetPasswordForFoundUser,
    roleUsersList,
    rolesFoundUser,
    rolesSearch,
    rolesSelectedEmail,
    saveRepresentativeTeam,
    startReplaceRole,
    unassignRepresentativeTeam,
  }
}
