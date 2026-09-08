<template>
  <article class="card admin-panel admin-notifications-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Коммуникации</p>
      <h3 class="section-title">Оповещения</h3>
      <p class="muted-text">Создание объявлений для пользователей и контроль ознакомления.</p>
    </header>

    <AdminEntityModeSwitch v-model="mode" :options="modeOptions" />

    <UiState v-if="error" tone="error" title="Операция не выполнена" :message="error" />
    <UiState v-if="success" tone="success" title="Готово" :message="success" />

    <form v-if="mode === 'create'" class="admin-step-section notification-admin-form" @submit.prevent="createNotification">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Новое оповещение</h4><p>Укажите содержание, аудиторию и необходимость подтверждения.</p></div>
      </div>

      <div class="notification-admin-fields">
        <label class="notification-admin-wide">
          Тема
          <input v-model.trim="form.title" type="text" maxlength="180" required placeholder="Например: Старт нового сезона" />
        </label>
        <label class="notification-admin-wide">
          Полный текст (HTML)
          <textarea v-model.trim="form.body" rows="7" maxlength="12000" required placeholder="Например: <p>Сезон <strong>начался</strong>.</p>"></textarea>
        </label>
        <label>
          Получатели
          <select v-model="form.audienceType">
            <option value="ALL">Все посетители сайта</option>
            <option value="ROLE">Определённая роль</option>
            <option value="TEAM">Представители команды</option>
          </select>
        </label>
        <label v-if="form.audienceType === 'ROLE'">
          Роль
          <select v-model="form.roleCode" required>
            <option value="">— выберите роль —</option>
            <option v-for="role in roleOptions" :key="role.value" :value="role.value">{{ role.label }}</option>
          </select>
        </label>
        <label v-else-if="form.audienceType === 'TEAM'">
          Команда
          <select v-model="form.teamId" required>
            <option value="">— выберите команду —</option>
            <option v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
          </select>
        </label>
        <div v-else class="notification-audience-summary">
          <small>Охват</small><strong>Все посетители, включая гостей без регистрации</strong>
        </div>
        <label>
          Важность
          <select v-model="form.severity">
            <option value="INFO">Информация</option>
            <option value="IMPORTANT">Важно</option>
            <option value="WARNING">Внимание</option>
          </select>
        </label>
        <label>
          Переход по нажатию
          <select v-model="form.actionUrl">
            <option v-for="action in actionOptions" :key="action.value" :value="action.value">{{ action.label }}</option>
          </select>
        </label>
        <label>
          Показывать до
          <input v-model="form.expiresAt" type="datetime-local" />
        </label>
        <label class="notification-ack-option">
          <input v-model="form.requiresAcknowledgement" type="checkbox" />
          <span><strong>Требовать подтверждение</strong><small>Получатель должен нажать «Ознакомился».</small></span>
        </label>
      </div>

      <div class="notification-admin-preview" :class="`is-${form.severity.toLowerCase()}`">
        <small>Предпросмотр</small>
        <strong>{{ form.title || 'Тема уведомления' }}</strong>
        <div class="notification-admin-preview-html" v-html="safePreviewHtml" />
      </div>

      <div class="admin-primary-actions">
        <button class="btn-primary" type="submit" :disabled="submitting">{{ submitting ? 'Отправляем…' : 'Отправить оповещение' }}</button>
      </div>
    </form>

    <section v-else class="admin-step-section">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>История рассылок</h4><p>Охват, прочтение и подтверждение ознакомления.</p></div>
        <span class="admin-step-count">{{ history.totalElements }}</span>
      </div>

      <UiState v-if="loading" tone="loading" title="Загружаем оповещения" />
      <div v-else-if="history.items.length" class="notification-admin-history">
        <details v-for="item in history.items" :key="item.id" class="notification-admin-history-item">
          <summary class="notification-history-row">
            <time>{{ formatDateTime(item.createdAt) }}</time>
            <b class="notification-history-severity" :class="`is-${String(item.severity || 'INFO').toLowerCase()}`">{{ severityLabel(item.severity) }}</b>
            <strong class="notification-history-title" :title="item.title">{{ item.title }}</strong>
            <small class="notification-history-audience" :title="item.audienceValue">{{ item.audienceValue }}</small>
            <span class="notification-history-stats">
              <span><small>Получили</small><strong>{{ item.recipientCount }}</strong></span>
              <span><small>Прочитали</small><strong>{{ item.readCount }}</strong></span>
              <span><small>Ознакомились</small><strong>{{ item.acknowledgedCount }}</strong></span>
            </span>
            <span class="notification-history-chevron" aria-hidden="true">⌄</span>
          </summary>
          <div class="notification-history-detail">
            <h5>{{ item.title }}</h5>
            <small>{{ item.audienceValue }}</small>
            <div class="notification-history-body" v-html="sanitizeHtml(item.body)" />
          </div>
        </details>
      </div>
      <nav v-if="history.totalElements" class="notification-history-pagination" aria-label="Страницы истории рассылок">
        <span>{{ history.pageNumber * PAGE_SIZE + 1 }}–{{ Math.min((history.pageNumber + 1) * PAGE_SIZE, history.totalElements) }} из {{ history.totalElements }}</span>
        <div>
          <button type="button" :disabled="loading || history.pageNumber === 0" @click="loadHistory(history.pageNumber - 1)">← Назад</button>
          <span aria-live="polite">{{ history.pageNumber + 1 }} / {{ Math.max(history.totalPages, 1) }}</span>
          <button type="button" :disabled="loading || history.pageNumber + 1 >= history.totalPages" @click="loadHistory(history.pageNumber + 1)">Далее →</button>
        </div>
      </nav>
      <div v-if="!loading && !error && !history.totalElements" class="admin-record-placeholder"><span>↳</span><span>Оповещения ещё не создавались.</span></div>
    </section>
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { createNotificationsApi } from '../../api/notifications'
import { useAuth } from '../../store/auth'
import AdminEntityModeSwitch from './AdminEntityModeSwitch.vue'
import UiState from '../UiState.vue'

const { authorizedApiRequest } = useAuth()
const notificationsApi = createNotificationsApi(authorizedApiRequest)
const mode = ref('create')
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const teams = ref([])
const PAGE_SIZE = 10
const history = reactive({ items: [], totalElements: 0, pageNumber: 0, totalPages: 0 })
const form = reactive({
  title: '', body: '', audienceType: 'ALL', roleCode: '', teamId: '', severity: 'INFO',
  actionUrl: '', expiresAt: '', requiresAcknowledgement: false,
})
const safePreviewHtml = computed(() => sanitizeHtml(form.body || '<p>Здесь появится полный текст сообщения.</p>'))

const modeOptions = [
  { value: 'create', label: 'Создать', description: 'Новое оповещение' },
  { value: 'history', label: 'История', description: 'Охват и ознакомление' },
]
const roleOptions = [
  { value: 'USER', label: 'Пользователи' },
  { value: 'TEAM_REP', label: 'Представители команд' },
  { value: 'REFEREE', label: 'Рефери' },
  { value: 'SUPER_ADMIN', label: 'Супер-администраторы' },
]
const actionOptions = [
  { value: '', label: 'Без перехода' },
  { value: '/', label: 'Туры и матчи' },
  { value: '/league', label: 'О лиге' },
  { value: '/team-rep-dashboard', label: 'Личный кабинет команды' },
  { value: '/team-rep-transfers', label: 'Трансферы сезона' },
  { value: '/admin', label: 'Админ-панель' },
]

watch(mode, (value) => {
  error.value = ''
  success.value = ''
  if (value === 'history') void loadHistory()
})

watch(() => form.audienceType, () => {
  form.roleCode = ''
  form.teamId = ''
})

onMounted(async () => {
  await loadTeams()
})

async function loadTeams() {
  try {
    const payload = await authorizedApiRequest('/api/teams?active_flag=1', { method: 'GET' })
    teams.value = Array.isArray(payload) ? payload : []
  } catch {
    teams.value = []
  }
}

async function loadHistory(pageNumber = 0) {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    const payload = await notificationsApi.adminList(pageNumber, PAGE_SIZE)
    history.items = Array.isArray(payload?.items) ? payload.items : []
    history.totalElements = Number(payload?.totalElements || 0)
    history.pageNumber = Number(payload?.pageNumber ?? pageNumber)
    history.totalPages = Number(payload?.totalPages ?? Math.ceil(history.totalElements / PAGE_SIZE))
  } catch (requestError) {
    error.value = requestError.message || 'Не удалось загрузить историю оповещений.'
  } finally {
    loading.value = false
  }
}

async function createNotification() {
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const payload = {
      title: form.title,
      body: form.body,
      audienceType: form.audienceType,
      roleCode: form.audienceType === 'ROLE' ? form.roleCode : null,
      teamId: form.audienceType === 'TEAM' ? Number(form.teamId) : null,
      severity: form.severity,
      actionUrl: form.actionUrl || null,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
      requiresAcknowledgement: form.requiresAcknowledgement,
    }
    const created = await notificationsApi.create(payload)
    success.value = form.audienceType === 'ALL'
      ? `Публичное оповещение опубликовано. Зарегистрированных адресатов: ${created.recipientCount}.`
      : `Оповещение отправлено. Получателей: ${created.recipientCount}.`
    resetForm()
  } catch (requestError) {
    error.value = requestError.message || 'Не удалось отправить оповещение.'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.title = ''
  form.body = ''
  form.audienceType = 'ALL'
  form.roleCode = ''
  form.teamId = ''
  form.severity = 'INFO'
  form.actionUrl = ''
  form.expiresAt = ''
  form.requiresAcknowledgement = false
}

function severityLabel(value) {
  if (value === 'IMPORTANT') return 'Важно'
  if (value === 'WARNING') return 'Внимание'
  return 'Информация'
}

function sanitizeHtml(value) {
  const template = document.createElement('template')
  template.innerHTML = value || ''
  const allowedTags = new Set(['P', 'BR', 'STRONG', 'B', 'EM', 'I', 'U', 'UL', 'OL', 'LI', 'A', 'H2', 'H3', 'H4', 'BLOCKQUOTE', 'CODE'])
  for (const element of [...template.content.querySelectorAll('*')]) {
    if (!allowedTags.has(element.tagName)) {
      element.replaceWith(...element.childNodes)
      continue
    }
    for (const attribute of [...element.attributes]) {
      if (element.tagName !== 'A' || !['href', 'target', 'rel'].includes(attribute.name)) element.removeAttribute(attribute.name)
    }
    if (element.tagName === 'A') {
      const href = element.getAttribute('href') || ''
      if (!/^(https?:|mailto:|\/)/i.test(href)) element.removeAttribute('href')
      element.setAttribute('rel', 'noopener noreferrer')
    }
  }
  return template.innerHTML
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date)
}
</script>

<style scoped>
.admin-notifications-panel { display: grid; gap: 18px; }
.notification-admin-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.notification-admin-fields > label { display: grid; gap: 7px; }
.notification-admin-wide { grid-column: 1 / -1; }
.notification-admin-fields textarea { width: 100%; min-height: 110px; }
.notification-audience-summary { display: grid; align-content: center; gap: 4px; padding: 10px 13px; border: 1px solid rgba(124, 163, 255, .14); border-radius: 10px; background: rgba(124, 163, 255, .035); }
.notification-audience-summary small { color: var(--muted); font-size: .66rem; }
.notification-audience-summary strong { font-size: .74rem; }
.notification-ack-option { display: flex !important; align-items: center; gap: 10px !important; padding: 12px 13px; border: 1px solid rgba(124, 163, 255, .14); border-radius: 10px; background: rgba(124, 163, 255, .035); }
.notification-ack-option input { flex: 0 0 auto; width: auto; }
.notification-ack-option span { display: grid; gap: 3px; }
.notification-ack-option strong { font-size: .73rem; }
.notification-ack-option small { color: var(--muted); font-size: .65rem; }
.notification-admin-preview { display: grid; gap: 6px; padding: 15px; border: 1px solid rgba(124, 163, 255, .16); border-radius: 11px; background: rgba(124, 163, 255, .04); box-shadow: inset 3px 0 0 #7996ec; }
.notification-admin-preview.is-important { box-shadow: inset 3px 0 0 var(--brand); }
.notification-admin-preview.is-warning { box-shadow: inset 3px 0 0 #ff9a5f; }
.notification-admin-preview small { color: var(--muted); font-size: .65rem; }
.notification-admin-preview strong { font-size: .82rem; }
.notification-admin-preview-html { color: var(--muted); font-size: .73rem; line-height: 1.5; }
.notification-admin-preview-html :deep(p) { margin: 0 0 8px; }
.notification-admin-preview-html :deep(p:last-child) { margin-bottom: 0; }
.notification-admin-history { display: grid; gap: 5px; }
.notification-admin-history-item { min-width: 0; border: 1px solid rgba(124, 163, 255, .15); border-radius: 8px; background: rgba(7, 13, 32, .34); }
.notification-history-row { display: grid; grid-template-columns: 115px 88px minmax(100px, 1fr) minmax(90px, .6fr) auto 14px; align-items: center; gap: 12px; min-height: 48px; padding: 8px 12px; cursor: pointer; list-style: none; }
.notification-history-row::-webkit-details-marker { display: none; }
.notification-history-row:hover { background: rgba(124, 163, 255, .055); }
.notification-history-row:focus-visible { outline: 2px solid var(--brand); outline-offset: 2px; border-radius: 8px; }
.notification-history-row time { color: var(--muted); font-size: .63rem; }
.notification-history-severity { color: var(--brand); font-size: .6rem; text-transform: uppercase; }
.notification-history-severity.is-warning { color: #ffb078; }
.notification-history-title, .notification-history-audience { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notification-history-title { font-size: .75rem; }
.notification-history-audience { color: #91aaf4; font-size: .64rem; }
.notification-history-stats { display: flex; gap: 14px; }
.notification-history-stats > span { display: grid; gap: 2px; text-align: center; }
.notification-history-stats small { color: var(--muted); font-size: .59rem; }
.notification-history-stats strong { font-size: .75rem; }
.notification-history-chevron { color: var(--brand); text-align: center; }
.notification-admin-history-item[open] .notification-history-chevron { transform: rotate(180deg); }
.notification-history-detail { padding: 16px; border-top: 1px solid rgba(124, 163, 255, .15); overflow-wrap: anywhere; }
.notification-history-detail h5 { margin: 0 0 6px; font-size: .85rem; }
.notification-history-detail > small { color: #91aaf4; }
.notification-history-body { margin-top: 12px; font-size: .78rem; line-height: 1.6; }
.notification-history-body :deep(p) { margin: 0 0 8px; }
.notification-history-body :deep(a) { color: #91aaf4; }
.notification-history-pagination, .notification-history-pagination > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.notification-history-pagination { margin-top: 12px; color: var(--muted); font-size: .7rem; flex-wrap: wrap; }
.notification-history-pagination button { min-height: 36px; padding: 6px 10px; border: 1px solid rgba(124, 163, 255, .2); border-radius: 7px; background: rgba(124, 163, 255, .06); color: var(--brand); cursor: pointer; }
.notification-history-pagination button:disabled { opacity: .4; cursor: default; }
@media (max-width: 1100px) {
  .notification-history-row { grid-template-columns: 105px minmax(0, 1fr) auto 14px; gap: 8px; }
  .notification-history-severity, .notification-history-audience { display: none; }
}
@media (max-width: 760px) {
  .notification-admin-fields { grid-template-columns: 1fr; }
  .notification-admin-wide { grid-column: 1; }
  .notification-history-row { grid-template-columns: minmax(0, 1fr) 14px; gap: 5px 8px; }
  .notification-history-row time { grid-column: 1; grid-row: 2; white-space: nowrap; }
  .notification-history-title { grid-column: 1; grid-row: 1; }
  .notification-history-stats { grid-column: 1; grid-row: 3; justify-content: space-between; gap: 6px; padding-top: 3px; }
  .notification-history-stats small { font-size: .53rem; }
  .notification-history-chevron { grid-column: 2; grid-row: 1 / 4; }
}
</style>
