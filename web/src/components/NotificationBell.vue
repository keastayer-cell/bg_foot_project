<template>
  <div ref="root" class="notification-center">
    <button
      class="notification-bell"
      :class="{ 'has-unread': badgeCount > 0, 'is-open': open }"
      type="button"
      :aria-label="bellLabel"
      :aria-expanded="open"
      @click="toggle"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
      </svg>
      <span v-if="badgeCount" class="notification-bell-count">{{ badgeCount > 99 ? '99+' : badgeCount }}</span>
    </button>

    <div v-if="open" class="notification-scrim" aria-hidden="true" @click="open = false" />

    <section v-if="open" class="notification-popover" aria-label="Уведомления">
      <header class="notification-popover-head">
        <button v-if="selectedItem" class="notification-back" type="button" @click="selectedItem = null">← Назад</button>
        <div v-else>
          <h3>Уведомления</h3>
          <small>{{ counterLabel }}</small>
        </div>
        <button v-if="!selectedItem && isAuthenticated && unreadCount" type="button" @click="markAllRead">Прочитать все</button>
        <button v-else-if="selectedItem" class="notification-close" type="button" @click="open = false">Закрыть</button>
      </header>

      <template v-if="selectedItem">
        <article class="notification-reader">
          <div class="notification-reader__meta">
            <span class="notification-reader__kind" :class="`is-${String(selectedItem.severity || 'INFO').toLowerCase()}`">
              <span class="notification-reader__kind-dot" aria-hidden="true" />
              {{ severityLabel(selectedItem.severity) }}
            </span>
            <span class="notification-reader__date">
              <small>Создано</small>
              <time>{{ formatDateTime(selectedItem.createdAt, true) }}</time>
            </span>
          </div>

          <div class="notification-reader__content">
            <span class="notification-reader__eyebrow">Сообщение</span>
            <h2>{{ selectedItem.title }}</h2>
            <div class="notification-reader__body">
              <div class="notification-reader__html" v-html="sanitizedSelectedBody" />
            </div>
          </div>

          <footer class="notification-reader__footer">
            <button
              v-if="selectedItem.requiresAcknowledgement && !selectedItem.acknowledgedAt"
              class="notification-acknowledge"
              type="button"
              @click="acknowledge(selectedItem)"
            >
              Ознакомился
            </button>
            <span v-else-if="selectedItem.requiresAcknowledgement" class="notification-acknowledged">✓ Ознакомление подтверждено</span>
            <button v-if="selectedItem.actionUrl" class="notification-open-action" type="button" @click="openAction(selectedItem)">
              {{ notificationActionLabel(selectedItem) }} →
            </button>
            <span v-if="!selectedItem.requiresAcknowledgement && !selectedItem.actionUrl" class="notification-reader__status">Уведомление прочитано</span>
          </footer>
        </article>
      </template>

      <template v-else>
        <p v-if="loading && !items.length" class="notification-state">Загружаем уведомления…</p>
        <p v-else-if="error" class="notification-state is-error">{{ error }}</p>
        <p v-else-if="!items.length" class="notification-state">Уведомлений пока нет.</p>

        <div v-else class="notification-list">
          <article
            v-for="item in items"
            :key="item.id"
            class="notification-item"
            :class="[`is-${String(item.severity || 'INFO').toLowerCase()}`, { 'is-unread': !item.readAt }]"
          >
            <button class="notification-item-main" type="button" @click="openNotification(item)">
              <span class="notification-item-meta">
                <b>{{ severityLabel(item.severity) }}</b>
                <time>{{ formatDateTime(item.createdAt) }}</time>
              </span>
              <span class="notification-item-copy">
                <strong>{{ item.title }}</strong>
                <span>{{ item.summary || plainText(item.body) }}</span>
              </span>
              <span class="notification-view">Просмотр →</span>
            </button>
          </article>
        </div>

        <footer v-if="totalElements" class="notification-popover-footer">
          <span>{{ pageStart }}–{{ pageEnd }} из {{ totalElements }}</span>
          <div class="notification-pagination">
            <button type="button" :disabled="pageNumber === 0 || loading" @click="changePage(pageNumber - 1)">←</button>
            <span>{{ pageNumber + 1 }} / {{ Math.max(totalPages, 1) }}</span>
            <button type="button" :disabled="pageNumber + 1 >= totalPages || loading" @click="changePage(pageNumber + 1)">→</button>
          </div>
          <button type="button" :disabled="loading" @click="reloadNotifications">Обновить</button>
        </footer>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { createNotificationsApi } from '../api/notifications'
import { useAuth } from '../store/auth'

const PAGE_SIZE = 10
const NOTIFICATION_REFRESH_INTERVAL_MS = 5 * 60 * 1000
const PUBLIC_READ_STORAGE_KEY = 'football_stats_public_notifications_read_v1'
const { authorizedApiRequest, optionalAuthApiRequest, isAuthenticated } = useAuth()
const notificationsApi = createNotificationsApi(authorizedApiRequest)
const publicNotificationsApi = createNotificationsApi(optionalAuthApiRequest)
const router = useRouter()
const root = ref(null)
const open = ref(false)
const loading = ref(false)
const unreadCount = ref(0)
const publicCount = ref(0)
const items = ref([])
const error = ref('')
const selectedItem = ref(null)
const pageNumber = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const publicReadState = ref(loadPublicReadState())
let pollTimer = null
let badgeRequest = null
let lastBadgeRefreshAt = 0
let publicFirstPageCache = null

const badgeCount = computed(() => isAuthenticated.value ? unreadCount.value : publicCount.value)
const pageStart = computed(() => totalElements.value ? pageNumber.value * PAGE_SIZE + 1 : 0)
const pageEnd = computed(() => Math.min((pageNumber.value + 1) * PAGE_SIZE, totalElements.value))
const sanitizedSelectedBody = computed(() => sanitizeHtml(selectedItem.value?.body || ''))
const bellLabel = computed(() => {
  if (isAuthenticated.value && unreadCount.value) return `Уведомления: непрочитанных ${unreadCount.value}`
  if (!isAuthenticated.value && publicCount.value) return `Публичные объявления: новых ${publicCount.value}`
  return 'Уведомления'
})
const counterLabel = computed(() => {
  if (isAuthenticated.value) return unreadCount.value ? `Новых: ${unreadCount.value}` : 'Нет новых'
  return publicCount.value ? `Новых: ${publicCount.value}` : 'Нет новых'
})

watch(isAuthenticated, () => {
  stopPolling()
  open.value = false
  items.value = []
  selectedItem.value = null
  pageNumber.value = 0
  totalElements.value = 0
  totalPages.value = 0
  unreadCount.value = 0
  publicCount.value = 0
  publicFirstPageCache = null
  lastBadgeRefreshAt = 0
  startPolling()
}, { immediate: true })

onMounted(() => {
  document.addEventListener('click', closeOnOutsideClick)
  window.addEventListener('focus', refreshBadgeCountIfStale)
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('click', closeOnOutsideClick)
  window.removeEventListener('focus', refreshBadgeCountIfStale)
})

async function toggle() {
  open.value = !open.value
  selectedItem.value = null
  if (open.value) {
    if (!isAuthenticated.value && badgeRequest) await badgeRequest
    await loadNotifications()
  }
}

async function loadUnreadCount() {
  if (!isAuthenticated.value) return
  try {
    const payload = await notificationsApi.unreadCount()
    unreadCount.value = Number(payload?.unreadCount || 0)
  } catch {
    // Фоновая проверка не должна мешать работе сайта.
  }
}

async function loadPublicCount() {
  try {
    const payload = await publicNotificationsApi.publicList(0, PAGE_SIZE)
    publicFirstPageCache = payload
    const publicItems = Array.isArray(payload?.items) ? payload.items : []
    publicCount.value = publicItems.filter((item) => !publicReadAt(item)).length
  } catch {
    // Публичная фоновая проверка не должна мешать работе сайта.
  }
}

function refreshBadgeCount() {
  if (badgeRequest) return badgeRequest
  badgeRequest = (isAuthenticated.value ? loadUnreadCount() : loadPublicCount())
    .finally(() => {
      lastBadgeRefreshAt = Date.now()
      badgeRequest = null
    })
  return badgeRequest
}

function refreshBadgeCountIfStale() {
  if (Date.now() - lastBadgeRefreshAt < NOTIFICATION_REFRESH_INTERVAL_MS) return
  void refreshBadgeCount()
}

async function loadNotifications(force = false) {
  if (loading.value) return
  if (!force && !isAuthenticated.value && pageNumber.value === 0 && publicFirstPageCache) {
    applyNotificationsPayload(publicFirstPageCache)
    return
  }
  loading.value = true
  error.value = ''
  try {
    const payload = isAuthenticated.value
      ? await notificationsApi.list(pageNumber.value, PAGE_SIZE)
      : await publicNotificationsApi.publicList(pageNumber.value, PAGE_SIZE)
    if (!isAuthenticated.value && pageNumber.value === 0) {
      publicFirstPageCache = payload
      lastBadgeRefreshAt = Date.now()
    }
    applyNotificationsPayload(payload)
  } catch (requestError) {
    error.value = requestError.message || 'Не удалось загрузить уведомления.'
  } finally {
    loading.value = false
  }
}

function applyNotificationsPayload(payload) {
  items.value = (Array.isArray(payload?.items) ? payload.items : []).map((item) => {
    const publicItem = !isAuthenticated.value
    return {
      ...item,
      id: item.id ?? `public-${item.notificationId}`,
      public: publicItem,
      requiresAcknowledgement: Boolean(item.requiresAcknowledgement),
      readAt: publicItem ? publicReadAt(item) : (item.readAt ?? null),
      acknowledgedAt: item.acknowledgedAt ?? null,
    }
  })
  totalElements.value = Number(payload?.totalElements || 0)
  totalPages.value = Number(payload?.totalPages || 0)
  if (isAuthenticated.value) unreadCount.value = Number(payload?.unreadCount || 0)
  else publicCount.value = items.value.filter((item) => !item.readAt).length
}

async function reloadNotifications() {
  await loadNotifications(true)
}

async function changePage(nextPage) {
  pageNumber.value = Math.max(0, nextPage)
  await loadNotifications()
}

async function openNotification(item) {
  selectedItem.value = item

  if (!isAuthenticated.value) {
    if (!item.readAt) {
      const readAt = new Date().toISOString()
      rememberPublicRead(item, readAt)
      const openedItem = { ...item, readAt }
      replaceItem(openedItem)
      selectedItem.value = openedItem
      publicCount.value = Math.max(0, publicCount.value - 1)
    }
    return
  }

  let openedItem = item
  if (!item.readAt) {
    try {
      openedItem = await notificationsApi.markRead(item.id)
      replaceItem(openedItem)
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (requestError) {
      error.value = requestError.message || 'Не удалось отметить уведомление прочитанным.'
    }
  }
  selectedItem.value = openedItem
}

async function acknowledge(item) {
  try {
    const wasUnread = !item.readAt
    const updated = await notificationsApi.acknowledge(item.id)
    replaceItem(updated)
    selectedItem.value = updated
    if (wasUnread) unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (requestError) {
    error.value = requestError.message || 'Не удалось подтвердить ознакомление.'
  }
}

async function openAction(item) {
  open.value = false
  await router.push(item.actionUrl)
}

function notificationActionLabel(item) {
  return String(item?.actionUrl || '').startsWith('/matches/') ? 'Открыть матч' : 'Перейти в связанный раздел'
}

async function markAllRead() {
  try {
    await notificationsApi.markAllRead()
    const now = new Date().toISOString()
    items.value = items.value.map((item) => item.readAt ? item : { ...item, readAt: now })
    unreadCount.value = 0
  } catch (requestError) {
    error.value = requestError.message || 'Не удалось отметить уведомления прочитанными.'
  }
}

function replaceItem(updated) {
  items.value = items.value.map((item) => item.id === updated.id ? updated : item)
}

function publicNotificationKey(item) {
  return String(item?.notificationId ?? item?.id ?? '')
}

function publicReadAt(item) {
  const key = publicNotificationKey(item)
  return key ? publicReadState.value[key] || null : null
}

function rememberPublicRead(item, readAt) {
  const key = publicNotificationKey(item)
  if (!key) return
  const nextState = { ...publicReadState.value, [key]: readAt }
  const entries = Object.entries(nextState)
    .sort((left, right) => String(right[1]).localeCompare(String(left[1])))
    .slice(0, 250)
  publicReadState.value = Object.fromEntries(entries)
  try {
    window.localStorage.setItem(PUBLIC_READ_STORAGE_KEY, JSON.stringify(publicReadState.value))
  } catch {
    // Просмотр уведомления должен работать даже при недоступном localStorage.
  }
}

function loadPublicReadState() {
  try {
    const value = JSON.parse(window.localStorage.getItem(PUBLIC_READ_STORAGE_KEY) || '{}')
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  } catch {
    return {}
  }
}

function startPolling() {
  stopPolling()
  void refreshBadgeCount()
  pollTimer = window.setInterval(refreshBadgeCount, NOTIFICATION_REFRESH_INTERVAL_MS)
}

function stopPolling() {
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = null
}

function closeOnOutsideClick(event) {
  if (!open.value || !root.value) return
  const eventPath = typeof event.composedPath === 'function' ? event.composedPath() : []
  const startedInside = eventPath.includes(root.value) || root.value.contains(event.target)
  if (!startedInside) open.value = false
}

function severityLabel(severity) {
  if (severity === 'IMPORTANT') return 'Важно'
  if (severity === 'WARNING') return 'Внимание'
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
  const elements = [...template.content.querySelectorAll('*')]
  for (const element of elements) {
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

function formatDateTime(value, withYear = false) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit', month: '2-digit', ...(withYear ? { year: 'numeric' } : {}), hour: '2-digit', minute: '2-digit',
  }).format(date)
}
</script>

<style scoped>
.notification-center { position: relative; z-index: 120; flex: 0 0 auto; }
.notification-bell { position: relative; z-index: 3; display: grid; width: 40px; height: 40px; padding: 0; place-items: center; border: 1px solid rgba(124, 163, 255, .2); border-radius: 10px; background: rgba(124, 163, 255, .07); color: var(--muted); cursor: pointer; }
.notification-bell:hover, .notification-bell.is-open, .notification-bell.has-unread { border-color: rgba(97, 232, 162, .36); color: var(--brand); background: rgba(97, 232, 162, .08); }
.notification-bell svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.notification-bell-count { position: absolute; top: -6px; right: -7px; display: grid; min-width: 19px; height: 19px; padding: 0 5px; place-items: center; border: 2px solid #0a1025; border-radius: 10px; background: #e84f6b; color: #fff; font-size: .58rem; font-weight: 900; }
.notification-scrim { position: fixed; z-index: 1; inset: 0; background: rgba(2, 6, 21, .38); backdrop-filter: blur(2px) saturate(.82); animation: notification-scrim-in .14s ease-out; }
.notification-popover { position: absolute; z-index: 2; top: calc(100% + 10px); right: 0; width: min(560px, calc(100vw - 20px)); overflow: hidden; border: 1px solid rgba(97, 232, 162, .34); border-radius: 14px; background: linear-gradient(155deg, #121d3e 0%, #0b132d 46%, #080f25 100%); box-shadow: 0 30px 85px rgba(0, 0, 0, .68), 0 0 0 1px rgba(124, 163, 255, .12), 0 0 36px rgba(97, 232, 162, .09); animation: notification-popover-in .16s ease-out; }
.notification-popover::before { position: absolute; z-index: 1; top: 0; right: 18px; left: 18px; height: 2px; border-radius: 0 0 3px 3px; background: linear-gradient(90deg, transparent, rgba(97, 232, 162, .92), transparent); content: ''; }
.notification-popover-head, .notification-popover-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 10px 12px; }
.notification-popover-head { min-height: 42px; border-bottom: 1px solid rgba(124, 163, 255, .2); background: rgba(20, 31, 66, .62); }
.notification-popover-head > div { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.notification-popover-head small { color: var(--muted); font-size: 10px; white-space: nowrap; }
.notification-popover-head h3 { margin: 0; font-size: 14px; }
.notification-popover-head button, .notification-popover-footer button { padding: 0; border: 0; background: transparent; color: var(--brand); font-size: 10px; font-weight: 700; cursor: pointer; }
.notification-popover-head button:disabled, .notification-popover-footer button:disabled { opacity: .35; cursor: default; }
.notification-list { display: grid; max-height: min(430px, calc(100vh - 150px)); overflow-y: auto; }
.notification-item { position: relative; border-bottom: 1px solid rgba(124, 163, 255, .14); background: rgba(17, 27, 58, .52); }
.notification-item:nth-child(even) { background: rgba(11, 19, 43, .58); }
.notification-item.is-unread { background: rgba(38, 82, 83, .24); box-shadow: inset 3px 0 0 var(--brand); }
.notification-item.is-warning { box-shadow: inset 3px 0 0 #ff9a5f; }
.notification-item-main { display: grid; grid-template-columns: 82px minmax(0, 1fr) auto; align-items: center; gap: 12px; width: 100%; min-width: 0; padding: 10px 12px; border: 0; background: transparent; color: var(--text); text-align: left; cursor: pointer; }
.notification-item-main:hover { background: rgba(124, 163, 255, .09); }
.notification-item-meta { display: grid; align-content: center; gap: 3px; color: var(--muted); font-size: 10px; }
.notification-item-meta b { color: var(--brand); text-transform: uppercase; }
.notification-item.is-warning .notification-item-meta b { color: #ffb078; }
.notification-item-copy { display: grid; grid-template-columns: minmax(120px, .7fr) minmax(0, 1fr); align-items: center; gap: 12px; min-width: 0; }
.notification-item-copy > strong, .notification-item-copy > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notification-item-copy > strong { font-size: 12px; }
.notification-item-copy > span { color: var(--muted); font-size: 11px; line-height: 1.35; }
.notification-view { color: #91aaf4; font-size: 10px; white-space: nowrap; }
.notification-state { margin: 0; padding: 22px 14px; color: var(--muted); font-size: 11px; text-align: center; }
.notification-state.is-error { color: #ff8da2; }
.notification-popover-footer { border-top: 1px solid rgba(124, 163, 255, .18); background: rgba(5, 10, 27, .46); color: var(--muted); font-size: 10px; }
.notification-pagination { display: flex; align-items: center; gap: 9px; }
.notification-pagination button { display: grid; width: 24px; height: 24px; place-items: center; border: 1px solid rgba(124, 163, 255, .18); border-radius: 6px; }
.notification-reader { display: block; max-height: min(540px, calc(100vh - 150px)); overflow-y: auto; background: linear-gradient(180deg, rgba(36, 51, 98, .18), rgba(8, 14, 35, .05)); }
.notification-reader__meta { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 52px; padding: 10px 18px; border-bottom: 1px solid rgba(124, 163, 255, .11); background: rgba(5, 10, 27, .28); }
.notification-reader__kind { display: inline-flex; align-items: center; gap: 7px; padding: 5px 9px; border: 1px solid rgba(97, 232, 162, .28); border-radius: 999px; background: rgba(97, 232, 162, .08); color: var(--brand); font-size: 10px; font-weight: 850; letter-spacing: .055em; line-height: 1; text-transform: uppercase; white-space: nowrap; }
.notification-reader__kind.is-warning { border-color: rgba(255, 154, 95, .34); background: rgba(255, 154, 95, .09); color: #ffb078; }
.notification-reader__kind-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 14%, transparent); }
.notification-reader__date { display: grid; grid-template-columns: auto auto; align-items: center; gap: 7px; color: #c4ccec; font-size: 10px; line-height: 1.2; white-space: nowrap; }
.notification-reader__date small { color: #7180aa; font-size: 9px; text-transform: uppercase; }
.notification-reader__content { display: grid; gap: 12px; padding: 20px 20px 18px; }
.notification-reader__eyebrow { color: #7180aa; font-size: 9px; font-weight: 800; letter-spacing: .14em; text-transform: uppercase; }
.notification-reader__content h2 { margin: 0; color: var(--text); font-size: clamp(17px, 2.2vw, 20px); line-height: 1.3; overflow-wrap: anywhere; }
.notification-reader__body { min-height: 74px; max-height: 280px; padding: 15px 16px; overflow-y: auto; border: 1px solid rgba(124, 163, 255, .14); border-radius: 9px; background: rgba(5, 10, 27, .32); box-shadow: inset 3px 0 0 rgba(97, 232, 162, .42); }
.notification-reader__html { color: #c5cdea; font-size: 12px; line-height: 1.65; overflow-wrap: anywhere; }
.notification-reader__html :deep(p) { margin: 0 0 10px; }
.notification-reader__html :deep(p:last-child) { margin-bottom: 0; }
.notification-reader__html :deep(ul), .notification-reader__html :deep(ol) { margin: 8px 0; padding-left: 20px; }
.notification-reader__html :deep(a) { color: var(--brand); font-weight: 700; }
.notification-reader__footer { display: flex; min-height: 54px; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 18px; border-top: 1px solid rgba(124, 163, 255, .12); background: rgba(5, 10, 27, .3); }
.notification-reader__status { color: #7f8bb3; font-size: 10px; }
.notification-acknowledge, .notification-open-action { padding: 7px 10px; border: 1px solid rgba(97, 232, 162, .32); border-radius: 7px; background: rgba(97, 232, 162, .1); color: var(--brand); font-size: 10px; font-weight: 800; cursor: pointer; }
.notification-open-action { margin-left: auto; border-color: rgba(124, 163, 255, .25); background: rgba(124, 163, 255, .08); color: #a9baff; }
.notification-acknowledged { color: var(--brand); font-size: 10px; }
@keyframes notification-scrim-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes notification-popover-in { from { opacity: 0; transform: translateY(-6px) scale(.985); } to { opacity: 1; transform: translateY(0) scale(1); } }
@media (max-width: 680px) {
  .notification-popover { position: fixed; top: 62px; right: 10px; left: 10px; width: auto; }
  .notification-item-main { grid-template-columns: 68px minmax(0, 1fr); gap: 9px; }
  .notification-item-copy { grid-template-columns: 1fr; gap: 3px; }
  .notification-view { display: none; }
  .notification-popover-footer > span:first-child { display: none; }
  .notification-reader__meta { padding: 9px 13px; }
  .notification-reader__date small { display: none; }
  .notification-reader__content { padding: 16px 13px; }
  .notification-reader__body { padding: 13px; }
  .notification-reader__footer { align-items: stretch; padding: 10px 13px; }
}
@media (prefers-reduced-motion: reduce) {
  .notification-scrim, .notification-popover { animation: none; }
}
</style>
