<template>
  <section class="section-wrap team-rep-page">
    <article class="card team-rep-profile-card">
      <div class="toolbar team-rep-card-head">
        <h2 class="section-title">Кабинет сезонных заявок команды</h2>
        <div class="actions-row">
          <button v-if="canOpenTransfers" class="btn-ghost" type="button" @click="router.push('/team-rep-transfers')">Трансферы</button>
          <button v-if="canManagePlayers" class="btn-primary" type="button" @click="openCreatePlayerModal()">Создать игрока</button>
        </div>
      </div>

      <div class="team-rep-profile-grid">
        <div v-if="isSuperAdminEditor">
          <span class="team-rep-label">Режим</span>
          <div class="team-rep-value">SUPER_ADMIN</div>
        </div>
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
          <div v-if="!isSuperAdminEditor" class="team-rep-value team-rep-team">{{ profile.teamName }}</div>
          <label v-else class="team-rep-admin-team-picker">
            <select v-model="selectedAdminTeamId">
              <option value="">— выберите команду —</option>
              <option v-for="team in adminTeams" :key="team.id" :value="String(team.id)">
                {{ team.name }}
              </option>
            </select>
          </label>
        </div>
      </div>

      <p class="error-text" v-if="pageError">{{ pageError }}</p>
      <p class="success-text" v-if="pageSuccess">{{ pageSuccess }}</p>

      <div v-if="canOpenTransfers && incomingTransfersSummary.totalPendingCount > 0" class="team-rep-transfer-alert">
        <button class="btn-primary team-rep-transfer-alert-btn" type="button" @click="openIncomingTransfersModal">
          У вас новая заявка на трансфер · {{ incomingTransfersSummary.totalPendingCount }}
        </button>
      </div>
    </article>

    <article class="card team-rep-history-card">
      <div class="toolbar team-rep-card-head">
        <h3 class="section-title">Сезоны команды</h3>
        <button class="btn-ghost" type="button" @click="loadDashboard" :disabled="dashboardLoading">Обновить</button>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка данных...</p>
      <p v-else-if="isSuperAdminEditor && !selectedAdminTeamId" class="muted-text">Выберите команду, чтобы открыть сезонные заявки.</p>
      <p v-else-if="!teamSeasons.length" class="muted-text">Для выбранной команды пока нет доступных сезонов.</p>

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
              <span class="team-rep-season-chip">
                В заявке:
                {{ `${selectedSeasonSummary.selectedPlayersCount} из ${selectedSeasonSummary.maxRosterSize || '∞'}` }}
              </span>
              <span class="team-rep-season-chip">{{ selectedSeasonSummary.applicationDeadline ? `Дедлайн: ${formatDateOnly(selectedSeasonSummary.applicationDeadline)}` : 'Дедлайн не задан' }}</span>
              <span class="team-rep-season-chip">Статус: {{ formatSeasonStatus(selectedSeasonSummary.status) }}</span>
              <span class="team-rep-season-chip" :class="applicationStatusChipClass(selectedSeasonSummary.applicationStatus)">Заявка: {{ formatApplicationStatus(selectedSeasonSummary.applicationStatus) }}</span>
              <span class="team-rep-season-chip" :class="selectedSeasonSummary.applicationOpen ? 'team-rep-season-chip-open' : 'team-rep-season-chip-closed'">
                {{ selectedSeasonSummary.applicationOpen ? 'Добавление открыто' : 'Добавление закрыто' }}
              </span>
            </div>
            <div class="actions-row team-rep-season-actions-row">
              <button
                class="btn-ghost"
                type="button"
                @click="toggleSelectedSeasonPlayersFilter"
              >
                {{ showSelectedSeasonPlayersOnly ? 'Показать весь состав' : 'Показать игроков в заявке' }}
              </button>
              <button class="btn-ghost" type="button" @click="submitSeasonApplication" :disabled="seasonLoading || !selectedSeasonSummary.applicationSubmittable">
                {{ selectedSeasonSummary.applicationStatus === 'RETURNED' ? 'Отправить повторно' : 'Отправить на проверку' }}
              </button>
              <button class="btn-primary" type="button" @click="openAddPlayerModal(selectedSeasonSummary.id)" :disabled="!canEditSelectedSeasonApplication">Добавить игрока</button>
            </div>
          </div>
          <div v-if="selectedSeasonSummary.applicationDecisionComment" class="team-rep-review-note" :class="applicationReviewNoteClass(selectedSeasonSummary.applicationStatus)">
            <strong>{{ selectedSeasonSummary.applicationStatus === 'APPROVED' ? 'Решение по заявке' : 'Комментарий проверяющего' }}</strong>
            <p>{{ selectedSeasonSummary.applicationDecisionComment }}</p>
          </div>
          <p v-if="!selectedSeasonSummary.applicationOpen" class="muted-text">
            {{ selectedSeasonSummary.status !== 'ACTIVE'
              ? 'Изменения заявки закрыты, потому что сезон не находится в активном статусе.'
              : 'Дедлайн изменений сезонной заявки истек.' }}
          </p>
          <p v-else-if="selectedSeasonSummary.applicationStatus === 'SUBMITTED'" class="muted-text">
            Заявка отправлена на проверку. Дождитесь решения рефери или администратора.
          </p>
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
        <button class="btn-ghost" type="button" @click="toggleTeamRosterVisibility">
          {{ isTeamRosterVisible ? 'Скрыть состав' : 'Показать состав' }}
        </button>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка состава...</p>
      <p v-else-if="!isTeamRosterVisible" class="muted-text">Состав скрыт. Нажмите «Показать состав», чтобы открыть список игроков.</p>
      <p v-else-if="!displayedTeamPlayers.length" class="muted-text">
        {{ showSelectedSeasonPlayersOnly ? 'Для выбранного сезона в текущем составе нет игроков.' : 'В текущем составе команды пока нет игроков.' }}
      </p>

      <div v-else class="team-rep-player-list">
        <article class="team-rep-player-item" v-for="player in displayedTeamPlayers" :key="player.id">
          <div class="team-rep-player-main">
            <strong>{{ player.fullName }}<span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
          </div>
          <img
            v-if="player.photoDataUrl"
            :src="player.photoDataUrl"
            alt="Фото игрока"
            class="team-rep-player-photo"
          />
          <div class="actions-row team-rep-player-row-actions">
            <button v-if="canManagePlayers" class="btn-ghost" type="button" @click="openEditPlayerModal(player)">Редактировать</button>
            <button
              v-if="selectedSeasonId && playerHasSelectedSeason(player)"
              class="btn-danger btn-compact"
              type="button"
              @click="removeFromSelectedSeason(player.id)"
              :disabled="!canEditSelectedSeasonApplication"
            >
              Убрать из сезона
            </button>
            <button v-if="canManagePlayers" class="btn-danger btn-compact" type="button" @click="removeFromTeam(player.id)">Удалить из команды</button>
          </div>
        </article>
      </div>
    </article>

    <div v-if="incomingTransfersModalOpen" class="modal-backdrop" @click.self="closeIncomingTransfersModal">
      <article class="card auth-modal team-rep-modal incoming-transfer-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Входящие заявки на трансфер</h3>
            <p class="muted-text">Здесь можно подтвердить или отклонить новые заявки.</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeIncomingTransfersModal">Закрыть</button>
        </div>

        <p v-if="incomingTransfersLoading" class="muted-text">Загрузка заявок...</p>
        <p v-else-if="!incomingTransfersSummary.requests.length" class="muted-text">Новых входящих заявок нет.</p>

        <div v-else class="incoming-transfer-list">
          <article class="incoming-transfer-item" v-for="request in incomingTransfersSummary.requests" :key="request.id">
            <div class="incoming-transfer-main">
              <strong>{{ request.playerName }}<span v-if="request.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
              <p class="muted-text">{{ request.toTeamName }} хочет забрать игрока из вашей команды</p>
              <p class="muted-text">Заявка создана: {{ formatDateTime(request.requestedAt) }}</p>
              <p v-if="request.requestComment" class="muted-text">Комментарий: {{ request.requestComment }}</p>
            </div>
            <textarea v-model.trim="incomingDecisionComments[request.id]" rows="2" placeholder="Комментарий к решению"></textarea>
            <div class="actions-row team-rep-player-row-actions">
              <button class="btn-primary btn-compact" type="button" @click="processIncomingTransfer(request.id, 'approve')" :disabled="incomingDecisionLoadingId === request.id">Подтвердить</button>
              <button class="btn-danger btn-compact" type="button" @click="processIncomingTransfer(request.id, 'reject')" :disabled="incomingDecisionLoadingId === request.id">Отклонить</button>
            </div>
          </article>
        </div>

        <div class="pagination-bar" v-if="incomingTransfersSummary.totalPages > 1">
          <button class="btn-ghost" type="button" @click="changeIncomingTransfersPage(incomingTransfersSummary.pageNumber - 1)" :disabled="incomingTransfersLoading || incomingTransfersSummary.pageNumber <= 0">Назад</button>
          <span class="muted-text">Страница {{ incomingTransfersSummary.pageNumber + 1 }} из {{ incomingTransfersSummary.totalPages }} · всего {{ incomingTransfersSummary.totalElements }}</span>
          <button class="btn-ghost" type="button" @click="changeIncomingTransfersPage(incomingTransfersSummary.pageNumber + 1)" :disabled="incomingTransfersLoading || incomingTransfersSummary.pageNumber + 1 >= incomingTransfersSummary.totalPages">Вперёд</button>
        </div>
      </article>
    </div>

    <div v-if="addPlayerModalOpen" class="modal-backdrop" @click.self="closeSeasonModals">
      <article class="card auth-modal team-rep-modal team-rep-season-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Добавить игрока в заявку</h3>
            <p v-if="seasonView" class="muted-text">{{ seasonView.seasonName }} · {{ seasonView.teamName }}</p>
          </div>
          <div class="actions-row team-rep-season-modal-head-actions">
            <button class="btn-ghost" type="button" @click="closeSeasonModals">Закрыть</button>
          </div>
        </div>

        <p v-if="seasonLoading && !seasonView" class="muted-text">Загрузка списка игроков...</p>

        <div v-else-if="seasonView" class="team-rep-inline-picker compact">
          <SearchableSelect
            :key="`team-rep-season-picker-${seasonView.seasonId}-${seasonSelectablePlayerOptions.length}`"
            v-model="selectedAvailablePlayerIds"
            :options="seasonSelectablePlayerOptions"
            multiple
            multiple-summary-text="Выбрано игроков"
            multiple-action-hint="После выбора нажмите «Добавить выбранных»"
            placeholder="Выберите игроков"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="Игрок по такому ФИО не найден"
          />
          <div class="team-rep-season-picker-meta">
            <span class="team-rep-selected-count">
              Выбрано: {{ selectedAvailablePlayerIds.length }}
            </span>
            <button
              class="btn-primary"
              type="button"
              @click="addAvailablePlayersToSeason"
              :disabled="seasonLoading || !seasonView || !seasonView.applicationOpen || !selectedAvailablePlayerIds.length"
            >
              Добавить выбранных
            </button>
          </div>
        </div>

        <p v-if="seasonView && !seasonView.applicationOpen" class="muted-text">
          Добавление новых игроков закрыто с {{ formatDateOnly(seasonView.applicationDeadline) }}.
        </p>

        <p v-if="!seasonView" class="muted-text">Не удалось загрузить доступных игроков.</p>

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
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../store/auth'
import SearchableSelect from '../components/SearchableSelect.vue'

const { user, isAuthenticated, hasRole, loadCurrentUser, authorizedApiRequest } = useAuth()
const route = useRoute()
const router = useRouter()

const dashboardLoading = ref(false)
const seasonLoading = ref(false)
const playerSaving = ref(false)
const pageError = ref('')
const pageSuccess = ref('')
const seasonError = ref('')
const seasonSuccess = ref('')
const incomingTransfersLoading = ref(false)
const incomingTransfersModalOpen = ref(false)
const incomingDecisionLoadingId = ref(null)
const incomingTransfersSummary = ref({
  totalPendingCount: 0,
  requests: [],
  pageNumber: 0,
  pageSize: 20,
  totalElements: 0,
  totalPages: 0,
})
const incomingDecisionComments = reactive({})

const adminTeams = ref([])
const teamSeasons = ref([])
const teamPlayers = ref([])
const seasonView = ref(null)
const selectedAdminTeamId = ref('')
const selectedSeasonId = ref('')
const selectedAvailablePlayerIds = ref([])
const addPlayerModalOpen = ref(false)
const showSelectedSeasonPlayersOnly = ref(false)
const isTeamRosterVisible = ref(false)

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
let suspendSeasonSelectionWatch = false

const isSuperAdminEditor = computed(() => hasRole('SUPER_ADMIN'))
const canOpenDashboard = computed(() => isAuthenticated.value && (hasRole('TEAM_REP') || hasRole('SUPER_ADMIN')))
const canOpenTransfers = computed(() => hasRole('TEAM_REP'))
const canManagePlayers = computed(() => hasRole('TEAM_REP'))

const activeTeamName = computed(() => {
  if (!isSuperAdminEditor.value) {
    return user.value?.teamName || 'Не назначена'
  }
  return adminTeams.value.find((team) => String(team.id) === String(selectedAdminTeamId.value))?.name || 'Не выбрана'
})

const profile = computed(() => ({
  id: user.value?.id || 0,
  name: user.value?.name || 'Неизвестный пользователь',
  email: user.value?.email || '-',
  teamName: activeTeamName.value,
}))

const selectedSeasonSummary = computed(() => {
  const summary = teamSeasons.value.find((season) => String(season.id) === String(selectedSeasonId.value)) || null
  if (!summary) {
    return null
  }
  if (!seasonView.value || String(seasonView.value.seasonId) !== String(selectedSeasonId.value)) {
    return summary
  }

  const liveSelectedPlayersCount = Array.isArray(seasonView.value.players)
    ? seasonView.value.players.filter((player) => player.selectedForSeason).length
    : summary.selectedPlayersCount

  return {
    ...summary,
    applicationDeadline: seasonView.value.applicationDeadline ?? summary.applicationDeadline,
    status: seasonView.value.status ?? summary.status,
    maxRosterSize: seasonView.value.maxRosterSize ?? summary.maxRosterSize,
    transferWindowStartDate: seasonView.value.transferWindowStartDate ?? summary.transferWindowStartDate,
    transferWindowEndDate: seasonView.value.transferWindowEndDate ?? summary.transferWindowEndDate,
    applicationOpen: Boolean(seasonView.value.applicationOpen),
    selectedPlayersCount: liveSelectedPlayersCount,
    applicationStatus: seasonView.value.applicationStatus ?? summary.applicationStatus,
    applicationSubmittedAt: seasonView.value.applicationSubmittedAt ?? summary.applicationSubmittedAt,
    applicationDecisionAt: seasonView.value.applicationDecisionAt ?? summary.applicationDecisionAt,
    applicationDecisionComment: seasonView.value.applicationDecisionComment ?? summary.applicationDecisionComment,
    applicationSubmittable: Boolean(seasonView.value.applicationSubmittable),
  }
})

const canEditSelectedSeasonApplication = computed(() => {
  if (!selectedSeasonSummary.value) {
    return false
  }
  const status = String(selectedSeasonSummary.value.applicationStatus || 'DRAFT')
  return selectedSeasonSummary.value.applicationOpen && (status === 'DRAFT' || status === 'RETURNED' || status === 'APPROVED')
})

const displayedTeamPlayers = computed(() => {
  const rosterPlayers = Array.isArray(teamPlayers.value) ? teamPlayers.value : []
  const hasLiveSeasonView = seasonView.value && String(seasonView.value.seasonId) === String(selectedSeasonId.value)

  if (!hasLiveSeasonView) {
    if (!showSelectedSeasonPlayersOnly.value || !selectedSeasonId.value) {
      return rosterPlayers
    }
    return rosterPlayers.filter(player => playerHasSelectedSeason(player))
  }

  const mergedPlayers = new Map()

  for (const player of rosterPlayers) {
    mergedPlayers.set(String(player.id), { ...player })
  }

  for (const player of seasonView.value.players || []) {
    const key = String(player.id)
    mergedPlayers.set(key, {
      ...(mergedPlayers.get(key) || {}),
      ...player,
    })
  }

  const players = Array.from(mergedPlayers.values()).sort((left, right) =>
    String(left.fullName || '').localeCompare(String(right.fullName || ''), 'ru', { sensitivity: 'base' })
  )

  if (!showSelectedSeasonPlayersOnly.value || !selectedSeasonId.value) {
    return players
  }

  return players.filter((player) => player.selectedForSeason || playerHasSelectedSeason(player))
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
    keywords: `${player.fullName || ''}`,
  }))
})

watchEffect(() => {
  if (canOpenDashboard.value) {
    return
  }

  closeSeasonModals()
  closePlayerModal()
  closeIncomingTransfersModal()
  router.replace('/')
})

onMounted(async () => {
  await loadCurrentUser().catch(() => null)
  if (!canOpenDashboard.value) {
    return
  }
  if (isSuperAdminEditor.value) {
    await loadAdminTeams()
    selectedAdminTeamId.value = String(route.query.teamId || '')
    return
  }
  if (route.query.seasonId) {
    selectedSeasonId.value = String(route.query.seasonId)
  }
  await loadDashboard()
})

watch(selectedSeasonId, async (seasonId) => {
  if (suspendSeasonSelectionWatch) {
    return
  }
  syncDashboardQuery(selectedAdminTeamId.value, seasonId)
  if (!seasonId) {
    seasonView.value = null
    seasonError.value = ''
    seasonSuccess.value = ''
    showSelectedSeasonPlayersOnly.value = false
    return
  }

  await loadSeasonView(seasonId)
})

watch(selectedAdminTeamId, async (teamId) => {
  if (!isSuperAdminEditor.value) {
    return
  }

  const preserveSeasonId = String(route.query.teamId || '') === String(teamId || '')
    ? String(route.query.seasonId || '')
    : ''
  suspendSeasonSelectionWatch = true
  clearDashboardState()
  suspendSeasonSelectionWatch = false
  syncDashboardQuery(teamId, preserveSeasonId)
  if (!teamId) {
    return
  }

  await loadDashboard()
})

watch(() => route.query.teamId, (teamId) => {
  if (!isSuperAdminEditor.value) {
    return
  }
  const normalized = String(teamId || '')
  if (normalized !== String(selectedAdminTeamId.value || '')) {
    selectedAdminTeamId.value = normalized
  }
})

watch(() => route.query.seasonId, (seasonId) => {
  const normalized = String(seasonId || '')
  if (normalized !== String(selectedSeasonId.value || '')) {
    selectedSeasonId.value = normalized
  }
})

async function loadAdminTeams() {
  try {
    const payload = await authorizedApiRequest('/api/teams?active_flag=1', { method: 'GET' })
    adminTeams.value = (Array.isArray(payload) ? payload : [])
      .map((item) => ({
        id: item.id,
        name: item.name || 'Без названия',
      }))
      .sort((left, right) => String(left.name).localeCompare(String(right.name), 'ru', { sensitivity: 'base' }))
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить список команд.'
    adminTeams.value = []
  }
}

function teamScopedPath(path) {
  if (!isSuperAdminEditor.value || !selectedAdminTeamId.value) {
    return path
  }
  const separator = path.includes('?') ? '&' : '?'
  return `${path}${separator}teamId=${encodeURIComponent(selectedAdminTeamId.value)}`
}

function syncDashboardQuery(teamId, seasonId) {
  if (!isSuperAdminEditor.value) {
    return
  }
  const nextTeamId = String(teamId || '')
  const nextSeasonId = String(seasonId || '')
  const currentTeamId = String(route.query.teamId || '')
  const currentSeasonId = String(route.query.seasonId || '')
  if (nextTeamId === currentTeamId && nextSeasonId === currentSeasonId) {
    return
  }
  router.replace({
    query: {
      ...route.query,
      teamId: nextTeamId || undefined,
      seasonId: nextSeasonId || undefined,
    },
  })
}

function clearDashboardState() {
  teamSeasons.value = []
  teamPlayers.value = []
  seasonView.value = null
  selectedSeasonId.value = ''
  selectedAvailablePlayerIds.value = []
  seasonError.value = ''
  seasonSuccess.value = ''
  showSelectedSeasonPlayersOnly.value = false
}

async function loadDashboard() {
  dashboardLoading.value = true
  pageError.value = ''

  if (isSuperAdminEditor.value && !selectedAdminTeamId.value) {
    clearDashboardState()
    dashboardLoading.value = false
    return
  }

  try {
    const [seasonsPayload, playersPayload] = await Promise.all([
      authorizedApiRequest(teamScopedPath('/api/team-rep/seasons'), { method: 'GET' }),
      authorizedApiRequest(teamScopedPath('/api/team-rep/players'), { method: 'GET' }),
    ])
    teamSeasons.value = Array.isArray(seasonsPayload) ? seasonsPayload : []
    teamPlayers.value = Array.isArray(playersPayload) ? playersPayload : []
    if (canOpenTransfers.value) {
      await loadIncomingTransfersNotifications(0)
    }
    const requestedSeasonId = String(route.query.seasonId || '')
    if (requestedSeasonId && teamSeasons.value.some((season) => String(season.id) === requestedSeasonId)) {
      selectedSeasonId.value = requestedSeasonId
    }
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

async function loadIncomingTransfersNotifications(pageNum = 0) {
  incomingTransfersLoading.value = true

  try {
    const payload = await authorizedApiRequest(`/api/team-rep/transfers/incoming-pending?pagenum=${pageNum}&pagesize=20`, {
      method: 'GET',
    })
    incomingTransfersSummary.value = {
      totalPendingCount: Number(payload?.totalPendingCount || 0),
      requests: Array.isArray(payload?.requests) ? payload.requests : [],
      pageNumber: Number(payload?.pageNumber || 0),
      pageSize: Number(payload?.pageSize || 20),
      totalElements: Number(payload?.totalElements || 0),
      totalPages: Number(payload?.totalPages || 0),
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить входящие трансферные заявки.'
  } finally {
    incomingTransfersLoading.value = false
  }
}

async function openIncomingTransfersModal() {
  incomingTransfersModalOpen.value = true
  await loadIncomingTransfersNotifications(0)
}

function closeIncomingTransfersModal() {
  incomingTransfersModalOpen.value = false
}

async function changeIncomingTransfersPage(pageNum) {
  if (pageNum < 0) return
  if (incomingTransfersSummary.value.totalPages && pageNum >= incomingTransfersSummary.value.totalPages) return
  await loadIncomingTransfersNotifications(pageNum)
}

async function processIncomingTransfer(requestId, action) {
  incomingDecisionLoadingId.value = requestId
  pageError.value = ''
  pageSuccess.value = ''

  try {
    await authorizedApiRequest(`/api/team-rep/transfers/${encodeURIComponent(requestId)}/${action}`, {
      method: 'POST',
      body: JSON.stringify({
        decisionComment: incomingDecisionComments[requestId] || null,
      }),
    })
    incomingDecisionComments[requestId] = ''
    pageSuccess.value = action === 'approve' ? 'Трансфер подтвержден.' : 'Трансфер отклонен.'
    await loadIncomingTransfersNotifications(incomingTransfersSummary.value.pageNumber || 0)
    await loadDashboard()
    if (selectedSeasonId.value) {
      await loadSeasonView(selectedSeasonId.value)
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось обработать входящую заявку.'
  } finally {
    incomingDecisionLoadingId.value = null
  }
}

async function loadSeasonView(seasonId) {
  seasonLoading.value = true
  seasonError.value = ''
  seasonSuccess.value = ''
  selectedAvailablePlayerIds.value = []

  try {
    seasonView.value = await authorizedApiRequest(teamScopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/players`), {
      method: 'GET',
    })
  } catch (error) {
    seasonError.value = error.message || 'Не удалось открыть заявку сезона.'
  } finally {
    seasonLoading.value = false
  }
}

async function openAddPlayerModal(seasonId) {
  seasonError.value = ''
  seasonSuccess.value = ''
  const summary = teamSeasons.value.find((season) => String(season.id) === String(seasonId)) || null
  if (summary && !canEditApplicationSummary(summary)) {
    seasonError.value = summary.applicationDeadline
      ? `Дедлайн добавления игроков истек ${formatDateOnly(summary.applicationDeadline)}.`
      : 'Добавление игроков в заявку этого сезона закрыто.'
    return
  }
  addPlayerModalOpen.value = true

  if (String(seasonView.value?.seasonId || '') === String(seasonId)) {
    return
  }

  await loadSeasonView(seasonId)
}

function closeSeasonModals() {
  addPlayerModalOpen.value = false
  selectedAvailablePlayerIds.value = []
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

function toggleTeamRosterVisibility() {
  isTeamRosterVisible.value = !isTeamRosterVisible.value
}

async function removeFromSeason(playerId) {
  await mutateSeasonPlayer(playerId, 'DELETE', 'Игрок убран из заявки сезона.')
}

async function addAvailablePlayersToSeason() {
  if (!seasonView.value || !selectedAvailablePlayerIds.value.length) {
    seasonError.value = 'Выберите хотя бы одного игрока из списка.'
    return
  }
  if (!canEditApplicationSummary(seasonView.value)) {
    seasonError.value = seasonView.value.applicationDeadline
      ? `Дедлайн добавления игроков истек ${formatDateOnly(seasonView.value.applicationDeadline)}.`
      : 'Добавление игроков в заявку этого сезона закрыто.'
    return
  }

  seasonLoading.value = true
  seasonError.value = ''
  seasonSuccess.value = ''

  try {
    seasonView.value = await authorizedApiRequest(
      teamScopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonView.value.seasonId)}/players`),
      {
        method: 'POST',
        body: JSON.stringify({
          playerIds: selectedAvailablePlayerIds.value.map((id) => Number(id)),
        }),
      }
    )
    selectedAvailablePlayerIds.value = []
    seasonSuccess.value = 'Выбранные игроки добавлены в заявку.'
    addPlayerModalOpen.value = false
    await loadDashboard()
  } catch (error) {
    seasonError.value = error.message || 'Не удалось изменить заявку сезона.'
  } finally {
    seasonLoading.value = false
  }
}

async function submitSeasonApplication() {
  if (!selectedSeasonId.value) {
    seasonError.value = 'Сначала выберите сезон.'
    return
  }

  seasonLoading.value = true
  seasonError.value = ''
  seasonSuccess.value = ''

  try {
    seasonView.value = await authorizedApiRequest(teamScopedPath(`/api/team-rep/seasons/${encodeURIComponent(selectedSeasonId.value)}/submit`), {
      method: 'POST',
    })
    seasonSuccess.value = 'Сезонная заявка отправлена на проверку.'
    await loadDashboard()
  } catch (error) {
    seasonError.value = error.message || 'Не удалось отправить сезонную заявку.'
  } finally {
    seasonLoading.value = false
  }
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
      teamScopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonView.value.seasonId)}/players/${encodeURIComponent(playerId)}`),
      { method }
    )
    selectedAvailablePlayerIds.value = []
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
  if (seasonView.value && String(seasonView.value.seasonId) === String(selectedSeasonId.value)) {
    return Array.isArray(seasonView.value.players)
      && seasonView.value.players.some((item) => String(item.id) === String(player?.id) && item.selectedForSeason)
  }

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

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatPlayerOptionLabel(player) {
  if (!player) return ''
  return `${player.fullName || ''}`
}

function canEditApplicationSummary(summary) {
  if (!summary) {
    return false
  }
  const status = String(summary.applicationStatus || 'DRAFT')
  return Boolean(summary.applicationOpen) && (status === 'DRAFT' || status === 'RETURNED' || status === 'APPROVED')
}

function formatSeasonStatus(status) {
  if (status === 'ACTIVE') return 'Активный'
  if (status === 'CLOSED') return 'Закрыт'
  if (status === 'DRAFT') return 'Черновик'
  return status || '—'
}

function formatApplicationStatus(status) {
  if (status === 'DRAFT') return 'Черновик'
  if (status === 'SUBMITTED') return 'На проверке'
  if (status === 'RETURNED') return 'На доработке'
  if (status === 'APPROVED') return 'Одобрена'
  if (status === 'REJECTED') return 'Отклонена'
  return status || '—'
}

function applicationStatusChipClass(status) {
  if (status === 'APPROVED') return 'team-rep-season-chip-open'
  if (status === 'RETURNED' || status === 'REJECTED') return 'team-rep-season-chip-closed'
  if (status === 'SUBMITTED') return 'team-rep-season-chip-review'
  return ''
}

function applicationReviewNoteClass(status) {
  if (status === 'APPROVED') return 'team-rep-review-note-approved'
  if (status === 'RETURNED') return 'team-rep-review-note-returned'
  if (status === 'REJECTED') return 'team-rep-review-note-rejected'
  return ''
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

.team-rep-season-chip-open {
  background: rgba(97, 232, 162, 0.1);
  border-color: rgba(97, 232, 162, 0.28);
}

.team-rep-season-chip-closed {
  background: rgba(255, 184, 107, 0.1);
  border-color: rgba(255, 184, 107, 0.28);
}

.team-rep-season-chip-review {
  background: rgba(123, 180, 255, 0.12);
  border-color: rgba(123, 180, 255, 0.32);
}

.team-rep-review-note {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 184, 107, 0.25);
  background: rgba(255, 184, 107, 0.08);
}

.team-rep-review-note p {
  margin: 0;
}

.team-rep-review-note-approved {
  border-color: rgba(97, 232, 162, 0.28);
  background: rgba(97, 232, 162, 0.08);
}

.team-rep-review-note-returned,
.team-rep-review-note-rejected {
  border-color: rgba(255, 154, 139, 0.28);
  background: rgba(255, 154, 139, 0.08);
}

.team-rep-filter-hint {
  margin-top: 6px;
}

.team-rep-transfer-alert {
  margin-top: 14px;
}

.team-rep-transfer-alert-btn {
  width: 100%;
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

.incoming-transfer-modal {
  width: min(760px, calc(100vw - 24px));
}

.team-rep-season-modal {
  width: min(560px, calc(100vw - 24px));
}

.incoming-transfer-list {
  display: grid;
  gap: 12px;
}

.incoming-transfer-item {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.team-rep-inline-picker {
  display: grid;
  gap: 12px;
}

.team-rep-inline-picker.compact {
  grid-template-columns: minmax(0, 1fr);
}

.team-rep-season-picker-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.team-rep-selected-count {
  color: var(--muted);
  font-size: 0.85rem;
}

.team-rep-season-modal-head-actions {
  justify-content: flex-end;
  align-items: center;
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
  .team-rep-season-modal-head-actions > * {
    width: 100%;
  }

  .team-rep-season-picker-meta > * {
    width: 100%;
  }

  .pagination-bar > * {
    width: 100%;
  }

  .team-rep-player-row-actions,
  .team-rep-season-modal-head-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .team-rep-inline-picker select,
  .team-rep-form input,
  .team-rep-form select {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .team-rep-page {
    gap: 14px;
  }

  .team-rep-profile-card,
  .team-rep-history-card,
  .team-rep-players-card {
    padding: 14px;
  }

  .team-rep-player-item {
    padding: 12px;
  }

  .team-rep-player-photo,
  .team-rep-player-photo-preview {
    width: 64px;
    height: 64px;
  }
}
</style>
