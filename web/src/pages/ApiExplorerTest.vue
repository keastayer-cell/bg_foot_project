<template>
  <section class="section-wrap api-test-page">
    <article class="card api-test-card" v-if="endpoint">
      <div class="toolbar api-test-head">
        <h2 class="section-title">{{ endpoint.title }}</h2>
        <router-link class="btn-ghost" to="/api-explorer">Назад к списку</router-link>
      </div>

      <p class="muted">{{ endpoint.description }}</p>

      <div class="api-test-base-row">
        <div>Метод: <b>{{ endpoint.method }}</b></div>
      </div>

      <input class="api-test-path-input" :value="resolvedPath" readonly />

      <div class="api-form-section" v-if="endpoint.pathParams?.length">
        <h3>Параметры URL</h3>
        <div class="api-params-grid">
          <label v-for="param in endpoint.pathParams" :key="param.name">
            <span>{{ param.name }}</span>
            <input v-model.trim="pathParams[param.name]" :placeholder="param.placeholder || ''" />
          </label>
        </div>
      </div>

      <div class="api-form-section" v-if="endpoint.queryParams?.length">
        <h3>Query-параметры</h3>
        <div class="api-params-grid">
          <label v-for="param in endpoint.queryParams" :key="param.name">
            <span>{{ param.name }}</span>
            <input v-model.trim="queryParams[param.name]" :placeholder="param.placeholder || ''" />
          </label>
        </div>
      </div>

      <div class="api-form-section" v-if="endpoint.bodyExample !== undefined">
        <div class="toolbar api-body-head">
          <h3>Тело запроса (JSON)</h3>
          <button class="btn-ghost" type="button" @click="resetBody">Сбросить</button>
        </div>
        <textarea v-model="requestBodyText" class="textarea api-body-textarea" spellcheck="false" />
      </div>

      <div class="actions-row" style="justify-content:flex-start; margin-top: 0;">
        <button class="btn-primary" type="button" @click="runTest" :disabled="isRunning">
          {{ isRunning ? 'Отправка...' : 'Тест' }}
        </button>
      </div>

      <p class="error-text" v-if="requestError">{{ requestError }}</p>

      <div class="api-response-card" v-if="responseMeta || responseBodyText">
        <div class="toolbar api-response-head">
          <h3>Result:</h3>
          <div class="api-response-badges" v-if="responseMeta">
            <span class="status-chip" :class="responseMeta.ok ? 'status-played' : 'status-scheduled'">
              {{ responseMeta.status }}
            </span>
            <span class="status-chip status-lineups">{{ responseMeta.durationMs }} мс</span>
          </div>
        </div>
        <pre class="api-response-output">{{ responseBodyText }}</pre>
      </div>
    </article>

    <article class="card" v-else>
      <h2 class="section-title">Endpoint не найден</h2>
      <router-link class="btn-ghost" to="/api-explorer">Назад к списку API</router-link>
    </article>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { findEndpointByKey } from '../data/apiExplorerCatalog'
import { useAuth } from '../store/auth'

const route = useRoute()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const { token } = useAuth()

const endpoint = computed(() => findEndpointByKey(String(route.params.endpointKey || '')))

const pathParams = reactive({})
const queryParams = reactive({})
const requestBodyText = ref('')
const responseBodyText = ref('')
const responseMeta = ref(null)
const requestError = ref('')
const isRunning = ref(false)

function prettyJson(value) {
  return JSON.stringify(value, null, 2)
}

function resetReactiveObject(target) {
  for (const key of Object.keys(target)) {
    delete target[key]
  }
}

function resetStateForEndpoint() {
  resetReactiveObject(pathParams)
  resetReactiveObject(queryParams)

  if (!endpoint.value) return

  for (const param of endpoint.value.pathParams || []) {
    pathParams[param.name] = ''
  }

  for (const param of endpoint.value.queryParams || []) {
    queryParams[param.name] = ''
  }

  requestBodyText.value = endpoint.value.bodyExample !== undefined
    ? prettyJson(endpoint.value.bodyExample)
    : ''

  responseBodyText.value = ''
  responseMeta.value = null
  requestError.value = ''
}

watch(endpoint, resetStateForEndpoint, { immediate: true })

function resetBody() {
  if (!endpoint.value) return
  requestBodyText.value = endpoint.value.bodyExample !== undefined
    ? prettyJson(endpoint.value.bodyExample)
    : ''
}

const resolvedPath = computed(() => {
  if (!endpoint.value) return ''

  let path = endpoint.value.path
  for (const param of endpoint.value.pathParams || []) {
    const value = String(pathParams[param.name] || '').trim()
    path = path.replace(`{${param.name}}`, value || `{${param.name}}`)
  }

  const searchParams = new URLSearchParams()
  for (const param of endpoint.value.queryParams || []) {
    const value = String(queryParams[param.name] || '').trim()
    if (value) searchParams.set(param.name, value)
  }

  const queryString = searchParams.toString()
  return queryString ? `${path}?${queryString}` : path
})

function parseRequestBody() {
  if (!endpoint.value || endpoint.value.bodyExample === undefined) {
    return undefined
  }

  const raw = requestBodyText.value.trim()
  if (!raw) return {}

  try {
    return JSON.parse(raw)
  } catch {
    throw new Error('Тело запроса должно быть валидным JSON.')
  }
}

function isBinaryContentType(contentType) {
  if (!contentType) return false
  const normalized = contentType.toLowerCase()
  return normalized.includes('application/pdf')
    || normalized.includes('application/zip')
    || normalized.includes('application/octet-stream')
}

function getFileNameFromDisposition(contentDisposition) {
  if (!contentDisposition) return null

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch ? plainMatch[1] : null
}

async function runTest() {
  if (!endpoint.value) return

  requestError.value = ''
  responseBodyText.value = ''
  responseMeta.value = null

  try {
    const path = resolvedPath.value
    if (path.includes('{') || path.includes('}')) {
      throw new Error('Заполните все параметры URL перед отправкой.')
    }

    const headers = {}
    if (endpoint.value.auth) {
      if (!token.value) throw new Error('Нет токена. Войдите в систему.')
      headers.Authorization = `Bearer ${token.value}`
    }

    let body
    const parsedBody = parseRequestBody()
    if (parsedBody !== undefined) {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(parsedBody)
    }

    const startedAt = performance.now()
    isRunning.value = true
    const response = await fetch(`${apiBaseUrl}${path}`, {
      method: endpoint.value.method,
      headers,
      body,
    })

    const durationMs = Math.round(performance.now() - startedAt)
    const contentType = response.headers.get('content-type') || ''
    const contentDisposition = response.headers.get('content-disposition') || ''

    let parsed
    if (isBinaryContentType(contentType)) {
      const blob = await response.blob()
      parsed = {
        kind: 'binary',
        contentType: contentType || '(не указан)',
        sizeBytes: blob.size,
        fileName: getFileNameFromDisposition(contentDisposition) || '(не указано)',
      }
    } else {
      const text = await response.text()
      parsed = text
      try {
        parsed = text ? JSON.parse(text) : {}
      } catch {
        parsed = text || '(пустой ответ)'
      }
    }

    responseMeta.value = {
      status: response.status,
      ok: response.ok,
      durationMs,
    }
    responseBodyText.value = typeof parsed === 'string' ? parsed : prettyJson(parsed)
  } catch (error) {
    requestError.value = error.message || 'Не удалось выполнить запрос.'
  } finally {
    isRunning.value = false
  }
}
</script>

<style scoped>
.api-test-page {
  display: grid;
  gap: 16px;
}

.api-test-card {
  display: grid;
  gap: 14px;
}

.api-test-head,
.api-body-head,
.api-response-head {
  align-items: center;
}

.api-test-base-row,
.api-form-section,
.api-response-card {
  display: grid;
  gap: 12px;
}

.api-params-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.api-params-grid label {
  display: grid;
  gap: 8px;
}

.api-test-path-input,
.api-body-textarea {
  width: 100%;
}

.api-body-textarea {
  min-height: 240px;
}

.api-response-output {
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 640px) {
  .api-test-head,
  .api-body-head,
  .api-response-head,
  .actions-row {
    align-items: stretch;
    flex-direction: column;
  }

  .api-params-grid {
    grid-template-columns: 1fr;
  }

  .actions-row > * {
    width: 100%;
  }
}
</style>
