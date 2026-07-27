<template>
  <section :class="embedded ? 'team-rep-transfers-embedded' : 'section-wrap team-rep-page team-rep-transfers-page'">
    <article v-if="!embedded" class="card team-rep-profile-card">
      <div class="toolbar team-rep-card-head">
        <div>
          <h2 class="section-title">Управление трансферами внутри сезона</h2>
        </div>
        <div class="actions-row">
          <button v-if="hasRole('TEAM_REP')" class="btn-ghost" type="button" @click="router.push('/team-rep-dashboard')">К заявке сезона</button>
          <button class="btn-primary" type="button" @click="openTransferRequestModal" :disabled="!selectedSeasonId || !canOpenTransferRequestModal">Заявка на трансфер</button>
          <button class="btn-ghost" type="button" @click="loadSeasons" :disabled="seasonLoading || overviewLoading">Обновить</button>
        </div>
      </div>

      <p class="error-text" v-if="pageError">{{ pageError }}</p>
      <p class="success-text" v-if="pageSuccess">{{ pageSuccess }}</p>

      <div class="team-rep-form">
        <label>
          Выберите сезон
          <select v-model="selectedSeasonId">
            <option value="">— выберите —</option>
            <option v-for="season in teamSeasons" :key="season.id" :value="String(season.id)">
              {{ season.name }}
            </option>
          </select>
        </label>
      </div>

      <div v-if="overview" class="team-rep-badge-row team-rep-transfer-badges">
        <span class="team-rep-season-chip">Статус: {{ formatSeasonStatus(overview.seasonStatus) }}</span>
        <span class="team-rep-season-chip" :class="overview.transferWindowOpen ? 'team-rep-season-chip-open' : 'team-rep-season-chip-closed'">
          {{ overview.transferWindowOpen ? 'Трансферы открыты' : 'Трансферы закрыты' }}
        </span>
        <span class="team-rep-season-chip" v-if="overview.maxRosterSize">
          Лимит заявки: {{ overview.selectedPlayersCount }} / {{ overview.maxRosterSize }}
        </span>
        <span class="team-rep-season-chip" v-else>
          В заявке: {{ overview.selectedPlayersCount }}
        </span>
      </div>
    </article>

    <article class="card team-rep-players-card" v-if="overview">
      <div class="toolbar team-rep-card-head">
        <div>
          <h3 class="section-title">Заявки и история</h3>
          <p class="muted-text">Компактный список заявок по выбранному сезону.</p>
        </div>
      </div>

      <p v-if="overviewLoading" class="muted-text">Загрузка трансферов...</p>
      <p v-else-if="!overview.requests.length" class="muted-text">По выбранному сезону пока нет трансферных заявок.</p>

      <div v-else>
        <div class="transfer-list-head team-rep-transfer-list-head">
          <span>Куда переходит</span>
          <span>ФИО</span>
          <span>Клуб откуда</span>
          <span>Дата заявки</span>
          <span>Статус трансфера</span>
          <span>Действие</span>
        </div>

        <div class="team-rep-transfer-list">
          <article v-for="request in overview.requests" :key="request.id" class="team-rep-transfer-item team-rep-transfer-row">
            <span class="transfer-cell transfer-team">{{ request.toTeamName }}</span>
            <span class="transfer-cell transfer-player">{{ request.playerName }}<span v-if="request.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></span>
            <span class="transfer-cell transfer-team">{{ request.fromTeamName }}</span>
            <span class="transfer-cell transfer-request-date">{{ formatDateOnly(request.requestedAt) }}</span>
            <span class="transfer-cell transfer-status-wrap">
              <span class="team-rep-season-chip" :class="statusChipClass(request.status)">{{ formatTransferStatus(request.status) }}</span>
            </span>
            <span class="transfer-cell transfer-action-cell">
              <span class="transfer-action-slot transfer-action-slot-left">
                <button
                  v-if="request.canApprove"
                  class="transfer-action-btn transfer-action-btn-approve"
                  type="button"
                  @click="processTransferAction(request.id, 'approve')"
                  :disabled="transferActionLoadingKey === `approve:${request.id}` || overviewLoading"
                  title="Подтвердить трансфер"
                  aria-label="Подтвердить трансфер"
                >
                  {{ transferActionLoadingKey === `approve:${request.id}` ? '...' : 'OK' }}
                </button>
                <span v-else class="transfer-action-placeholder" aria-hidden="true"></span>
              </span>
              <span class="transfer-action-slot transfer-action-slot-center">
                <button
                  v-if="request.canReject"
                  class="transfer-action-btn transfer-action-btn-reject"
                  type="button"
                  @click="processTransferAction(request.id, 'reject')"
                  :disabled="transferActionLoadingKey === `reject:${request.id}` || overviewLoading"
                  title="Отклонить трансфер"
                  aria-label="Отклонить трансфер"
                >
                  {{ transferActionLoadingKey === `reject:${request.id}` ? '...' : 'NO' }}
                </button>
                <span v-else class="transfer-action-placeholder" aria-hidden="true"></span>
              </span>
              <span class="transfer-action-slot transfer-action-slot-right">
                <button
                  v-if="request.canRevoke"
                  class="transfer-action-btn transfer-action-btn-revoke"
                  type="button"
                  @click="processTransferAction(request.id, 'revoke')"
                  :disabled="transferActionLoadingKey === `revoke:${request.id}` || overviewLoading"
                  title="Отозвать трансфер"
                  aria-label="Отозвать трансфер"
                >
                  {{ transferActionLoadingKey === `revoke:${request.id}` ? '...' : 'X' }}
                </button>
                <span v-else class="transfer-action-placeholder" aria-hidden="true"></span>
              </span>
            </span>
          </article>
        </div>

        <div class="pagination-bar" v-if="overview.totalPages > 1">
          <button class="btn-ghost" type="button" @click="changeTransfersPage(overview.pageNumber - 1)" :disabled="overviewLoading || overview.pageNumber <= 0">Назад</button>
          <span class="muted-text">Страница {{ overview.pageNumber + 1 }} из {{ overview.totalPages }} · всего {{ overview.totalElements }}</span>
          <button class="btn-ghost" type="button" @click="changeTransfersPage(overview.pageNumber + 1)" :disabled="overviewLoading || overview.pageNumber + 1 >= overview.totalPages">Вперёд</button>
        </div>
      </div>
    </article>

    <div v-if="transferRequestModalOpen" class="modal-backdrop" @click.self="closeTransferRequestModal">
      <article class="card auth-modal team-rep-modal team-rep-season-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Заявка на трансфер</h3>
            <p v-if="overview" class="muted-text">{{ overview.seasonName }} · {{ overview.teamName }}</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeTransferRequestModal">Закрыть</button>
        </div>

        <div class="team-rep-form team-rep-transfer-form">
          <label v-if="overview?.privilegedAccess">
            Команда назначения
            <select v-model="targetTeamId" :disabled="!overview?.transferWindowOpen">
              <option value="">— выберите —</option>
              <option v-for="team in availableTargetTeams" :key="team.id" :value="String(team.id)">
                {{ team.name }}
              </option>
            </select>
          </label>

          <label>
            Команда, из которой переводим игрока
            <select v-model="sourceTeamId" :disabled="!canPickSourceTeam">
              <option value="">— выберите —</option>
              <option v-for="team in availableSourceTeams" :key="team.id" :value="String(team.id)">
                {{ team.name }}
              </option>
            </select>
          </label>

          <label>
            Игрок
            <SearchableSelect
              :key="`transfer-player-${selectedSeasonId}-${sourceTeamId}-${candidateOptions.length}`"
              v-model="selectedPlayerId"
              :options="candidateOptions"
              placeholder="Выберите игрока"
              search-placeholder="Начните вводить ФИО игрока"
              empty-text="Игрок по такому ФИО не найден"
              :disabled="!canPickSourceTeam || !sourceTeamId || candidatesLoading"
            />
          </label>

          <label>
            Комментарий к заявке
            <textarea v-model.trim="requestComment" rows="3" placeholder="Необязательно"></textarea>
          </label>

          <div class="actions-row">
            <button class="btn-primary" type="button" @click="submitTransferRequest" :disabled="!canSubmitTransferRequest || createLoading">
              {{ createLoading ? 'Отправляем...' : 'Отправить заявку' }}
            </button>
          </div>
        </div>

        <p v-if="!overview?.transferWindowOpen" class="muted-text">Создание заявок недоступно, пока сезон не активен или окно трансферов закрыто.</p>
        <p v-else-if="!overview?.privilegedAccess && overview?.maxRosterSize && overview.selectedPlayersCount >= overview.maxRosterSize" class="muted-text">
          Лимит заявки уже достигнут. Новые входящие трансферы не будут подтверждены, пока не освободится место.
        </p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import SearchableSelect from '../components/SearchableSelect.vue'
import { useAuth } from '../store/auth'
import { createTeamRepTransfersApi } from '../api/teamRepTransfers'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
})

const { isAuthenticated, hasRole, loadCurrentUser, authorizedApiRequest } = useAuth()
const transfersApi = createTeamRepTransfersApi(authorizedApiRequest)
const router = useRouter()
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

function formatTransferWindow(startDate, endDate) {
  if (!startDate && !endDate) return 'без ограничений'
  if (startDate && endDate) return `${formatDateOnly(startDate)} - ${formatDateOnly(endDate)}`
  if (startDate) return `с ${formatDateOnly(startDate)}`
  return `до ${formatDateOnly(endDate)}`
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
</style>
