<template>
  <section class="section-wrap">
    <h2 class="section-title">⚽ Игроки</h2>

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
          <div>
            <button class="players-name-btn" type="button" @click="openPlayerModal(player)">
              {{ player.name }}
            </button>
          </div>
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

    <p class="empty-text" v-if="!loading && !errorText && players.length === 0 && search.trim()">Ничего не найдено. Уточните фамилию или нажмите «Сбросить поиск».</p>
    <p class="empty-text" v-else-if="!loading && !errorText && players.length === 0">Пока нет данных по игрокам.</p>

    <!-- Player Modal -->
    <div v-if="showPlayerModal && selectedPlayer" class="modal-backdrop" @click.self="closePlayerModal">
      <article class="card player-modal">
        <div class="player-modal-header">
          <h3>{{ selectedPlayer.name }}</h3>
          <button class="btn-ghost" type="button" @click="closePlayerModal">✕</button>
        </div>

        <div class="player-modal-content">
          <div class="player-avatar">
            <div class="avatar-placeholder">📷</div>
          </div>

          <div class="player-info">
            <div class="info-section">
              <label>ФИО:</label>
              <p>{{ selectedPlayer.name }}</p>
            </div>

            <div class="info-section">
              <label>Дата рождения:</label>
              <p>{{ selectedPlayer.birthDate || 'Не указана' }}</p>
            </div>

            <div class="info-section full-width">
              <label>История команд:</label>
              <div class="teams-history">
                <div class="team-season" v-for="(season, idx) in selectedPlayer.teamsHistory" :key="idx">
                  <span class="season-year">{{ season.season }}:</span>
                  <span class="season-team">{{ season.team }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="player-stats">
          <div class="stat-item">
            <span class="stat-label">Голы</span>
            <span class="stat-value">{{ selectedPlayer.stats.goals }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">ЖК</span>
            <span class="stat-value yellow">{{ selectedPlayer.stats.yellowCards }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">КК</span>
            <span class="stat-value red">{{ selectedPlayer.stats.redCards }}</span>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-ghost" type="button" @click="closePlayerModal">Закрыть</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'

const { optionalAuthApiRequest, isAuthenticated, loadCurrentUser } = useAuth()

import { useDebounce } from '../composables/useDebounce'

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

// Player Modal
const showPlayerModal = ref(false)
const selectedPlayer = ref(null)

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

    const path = `/api/players?${params.toString()}`
    const payload = await optionalAuthApiRequest(path)
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
  const query = val.trim()
  loadPlayers(query, 0)
})

function resetSearch() {
  search.value = ''
}

function goToPreviousPage() {
  if (pageNum.value === 0) {
    return
  }
  loadPlayers(search.value, pageNum.value - 1)
}

function goToNextPage() {
  if (isLastPage.value) {
    return
  }
  loadPlayers(search.value, pageNum.value + 1)
}

function generatePlayerDetails(player) {
  // Генерируем фиктивные данные на основе ФИО
  const hashCode = player.name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)
  const seed = hashCode % 100

  return {
    id: player.id,
    name: player.name,
    birthDate: new Date(1995 + (seed % 15), seed % 12, (seed % 28) + 1).toLocaleDateString('ru-RU'),
    teamsHistory: [
      { season: '2025/26', team: 'Основная команда' },
      { season: '2024/25', team: 'Основная команда' },
      { season: '2023/24', team: 'Резервная команда' },
    ],
    stats: {
      goals: seed % 20,
      yellowCards: seed % 8,
      redCards: seed % 3,
    },
  }
}

function openPlayerModal(player) {
  selectedPlayer.value = generatePlayerDetails(player)
  showPlayerModal.value = true
}

function closePlayerModal() {
  showPlayerModal.value = false
  selectedPlayer.value = null
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

// debounce composable не требует очистки таймера
</script>
