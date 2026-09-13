<template>
  <section class="section-wrap players-page catalog-page">
    <PublicCatalogHeader title="Игроки" description="Футболисты лиги, статистика и история выступлений." :count="!loading && !errorText ? totalElements : null" count-label="игроков" />

    <div class="card" v-if="loading">
      <p class="muted">Загружаем список игроков...</p>
    </div>

    <UiState
      v-else-if="errorText"
      tone="error"
      title="Не удалось загрузить игроков"
      :message="errorText"
      action-label="Повторить"
      @action="loadPlayers"
    />

    <div class="catalog-toolbar" v-else>
      <label class="catalog-search"><span>Поиск игрока</span>
      <input
        v-model.trim="search"
        type="text"
        placeholder="Поиск по фамилии"
        aria-label="Поиск по фамилии"
      />
      </label>
      <button class="btn-ghost" type="button" :disabled="!search.trim()" @click="resetSearch">
        Сбросить поиск
      </button>
    </div>

    <div class="catalog-results-meta" v-if="!loading && !errorText && totalElements > 0">
      <span class="muted">Список игроков</span>
      <span class="muted">Страница {{ pageNum + 1 }} из {{ totalPages }}</span>
    </div>

    <div class="catalog-surface" v-if="!loading && !errorText && players.length">
      <div class="players-table-head"><span>№</span><span>Игрок</span><span>Дата рождения · возраст</span><span /></div>
      <div class="players-list">
        <article class="players-row" v-for="(player, index) in players" :key="player.id">
          <span class="players-number">{{ String(pageNum * pageSize + index + 1).padStart(2, '0') }}</span>
          <button class="players-name-btn" type="button" @click="openPlayerPage(player)">
            <span>{{ player.name }}</span><span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span>
          </button>
          <span class="players-birth-meta">{{ player.birthDate ? formatBirthDateWithAge(player.birthDate) : '—' }}</span>
          <span class="catalog-row-link" aria-hidden="true">↗</span>
        </article>
      </div>
    </div>

    <div class="catalog-pagination" v-if="!loading && !errorText && totalPages > 1">
      <button class="btn-ghost" type="button" :disabled="pageNum === 0" @click="goToPreviousPage">
        Назад
      </button>
      <span>Страница {{ pageNum + 1 }} из {{ totalPages }}</span>
      <button class="btn-ghost" type="button" :disabled="isLastPage" @click="goToNextPage">
        Вперед
      </button>
    </div>

    <UiState
      v-if="!loading && !errorText && players.length === 0 && search.trim()"
      title="Игроки не найдены"
      message="Уточните фамилию или сбросьте поиск."
      action-label="Сбросить поиск"
      @action="resetSearch"
    />
    <UiState
      v-else-if="!loading && !errorText && players.length === 0"
      title="Игроков пока нет"
      message="Игроки появятся здесь после регистрации в лиге."
    />

    <div v-if="showPlayerModal" class="profile-backdrop" @click.self="closePlayerModal" @keydown.esc.stop="closePlayerModal" @keydown.tab="trapProfileFocus">
      <article ref="profileDialog" class="player-profile" role="dialog" aria-modal="true" aria-labelledby="player-profile-title" tabindex="-1">
        <header class="profile-heading">
          <div class="profile-heading-copy"><span class="profile-kicker">Профиль игрока</span><button v-if="returnToHall" class="profile-return" type="button" @click="closePlayerModal">← Назад в Зал славы</button></div>
          <button ref="profileClose" class="profile-close" type="button" aria-label="Закрыть карточку игрока" @click="closePlayerModal">×</button>
        </header>

        <div class="profile-identity">
          <div class="profile-photo">
            <img v-if="playerDetails?.photoDataUrl" :src="playerDetails.photoDataUrl" :alt="modalTitle" />
            <template v-else><strong aria-hidden="true">{{ playerInitials }}</strong><span>Фото не добавлено</span></template>
          </div>
          <div class="profile-bio">
            <h2 id="player-profile-title">{{ modalTitle }}</h2>
            <FavoriteButton v-if="playerDetails?.id" type="PLAYER" :target-id="playerDetails.id" />
            <span v-if="playerDetails?.isGoalkeeper" class="profile-position">Вратарь</span>
            <dl v-if="playerDetails" class="profile-facts">
              <div><dt>Команда</dt><dd>{{ playerDetails.currentTeamName || 'Без команды' }}</dd></div>
              <div><dt>Дата рождения</dt><dd>{{ playerDetails.birthDate ? formatBirthDateWithAge(playerDetails.birthDate) : 'Не указана' }}</dd></div>
              <div><dt>Город</dt><dd>{{ playerDetails.residence || 'Не указан' }}</dd></div>
            </dl>
          </div>
        </div>

        <UiState v-if="modalLoading" tone="loading" title="Загружаем карточку игрока" />
        <UiState v-else-if="modalErrorText" tone="error" title="Не удалось загрузить игрока" :message="modalErrorText" action-label="Повторить" @action="reloadPlayerModal" />

        <template v-else-if="playerDetails">
          <section class="profile-section" aria-labelledby="profile-stats-title">
            <h3 id="profile-stats-title">Статистика</h3>
            <dl class="profile-stats">
              <div><dt>Голы</dt><dd>{{ playerDetails.goals ?? 0 }}</dd></div>
              <div><dt><i class="profile-card-mark is-yellow" aria-hidden="true" />Жёлтые карточки</dt><dd>{{ playerDetails.yellowCards ?? 0 }}</dd></div>
              <div><dt><i class="profile-card-mark is-red" aria-hidden="true" />Красные карточки</dt><dd>{{ playerDetails.redCards ?? 0 }}</dd></div>
            </dl>
          </section>
          <section class="profile-section profile-history">
            <button class="profile-history-toggle" type="button" :aria-expanded="historyExpanded" aria-controls="profile-team-history" @click="historyExpanded = !historyExpanded">
              <span>История команд <small>{{ playerHistory.length }}</small></span>
              <span aria-hidden="true">{{ historyExpanded ? '−' : '+' }}</span>
            </button>
            <div v-if="historyExpanded" id="profile-team-history">
              <div v-for="entry in playerHistory" :key="historyKey(entry)" class="profile-history-row">
                <div><strong>{{ entry.teamName }}</strong><span class="profile-history-period">{{ formatDate(entry.validFrom) }} — {{ formatHistoryEndDate(entry.validTo, entry.active) }}</span></div>
                <span class="profile-history-status" :class="{ 'is-active': entry.active }">{{ entry.active ? 'Текущая' : 'Архив' }}</span>
              </div>
              <p v-if="!playerHistory.length" class="profile-empty">История выступлений пока не заполнена.</p>
            </div>
          </section>
        </template>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiState from '../components/UiState.vue'
import PublicCatalogHeader from '../components/PublicCatalogHeader.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'
import { useDebounce } from '../composables/useDebounce'

const { optionalAuthApiRequest, isAuthenticated, loadCurrentUser } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
const route = useRoute()
const router = useRouter()

function openPlayerPage(player) {
  router.push({ name: 'player-profile', params: { playerId: player.id }, query: route.query })
}

const search = ref('')
const debouncedSearch = useDebounce(search, 1000)
const players = ref([])
const loading = ref(false)
const errorText = ref('')
const pageNum = ref(0)
const pageSize = 20
const totalPages = ref(0)
const totalElements = ref(0)
const isLastPage = ref(true)

const showPlayerModal = ref(false)
const selectedPlayerId = ref(null)
const selectedPlayerName = ref('')
const modalLoading = ref(false)
const modalErrorText = ref('')
const playerDetails = ref(null)
const playerHistory = ref([])
const historyExpanded = ref(false)

const modalTitle = computed(() => playerDetails.value?.fullName || selectedPlayerName.value || 'Игрок')
const returnToHall = computed(() => route.query.from === 'hall-of-fame')
const playerInitials = computed(() => modalTitle.value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase())
const profileDialog = ref(null)
const profileClose = ref(null)
let profileTrigger = null
let previousBodyOverflow = ''

watch(showPlayerModal, async (open) => {
  if (open) {
    previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    await nextTick()
    profileClose.value?.focus()
  } else {
    document.body.style.overflow = previousBodyOverflow
    profileTrigger?.focus()
  }
})

onBeforeUnmount(() => {
  if (showPlayerModal.value) document.body.style.overflow = previousBodyOverflow
})

function trapProfileFocus(event) {
  const controls = [...profileDialog.value.querySelectorAll('button:not(:disabled), a[href], [tabindex="0"]')]
  const first = controls[0]
  const last = controls[controls.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}

async function loadPlayers(nameQuery = '', requestedPage = 0) {
  loading.value = true
  errorText.value = ''

  try {
    const query = (nameQuery || '').trim()
    const params = new URLSearchParams({
      pagenum: String(requestedPage),
      pagesize: String(pageSize),
    })
    if (query) {
      params.set('name', query)
    }

    const payload = await catalogApi.getPlayers(params)
    const content = Array.isArray(payload?.content) ? payload.content : []

    players.value = content.map((item) => ({
      id: item.id,
      name: item.fullName,
      birthDate: item.birthDate || '',
      isGoalkeeper: Boolean(item.isGoalkeeper),
    }))
    pageNum.value = Number.isInteger(payload?.number) ? payload.number : requestedPage
    totalPages.value = Number.isInteger(payload?.totalPages) ? payload.totalPages : 0
    totalElements.value = Number.isInteger(payload?.totalElements) ? payload.totalElements : 0
    isLastPage.value = Boolean(payload?.last)
  } catch (error) {
    errorText.value = error.message || 'Не удалось загрузить игроков.'
    players.value = []
    totalPages.value = 0
    totalElements.value = 0
    isLastPage.value = true
  } finally {
    loading.value = false
  }
}

watch(debouncedSearch, (val) => {
  pageNum.value = 0
  loadPlayers(val.trim(), 0)
})

function resetSearch() {
  search.value = ''
}

function goToPreviousPage() {
  if (pageNum.value === 0) return
  loadPlayers(search.value, pageNum.value - 1)
}

function goToNextPage() {
  if (isLastPage.value) return
  loadPlayers(search.value, pageNum.value + 1)
}

async function fetchPlayerModalData(playerId) {
  modalLoading.value = true
  modalErrorText.value = ''

  try {
    const [detailsPayload, historyPayload] = await Promise.all([
      catalogApi.getPlayer(playerId),
      catalogApi.getPlayerHistory(playerId),
    ])

    playerDetails.value = detailsPayload || null
    playerHistory.value = Array.isArray(historyPayload?.history) ? historyPayload.history : []
  } catch (error) {
    modalErrorText.value = error.message || 'Не удалось загрузить карточку игрока.'
    playerDetails.value = null
    playerHistory.value = []
  } finally {
    modalLoading.value = false
  }
}

function openPlayerModal(player, trigger) {
  profileTrigger = trigger || document.activeElement
  selectedPlayerId.value = player.id
  selectedPlayerName.value = player.name || ''
  showPlayerModal.value = true
  playerDetails.value = null
  playerHistory.value = []
  historyExpanded.value = false
  fetchPlayerModalData(player.id)
}

function closePlayerModal() {
  showPlayerModal.value = false
  selectedPlayerId.value = null
  selectedPlayerName.value = ''
  playerDetails.value = null
  playerHistory.value = []
  historyExpanded.value = false
  modalErrorText.value = ''
  modalLoading.value = false
  if (route.name === 'player-profile') router.push(returnToHall.value ? { name: 'hall-of-fame' } : { name: 'players' })
}

function reloadPlayerModal() {
  if (!selectedPlayerId.value) return
  fetchPlayerModalData(selectedPlayerId.value)
}

function formatDate(value) {
  if (!value) return 'Не указана'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Не указана'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatHistoryEndDate(value, isActive) {
  if (isActive && !value) {
    return 'по настоящее время'
  }
  return formatDate(value)
}

function formatBirthDateWithAge(value) {
  if (!value) return ''

  const birthDate = new Date(value)
  if (Number.isNaN(birthDate.getTime())) return ''

  const now = new Date()
  let age = now.getFullYear() - birthDate.getFullYear()
  const monthDiff = now.getMonth() - birthDate.getMonth()
  const dayDiff = now.getDate() - birthDate.getDate()

  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age -= 1
  }

  return `${formatDate(value)} (${age} ${formatYears(age)})`
}

function formatYears(value) {
  const remainder100 = Math.abs(value) % 100
  const remainder10 = remainder100 % 10

  if (remainder100 >= 11 && remainder100 <= 14) {
    return 'лет'
  }
  if (remainder10 === 1) {
    return 'год'
  }
  if (remainder10 >= 2 && remainder10 <= 4) {
    return 'года'
  }
  return 'лет'
}

function historyKey(entry) {
  return `${entry.teamId}-${entry.validFrom || 'none'}-${entry.validTo || 'active'}`
}

onMounted(async () => {
  if (!isAuthenticated.value) {
    try {
      await loadCurrentUser()
    } catch {
      errorText.value = 'Нужно войти в систему (или как гость), чтобы увидеть список игроков.'
      return
    }
  }

  await loadPlayers()
  if (route.params.playerId) {
    openPlayerModal({ id: Number(route.params.playerId), name: '' }, null)
  }
})
</script>

<style scoped>
.players-table-head, .players-row { display: grid; grid-template-columns: 36px minmax(0, 1fr) 210px 16px; align-items: center; gap: 14px; padding: 0 20px; }
.players-table-head { min-height: 38px; color: var(--muted); font-size: .62rem; border-bottom: 1px solid rgba(124, 163, 255, .15); background: rgba(124, 163, 255, .035); }
.players-row { min-height: 58px; border-bottom: 1px solid rgba(124, 163, 255, .1); }
.players-row:last-child { border-bottom: 0; }
.players-row:hover { background: rgba(97, 232, 162, .035); }
.players-number { color: #7890bd; font-size: .65rem; font-variant-numeric: tabular-nums; }
.players-name-btn { min-height: 44px; min-width: 0; border: 0; background: transparent; color: var(--text); font-size: .8rem; font-weight: 700; padding: 8px 0; cursor: pointer; text-align: left; overflow-wrap: anywhere; }
.players-name-btn:hover { color: var(--brand); }
.profile-heading-copy { display: grid; gap: 5px; }
.profile-return { width: max-content; padding: 0; border: 0; background: transparent; color: #8da7df; font: inherit; font-size: .65rem; cursor: pointer; }
.profile-return:hover { color: var(--brand); }
.players-birth-meta { color: var(--muted); font-size: .7rem; }
.goalkeeper-icon { margin-left: 6px; font-size: .9em; }
@media (max-width: 640px) {
  .players-table-head { display: none; }
  .players-row { grid-template-columns: 24px minmax(0, 1fr) 14px; gap: 0 8px; padding: 8px 12px; }
  .players-number { grid-column: 1; grid-row: 1 / 3; }
  .players-name-btn { grid-column: 2; grid-row: 1; }
  .players-birth-meta { grid-column: 2; grid-row: 2; padding-bottom: 4px; }
  .players-row > .catalog-row-link { grid-column: 3; grid-row: 1 / 3; }
}

.profile-backdrop { position: fixed; inset: 0; z-index: 200; display: grid; place-items: center; padding: 24px; background: rgba(3, 7, 20, .78); backdrop-filter: blur(5px); }
.player-profile { width: min(680px, 100%); max-height: calc(100dvh - 48px); overflow-y: auto; padding: 22px 26px 8px; border: 1px solid rgba(124, 163, 255, .24); border-radius: 16px; background: linear-gradient(135deg, #131f3d, #0b132b 55%); box-shadow: 0 28px 90px rgba(0, 0, 0, .5); }
.profile-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.profile-kicker { color: var(--brand); font-size: .6rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
.profile-close { display: grid; place-items: center; flex: 0 0 auto; width: 36px; height: 36px; padding: 0; border: 1px solid rgba(124, 163, 255, .2); border-radius: 8px; background: rgba(124, 163, 255, .05); color: var(--muted); font-size: 1.4rem; cursor: pointer; }
.profile-close:hover { color: var(--text); border-color: var(--brand); }
.profile-identity { display: grid; grid-template-columns: 116px minmax(0, 1fr); gap: 24px; margin-bottom: 24px; }
.profile-photo { display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 12px; width: 116px; height: 146px; overflow: hidden; border: 1px solid rgba(124, 163, 255, .17); border-radius: 10px; background: rgba(124, 163, 255, .045); }
.profile-photo img { width: 100%; height: 100%; object-fit: cover; }
.profile-photo strong { color: #8199c6; font-size: 2rem; font-weight: 600; letter-spacing: .04em; }
.profile-photo > span { color: #8190b0; font-size: .54rem; }
.profile-bio { min-width: 0; }
.profile-bio h2 { margin: 0 0 10px; color: var(--text); font-size: 1.25rem; font-weight: 700; line-height: 1.3; overflow-wrap: anywhere; }
.profile-position { display: inline-block; margin-bottom: 12px; color: var(--brand); font-size: .64rem; }
.profile-facts { display: grid; gap: 8px; margin: 0; }
.profile-facts > div { display: grid; grid-template-columns: 105px minmax(0, 1fr); gap: 10px; font-size: .7rem; line-height: 1.45; }
.profile-facts dt { color: #8c9abb; }
.profile-facts dd { margin: 0; color: var(--text); overflow-wrap: anywhere; }
.profile-section { border-top: 1px solid rgba(124, 163, 255, .16); padding: 18px 0; }
.profile-section h3 { margin: 0 0 14px; font-size: .78rem; color: var(--text); }
.profile-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); margin: 0; }
.profile-stats > div { padding: 2px 16px; border-left: 1px solid rgba(124, 163, 255, .15); }
.profile-stats > div:first-child { padding-left: 0; border-left: 0; }
.profile-stats dt { display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: .63rem; line-height: 1.4; }
.profile-stats dd { margin: 9px 0 0; color: var(--text); font-size: 1.4rem; font-weight: 700; font-variant-numeric: tabular-nums; }
.profile-card-mark { width: 6px; height: 9px; flex-shrink: 0; border-radius: 1px; }
.profile-card-mark.is-yellow { background: #dfb966; }
.profile-card-mark.is-red { background: #e47d86; }
.profile-history-toggle { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; min-height: 44px; padding: 0; border: 0; background: transparent; color: var(--text); font-size: .78rem; font-weight: 700; text-align: left; cursor: pointer; }
.profile-history-toggle small { margin-left: 8px; padding: 2px 6px; border-radius: 4px; background: rgba(124, 163, 255, .1); color: #8da7df; font-size: .6rem; }
.profile-history-toggle > span:last-child { color: var(--brand); font-size: 1rem; }
.profile-history-row { display: flex; align-items: start; justify-content: space-between; gap: 14px; padding: 14px 0; border-top: 1px solid rgba(124, 163, 255, .1); }
.profile-history-row > div { display: grid; gap: 5px; min-width: 0; }
.profile-history-row strong { font-size: .72rem; font-weight: 600; overflow-wrap: anywhere; }
.profile-history-period { color: var(--muted); font-size: .62rem; line-height: 1.5; }
.profile-history-status { color: var(--muted); font-size: .59rem; }
.profile-history-status.is-active { color: var(--brand); }
.profile-empty { color: var(--muted); font-size: .7rem; }
@media (max-width: 540px) {
  .profile-backdrop { padding: 10px; }
  .player-profile { max-height: calc(100dvh - 20px); padding: 14px 16px 4px; border-radius: 12px; }
  .profile-heading { margin-bottom: 16px; }
  .profile-close { width: 44px; height: 44px; }
  .profile-identity { grid-template-columns: 64px minmax(0, 1fr); gap: 14px; }
  .profile-photo { width: 64px; height: 82px; gap: 5px; }
  .profile-photo strong { font-size: 1.3rem; }
  .profile-photo > span { max-width: 55px; text-align: center; font-size: .48rem; }
  .profile-bio h2 { font-size: 1rem; }
  .profile-facts > div { grid-template-columns: 1fr; gap: 2px; font-size: .65rem; }
  .profile-stats > div { padding: 2px 8px; }
  .profile-stats dt { align-items: baseline; font-size: .59rem; }
  .profile-stats dd { font-size: 1.25rem; }
}
</style>
