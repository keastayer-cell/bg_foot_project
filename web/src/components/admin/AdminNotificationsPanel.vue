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
        <article v-for="item in history.items" :key="item.id" class="notification-admin-history-item">
          <div class="notification-history-main">
            <span class="notification-history-meta"><b>{{ severityLabel(item.severity) }}</b><time>{{ formatDateTime(item.createdAt) }}</time></span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary || plainText(item.body) }}</p>
            <small>{{ item.audienceValue }}</small>
          </div>
          <div class="notification-history-stats">
            <span><small>Получили</small><strong>{{ item.recipientCount }}</strong></span>
            <span><small>Прочитали</small><strong>{{ item.readCount }}</strong></span>
            <span><small>Ознакомились</small><strong>{{ item.acknowledgedCount }}</strong></span>
          </div>
        </article>
      </div>
      <div v-else class="admin-record-placeholder"><span>↳</span><span>Оповещения ещё не создавались.</span></div>
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
const history = reactive({ items: [], totalElements: 0 })
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

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    const payload = await notificationsApi.adminList(0, 50)
    history.items = Array.isArray(payload?.items) ? payload.items : []
    history.totalElements = Number(payload?.totalElements || 0)
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

function plainText(value) {
  const element = document.createElement('div')
  element.innerHTML = value || ''
  return element.textContent || ''
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
.notification-admin-history { display: grid; gap: 9px; }
.notification-admin-history-item { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(260px, .7fr); gap: 16px; padding: 15px; border: 1px solid rgba(124, 163, 255, .15); border-radius: 11px; background: rgba(7, 13, 32, .34); }
.notification-history-main { display: grid; gap: 6px; min-width: 0; }
.notification-history-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: .63rem; }
.notification-history-meta b { color: var(--brand); text-transform: uppercase; }
.notification-history-main > strong { font-size: .78rem; }
.notification-history-main p { margin: 0; overflow: hidden; color: var(--muted); font-size: .7rem; text-overflow: ellipsis; white-space: nowrap; }
.notification-history-main > small { color: #91aaf4; font-size: .64rem; }
.notification-history-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); overflow: hidden; border: 1px solid rgba(124, 163, 255, .13); border-radius: 9px; }
.notification-history-stats > span { display: grid; align-content: center; gap: 3px; padding: 9px; border-right: 1px solid rgba(124, 163, 255, .12); text-align: center; }
.notification-history-stats > span:last-child { border-right: 0; }
.notification-history-stats small { color: var(--muted); font-size: .59rem; }
.notification-history-stats strong { font-size: .8rem; }
@media (max-width: 760px) {
  .notification-admin-fields, .notification-admin-history-item { grid-template-columns: 1fr; }
  .notification-admin-wide { grid-column: 1; }
}
</style>
