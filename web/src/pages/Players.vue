<template>
  <section class="section-wrap players-page">
    <h2 class="section-title">Игроки</h2>

    <div class="card" v-if="loading">
      <p class="muted">Загружаем список игроков...</p>
    </div>

    <div class="card" v-else-if="errorText">
      <p class="error-text">{{ errorText }}</p>
      <div class="actions-row">
        <button class="btn-primary" type="button" @click="loadPlayers">Повторить</button>
      </div>
    </div>

    <div class="card player-filters" v-else>
      <input
        v-model.trim="search"
        type="text"
        placeholder="Поиск по фамилии"
        aria-label="Поиск по фамилии"
      />
      <button class="btn-ghost" type="button" :disabled="!search.trim()" @click="resetSearch">
        Сбросить поиск
      </button>
    </div>

    <div class="players-meta" v-if="!loading && !errorText && totalElements > 0">
      <span class="muted">Всего игроков: {{ totalElements }}</span>
      <span class="muted">Страница {{ pageNum + 1 }} из {{ totalPages }}</span>
    </div>

    <div class="card" v-if="!loading && !errorText && players.length">
      <div class="players-list">
        <article class="players-row" v-for="player in players" :key="player.id">
          <button class="players-name-btn" type="button" @click="openPlayerModal(player)">
            {{ player.name }}
          </button>
        </article>
      </div>
    </div>

    <div class="pagination-row" v-if="!loading && !errorText && totalPages > 1">
      <button class="btn-ghost" type="button" :disabled="pageNum === 0" @click="goToPreviousPage">
        Назад
      </button>
      <button class="btn-ghost" type="button" :disabled="isLastPage" @click="goToNextPage">
        Вперед
      </button>
    </div>

    <p class="empty-text" v-if="!loading && !errorText && players.length === 0 && search.trim()">
      Ничего не найдено. Уточните фамилию или нажмите «Сбросить поиск».
    </p>
    <p class="empty-text" v-else-if="!loading && !errorText && players.length === 0">
      Пока нет данных по игрокам.
    </p>

    <div v-if="showPlayerModal" class="modal-backdrop" @click.self="closePlayerModal">
      <article class="card player-modal">
        <div class="player-modal-header">
          <div>
            <h3>{{ modalTitle }}</h3>
            <p v-if="playerDetails?.currentTeamName" class="muted player-team-copy">
              Текущая команда: {{ playerDetails.currentTeamName }}
            </p>
          </div>
          <button class="btn-ghost" type="button" @click="closePlayerModal">✕</button>
        </div>

        <div v-if="modalLoading" class="player-modal-state">
          <p class="muted">Загружаем карточку игрока...</p>
        </div>

        <div v-else-if="modalErrorText" class="player-modal-state">
          <p class="error-text">{{ modalErrorText }}</p>
          <div class="modal-actions">
            <button class="btn-primary" type="button" @click="reloadPlayerModal">Повторить</button>
            <button class="btn-ghost" type="button" @click="closePlayerModal">Закрыть</button>
          </div>
        </div>

        <template v-else-if="playerDetails">
          <div class="player-modal-content">
            <div class="player-avatar-wrap">
              <img v-if="playerDetails.photoDataUrl" :src="playerDetails.photoDataUrl" :alt="playerDetails.fullName" class="player-avatar" />
              <div v-else class="avatar-placeholder">Нет фото</div>
            </div>

            <div class="player-info-grid">
              <div class="info-section">
                <label>ФИО</label>
                <p>{{ playerDetails.fullName }}</p>
              </div>

              <div class="info-section">
                <label>Дата рождения</label>
                <p>{{ formatDate(playerDetails.birthDate) }}</p>
              </div>

              <div class="info-section">
                <label>Текущая команда</label>
                <p>{{ playerDetails.currentTeamName || 'Не указана' }}</p>
              </div>

              <div class="info-section">
                <label>Город проживания</label>
                <p>{{ playerDetails.residence || 'Не указан' }}</p>
              </div>
            </div>
          </div>

          <div class="player-stats-grid">
            <div class="stat-item">
              <span class="stat-label">Голы</span>
              <span class="stat-value">{{ playerDetails.goals }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">ЖК</span>
              <span class="stat-value yellow">{{ playerDetails.yellowCards }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">КК</span>
              <span class="stat-value red">{{ playerDetails.redCards }}</span>
            </div>
          </div>

          <div class="history-section">
            <div class="section-head history-head">
              <h4>История команд</h4>
            </div>

            <div v-if="playerHistory.length" class="teams-history">
              <div class="team-season" v-for="entry in playerHistory" :key="historyKey(entry)">
                <div class="history-line">
                  <strong>{{ entry.teamName }}</strong>
                  <span class="history-status" :class="{ active: entry.active }">{{ entry.active ? 'Текущая' : 'Архив' }}</span>
                </div>
                <div class="history-period">
                  <span>{{ formatDate(entry.validFrom) }}</span>
                  <span>—</span>
                  <span>{{ formatDate(entry.validTo) }}</span>
                </div>
              </div>
            </div>
            <p v-else class="empty-text">История переходов пока не заполнена.</p>
          </div>

          <div class="modal-actions">
            <button class="btn-ghost" type="button" @click="closePlayerModal">Закрыть</button>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'
import { useDebounce } from '../composables/useDebounce'

const { optionalAuthApiRequest, isAuthenticated, loadCurrentUser } = useAuth()

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

const modalTitle = computed(() => playerDetails.value?.fullName || selectedPlayerName.value || 'Игрок')

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

    const payload = await optionalAuthApiRequest(`/api/players?${params.toString()}`)
    const content = Array.isArray(payload?.content) ? payload.content : []

    players.value = content.map((item) => ({
      id: item.id,
      name: item.fullName,
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
      optionalAuthApiRequest(`/api/players/${encodeURIComponent(playerId)}`),
      optionalAuthApiRequest(`/api/players/${encodeURIComponent(playerId)}/history`),
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

function openPlayerModal(player) {
  selectedPlayerId.value = player.id
  selectedPlayerName.value = player.name || ''
  showPlayerModal.value = true
  playerDetails.value = null
  playerHistory.value = []
  fetchPlayerModalData(player.id)
}

function closePlayerModal() {
  showPlayerModal.value = false
  selectedPlayerId.value = null
  selectedPlayerName.value = ''
  playerDetails.value = null
  playerHistory.value = []
  modalErrorText.value = ''
  modalLoading.value = false
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
})
</script>

<style scoped>
.players-page {
  display: grid;
  gap: 18px;
}

.player-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.player-filters input {
  min-width: min(420px, 100%);
  flex: 1 1 320px;
}

.players-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.players-list {
  display: grid;
  gap: 8px;
}

.players-row {
  border-bottom: 1px solid var(--line);
  padding-bottom: 8px;
}

.players-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.players-name-btn {
  border: none;
  background: transparent;
  color: var(--text);
  font: inherit;
  padding: 0;
  cursor: pointer;
  text-align: left;
}

.players-name-btn:hover {
  color: var(--brand);
}

.pagination-row {
  display: flex;
  gap: 12px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(8, 12, 20, 0.72);
  display: grid;
  place-items: center;
  padding: 28px;
  z-index: 40;
}

.player-modal {
  width: min(980px, calc(100vw - 32px));
  max-height: min(88vh, 920px);
  overflow: auto;
  display: grid;
  gap: 22px;
  padding: 26px;
}

.player-modal-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.player-modal-header h3 {
  margin: 0;
  font-size: 1.6rem;
}

.player-team-copy {
  margin: 8px 0 0;
}

.player-modal-state {
  display: grid;
  gap: 16px;
}

.player-modal-content {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.player-avatar-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-avatar {
  width: 220px;
  height: 260px;
  border-radius: 20px;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
}

.avatar-placeholder {
  width: 220px;
  height: 260px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px dashed var(--line);
  color: var(--muted);
}

.player-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.info-section {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--line);
}

.info-section label {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 0.84rem;
}

.info-section p {
  margin: 0;
  font-weight: 600;
}

.player-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat-item {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
  display: grid;
  gap: 8px;
}

.stat-label {
  color: var(--muted);
  font-size: 0.84rem;
}

.stat-value {
  font-size: 2rem;
  font-weight: 800;
  line-height: 1;
}

.stat-value.yellow {
  color: #f5c44b;
}

.stat-value.red {
  color: #ef6461;
}

.history-section {
  display: grid;
  gap: 14px;
}

.history-head {
  margin-bottom: 0;
}

.history-head h4 {
  margin: 0;
  font-size: 1.05rem;
}

.teams-history {
  display: grid;
  gap: 10px;
}

.team-season {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  display: grid;
  gap: 8px;
}

.history-line,
.history-period {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.history-status {
  color: var(--muted);
}

.history-status.active {
  color: var(--brand);
}

.history-period {
  color: var(--muted);
  font-size: 0.92rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 860px) {
  .player-modal {
    width: min(100vw - 20px, 760px);
    padding: 20px;
  }

  .player-modal-content {
    grid-template-columns: 1fr;
  }

  .player-avatar-wrap {
    justify-content: start;
  }

  .player-info-grid,
  .player-stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .modal-backdrop {
    padding: 10px;
  }

  .player-modal {
    width: calc(100vw - 20px);
    max-height: 92vh;
  }

  .player-modal-header,
  .players-meta,
  .modal-actions {
    align-items: start;
    flex-direction: column;
  }

  .modal-actions {
    width: 100%;
  }
}
</style>
