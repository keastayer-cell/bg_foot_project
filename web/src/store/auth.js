import { computed, ref } from 'vue'
import { requestJson, requestRaw } from '../api/http'
import { createAccountApi } from '../api/account'

const PERSISTENT_SESSION_KEY = 'football_stats_persistent_session'
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const token = ref('')
const user = ref(null)
let refreshPromise = null

function normalizeTeamScope(scope) {
  const teamId = Number(scope?.teamId)
  const teamName = String(scope?.teamName || '').trim()
  if (!Number.isFinite(teamId) || teamId <= 0 || !teamName) {
    return null
  }
  return {
    teamId,
    teamName,
    canEditRoster: Boolean(scope?.canEditRoster),
    canEditApplication: Boolean(scope?.canEditApplication),
    validFrom: scope?.validFrom || null,
    validTo: scope?.validTo || null,
  }
}

export function normalizeUserAccess(rawUser = {}, accessProfile = null) {
  const teamScopes = (Array.isArray(accessProfile?.teamScopes) ? accessProfile.teamScopes : [])
    .map(normalizeTeamScope)
    .filter(Boolean)
  const primaryTeamScope = teamScopes[0] || null

  return {
    ...rawUser,
    roles: Array.isArray(accessProfile?.roles)
      ? accessProfile.roles
      : Array.isArray(rawUser?.roles) ? rawUser.roles : [],
    roleAnnotations: Array.isArray(accessProfile?.roleAnnotations)
      ? accessProfile.roleAnnotations
      : [],
    mustChangePassword: Boolean(accessProfile?.mustChangePassword ?? rawUser?.mustChangePassword),
    teamScopes,
    teamId: primaryTeamScope?.teamId || null,
    teamName: primaryTeamScope?.teamName || '',
    teamScope: primaryTeamScope,
  }
}

function setPersistentSession(enabled) {
  if (enabled) {
    localStorage.setItem(PERSISTENT_SESSION_KEY, '1')
  } else {
    localStorage.removeItem(PERSISTENT_SESSION_KEY)
  }
}

function hasPersistentSessionHint() {
  return localStorage.getItem(PERSISTENT_SESSION_KEY) === '1'
}

function clearLocalAuthState() {
  token.value = ''
  user.value = null
  setPersistentSession(false)
}

function createHttpError(message, status, body) {
  const error = new Error(message)
  error.status = status
  error.body = body
  return error
}

function isUnauthorizedError(error) {
  return Number(error?.status) === 401
}

async function apiRequest(path, options = {}) {
  try {
    return await requestJson(apiBaseUrl, path, options)
  } catch (error) {
    if (Number.isFinite(error?.status)) {
      throw createHttpError(error.message, error.status, error.body)
    }
    throw error
  }
}

async function apiRequestRaw(path, options = {}) {
  return requestRaw(apiBaseUrl, path, options)
}

function applyAuthResponse(payload) {
  token.value = payload.token
  user.value = normalizeUserAccess({
    id: payload.userId,
    email: payload.email,
    name: payload.name,
    roles: payload.roles || [],
    mustChangePassword: Boolean(payload.mustChangePassword),
  })
}

function applyAccountResponse(payload) {
  user.value = normalizeUserAccess({
    id: payload.userId,
    email: payload.email,
    name: payload.name,
    roles: payload.roles || [],
    mustChangePassword: Boolean(payload.mustChangePassword),
  }, payload)
  return user.value
}

async function register({ email, name, password }) {
  const payload = await apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, name, password }),
  })

  applyAuthResponse(payload)
  setPersistentSession(true)
  await loadCurrentUser().catch(() => null)
  return user.value
}

async function login({ email, password }) {
  const payload = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  applyAuthResponse(payload)
  setPersistentSession(true)
  await loadCurrentUser().catch(() => null)
  return user.value
}

async function guestLogin() {
  const payload = await apiRequest('/api/auth/guest', {
    method: 'POST',
    body: JSON.stringify({}),
  })

  applyAuthResponse(payload)
  setPersistentSession(false)
  return user.value
}

async function loadCurrentUser() {
  if (!token.value) return null

  const payload = await apiRequest('/api/auth/me', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token.value}`,
    },
  })

  const accessProfile = await apiRequest('/api/admin/access/me', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token.value}`,
    },
  }).catch(() => null)

  user.value = normalizeUserAccess(payload, accessProfile)
  return user.value
}

async function refreshSession({ suppressErrors = false } = {}) {
  if (refreshPromise) {
    return refreshPromise
  }

  refreshPromise = (async () => {
    try {
      const payload = await apiRequest('/api/auth/refresh', {
        method: 'POST',
      })

      applyAuthResponse(payload)
      setPersistentSession(true)
      return payload
    } catch (error) {
      await logout({ remote: true, suppressErrors: true })
      if (!suppressErrors) {
        throw error
      }
      return null
    } finally {
      refreshPromise = null
    }
  })()

  return refreshPromise
}

async function ensureSession({ forceRefresh = false } = {}) {
  if (token.value) {
    try {
      await loadCurrentUser()
      return user.value
    } catch (error) {
      if (!isUnauthorizedError(error)) {
        throw error
      }
    }
  }

  if (!forceRefresh && !hasPersistentSessionHint()) {
    return null
  }

  const refreshed = await refreshSession({ suppressErrors: true })
  if (!refreshed || !token.value) {
    return null
  }

  try {
    await loadCurrentUser()
    return user.value
  } catch {
    clearLocalAuthState()
    return null
  }
}

async function authorizedApiRequest(path, options = {}) {
  const { __retriedAfterRefresh, ...requestOptions } = options

  if (!token.value) {
    await ensureSession({ forceRefresh: true })
  }

  if (!token.value) {
    throw new Error('Требуется авторизация.')
  }

  const headers = {
    ...(requestOptions.headers || {}),
    Authorization: `Bearer ${token.value}`,
  }

  try {
    return await apiRequest(path, {
      ...requestOptions,
      headers,
    })
  } catch (error) {
    if (!__retriedAfterRefresh && isUnauthorizedError(error)) {
      await refreshSession()
      return authorizedApiRequest(path, {
        ...requestOptions,
        __retriedAfterRefresh: true,
      })
    }
    throw error
  }
}

async function authorizedApiRequestRaw(path, options = {}) {
  const { __retriedAfterRefresh, ...requestOptions } = options

  if (!token.value) {
    await ensureSession({ forceRefresh: true })
  }

  if (!token.value) {
    throw new Error('Требуется авторизация.')
  }

  const headers = {
    ...(requestOptions.headers || {}),
    Authorization: `Bearer ${token.value}`,
  }

  const response = await apiRequestRaw(path, {
    ...requestOptions,
    headers,
  })

  if (response.status === 401 && !__retriedAfterRefresh) {
    await refreshSession()
    return authorizedApiRequestRaw(path, {
      ...requestOptions,
      __retriedAfterRefresh: true,
    })
  }

  if (!response.ok) {
    let body = {}
    try {
      body = await response.json()
    } catch {
      body = {}
    }
    throw createHttpError(body.error || 'Не удалось выполнить запрос.', response.status, body)
  }

  return response
}

const accountApi = createAccountApi(authorizedApiRequest)

async function loadAccount() {
  return applyAccountResponse(await accountApi.get())
}

async function updateProfile({ email, name }) {
  return applyAccountResponse(await accountApi.update({ email, name }))
}

async function changePassword({ currentPassword, newPassword }) {
  const payload = await accountApi.changePassword({ currentPassword, newPassword })

  applyAuthResponse(payload)
  setPersistentSession(true)
  await loadCurrentUser().catch(() => null)
  return user.value
}

async function logoutAllDevices() {
  try {
    await accountApi.logoutAll()
  } finally {
    clearLocalAuthState()
  }
}

async function loadNotificationSettings() {
  return accountApi.getNotificationSettings()
}

async function updateNotificationSettings(settings) {
  return accountApi.updateNotificationSettings(settings)
}

async function loadFavorites() {
  return accountApi.getFavorites()
}

async function addFavorite(type, targetId) {
  return accountApi.addFavorite(type, targetId)
}

async function removeFavorite(type, targetId) {
  return accountApi.removeFavorite(type, targetId)
}

// Запрос с опциональной авторизацией (для гостей)
async function optionalAuthApiRequest(path, options = {}) {
  const headers = options.headers || {}

  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`
  }

  return apiRequest(path, {
    ...options,
    headers,
  })
}

async function optionalAuthApiRequestRaw(path, options = {}) {
  const headers = {
    ...(options.headers || {}),
  }

  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`
  }

  const response = await apiRequestRaw(path, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let body = {}
    try {
      body = await response.json()
    } catch {
      body = {}
    }
    throw createHttpError(body.error || 'Не удалось выполнить запрос.', response.status, body)
  }

  return response
}

async function logout({ remote = true, suppressErrors = false } = {}) {
  try {
    if (remote) {
      await apiRequest('/api/auth/logout', {
        method: 'POST',
      })
    }
  } catch (error) {
    if (!suppressErrors) {
      throw error
    }
  } finally {
    clearLocalAuthState()
  }
}

const isAuthenticated = computed(() => Boolean(token.value))

function hasRole(roleCode) {
  const roles = user.value?.roles
  return Array.isArray(roles) && roles.includes(roleCode)
}

function isTeamRepresentative() {
  return hasRole('TEAM_REP')
}

function hasTeamAccess(permission = '') {
  if (!Number.isFinite(Number(user.value?.teamId)) || Number(user.value?.teamId) <= 0) {
    return false
  }
  if (!permission) {
    return true
  }
  return Boolean(user.value?.teamScope?.[permission])
}

export function useAuth() {
  return {
    token,
    user,
    isAuthenticated,
    register,
    login,
    guestLogin,
    logout,
    changePassword,
    loadAccount,
    updateProfile,
    logoutAllDevices,
    loadNotificationSettings,
    updateNotificationSettings,
    loadFavorites,
    addFavorite,
    removeFavorite,
    loadCurrentUser,
    ensureSession,
    refreshSession,
    authorizedApiRequest,
    authorizedApiRequestRaw,
    optionalAuthApiRequest,
    optionalAuthApiRequestRaw,
    hasRole,
    hasTeamAccess,
    isTeamRepresentative,
  }
}
