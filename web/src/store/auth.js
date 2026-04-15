import { computed, ref } from 'vue'

const STORAGE_KEY = 'football_stats_auth'
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

function restoreAuth() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return

  try {
    const parsed = JSON.parse(raw)
    token.value = parsed.token || ''
    user.value = parsed.user || null
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
  }
}

function persistAuth() {
  sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: token.value,
      user: user.value,
    })
  )
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
    throw new Error(body.error || 'Не удалось выполнить запрос.')
  }

  return body
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
  persistAuth()
}

async function register({ email, name, password }) {
  const payload = await apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, name, password }),
  })

  applyAuthResponse(payload)
  await loadCurrentUser().catch(() => null)
  return user.value
}

async function login({ email, password }) {
  const payload = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  applyAuthResponse(payload)
  await loadCurrentUser().catch(() => null)
  return user.value
}

async function guestLogin() {
  const payload = await apiRequest('/api/auth/guest', {
    method: 'POST',
    body: JSON.stringify({}),
  })

  applyAuthResponse(payload)
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

  persistAuth()
  return user.value
}

async function authorizedApiRequest(path, options = {}) {
  if (!token.value) {
    throw new Error('Требуется авторизация.')
  }

  const headers = {
    ...(options.headers || {}),
    Authorization: `Bearer ${token.value}`,
  }

  return apiRequest(path, {
    ...options,
    headers,
  })
}

async function changePassword({ currentPassword, newPassword }) {
  const payload = await authorizedApiRequest('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })

  applyAuthResponse(payload)
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

function logout() {
  token.value = ''
  user.value = null
  sessionStorage.removeItem(STORAGE_KEY)
}

restoreAuth()

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
    authorizedApiRequest,
    optionalAuthApiRequest,
    hasRole,
    isTeamRepresentative,
  }
}
