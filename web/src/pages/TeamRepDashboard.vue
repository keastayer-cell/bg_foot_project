<template>
  <section class="section-wrap team-rep-page">
    <article class="card team-rep-profile-card">
      <div class="toolbar team-rep-card-head">
        <h2 class="section-title">Личный кабинет представителя команды</h2>
        <button class="btn-primary" type="button" @click="openCreatePlayerModal()">Создать игрока</button>
      </div>

      <div class="team-rep-profile-grid">
        <div>
          <span class="team-rep-label">Имя</span>
          <div class="team-rep-value">{{ profile.name }}</div>
        </div>
        <div>
          <span class="team-rep-label">Почта</span>
          <div class="team-rep-value">{{ profile.email }}</div>
        </div>
        <div>
          <span class="team-rep-label">Команда</span>
          <div class="team-rep-value team-rep-team">{{ profile.teamName }}</div>
        </div>
      </div>

      <p class="error-text" v-if="pageError">{{ pageError }}</p>
      <p class="success-text" v-if="pageSuccess">{{ pageSuccess }}</p>
    </article>

    <article class="card team-rep-history-card">
      <div class="toolbar team-rep-card-head">
        <h3 class="section-title">Сезоны команды</h3>
        <button class="btn-ghost" type="button" @click="loadDashboard" :disabled="dashboardLoading">Обновить</button>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка данных...</p>
      <p v-else-if="!teamSeasons.length" class="muted-text">Для вашей команды пока нет доступных сезонов.</p>

      <div v-else class="team-rep-form">
        <label>
          Выберите сезон для просмотра заявки
          <select v-model="selectedSeasonId">
            <option value="">— выберите —</option>
            <option v-for="season in teamSeasons" :key="season.id" :value="String(season.id)">
              {{ season.name }}
            </option>
          </select>
        </label>

        <template v-if="selectedSeasonSummary">
          <div class="toolbar team-rep-card-head team-rep-season-actions">
            <div class="team-rep-badge-row">
              <span class="team-rep-season-chip">В заявке: {{ selectedSeasonSummary.selectedPlayersCount }}</span>
            </div>
            <div class="actions-row team-rep-season-actions-row">
              <button
                class="btn-ghost"
                type="button"
                @click="toggleSelectedSeasonPlayersFilter"
              >
                {{ showSelectedSeasonPlayersOnly ? 'Показать весь состав' : 'Показать игроков в заявке' }}
              </button>
              <button class="btn-primary" type="button" @click="openAddPlayerModal(selectedSeasonSummary.id)">Добавить игрока</button>
            </div>
          </div>
        </template>

        <p v-if="seasonError" class="error-text">{{ seasonError }}</p>
        <p v-if="seasonSuccess" class="success-text">{{ seasonSuccess }}</p>
      </div>
    </article>

    <article class="card team-rep-players-card">
      <div class="toolbar team-rep-card-head">
        <div>
          <h3 class="section-title">Текущий состав команды</h3>
          <p v-if="showSelectedSeasonPlayersOnly && selectedSeasonSummary" class="muted-text team-rep-filter-hint">
            Показаны только игроки, относящиеся к сезону «{{ selectedSeasonSummary.name }}».
          </p>
        </div>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка состава...</p>
      <p v-else-if="!displayedTeamPlayers.length" class="muted-text">
        {{ showSelectedSeasonPlayersOnly ? 'Для выбранного сезона в текущем составе нет игроков.' : 'В текущем составе команды пока нет игроков.' }}
      </p>

      <div v-else class="team-rep-player-list">
        <article class="team-rep-player-item" v-for="player in displayedTeamPlayers" :key="player.id">
          <div class="team-rep-player-main">
            <strong>{{ player.fullName }}<span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
            <span class="muted-text" v-if="player.birthDate">ДР: {{ formatDateOnly(player.birthDate) }}</span>
            <span class="muted-text" v-if="player.residence">Прописка: {{ player.residence }}</span>
            <div class="team-rep-season-chip-row" v-if="player.seasons?.length">
              <span class="team-rep-season-chip" v-for="season in player.seasons" :key="season.id">{{ season.name }}</span>
            </div>
          </div>
          <img
            v-if="player.photoDataUrl"
            :src="player.photoDataUrl"
            alt="Фото игрока"
            class="team-rep-player-photo"
          />
          <div class="actions-row team-rep-player-row-actions">
            <button class="btn-ghost" type="button" @click="openEditPlayerModal(player)">Редактировать</button>
            <button
              v-if="selectedSeasonId && playerHasSelectedSeason(player)"
              class="btn-danger btn-compact"
              type="button"
              @click="removeFromSelectedSeason(player.id)"
            >
              Убрать из сезона
            </button>
            <button class="btn-danger btn-compact" type="button" @click="removeFromTeam(player.id)">Удалить из команды</button>
          </div>
        </article>
      </div>
    </article>

    <div v-if="addPlayerModalOpen && seasonView" class="modal-backdrop" @click.self="closeSeasonModals">
      <article class="card auth-modal team-rep-modal team-rep-season-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Добавить игрока в заявку</h3>
            <p class="muted-text">{{ seasonView.seasonName }} · {{ seasonView.teamName }}</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeSeasonModals">Закрыть</button>
        </div>

        <div class="team-rep-inline-picker compact">
          <SearchableSelect
            v-model="selectedAvailablePlayerId"
            :options="seasonSelectablePlayerOptions"
            placeholder="Выберите игрока"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="Игрок по такому ФИО не найден"
          />
          <div class="actions-row team-rep-season-modal-actions">
            <button class="btn-primary" type="button" @click="addAvailablePlayerToSeason" :disabled="seasonLoading || !selectedAvailablePlayerId">
              Добавить
            </button>
          </div>
        </div>

        <p class="error-text" v-if="seasonError">{{ seasonError }}</p>
      </article>
    </div>

    <div v-if="playerModalOpen" class="modal-backdrop" @click.self="closePlayerModal">
      <article class="card auth-modal team-rep-modal">
        <div class="toolbar auth-modal-head">
          <h3 class="section-title">{{ editingPlayerId ? 'Редактировать игрока' : 'Создать игрока' }}</h3>
          <button class="btn-ghost" type="button" @click="closePlayerModal">Закрыть</button>
        </div>

        <form class="team-rep-form" @submit.prevent="savePlayer">
          <label>
            ФИО
            <input v-model.trim="playerForm.fullName" type="text" required minlength="5" />
          </label>

          <label>
            Дата рождения
            <input v-model="playerForm.birthDate" type="date" />
          </label>

          <label>
            Прописка
            <input v-model.trim="playerForm.residence" type="text" placeholder="Например: Богородск" />
          </label>

          <label class="team-rep-checkbox-row">
            <input v-model="playerForm.isGoalkeeper" type="checkbox" />
            <span>Вратарь</span>
          </label>

          <label>
            Фото
            <input type="file" accept="image/*" @change="onPhotoSelected" />
          </label>

          <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото" class="team-rep-player-photo-preview" />

          <p class="error-text" v-if="playerModalError">{{ playerModalError }}</p>

          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="playerSaving">Сохранить</button>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'
import SearchableSelect from '../components/SearchableSelect.vue'

const { user, isAuthenticated, hasRole, loadCurrentUser, authorizedApiRequest } = useAuth()
const router = useRouter()

const dashboardLoading = ref(false)
const seasonLoading = ref(false)
const playerSaving = ref(false)
const pageError = ref('')
const pageSuccess = ref('')
const seasonError = ref('')
const seasonSuccess = ref('')

const teamSeasons = ref([])
const teamPlayers = ref([])
const seasonView = ref(null)
const selectedSeasonId = ref('')
const selectedAvailablePlayerId = ref('')
const addPlayerModalOpen = ref(false)
const showSelectedSeasonPlayersOnly = ref(false)

const playerModalOpen = ref(false)
const editingPlayerId = ref(null)
const playerModalError = ref('')
const playerForm = reactive({
  fullName: '',
  birthDate: '',
  residence: '',
  isGoalkeeper: false,
  photoDataUrl: '',
})

const profile = computed(() => ({
  id: user.value?.id || 0,
  name: user.value?.name || 'Неизвестный пользователь',
  email: user.value?.email || '-',
  teamName: user.value?.teamName || 'Не назначена',
}))

const selectedSeasonSummary = computed(() => {
  return teamSeasons.value.find((season) => String(season.id) === String(selectedSeasonId.value)) || null
})

const displayedTeamPlayers = computed(() => {
  const players = Array.isArray(teamPlayers.value) ? teamPlayers.value : []
  if (!showSelectedSeasonPlayersOnly.value || !selectedSeasonId.value) {
    return players
  }

  return players.filter(player => playerHasSelectedSeason(player))
})

const seasonSelectablePlayers = computed(() => {
  if (!seasonView.value) {
    return []
  }

  const combined = new Map()

  for (const player of seasonView.value.players || []) {
    if (!player.selectedForSeason) {
      combined.set(String(player.id), player)
    }
  }

  for (const player of seasonView.value.availablePlayers || []) {
    combined.set(String(player.id), player)
  }

  return Array.from(combined.values()).sort((left, right) =>
    String(left.fullName || '').localeCompare(String(right.fullName || ''), 'ru', { sensitivity: 'base' })
  )
})

const seasonSelectablePlayerOptions = computed(() => {
  return seasonSelectablePlayers.value.map((player) => ({
    value: String(player.id),
    label: formatPlayerOptionLabel(player),
    caption: player.residence || '',
    keywords: `${player.fullName || ''} ${player.residence || ''}`,
  }))
})

watchEffect(() => {
  if (isAuthenticated.value && hasRole('TEAM_REP')) {
    return
  }

  closeSeasonModals()
  closePlayerModal()
  router.replace('/')
})

onMounted(async () => {
  await loadCurrentUser().catch(() => null)
  if (isAuthenticated.value && hasRole('TEAM_REP')) {
    await loadDashboard()
  }
})

watch(selectedSeasonId, async (seasonId) => {
  if (!seasonId) {
    seasonView.value = null
    seasonError.value = ''
    seasonSuccess.value = ''
    showSelectedSeasonPlayersOnly.value = false
    return
  }

  await loadSeasonView(seasonId)
})

async function loadDashboard() {
  dashboardLoading.value = true
  pageError.value = ''

  try {
    const [seasonsPayload, playersPayload] = await Promise.all([
      authorizedApiRequest('/api/team-rep/seasons', { method: 'GET' }),
      authorizedApiRequest('/api/team-rep/players', { method: 'GET' }),
    ])
    teamSeasons.value = Array.isArray(seasonsPayload) ? seasonsPayload : []
    teamPlayers.value = Array.isArray(playersPayload) ? playersPayload : []
    if (selectedSeasonId.value) {
      const stillExists = teamSeasons.value.some((season) => String(season.id) === String(selectedSeasonId.value))
      if (!stillExists) {
        selectedSeasonId.value = ''
        seasonView.value = null
      }
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить кабинет представителя.'
  } finally {
    dashboardLoading.value = false
  }
}

async function loadSeasonView(seasonId) {
  seasonLoading.value = true
  seasonError.value = ''
  seasonSuccess.value = ''
  selectedAvailablePlayerId.value = ''

  try {
    seasonView.value = await authorizedApiRequest(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/players`, {
      method: 'GET',
    })
  } catch (error) {
    seasonError.value = error.message || 'Не удалось открыть заявку сезона.'
  } finally {
    seasonLoading.value = false
  }
}

async function openAddPlayerModal(seasonId) {
  await loadSeasonView(seasonId)
  if (!seasonView.value) {
    return
  }
  addPlayerModalOpen.value = true
}

function closeSeasonModals() {
  addPlayerModalOpen.value = false
  selectedAvailablePlayerId.value = ''
  seasonError.value = ''
  seasonSuccess.value = ''
}

function toggleSelectedSeasonPlayersFilter() {
  if (!selectedSeasonId.value) {
    seasonError.value = 'Сначала выберите сезон.'
    return
  }

  seasonError.value = ''
  showSelectedSeasonPlayersOnly.value = !showSelectedSeasonPlayersOnly.value
}

async function removeFromSeason(playerId) {
  await mutateSeasonPlayer(playerId, 'DELETE', 'Игрок убран из заявки сезона.')
}

async function addAvailablePlayerToSeason() {
  if (!seasonView.value || !selectedAvailablePlayerId.value) {
    seasonError.value = 'Выберите игрока из списка.'
    return
  }
  await mutateSeasonPlayer(selectedAvailablePlayerId.value, 'POST', 'Игрок переведён в вашу команду и добавлен в заявку.')
}

async function mutateSeasonPlayer(playerId, method, successMessage) {
  if (!seasonView.value) {
    return
  }

  seasonLoading.value = true
  seasonError.value = ''
  seasonSuccess.value = ''

  try {
    seasonView.value = await authorizedApiRequest(
      `/api/team-rep/seasons/${encodeURIComponent(seasonView.value.seasonId)}/players/${encodeURIComponent(playerId)}`,
      { method }
    )
    selectedAvailablePlayerId.value = ''
    seasonSuccess.value = successMessage
    await loadDashboard()
    if (method === 'POST') {
      addPlayerModalOpen.value = false
    }
  } catch (error) {
    seasonError.value = error.message || 'Не удалось изменить заявку сезона.'
  } finally {
    seasonLoading.value = false
  }
}

function playerHasSelectedSeason(player) {
  return Array.isArray(player?.seasons)
    && player.seasons.some((season) => String(season.id) === String(selectedSeasonId.value))
}

async function removeFromSelectedSeason(playerId) {
  if (!selectedSeasonId.value) {
    pageError.value = 'Сначала выберите сезон.'
    return
  }

  await removeFromSeason(playerId)
}

async function removeFromTeam(playerId) {
  pageError.value = ''
  pageSuccess.value = ''

  const teamId = user.value?.teamId
  if (!teamId) {
    pageError.value = 'Не удалось определить текущую команду пользователя.'
    return
  }

  try {
    await authorizedApiRequest(`/api/teams/${encodeURIComponent(teamId)}/players/${encodeURIComponent(playerId)}`, {
      method: 'DELETE',
    })
    pageSuccess.value = 'Игрок отвязан от текущей команды.'
    await loadDashboard()
    if (selectedSeasonId.value) {
      await loadSeasonView(selectedSeasonId.value)
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось удалить игрока из команды.'
  }
}

function openCreatePlayerModal() {
  editingPlayerId.value = null
  playerModalError.value = ''
  playerForm.fullName = ''
  playerForm.birthDate = ''
  playerForm.residence = ''
  playerForm.isGoalkeeper = false
  playerForm.photoDataUrl = ''
  playerModalOpen.value = true
}

function openEditPlayerModal(player) {
  editingPlayerId.value = player.id
  playerModalError.value = ''
  playerForm.fullName = player.fullName || ''
  playerForm.birthDate = player.birthDate || ''
  playerForm.residence = player.residence || ''
  playerForm.isGoalkeeper = Boolean(player.isGoalkeeper)
  playerForm.photoDataUrl = player.photoDataUrl || ''
  playerModalOpen.value = true
}

function closePlayerModal() {
  playerModalOpen.value = false
  playerModalError.value = ''
}

async function savePlayer() {
  playerModalError.value = ''
  pageSuccess.value = ''
  playerSaving.value = true

  try {
    const path = editingPlayerId.value
      ? `/api/team-rep/players/${encodeURIComponent(editingPlayerId.value)}`
      : '/api/team-rep/players'
    const method = editingPlayerId.value ? 'PUT' : 'POST'

    await authorizedApiRequest(path, {
      method,
      body: JSON.stringify({
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate || null,
        residence: playerForm.residence || null,
        isGoalkeeper: Boolean(playerForm.isGoalkeeper),
        photoDataUrl: playerForm.photoDataUrl || null,
      }),
    })

    closePlayerModal()
    pageSuccess.value = editingPlayerId.value ? 'Игрок обновлён.' : 'Игрок создан и добавлен в состав команды.'
    await loadDashboard()

    if (seasonView.value) {
      await loadSeasonView(seasonView.value.seasonId)
    }
  } catch (error) {
    playerModalError.value = error.message || 'Не удалось сохранить игрока.'
  } finally {
    playerSaving.value = false
  }
}

function onPhotoSelected(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    playerForm.photoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function formatDateOnly(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatPlayerOptionLabel(player) {
  if (!player) return ''
  return `${player.fullName || ''}${player.isGoalkeeper ? ' 🧤' : ''}`
}
</script>

<style scoped>
.team-rep-page {
  display: grid;
  gap: 16px;
}

.team-rep-card-head {
  align-items: start;
}

.team-rep-profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.team-rep-label {
  display: block;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 0.82rem;
}

.team-rep-value {
  font-weight: 600;
}

.team-rep-form {
  display: grid;
  gap: 12px;
}

.team-rep-form label {
  display: grid;
  gap: 6px;
}

.team-rep-checkbox-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.goalkeeper-icon {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
  font-size: 0.9em;
  line-height: 1;
}

.team-rep-season-actions {
  align-items: center;
}

.team-rep-season-actions-row {
  align-items: center;
}

.team-rep-badge-row,
.team-rep-season-chip-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.team-rep-season-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.08);
  border: 1px solid rgba(97, 232, 162, 0.18);
  color: var(--text);
  font-size: 0.82rem;
}

.team-rep-filter-hint {
  margin-top: 6px;
}

.team-rep-player-list {
  display: grid;
  gap: 10px;
}

.team-rep-player-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.team-rep-player-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.team-rep-player-row-actions {
  justify-content: flex-start;
  margin-top: 0;
}

.team-rep-player-photo,
.team-rep-player-photo-preview {
  width: 84px;
  height: 84px;
  border-radius: 14px;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
}

.team-rep-modal {
  width: min(720px, calc(100vw - 24px));
}

.team-rep-season-modal {
  width: min(560px, calc(100vw - 24px));
}

.team-rep-inline-picker {
  display: grid;
  gap: 12px;
}

.team-rep-inline-picker.compact {
  grid-template-columns: minmax(0, 1fr);
}

.team-rep-season-modal-actions {
  justify-content: flex-end;
}

.btn-compact {
  min-width: 0;
  padding-inline: 12px;
}

@media (max-width: 860px) {
  .team-rep-profile-grid {
    grid-template-columns: 1fr;
  }

  .team-rep-season-actions,
  .team-rep-season-actions-row,
  .team-rep-card-head {
    align-items: start;
    flex-direction: column;
  }

  .team-rep-player-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .team-rep-modal,
  .team-rep-season-modal {
    width: calc(100vw - 20px);
  }

  .team-rep-player-photo,
  .team-rep-player-photo-preview {
    width: 72px;
    height: 72px;
  }

  .team-rep-card-head > .btn-primary,
  .team-rep-card-head > .btn-ghost,
  .team-rep-season-actions-row > *,
  .team-rep-player-row-actions > *,
  .team-rep-season-modal-actions > * {
    width: 100%;
  }

  .team-rep-player-row-actions,
  .team-rep-season-modal-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .team-rep-inline-picker select,
  .team-rep-form input,
  .team-rep-form select {
    width: 100%;
  }
}
</style>
