<template>
  <section :class="embedded ? 'team-rep-transfers-embedded' : 'section-wrap team-rep-page team-rep-transfers-page'">
    <article v-if="!embedded" class="card transfer-control-card">
      <header class="transfer-page-header">
        <div>
          <p class="admin-panel-kicker">Турнир / Трансферы</p>
          <h2 class="section-title">Трансферы сезона</h2>
          <p class="muted-text">Создание, согласование и история переходов игроков между командами.</p>
        </div>
        <div class="transfer-header-actions">
          <button v-if="hasRole('TEAM_REP')" class="btn-ghost" type="button" @click="router.push('/team-rep-dashboard')">К заявке сезона</button>
          <button class="btn-ghost" type="button" @click="loadSeasons" :disabled="seasonLoading || overviewLoading">Обновить данные</button>
        </div>
      </header>

      <UiState v-if="pageError" tone="error" title="Операция не выполнена" :message="pageError" />
      <UiState v-if="pageSuccess" tone="success" title="Готово" :message="pageSuccess" />

      <section class="transfer-context-card">
        <div class="transfer-step-heading">
          <span class="admin-step-number">1</span>
          <div><h3>Рабочий сезон</h3><p>Все заявки и доступные команды зависят от выбранного сезона.</p></div>
        </div>
        <div class="transfer-context-layout">
          <label class="transfer-season-field">
            <span>Сезон</span>
            <select v-model="selectedSeasonId">
              <option value="">— выберите сезон —</option>
              <option v-for="season in teamSeasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option>
            </select>
          </label>

          <div v-if="overview" class="transfer-context-metrics">
            <div><small>Статус сезона</small><strong>{{ formatSeasonStatus(overview.seasonStatus) }}</strong></div>
            <div :class="overview.transferWindowOpen ? 'is-open' : 'is-closed'">
              <small>Трансферное окно</small><strong>{{ overview.transferWindowOpen ? 'Открыто' : 'Закрыто' }}</strong>
            </div>
            <div><small>{{ overview.maxRosterSize ? 'Заявка команды' : 'Всего заявок' }}</small><strong>{{ overview.maxRosterSize ? `${overview.selectedPlayersCount} / ${overview.maxRosterSize}` : overview.totalElements }}</strong></div>
          </div>
        </div>
        <div v-if="overview" class="transfer-context-footer">
          <p>{{ transferWindowDescription }}</p>
          <button class="btn-primary" type="button" @click="openTransferRequestModal" :disabled="!canOpenTransferRequestModal">Создать трансфер</button>
        </div>
      </section>
    </article>

    <article v-if="overview" class="card transfer-journal-card">
      <header class="transfer-step-heading transfer-journal-heading">
        <span class="admin-step-number">2</span>
        <div><h3>Журнал трансферов</h3><p>Текущие заявки и завершённые переходы выбранного сезона.</p></div>
        <span class="admin-step-count">{{ overview.totalElements }}</span>
      </header>

      <div class="transfer-journal-summary">
        <div><small>Всего</small><strong>{{ overview.totalElements }}</strong></div>
        <div><small>Ожидают решения</small><strong>{{ pendingTransfersCount }}</strong></div>
        <div><small>Подтверждены на странице</small><strong>{{ approvedTransfersCount }}</strong></div>
      </div>

      <UiState v-if="overviewLoading" tone="loading" title="Загружаем трансферы" />
      <UiState v-else-if="!overview.requests.length" title="Трансферных заявок пока нет" message="Созданные заявки и история решений появятся здесь." />

      <div v-else class="transfer-journal">
        <div class="transfer-journal-columns" aria-hidden="true">
          <span>Игрок</span><span>Переход</span><span>Заявка</span><span>Статус</span><span>Действия</span>
        </div>
        <div class="transfer-journal-list">
          <article v-for="request in overview.requests" :key="request.id" class="transfer-journal-row">
            <div class="transfer-player-cell">
              <small>Игрок</small>
              <strong>{{ request.playerName }} <span v-if="request.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
              <span v-if="request.requestComment" :title="request.requestComment">{{ request.requestComment }}</span>
            </div>
            <div class="transfer-route-cell">
              <small>Переход</small>
              <span>{{ request.fromTeamName }}</span>
              <b aria-hidden="true">→</b>
              <strong>{{ request.toTeamName }}</strong>
            </div>
            <div class="transfer-date-cell">
              <small>Заявка</small>
              <strong>{{ formatDateOnly(request.requestedAt) }}</strong>
              <span>{{ request.requestedByName || '—' }}</span>
            </div>
            <div class="transfer-status-cell">
              <small>Статус</small>
              <span class="team-rep-season-chip" :class="statusChipClass(request.status)">{{ formatTransferStatus(request.status) }}</span>
            </div>
            <div class="transfer-row-actions">
              <small>Действия</small>
              <button v-if="request.canApprove" class="transfer-decision transfer-decision-approve" type="button" @click="processTransferAction(request.id, 'approve')" :disabled="transferActionLoadingKey === `approve:${request.id}` || overviewLoading">
                {{ transferActionLoadingKey === `approve:${request.id}` ? 'Сохраняем…' : 'Подтвердить' }}
              </button>
              <button v-if="request.canReject" class="transfer-decision transfer-decision-reject" type="button" @click="processTransferAction(request.id, 'reject')" :disabled="transferActionLoadingKey === `reject:${request.id}` || overviewLoading">
                {{ transferActionLoadingKey === `reject:${request.id}` ? 'Сохраняем…' : 'Отклонить' }}
              </button>
              <button v-if="request.canRevoke" class="transfer-decision transfer-decision-revoke" type="button" @click="processTransferAction(request.id, 'revoke')" :disabled="transferActionLoadingKey === `revoke:${request.id}` || overviewLoading">
                {{ transferActionLoadingKey === `revoke:${request.id}` ? 'Сохраняем…' : 'Отозвать' }}
              </button>
              <span v-if="!request.canApprove && !request.canReject && !request.canRevoke" class="transfer-no-actions">Действий нет</span>
            </div>
          </article>
        </div>

        <div v-if="overview.totalPages > 1" class="pagination-bar">
          <button class="btn-ghost" type="button" @click="changeTransfersPage(overview.pageNumber - 1)" :disabled="overviewLoading || overview.pageNumber <= 0">Назад</button>
          <span class="muted-text">Страница {{ overview.pageNumber + 1 }} из {{ overview.totalPages }} · всего {{ overview.totalElements }}</span>
          <button class="btn-ghost" type="button" @click="changeTransfersPage(overview.pageNumber + 1)" :disabled="overviewLoading || overview.pageNumber + 1 >= overview.totalPages">Вперёд</button>
        </div>
      </div>
    </article>

    <div v-if="transferRequestModalOpen" class="modal-backdrop" @click.self="closeTransferRequestModal">
      <article class="card auth-modal transfer-create-modal">
        <header class="transfer-create-header">
          <div><p class="admin-panel-kicker">Новая операция</p><h3 class="section-title">Создать трансфер</h3><p v-if="overview" class="muted-text">{{ overview.seasonName }}{{ overview.teamName ? ` · ${overview.teamName}` : '' }}</p></div>
          <button class="btn-ghost" type="button" @click="closeTransferRequestModal">Закрыть</button>
        </header>

        <div class="transfer-create-flow">
          <label v-if="overview?.privilegedAccess" class="transfer-create-step">
            <span class="transfer-create-step-label"><b>01</b><span><strong>Команда назначения</strong><small>Куда переходит игрок</small></span></span>
            <select v-model="targetTeamId" :disabled="!overview?.transferWindowOpen">
              <option value="">— выберите команду —</option>
              <option v-for="team in availableTargetTeams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>

          <label class="transfer-create-step">
            <span class="transfer-create-step-label"><b>{{ overview?.privilegedAccess ? '02' : '01' }}</b><span><strong>Исходная команда</strong><small>Откуда забираем игрока</small></span></span>
            <select v-model="sourceTeamId" :disabled="!canPickSourceTeam">
              <option value="">{{ canPickSourceTeam ? '— выберите команду —' : 'Сначала выберите команду назначения' }}</option>
              <option v-for="team in availableSourceTeams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>

          <label class="transfer-create-step">
            <span class="transfer-create-step-label"><b>{{ overview?.privilegedAccess ? '03' : '02' }}</b><span><strong>Игрок</strong><small>Кандидат из состава исходной команды</small></span></span>
            <SearchableSelect
              :key="`transfer-player-${selectedSeasonId}-${sourceTeamId}-${candidateOptions.length}`"
              v-model="selectedPlayerId"
              :options="candidateOptions"
              placeholder="Выберите игрока"
              search-placeholder="Начните вводить ФИО игрока"
              empty-text="Подходящий игрок не найден"
              :disabled="!canPickSourceTeam || !sourceTeamId || candidatesLoading"
            />
          </label>

          <label class="transfer-create-step transfer-create-comment">
            <span class="transfer-create-step-label"><b>{{ overview?.privilegedAccess ? '04' : '03' }}</b><span><strong>Комментарий</strong><small>Необязательное пояснение к заявке</small></span></span>
            <textarea v-model.trim="requestComment" rows="3" placeholder="Добавьте пояснение при необходимости"></textarea>
          </label>
        </div>

        <div class="transfer-create-summary" :class="{ 'is-ready': canSubmitTransferRequest }">
          <div><small>Будет создан переход</small><strong>{{ transferDraftLabel }}</strong></div>
          <button class="btn-primary" type="button" @click="submitTransferRequest" :disabled="!canSubmitTransferRequest || createLoading">{{ createLoading ? 'Отправляем…' : 'Отправить заявку' }}</button>
        </div>

        <p v-if="!overview?.transferWindowOpen" class="transfer-create-warning">Создание заявок недоступно: сезон не активен или трансферное окно закрыто.</p>
        <p v-else-if="!overview?.privilegedAccess && overview?.maxRosterSize && overview.selectedPlayersCount >= overview.maxRosterSize" class="transfer-create-warning">Лимит заявки достигнут. Сначала освободите место в составе.</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SearchableSelect from '../components/SearchableSelect.vue'
import UiState from '../components/UiState.vue'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { useAuth } from '../store/auth'
import { createTeamRepTransfersApi } from '../api/teamRepTransfers'

defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const { isAuthenticated, hasRole, loadCurrentUser, authorizedApiRequest } = useAuth()
const { confirmAction } = useConfirmDialog()
const transfersApi = createTeamRepTransfersApi(authorizedApiRequest)
const router = useRouter()
const route = useRoute()
const pageSize = 20

const teamSeasons = ref([])
const selectedSeasonId = ref('')
const overview = ref(null)
const seasonLoading = ref(false)
const overviewLoading = ref(false)
const candidatesLoading = ref(false)
const createLoading = ref(false)
const transferActionLoadingKey = ref('')
const pageError = ref('')
const pageSuccess = ref('')
const sourceTeamId = ref('')
const targetTeamId = ref('')
const selectedPlayerId = ref('')
const requestComment = ref('')
const candidates = ref([])
const transferRequestModalOpen = ref(false)

const isPrivilegedTransferManager = computed(() => hasRole('SUPER_ADMIN') || hasRole('REFEREE'))
const canManageTransfers = computed(() => isAuthenticated.value && (hasRole('TEAM_REP') || isPrivilegedTransferManager.value))

const availableTargetTeams = computed(() => overview.value?.targetTeams || [])
const resolvedTargetTeamId = computed(() => {
  if (overview.value?.privilegedAccess) {
    return targetTeamId.value
  }
  return overview.value?.teamId ? String(overview.value.teamId) : ''
})

const availableSourceTeams = computed(() => {
  const teams = overview.value?.sourceTeams || []
  if (!overview.value?.privilegedAccess || !resolvedTargetTeamId.value) {
    return teams
  }
  return teams.filter((team) => String(team.id) !== String(resolvedTargetTeamId.value))
})

const candidateOptions = computed(() => {
  return candidates.value.map((player) => ({
    value: String(player.id),
    label: player.fullName,
    keywords: `${player.fullName || ''} ${player.residence || ''}`,
  }))
})

const canOpenTransferRequestModal = computed(() => {
  if (!overview.value) {
    return false
  }
  if (!overview.value.transferWindowOpen) {
    return false
  }
  if (!overview.value.privilegedAccess && overview.value.maxRosterSize && overview.value.selectedPlayersCount >= overview.value.maxRosterSize) {
    return false
  }
  return true
})

const canPickSourceTeam = computed(() => {
  return canOpenTransferRequestModal.value && Boolean(resolvedTargetTeamId.value)
})

const canSubmitTransferRequest = computed(() => {
  return canPickSourceTeam.value && sourceTeamId.value && selectedPlayerId.value
})

const pendingTransfersCount = computed(() => overview.value?.requests?.filter(
  (request) => request.status === 'PENDING'
).length || 0)

const approvedTransfersCount = computed(() => overview.value?.requests?.filter(
  (request) => request.status === 'APPROVED'
).length || 0)

const transferWindowDescription = computed(() => {
  if (!overview.value) return ''
  const period = overview.value.transferWindowStartDate && overview.value.transferWindowEndDate
    ? `Период: ${formatDateOnly(overview.value.transferWindowStartDate)} — ${formatDateOnly(overview.value.transferWindowEndDate)}.`
    : 'Период трансферного окна не задан.'
  return overview.value.transferWindowOpen
    ? `${period} Новые заявки можно создавать.`
    : `${period} Создание новых заявок сейчас недоступно.`
})

const selectedSourceTeam = computed(() => availableSourceTeams.value.find(
  (team) => String(team.id) === String(sourceTeamId.value)
) || null)

const selectedTargetTeam = computed(() => availableTargetTeams.value.find(
  (team) => String(team.id) === String(resolvedTargetTeamId.value)
) || null)

const selectedTransferPlayer = computed(() => candidates.value.find(
  (player) => String(player.id) === String(selectedPlayerId.value)
) || null)

const transferDraftLabel = computed(() => {
  if (!selectedSourceTeam.value && !selectedTargetTeam.value) return 'Заполните направление и выберите игрока'
  const playerName = selectedTransferPlayer.value?.fullName || 'Игрок не выбран'
  const fromTeam = selectedSourceTeam.value?.name || 'Исходная команда'
  const toTeam = selectedTargetTeam.value?.name || overview.value?.teamName || 'Команда назначения'
  return `${playerName}: ${fromTeam} → ${toTeam}`
})

watchEffect(() => {
  if (canManageTransfers.value) {
    return
  }
  router.replace('/')
})

onMounted(async () => {
  await loadCurrentUser().catch(() => null)
  if (canManageTransfers.value) {
    await loadSeasons()
  }
})

watch(selectedSeasonId, async (seasonId) => {
  overview.value = null
  pageError.value = ''
  pageSuccess.value = ''
  resetTransferDraft()
  targetTeamId.value = ''

  if (!seasonId) {
    return
  }

  await loadOverview(seasonId, 0)
})

watch(() => route.query.season, (seasonId) => {
  const requestedSeasonId = String(seasonId || '')
  if (requestedSeasonId && teamSeasons.value.some((season) => String(season.id) === requestedSeasonId)) {
    selectedSeasonId.value = requestedSeasonId
  }
})

watch([sourceTeamId, resolvedTargetTeamId], async ([teamId, currentTargetTeamId]) => {
  selectedPlayerId.value = ''
  candidates.value = []
  if (!selectedSeasonId.value || !teamId || !currentTargetTeamId || !canOpenTransferRequestModal.value || !transferRequestModalOpen.value) {
    return
  }
  await loadCandidates(selectedSeasonId.value, teamId, currentTargetTeamId)
})

async function loadSeasons() {
  seasonLoading.value = true
  pageError.value = ''

  try {
    const payload = await transfersApi.getSeasons(isPrivilegedTransferManager.value)
    teamSeasons.value = Array.isArray(payload) ? payload : []
    if (selectedSeasonId.value) {
      const stillExists = teamSeasons.value.some((season) => String(season.id) === String(selectedSeasonId.value))
      if (!stillExists) {
        selectedSeasonId.value = ''
      }
    }
    const requestedSeasonId = String(route.query.season || '')
    if (!selectedSeasonId.value && teamSeasons.value.some((season) => String(season.id) === requestedSeasonId)) {
      selectedSeasonId.value = requestedSeasonId
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить сезоны.'
  } finally {
    seasonLoading.value = false
  }
}

async function loadOverview(seasonId, pageNum = 0) {
  overviewLoading.value = true
  pageError.value = ''

  try {
    overview.value = await transfersApi.getOverview(seasonId, pageNum, pageSize)
    if (overview.value?.privilegedAccess) {
      const exists = (overview.value.targetTeams || []).some((team) => String(team.id) === String(targetTeamId.value))
      if (!exists) {
        targetTeamId.value = ''
      }
    } else {
      targetTeamId.value = overview.value?.teamId ? String(overview.value.teamId) : ''
    }
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить трансферы сезона.'
  } finally {
    overviewLoading.value = false
  }
}

function resetTransferDraft() {
  targetTeamId.value = overview.value?.privilegedAccess ? '' : (overview.value?.teamId ? String(overview.value.teamId) : '')
  sourceTeamId.value = ''
  selectedPlayerId.value = ''
  requestComment.value = ''
  candidates.value = []
}

function openTransferRequestModal() {
  if (!selectedSeasonId.value) {
    pageError.value = 'Сначала выберите сезон.'
    return
  }
  if (!canOpenTransferRequestModal.value) {
    return
  }
  transferRequestModalOpen.value = true
}

function closeTransferRequestModal() {
  transferRequestModalOpen.value = false
  resetTransferDraft()
}

async function changeTransfersPage(pageNum) {
  if (!selectedSeasonId.value) return
  if (pageNum < 0) return
  if (overview.value?.totalPages && pageNum >= overview.value.totalPages) return
  await loadOverview(selectedSeasonId.value, pageNum)
}

async function loadCandidates(seasonId, teamId, currentTargetTeamId) {
  candidatesLoading.value = true
  pageError.value = ''

  try {
    const payload = await transfersApi.getCandidates(seasonId, teamId, currentTargetTeamId)
    candidates.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить список игроков для трансфера.'
  } finally {
    candidatesLoading.value = false
  }
}

async function submitTransferRequest() {
  if (!canSubmitTransferRequest.value || !selectedSeasonId.value) {
    pageError.value = 'Сначала выберите команду-источник и игрока.'
    return
  }

  createLoading.value = true
  pageError.value = ''
  pageSuccess.value = ''

  try {
    await transfersApi.create(selectedSeasonId.value, {
      fromTeamId: Number(sourceTeamId.value),
      toTeamId: Number(resolvedTargetTeamId.value),
      playerId: Number(selectedPlayerId.value),
      requestComment: requestComment.value || null,
    })
    pageSuccess.value = 'Трансферная заявка отправлена.'
    closeTransferRequestModal()
    await loadSeasons()
    await loadOverview(selectedSeasonId.value, 0)
  } catch (error) {
    pageError.value = error.message || 'Не удалось создать трансферную заявку.'
  } finally {
    createLoading.value = false
  }
}

async function processTransferAction(requestId, action) {
  if (!selectedSeasonId.value || !requestId) {
    return
  }

  const request = overview.value?.requests?.find((item) => String(item.id) === String(requestId))
  const actionCopy = {
    approve: {
      title: 'Подтвердить трансфер?',
      message: `${request?.playerName || 'Игрок'} перейдет из «${request?.fromTeamName || 'исходной команды'}» в «${request?.toTeamName || 'новую команду'}».`,
      confirmLabel: 'Подтвердить трансфер',
      tone: 'warning',
    },
    reject: {
      title: 'Отклонить трансфер?',
      message: `Заявка на переход игрока ${request?.playerName || ''} будет закрыта без изменения состава.`,
      confirmLabel: 'Отклонить',
    },
    revoke: {
      title: 'Отозвать трансфер?',
      message: 'Если переход уже подтвержден, игрок будет возвращен в исходную команду.',
      confirmLabel: 'Отозвать трансфер',
    },
  }[action]
  const accepted = await confirmAction(actionCopy)
  if (!accepted) return

  transferActionLoadingKey.value = `${action}:${requestId}`
  pageError.value = ''
  pageSuccess.value = ''

  try {
    await transfersApi.process(requestId, action)
    if (action === 'approve') {
      pageSuccess.value = 'Трансфер подтвержден.'
    } else if (action === 'reject') {
      pageSuccess.value = 'Трансфер отклонен.'
    } else {
      pageSuccess.value = 'Трансфер отозван. Если переход уже был подтвержден, игрок возвращен в исходную команду.'
    }
    await loadOverview(selectedSeasonId.value, 0)
  } catch (error) {
    if (action === 'approve') {
      pageError.value = error.message || 'Не удалось подтвердить трансфер.'
    } else if (action === 'reject') {
      pageError.value = error.message || 'Не удалось отклонить трансфер.'
    } else {
      pageError.value = error.message || 'Не удалось отозвать трансфер.'
    }
  } finally {
    transferActionLoadingKey.value = ''
  }
}

function formatSeasonStatus(status) {
  if (status === 'ACTIVE') return 'Активный'
  if (status === 'CLOSED') return 'Закрыт'
  if (status === 'DRAFT') return 'Черновик'
  return status || '—'
}

function formatTransferStatus(status) {
  if (status === 'PENDING') return 'Трансфер запрошен'
  if (status === 'APPROVED') return 'Трансфер одобрен'
  if (status === 'REJECTED') return 'Трансфер отклонен'
  if (status === 'REVOKED') return 'Трансфер отозван'
  return status || '—'
}

function statusChipClass(status) {
  if (status === 'APPROVED') return 'team-rep-season-chip-open'
  if (status === 'REJECTED') return 'team-rep-season-chip-closed'
  if (status === 'REVOKED') return 'team-rep-season-chip-muted'
  return ''
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

<style scoped>
.team-rep-transfers-page {
  display: grid;
  gap: 16px;
}

.team-rep-transfers-embedded {
  display: grid;
  gap: 16px;
}

.team-rep-transfer-badges {
  margin-top: 12px;
}

.team-rep-transfer-form textarea,
.team-rep-transfer-actions textarea {
  width: 100%;
  min-height: 88px;
}

.team-rep-transfer-list {
  display: grid;
  gap: 8px;
}

.team-rep-transfer-list-head,
.team-rep-transfer-row {
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.15fr) minmax(0, 1.05fr) 140px 180px minmax(240px, 1.15fr);
  align-items: center;
}

.team-rep-transfer-list-head {
  display: grid;
  gap: 12px;
  margin-top: 18px;
  padding: 0 14px 6px;
  color: rgba(241, 244, 255, 0.88);
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 1.2;
}

.team-rep-transfer-item {
  display: grid;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.transfer-cell {
  min-width: 0;
  display: block;
}

.transfer-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.transfer-team {
  display: block;
  color: var(--text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-player {
  display: block;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-status-wrap {
  display: flex;
  justify-content: flex-start;
  min-width: 0;
}

.transfer-status-wrap .team-rep-season-chip {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-action-cell {
  display: grid;
  grid-template-columns: repeat(3, minmax(58px, 72px));
  justify-content: start;
  align-items: center;
  gap: 8px;
}

.transfer-action-slot {
  display: flex;
}

.transfer-action-slot-left {
  justify-content: flex-start;
}

.transfer-action-slot-center {
  justify-content: center;
}

.transfer-action-slot-right {
  justify-content: flex-end;
}

.transfer-action-btn {
  min-width: 58px;
  height: 42px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.transfer-action-placeholder {
  display: inline-block;
  width: 58px;
  height: 42px;
}

.transfer-action-btn:disabled {
  opacity: 0.65;
  cursor: wait;
}

.transfer-action-btn-approve {
  background: linear-gradient(180deg, #5ef0a0 0%, #30cf77 100%);
  border-color: rgba(65, 214, 131, 0.6);
  color: #062b17;
}

.transfer-action-btn-reject {
  background: linear-gradient(180deg, #ffb14f 0%, #ee7c1f 100%);
  border-color: rgba(255, 160, 66, 0.65);
  color: #331500;
}

.transfer-action-btn-revoke {
  background: linear-gradient(180deg, #e35b74 0%, #bb324e 100%);
  border-color: rgba(214, 73, 103, 0.65);
}

.team-rep-season-chip-muted {
  background: rgba(135, 145, 170, 0.16);
  border-color: rgba(135, 145, 170, 0.4);
  color: rgba(231, 236, 255, 0.82);
}

.transfer-request-date {
  color: rgba(231, 236, 255, 0.78);
  font-size: 0.9rem;
  white-space: nowrap;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .team-rep-transfer-list-head {
    display: none;
  }

  .team-rep-transfer-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .transfer-team,
  .transfer-player,
  .transfer-request-date {
    white-space: normal;
    overflow: visible;
    text-overflow: clip;
  }

  .transfer-status-wrap {
    justify-content: flex-start;
  }

  .transfer-action-cell {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .transfer-action-slot,
  .transfer-action-slot-left,
  .transfer-action-slot-center,
  .transfer-action-slot-right {
    justify-content: stretch;
  }

  .transfer-action-btn,
  .transfer-action-placeholder {
    width: 100%;
    min-width: 0;
  }

  .team-rep-card-head > .actions-row {
    width: 100%;
  }

  .team-rep-card-head > .actions-row > * {
    width: 100%;
  }

  .pagination-bar > * {
    width: 100%;
  }
}

.transfer-control-card,
.transfer-journal-card {
  display: grid;
  gap: 18px;
}

.transfer-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 17px;
  border-bottom: 1px solid rgba(124, 163, 255, .14);
}

.transfer-page-header .section-title,
.transfer-page-header .muted-text { margin: 0; }
.transfer-page-header .section-title { margin-top: 5px; }
.transfer-page-header .muted-text { margin-top: 7px; }

.transfer-header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.transfer-context-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(124, 163, 255, .17);
  border-radius: 14px;
  background: rgba(7, 13, 32, .38);
}

.transfer-step-heading {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(124, 163, 255, .14);
}

.transfer-step-heading > div { min-width: 0; }
.transfer-step-heading h3,
.transfer-step-heading p { margin: 0; }
.transfer-step-heading h3 { font-size: .96rem; }
.transfer-step-heading p { margin-top: 4px; color: var(--muted); font-size: .76rem; }

.transfer-context-layout {
  display: grid;
  grid-template-columns: minmax(260px, .72fr) minmax(0, 1.28fr);
  gap: 14px;
  align-items: end;
}

.transfer-season-field {
  display: grid;
  gap: 8px;
}

.transfer-season-field > span {
  color: var(--muted);
  font-size: .72rem;
  font-weight: 700;
}

.transfer-context-metrics,
.transfer-journal-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 10px;
  background: rgba(124, 163, 255, .035);
}

.transfer-context-metrics > div,
.transfer-journal-summary > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 11px 13px;
  border-right: 1px solid rgba(124, 163, 255, .12);
}

.transfer-context-metrics > div:last-child,
.transfer-journal-summary > div:last-child { border-right: 0; }
.transfer-context-metrics small,
.transfer-journal-summary small { color: var(--muted); font-size: .66rem; }
.transfer-context-metrics strong,
.transfer-journal-summary strong { overflow: hidden; font-size: .78rem; text-overflow: ellipsis; white-space: nowrap; }
.transfer-context-metrics .is-open strong { color: var(--brand); }
.transfer-context-metrics .is-closed strong { color: #ff8da2; }

.transfer-context-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(124, 163, 255, .14);
}

.transfer-context-footer p {
  margin: 0;
  color: var(--muted);
  font-size: .72rem;
}

.transfer-journal-heading .admin-step-count { margin-left: auto; }
.transfer-journal-summary { margin-top: -2px; }
.transfer-journal-summary strong { color: var(--text); font-size: 1rem; }

.transfer-journal {
  min-width: 0;
}

.transfer-journal-columns,
.transfer-journal-row {
  display: grid;
  grid-template-columns: minmax(170px, 1.05fr) minmax(250px, 1.5fr) minmax(130px, .75fr) minmax(165px, .9fr) minmax(260px, 1.25fr);
  gap: 12px;
  align-items: center;
}

.transfer-journal-columns {
  padding: 0 13px 8px;
  color: var(--muted);
  font-size: .68rem;
  font-weight: 800;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.transfer-journal-list {
  display: grid;
  gap: 8px;
}

.transfer-journal-row {
  padding: 13px;
  border: 1px solid rgba(124, 163, 255, .15);
  border-radius: 12px;
  background: rgba(7, 13, 32, .34);
}

.transfer-journal-row small {
  display: none;
  color: var(--muted);
  font-size: .64rem;
  font-weight: 700;
  text-transform: uppercase;
}

.transfer-player-cell,
.transfer-date-cell,
.transfer-status-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.transfer-player-cell strong,
.transfer-player-cell > span,
.transfer-date-cell strong,
.transfer-date-cell > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-player-cell strong { font-size: .78rem; }
.transfer-player-cell > span,
.transfer-date-cell > span { color: var(--muted); font-size: .67rem; }
.transfer-date-cell strong { font-size: .74rem; }

.transfer-route-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.transfer-route-cell > span,
.transfer-route-cell > strong {
  overflow: hidden;
  font-size: .74rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-route-cell > span { color: var(--muted); }
.transfer-route-cell > b { color: var(--brand); font-size: .85rem; }
.transfer-status-cell .team-rep-season-chip { justify-self: start; max-width: 100%; }

.transfer-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.transfer-decision {
  min-height: 32px;
  padding: 6px 9px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: .66rem;
  font-weight: 800;
  cursor: pointer;
}

.transfer-decision:disabled { opacity: .55; cursor: wait; }
.transfer-decision-approve { border-color: rgba(97, 232, 162, .35); background: rgba(97, 232, 162, .13); color: var(--brand); }
.transfer-decision-reject { border-color: rgba(255, 177, 79, .35); background: rgba(255, 177, 79, .1); color: #ffb86a; }
.transfer-decision-revoke { border-color: rgba(227, 91, 116, .36); background: rgba(227, 91, 116, .11); color: #ff8da2; }
.transfer-no-actions { color: var(--muted); font-size: .68rem; }

.transfer-create-modal {
  width: min(880px, calc(100vw - 32px));
  max-width: 880px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.transfer-create-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(124, 163, 255, .14);
}

.transfer-create-header .section-title,
.transfer-create-header .muted-text { margin: 0; }
.transfer-create-header .section-title { margin-top: 4px; }
.transfer-create-header .muted-text { margin-top: 5px; }

.transfer-create-flow {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.transfer-create-step {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(124, 163, 255, .15);
  border-radius: 11px;
  background: rgba(7, 13, 32, .38);
}

.transfer-create-step-label {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.transfer-create-step-label > b {
  color: var(--brand);
  font-size: .68rem;
}

.transfer-create-step-label > span {
  display: grid;
  gap: 3px;
}

.transfer-create-step-label strong { font-size: .76rem; }
.transfer-create-step-label small { color: var(--muted); font-size: .66rem; }
.transfer-create-step textarea { width: 100%; min-height: 82px; }

.transfer-create-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  padding: 14px;
  border: 1px solid rgba(124, 163, 255, .15);
  border-radius: 11px;
  background: rgba(7, 13, 32, .38);
}

.transfer-create-summary.is-ready { border-color: rgba(97, 232, 162, .3); background: rgba(97, 232, 162, .055); }
.transfer-create-summary > div { display: grid; gap: 4px; min-width: 0; }
.transfer-create-summary small { color: var(--muted); font-size: .66rem; }
.transfer-create-summary strong { overflow: hidden; font-size: .76rem; text-overflow: ellipsis; white-space: nowrap; }

.transfer-create-warning {
  margin: 12px 0 0;
  padding: 11px 13px;
  border: 1px solid rgba(255, 177, 79, .24);
  border-radius: 9px;
  background: rgba(255, 177, 79, .07);
  color: #ffc17a;
  font-size: .7rem;
}

@media (max-width: 1080px) {
  .transfer-journal-columns { display: none; }
  .transfer-journal-row { grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; }
  .transfer-journal-row small { display: block; }
  .transfer-route-cell { grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); }
  .transfer-route-cell > small { grid-column: 1 / -1; }
  .transfer-row-actions { align-items: center; grid-column: 1 / -1; padding-top: 10px; border-top: 1px solid rgba(124, 163, 255, .12); }
  .transfer-row-actions > small { width: 100%; }
}

@media (max-width: 760px) {
  .transfer-page-header,
  .transfer-context-footer,
  .transfer-create-header,
  .transfer-create-summary { align-items: stretch; flex-direction: column; }
  .transfer-header-actions { justify-content: flex-start; }
  .transfer-context-layout,
  .transfer-create-flow { grid-template-columns: 1fr; }
  .transfer-context-metrics,
  .transfer-journal-summary { grid-template-columns: 1fr; }
  .transfer-context-metrics > div,
  .transfer-journal-summary > div { border-right: 0; border-bottom: 1px solid rgba(124, 163, 255, .12); }
  .transfer-context-metrics > div:last-child,
  .transfer-journal-summary > div:last-child { border-bottom: 0; }
  .transfer-context-footer .btn-primary,
  .transfer-create-summary .btn-primary { width: 100%; }
}

@media (max-width: 560px) {
  .transfer-journal-row { grid-template-columns: 1fr; }
  .transfer-row-actions { grid-column: 1; }
  .transfer-decision { flex: 1 1 120px; }
  .transfer-route-cell > span,
  .transfer-route-cell > strong { white-space: normal; }
}
</style>
