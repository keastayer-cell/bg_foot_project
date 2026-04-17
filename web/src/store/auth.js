import { computed, ref } from 'vue'

const PERSISTENT_SESSION_KEY = 'football_stats_persistent_session'
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const isDev = import.meta.env.DEV
const TEAM_POOL = [
  'Север',
  'Юг',
  'Восток',
  'Центр',
  'Спартак',
  'Динамо',
  'Локомотив',
  'Олимп',
  'Волга',
  'Заря',
  'Старт',
  'Факел',
]

const token = ref('')
const user = ref(null)
let refreshPromise = null

function hashString(value) {
  const source = String(value || '')
  let hash = 0

  for (let index = 0; index < source.length; index += 1) {
    hash = (hash * 31 + source.charCodeAt(index)) >>> 0
  }

  return hash
}

function normalizeTeamName(value) {
  const raw = String(value || '').trim()
  if (!raw) return ''

  const exact = TEAM_POOL.find((team) => team.toLowerCase() === raw.toLowerCase())
  if (exact) return exact

  return raw
}

function resolveTeamName(rawUser) {
  const directTeam =
    rawUser?.teamName ||
    rawUser?.team ||
    rawUser?.team_title ||
    rawUser?.teamTitle ||
    rawUser?.team_name ||
    rawUser?.teamNameRu ||
    rawUser?.team?.name ||
    ''

  const normalized = normalizeTeamName(directTeam)
  if (normalized) return normalized

  const numericTeamId = Number(rawUser?.teamId || rawUser?.team?.id || 0)
  if (Number.isFinite(numericTeamId) && numericTeamId > 0) {
    return TEAM_POOL[(numericTeamId - 1) % TEAM_POOL.length]
  }

  const seed = `${rawUser?.email || ''}:${rawUser?.name || ''}:${rawUser?.id || ''}`
  return TEAM_POOL[hashString(seed) % TEAM_POOL.length]
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

function maskSensitive(value) {
  if (!value || typeof value !== 'object') return value

  if (Array.isArray(value)) {
    return value.map((item) => maskSensitive(item))
  }

  const result = {}
  for (const [key, fieldValue] of Object.entries(value)) {
    const secret = /password|token|secret|authorization/i.test(key)
    result[key] = secret ? '***' : maskSensitive(fieldValue)
  }
  return result
}

function parseRequestBody(body) {
  if (!body || typeof body !== 'string') return null
  try {
    return maskSensitive(JSON.parse(body))
  } catch {
    return body
  }
}

async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const url = `${apiBaseUrl}${path}`
  const startedAt = performance.now()
  const requestOptions = {
    ...options,
    credentials: options.credentials || 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  }

  if (isDev) {
    console.groupCollapsed(`[API] ${method} ${path}`)
    console.log('Запрос:', {
      method,
      url,
      headers: requestOptions.headers || {},
      body: parseRequestBody(options.body),
    })
  }

  let response
  try {
    response = await fetch(url, requestOptions)
  } catch (error) {
    if (isDev) {
      console.error('Ошибка сети:', error)
      console.groupEnd()
    }
    throw new Error('Сервер недоступен. Попробуйте позже.')
  }

  const body = await response.json().catch(() => ({}))

  if (isDev) {
    console.log('Ответ:', {
      status: response.status,
      ok: response.ok,
      durationMs: Math.round(performance.now() - startedAt),
      body: maskSensitive(body),
    })
    console.groupEnd()
  }

  if (!response.ok) {
    throw createHttpError(body.error || 'Не удалось выполнить запрос.', response.status, body)
  }

  return body
}

async function apiRequestRaw(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const url = `${apiBaseUrl}${path}`
  const requestOptions = {
    ...options,
    credentials: options.credentials || 'include',
    headers: {
      ...(options.headers || {}),
    },
  }

  try {
    return await fetch(url, requestOptions)
  } catch {
    throw new Error('Сервер недоступен. Попробуйте позже.')
  }
}

function applyAuthResponse(payload) {
  token.value = payload.token
  user.value = {
    id: payload.userId,
    email: payload.email,
    name: payload.name,
    roles: payload.roles || [],
    mustChangePassword: Boolean(payload.mustChangePassword),
    teamName: resolveTeamName(payload),
  }
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

  user.value = payload
  if (!Array.isArray(user.value.roles)) {
    user.value.roles = []
  }
  user.value.mustChangePassword = Boolean(user.value.mustChangePassword)

  const accessProfile = await apiRequest('/api/admin/access/me', {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token.value}`,
    },
  }).catch(() => null)

  const teamScope = accessProfile?.teamScopes?.[0]
  if (teamScope?.teamName) {
    user.value.teamName = teamScope.teamName
    user.value.teamId = teamScope.teamId || null
    user.value.teamScope = {
      canEditRoster: Boolean(teamScope.canEditRoster),
      canEditApplication: Boolean(teamScope.canEditApplication),
    }
  } else {
    user.value.teamName = resolveTeamName(user.value)
  }
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

async function changePassword({ currentPassword, newPassword }) {
  const payload = await authorizedApiRequest('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })

  applyAuthResponse(payload)
  setPersistentSession(true)
  await loadCurrentUser().catch(() => null)
  return user.value
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
    loadCurrentUser,
    ensureSession,
    refreshSession,
    authorizedApiRequest,
    authorizedApiRequestRaw,
    optionalAuthApiRequest,
    optionalAuthApiRequestRaw,
    hasRole,
    isTeamRepresentative,
  }
}
