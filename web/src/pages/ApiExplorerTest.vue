<template>
  <section class="section-wrap api-test-page">
    <article v-if="endpoint" class="api-test-shell">
      <header class="api-test-head">
        <router-link class="btn-ghost api-back-link" to="/api-explorer">К списку API</router-link>
        <div class="api-test-title-block">
          <div class="api-test-route">
            <span class="api-method-chip" :data-method="endpoint.method">{{ endpoint.method }}</span>
            <code>{{ endpoint.path }}</code>
          </div>
          <h2 class="section-title">{{ endpoint.title }}</h2>
          <p class="muted">{{ endpoint.description }}</p>
        </div>
        <div class="api-test-access">
          <span>{{ endpoint.auth ? 'С авторизацией' : 'Публичный' }}</span>
          <strong>{{ endpoint.access }}</strong>
        </div>
      </header>

      <div class="api-request-layout">
        <div class="api-request-form">
          <section v-if="endpoint.pathParams?.length" class="api-form-section">
            <h3>Параметры пути</h3>
            <div class="api-params-grid">
              <label v-for="param in endpoint.pathParams" :key="param.name">
                <span>{{ param.name }}</span>
                <input
                  v-model.trim="pathParams[param.name]"
                  :placeholder="param.placeholder || ''"
                />
              </label>
            </div>
          </section>

          <section v-if="endpoint.queryParams?.length" class="api-form-section">
            <h3>Query-параметры</h3>
            <div class="api-params-grid">
              <label v-for="param in endpoint.queryParams" :key="param.name">
                <span>{{ param.name }}</span>
                <input
                  v-model.trim="queryParams[param.name]"
                  :placeholder="param.placeholder || ''"
                />
              </label>
            </div>
          </section>

          <section v-if="endpoint.bodyExample !== undefined" class="api-form-section">
            <div class="api-body-head">
              <h3>Тело запроса</h3>
              <button class="btn-ghost" type="button" @click="resetBody">Сбросить JSON</button>
            </div>
            <textarea
              v-model="requestBodyText"
              class="textarea api-body-textarea"
              spellcheck="false"
              aria-label="Тело запроса в формате JSON"
            />
          </section>

          <label v-if="isWriteRequest" class="api-write-confirmation">
            <input v-model="writeConfirmed" type="checkbox" />
            <span>Подтверждаю выполнение запроса, изменяющего данные</span>
          </label>

          <p v-if="requestError" class="error-text api-request-error">{{ requestError }}</p>

          <button
            class="btn-primary api-run-button"
            type="button"
            :disabled="isRunning || (isWriteRequest && !writeConfirmed)"
            @click="runTest"
          >
            {{ isRunning ? 'Отправка...' : 'Отправить запрос' }}
          </button>
        </div>

        <aside class="api-request-preview">
          <div class="api-preview-head">
            <h3>Запрос</h3>
            <button class="btn-ghost api-copy-button" type="button" @click="copyRequestUrl">
              {{ copyLabel }}
            </button>
          </div>
          <code class="api-resolved-url">{{ resolvedUrl }}</code>
          <dl>
            <div>
              <dt>Метод</dt>
              <dd>{{ endpoint.method }}</dd>
            </div>
            <div>
              <dt>Токен</dt>
              <dd>{{ endpoint.auth ? (token ? 'Добавлен' : 'Нет токена') : 'Не требуется' }}</dd>
            </div>
          </dl>
        </aside>
      </div>

      <section v-if="responseMeta || responseBodyText" class="api-response-panel">
        <div class="api-response-head">
          <h3>Ответ</h3>
          <div v-if="responseMeta" class="api-response-badges">
            <span class="status-chip" :class="responseMeta.ok ? 'status-played' : 'status-scheduled'">
              HTTP {{ responseMeta.status }}
            </span>
            <span class="status-chip status-lineups">{{ responseMeta.durationMs }} мс</span>
          </div>
        </div>
        <a
          v-if="binaryDownloadUrl"
          class="btn-ghost api-download-link"
          :href="binaryDownloadUrl"
          :download="binaryFileName"
        >
          Скачать {{ binaryFileName }}
        </a>
        <pre class="api-response-output">{{ responseBodyText }}</pre>
      </section>
    </article>

    <article v-else class="api-not-found">
      <h2 class="section-title">Endpoint не найден</h2>
      <router-link class="btn-ghost" to="/api-explorer">К списку API</router-link>
    </article>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { findEndpointByKey } from '../data/apiExplorerCatalog'
import { useAuth } from '../store/auth'
import { requestRaw } from '../api/http'

const route = useRoute()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const { token } = useAuth()

const endpoint = computed(() => findEndpointByKey(String(route.params.endpointKey || '')))
const isWriteRequest = computed(() => endpoint.value?.method !== 'GET')

const pathParams = reactive({})
const queryParams = reactive({})
const requestBodyText = ref('')
const responseBodyText = ref('')
const responseMeta = ref(null)
const requestError = ref('')
const isRunning = ref(false)
const writeConfirmed = ref(false)
const binaryDownloadUrl = ref('')
const binaryFileName = ref('')
const copyLabel = ref('Копировать URL')
let copyLabelTimer

function prettyJson(value) {
  return JSON.stringify(value, null, 2)
}

function resetReactiveObject(target) {
  for (const key of Object.keys(target)) delete target[key]
}

function clearBinaryDownload() {
  if (binaryDownloadUrl.value) URL.revokeObjectURL(binaryDownloadUrl.value)
  binaryDownloadUrl.value = ''
  binaryFileName.value = ''
}

function resetStateForEndpoint() {
  resetReactiveObject(pathParams)
  resetReactiveObject(queryParams)
  clearBinaryDownload()
  writeConfirmed.value = false

  if (!endpoint.value) return

  for (const param of endpoint.value.pathParams || []) pathParams[param.name] = ''
  for (const param of endpoint.value.queryParams || []) queryParams[param.name] = ''

  requestBodyText.value = endpoint.value.bodyExample !== undefined
    ? prettyJson(endpoint.value.bodyExample)
    : ''
  responseBodyText.value = ''
  responseMeta.value = null
  requestError.value = ''
}

watch(endpoint, resetStateForEndpoint, { immediate: true })
onBeforeUnmount(() => {
  clearBinaryDownload()
  window.clearTimeout(copyLabelTimer)
})

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

const resolvedUrl = computed(() => `${apiBaseUrl}${resolvedPath.value}`)

async function copyRequestUrl() {
  try {
    await navigator.clipboard.writeText(resolvedUrl.value)
    copyLabel.value = 'Скопировано'
  } catch {
    copyLabel.value = 'Не скопировано'
  }

  window.clearTimeout(copyLabelTimer)
  copyLabelTimer = window.setTimeout(() => {
    copyLabel.value = 'Копировать URL'
  }, 1600)
}

function parseRequestBody() {
  if (!endpoint.value || endpoint.value.bodyExample === undefined) return undefined

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
  clearBinaryDownload()

  try {
    const path = resolvedPath.value
    if (path.includes('{') || path.includes('}')) {
      throw new Error('Заполните все параметры пути перед отправкой.')
    }

    const headers = {}
    if (endpoint.value.auth) {
      if (!token.value) throw new Error('Нет токена. Войдите в систему заново.')
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
    const response = await requestRaw(apiBaseUrl, path, {
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
      binaryFileName.value = getFileNameFromDisposition(contentDisposition) || 'download'
      binaryDownloadUrl.value = URL.createObjectURL(blob)
      parsed = {
        type: 'binary',
        contentType: contentType || '(не указан)',
        sizeBytes: blob.size,
        fileName: binaryFileName.value,
      }
    } else {
      const text = await response.text()
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
}

.api-test-shell {
  display: grid;
  gap: 22px;
}

.api-test-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(180px, 260px);
  gap: 18px;
  align-items: start;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.api-back-link {
  text-decoration: none;
}

.api-test-title-block {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.api-test-title-block h2,
.api-test-title-block p {
  margin: 0;
}

.api-test-route {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.api-test-route code {
  overflow-wrap: anywhere;
}

.api-method-chip {
  display: inline-flex;
  flex: 0 0 66px;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 7px;
  border: 1px solid rgba(124, 163, 255, 0.28);
  border-radius: 5px;
  background: rgba(124, 163, 255, 0.12);
  font-size: 0.75rem;
  font-weight: 800;
}

.api-method-chip[data-method='POST'] {
  border-color: rgba(97, 232, 162, 0.34);
  background: rgba(97, 232, 162, 0.1);
}

.api-method-chip[data-method='PUT'],
.api-method-chip[data-method='PATCH'] {
  border-color: rgba(245, 190, 80, 0.38);
  background: rgba(245, 190, 80, 0.1);
}

.api-method-chip[data-method='DELETE'] {
  border-color: rgba(226, 88, 112, 0.42);
  background: rgba(226, 88, 112, 0.11);
}

.api-test-access {
  display: grid;
  gap: 5px;
  padding-left: 14px;
  border-left: 2px solid var(--line);
}

.api-test-access span {
  color: var(--muted);
  font-size: 0.78rem;
}

.api-test-access strong {
  overflow-wrap: anywhere;
  font-size: 0.86rem;
}

.api-request-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 340px);
  gap: 22px;
  align-items: start;
}

.api-request-form,
.api-form-section {
  display: grid;
  gap: 14px;
}

.api-form-section + .api-form-section {
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.api-form-section h3,
.api-preview-head h3,
.api-response-head h3 {
  margin: 0;
  font-size: 1rem;
}

.api-params-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.api-params-grid label {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.api-params-grid label span {
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.api-body-head,
.api-preview-head,
.api-response-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.api-body-textarea {
  width: 100%;
  min-height: 230px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.84rem;
  line-height: 1.5;
}

.api-write-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 11px 12px;
  border: 1px solid rgba(245, 190, 80, 0.32);
  border-radius: 6px;
  background: rgba(245, 190, 80, 0.07);
  font-size: 0.88rem;
}

.api-write-confirmation input {
  flex: 0 0 auto;
  margin-top: 2px;
}

.api-run-button {
  justify-self: start;
  min-width: 170px;
}

.api-request-error {
  margin: 0;
}

.api-request-preview {
  display: grid;
  gap: 14px;
  position: sticky;
  top: 16px;
  padding: 15px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.025);
}

.api-copy-button {
  padding: 7px 9px;
}

.api-resolved-url {
  display: block;
  padding: 11px;
  overflow-wrap: anywhere;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: rgba(0, 0, 0, 0.18);
  font-size: 0.82rem;
  line-height: 1.5;
}

.api-request-preview dl {
  display: grid;
  gap: 9px;
  margin: 0;
}

.api-request-preview dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.api-request-preview dt {
  color: var(--muted);
}

.api-request-preview dd {
  margin: 0;
  text-align: right;
}

.api-response-panel {
  display: grid;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.api-response-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.api-download-link {
  justify-self: start;
  text-decoration: none;
}

.api-response-output {
  max-height: 540px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: rgba(0, 0, 0, 0.22);
  font-size: 0.84rem;
  line-height: 1.5;
}

.api-not-found {
  display: grid;
  justify-items: start;
  gap: 14px;
}

@media (max-width: 900px) {
  .api-test-head {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .api-test-access {
    grid-column: 2;
  }

  .api-request-layout {
    grid-template-columns: 1fr;
  }

  .api-request-preview {
    grid-row: 1;
    position: static;
  }
}

@media (max-width: 640px) {
  .api-test-head {
    grid-template-columns: 1fr;
  }

  .api-back-link {
    justify-self: start;
  }

  .api-test-access {
    grid-column: 1;
  }

  .api-test-route {
    align-items: flex-start;
  }

  .api-params-grid {
    grid-template-columns: 1fr;
  }

  .api-body-head,
  .api-response-head {
    align-items: stretch;
    flex-direction: column;
  }

  .api-run-button,
  .api-download-link {
    width: 100%;
  }
}
</style>
