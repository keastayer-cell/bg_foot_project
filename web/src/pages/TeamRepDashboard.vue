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

      <div v-else class="team-rep-season-grid">
        <article class="team-rep-season-card" v-for="season in teamSeasons" :key="season.id">
          <div class="team-rep-history-main">
            <div class="team-rep-history-season">{{ season.name }}</div>
          </div>
          <div class="team-rep-badge-row">
            <span class="team-rep-season-chip">Состав: {{ season.rosterPlayersCount }}</span>
            <span class="team-rep-season-chip">В заявке: {{ season.selectedPlayersCount }}</span>
          </div>
          <div class="actions-row team-rep-history-actions">
            <button class="btn-primary" type="button" @click="openSeason(season.id)">Открыть заявку</button>
          </div>
        </article>
      </div>
    </article>

    <article class="card team-rep-players-card">
      <div class="toolbar team-rep-card-head">
        <h3 class="section-title">Текущий состав команды</h3>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка состава...</p>
      <p v-else-if="!teamPlayers.length" class="muted-text">В текущем составе команды пока нет игроков.</p>

      <div v-else class="team-rep-player-list">
        <article class="team-rep-player-item" v-for="player in teamPlayers" :key="player.id">
          <div class="team-rep-player-main">
            <strong>{{ player.fullName }}</strong>
            <span class="muted-text" v-if="player.birthDate">ДР: {{ formatDateOnly(player.birthDate) }}</span>
            <span class="muted-text" v-if="player.residence">Прописка: {{ player.residence }}</span>
            <div class="team-rep-season-chip-row" v-if="player.seasonIds?.length">
              <span class="team-rep-season-chip" v-for="seasonId in player.seasonIds" :key="seasonId">Сезон #{{ seasonId }}</span>
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
          </div>
        </article>
      </div>
    </article>

    <div v-if="seasonView" class="team-rep-fullscreen">
      <article class="card team-rep-fullscreen-card">
        <div class="toolbar team-rep-players-head">
          <div>
            <h3 class="section-title">Заявка: {{ seasonView.seasonName }}</h3>
            <p class="muted-text">Команда: {{ seasonView.teamName }}</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeSeason">Закрыть</button>
        </div>

        <div class="team-rep-inline-picker">
          <div>
            <strong>Добавить существующего игрока</strong>
            <p class="muted-text">Список показывает игроков, которые ещё не привязаны к этому сезону. После добавления игрок перейдёт в вашу команду и сразу попадёт в заявку.</p>
          </div>
          <div class="team-rep-inline-picker-form">
            <select v-model="selectedAvailablePlayerId">
              <option value="">Выберите игрока</option>
              <option v-for="player in seasonView.availablePlayers" :key="player.id" :value="String(player.id)">
                {{ player.fullName }}
              </option>
            </select>
            <button class="btn-primary" type="button" @click="addAvailablePlayerToSeason" :disabled="seasonLoading || !selectedAvailablePlayerId">
              Добавить
            </button>
          </div>
        </div>

        <p class="error-text" v-if="seasonError">{{ seasonError }}</p>
        <p class="success-text" v-if="seasonSuccess">{{ seasonSuccess }}</p>

        <div class="team-rep-player-list" v-if="seasonView.players.length">
          <article class="team-rep-player-item" v-for="player in seasonView.players" :key="player.id">
            <div class="team-rep-player-main">
              <strong>{{ player.fullName }}</strong>
              <span class="muted-text" v-if="player.birthDate">ДР: {{ formatDateOnly(player.birthDate) }}</span>
              <span class="muted-text" v-if="player.residence">Прописка: {{ player.residence }}</span>
            </div>
            <img
              v-if="player.photoDataUrl"
              :src="player.photoDataUrl"
              alt="Фото игрока"
              class="team-rep-player-photo"
            />
            <div class="actions-row team-rep-player-row-actions">
              <button
                v-if="player.selectedForSeason"
                class="btn-ghost"
                type="button"
                @click="removeFromSeason(player.id)"
                :disabled="seasonLoading"
              >
                Убрать из заявки
              </button>
              <button
                v-else
                class="btn-primary"
                type="button"
                @click="addRosterPlayerToSeason(player.id)"
                :disabled="seasonLoading"
              >
                Добавить в заявку
              </button>
            </div>
          </article>
        </div>
        <p v-else class="muted-text">В текущем составе команды нет игроков.</p>
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
import { computed, onMounted, reactive, ref, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'

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
const selectedAvailablePlayerId = ref('')

const playerModalOpen = ref(false)
const editingPlayerId = ref(null)
const playerModalError = ref('')
const playerForm = reactive({
  fullName: '',
  birthDate: '',
  residence: '',
  photoDataUrl: '',
})

const profile = computed(() => ({
  id: user.value?.id || 0,
  name: user.value?.name || 'Неизвестный пользователь',
  email: user.value?.email || '-',
  teamName: user.value?.teamName || 'Не назначена',
}))

watchEffect(() => {
  if (isAuthenticated.value && hasRole('TEAM_REP')) {
    return
  }

  closeSeason()
  closePlayerModal()
  router.replace('/')
})

onMounted(async () => {
  await loadCurrentUser().catch(() => null)
  if (isAuthenticated.value && hasRole('TEAM_REP')) {
    await loadDashboard()
  }
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
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить кабинет представителя.'
  } finally {
    dashboardLoading.value = false
  }
}

async function openSeason(seasonId) {
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

function closeSeason() {
  seasonView.value = null
  selectedAvailablePlayerId.value = ''
  seasonError.value = ''
  seasonSuccess.value = ''
}

async function addRosterPlayerToSeason(playerId) {
  await mutateSeasonPlayer(playerId, 'POST', 'Игрок добавлен в заявку сезона.')
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
  } catch (error) {
    seasonError.value = error.message || 'Не удалось изменить заявку сезона.'
  } finally {
    seasonLoading.value = false
  }
}

function openCreatePlayerModal() {
  editingPlayerId.value = null
  playerModalError.value = ''
  playerForm.fullName = ''
  playerForm.birthDate = ''
  playerForm.residence = ''
  playerForm.photoDataUrl = ''
  playerModalOpen.value = true
}

function openEditPlayerModal(player) {
  editingPlayerId.value = player.id
  playerModalError.value = ''
  playerForm.fullName = player.fullName || ''
  playerForm.birthDate = player.birthDate || ''
  playerForm.residence = player.residence || ''
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
        photoDataUrl: playerForm.photoDataUrl || null,
      }),
    })

    closePlayerModal()
    pageSuccess.value = editingPlayerId.value ? 'Игрок обновлён.' : 'Игрок создан и добавлен в состав команды.'
    await loadDashboard()

    if (seasonView.value) {
      await openSeason(seasonView.value.seasonId)
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
</script>
