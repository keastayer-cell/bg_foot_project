<template>
  <section class="section-wrap review-page">
    <article class="card review-header-card">
      <div class="toolbar review-header">
        <div>
          <h2 class="section-title">Заявки на сезон</h2>
          <p class="muted-text">Проверка допуска команд к сезону для рефери и администратора.</p>
        </div>
        <div class="actions-row">
          <select v-model="selectedSeasonId" class="review-season-select">
            <option value="">Все сезоны</option>
            <option v-for="season in seasons" :key="season.id" :value="String(season.id)">
              {{ season.name }}
            </option>
          </select>
          <button class="btn-ghost" type="button" @click="router.push('/admin')">В админ-панель</button>
          <button class="btn-ghost" type="button" @click="loadQueue" :disabled="loading">Обновить</button>
        </div>
      </div>
      <p v-if="pageError" class="error-text">{{ pageError }}</p>
      <p v-if="pageSuccess" class="success-text">{{ pageSuccess }}</p>
    </article>

    <article class="card review-queue-card">
      <div class="toolbar review-subhead">
        <h3 class="section-title">Очередь заявок</h3>
        <span class="muted-text">{{ queueItems.length }}</span>
      </div>

      <p v-if="loading && !queueItems.length" class="muted-text">Загрузка заявок...</p>
      <p v-else-if="!queueItems.length" class="muted-text">Заявок пока нет.</p>

      <div v-else class="review-queue-list compact">
        <article
          v-for="item in queueItems"
          :key="item.applicationId"
          class="review-queue-row"
          @click="openDetailsModal(item)"
        >
          <div class="review-queue-row-main">
            <strong>{{ item.teamName }}</strong>
            <span class="muted-text">Игроков: {{ item.playersCount }}</span>
            <span class="muted-text">Представитель: {{ item.representativeName || '—' }}</span>
            <span class="review-status-chip" :class="`review-status-chip--${item.status.toLowerCase()}`">
              {{ formatReviewStatus(item.status) }}
            </span>
          </div>

          <div v-if="canModerate(item)" class="review-queue-row-actions">
            <button
              v-if="hasRole('SUPER_ADMIN')"
              class="btn-ghost"
              type="button"
              @click.stop="openEditor(item)"
            >
              Редактировать
            </button>
            <button
              class="btn-primary"
              type="button"
              @click.stop="processDecision(item.applicationId, 'approve')"
              :disabled="!canModerate(item) || actionLoadingKey === `approve:${item.applicationId}`"
            >
              Согласовать
            </button>
            <button
              class="btn-danger"
              type="button"
              @click.stop="openRejectModal(item)"
              :disabled="!canModerate(item) || actionLoadingKey === `reject:${item.applicationId}`"
            >
              Отклонить
            </button>
          </div>
          <div v-else class="review-queue-row-actions review-queue-row-actions--status-only">
            <button
              v-if="hasRole('SUPER_ADMIN')"
              class="btn-ghost"
              type="button"
              @click.stop="openEditor(item)"
            >
              Редактировать
            </button>
          </div>
        </article>
      </div>
    </article>

    <div v-if="detailsModalOpen" class="modal-backdrop" @click.self="closeDetailsModal">
      <article class="card auth-modal review-details-modal">
        <div class="toolbar review-header">
          <div>
            <h3 class="section-title">{{ modalDetails?.teamName || 'Заявка' }}</h3>
            <p class="muted-text">{{ modalDetails?.seasonName || '—' }} · {{ modalDetails?.representativeName || 'Представитель не указан' }}</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeDetailsModal">Закрыть</button>
        </div>

        <p v-if="detailsLoading" class="muted-text">Загрузка игроков...</p>
        <p v-else-if="!modalDetails?.players?.length" class="muted-text">В заявке пока нет игроков.</p>

        <div v-else class="review-player-list">
          <article v-for="player in modalDetails.players" :key="player.id" class="review-player-row">
            <strong>{{ player.fullName }}</strong>
            <span class="muted-text">{{ player.residence || 'не указана' }}</span>
            <span class="muted-text">{{ formatBirthYear(player.birthDate) }}</span>
          </article>
        </div>
      </article>
    </div>

    <div v-if="rejectModalOpen" class="modal-backdrop" @click.self="closeRejectModal">
      <article class="card auth-modal review-reject-modal">
        <div class="toolbar review-header">
          <div>
            <h3 class="section-title">Отклонить заявку</h3>
            <p class="muted-text">
              {{ rejectTarget?.teamName || 'Команда' }} · {{ rejectTarget?.seasonName || 'Сезон' }}
            </p>
          </div>
          <button class="btn-ghost" type="button" @click="closeRejectModal">Закрыть</button>
        </div>

        <label class="review-reject-field">
          <span>Комментарий для команды</span>
          <textarea
            v-model.trim="rejectComment"
            rows="4"
            placeholder="Опишите причину отклонения"
          />
        </label>

        <p class="error-text" v-if="rejectModalError">{{ rejectModalError }}</p>

        <div class="actions-row review-reject-actions">
          <button class="btn-ghost" type="button" @click="closeRejectModal">Отмена</button>
          <button
            class="btn-danger"
            type="button"
            @click="confirmReject"
            :disabled="!rejectTarget || actionLoadingKey === `reject:${rejectTarget?.applicationId}`"
          >
            Отклонить заявку
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'

const router = useRouter()
const { authorizedApiRequest, hasRole } = useAuth()

const loading = ref(false)
const actionLoadingKey = ref('')
const pageError = ref('')
const pageSuccess = ref('')
const queueItems = ref([])
const seasons = ref([])
const selectedSeasonId = ref('')
const detailsModalOpen = ref(false)
const detailsLoading = ref(false)
const modalDetails = ref(null)
const rejectModalOpen = ref(false)
const rejectTarget = ref(null)
const rejectComment = ref('')
const rejectModalError = ref('')

function canModerate(item) {
  return item?.status === 'SUBMITTED' && (hasRole('REFEREE') || hasRole('SUPER_ADMIN'))
}

onMounted(async () => {
  await loadSeasons()
  await loadQueue()
})

watch(selectedSeasonId, async () => {
  await loadQueue()
})

async function loadSeasons() {
  try {
    const payload = await authorizedApiRequest('/api/seasons?active_flag=0', { method: 'GET' })
    seasons.value = Array.isArray(payload) ? payload : []
  } catch {
    seasons.value = []
  }
}

async function loadQueue() {
  loading.value = true
  pageError.value = ''

  try {
    const seasonQuery = selectedSeasonId.value ? `?seasonId=${encodeURIComponent(selectedSeasonId.value)}` : ''
    const payload = await authorizedApiRequest(`/api/season-applications${seasonQuery}`, { method: 'GET' })
    queueItems.value = Array.isArray(payload?.items) ? payload.items : []
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить сезонные заявки.'
  } finally {
    loading.value = false
  }
}

async function processDecision(applicationId, action) {
  const item = queueItems.value.find((candidate) => candidate.applicationId === applicationId)
  if (!item || !canModerate(item)) {
    return
  }
  const comment = action === 'reject' ? String(rejectComment.value || '').trim() : null
  if (action === 'reject' && !comment) {
    rejectModalError.value = 'Для отклонения заявки нужно указать комментарий.'
    pageError.value = 'Для отклонения заявки нужно указать комментарий.'
    return
  }

  actionLoadingKey.value = `${action}:${applicationId}`
  pageError.value = ''
  pageSuccess.value = ''

  try {
    await authorizedApiRequest(`/api/season-applications/${encodeURIComponent(applicationId)}/${action}`, {
      method: 'POST',
      body: JSON.stringify({
        decisionComment: comment,
      }),
    })
    pageSuccess.value = action === 'approve'
      ? 'Команда допущена к сезону.'
      : 'Заявка отклонена.'
    if (action === 'reject') {
      closeRejectModal()
    }
    await loadQueue()
  } catch (error) {
    if (action === 'reject') {
      rejectModalError.value = error.message || 'Не удалось отклонить сезонную заявку.'
    }
    pageError.value = error.message || 'Не удалось обработать сезонную заявку.'
  } finally {
    actionLoadingKey.value = ''
  }
}

function openRejectModal(item) {
  if (!item || !canModerate(item)) {
    return
  }
  rejectTarget.value = item
  rejectComment.value = ''
  rejectModalError.value = ''
  rejectModalOpen.value = true
}

function closeRejectModal() {
  rejectModalOpen.value = false
  rejectTarget.value = null
  rejectComment.value = ''
  rejectModalError.value = ''
}

async function confirmReject() {
  if (!rejectTarget.value?.applicationId) {
    return
  }
  await processDecision(rejectTarget.value.applicationId, 'reject')
}

async function openDetailsModal(item) {
  if (!item?.applicationId) {
    return
  }

  detailsModalOpen.value = true
  detailsLoading.value = true
  pageError.value = ''

  try {
    modalDetails.value = await authorizedApiRequest(`/api/season-applications/${encodeURIComponent(item.applicationId)}`, {
      method: 'GET',
    })
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить состав заявки.'
    detailsModalOpen.value = false
    modalDetails.value = null
  } finally {
    detailsLoading.value = false
  }
}

function openEditor(item) {
  if (!hasRole('SUPER_ADMIN') || !item?.teamId || !item?.seasonId) {
    return
  }
  router.push({
    path: '/team-rep-dashboard',
    query: {
      teamId: String(item.teamId),
      seasonId: String(item.seasonId),
    },
  })
}

function closeDetailsModal() {
  detailsModalOpen.value = false
  detailsLoading.value = false
  modalDetails.value = null
}

function formatReviewStatus(status) {
  if (status === 'APPROVED') return 'Согласована'
  if (status === 'SUBMITTED') return 'На согласовании'
  return status || '—'
}

function formatBirthYear(value) {
  if (!value) return 'не указан'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'не указан'
  return String(date.getFullYear())
}
</script>

<style scoped>
.review-page {
  display: grid;
  gap: 16px;
}

.review-season-select {
  min-width: 220px;
}

.review-header {
  align-items: start;
}

.review-queue-list {
  display: grid;
  gap: 10px;
}

.review-queue-list.compact {
  gap: 12px;
}

.review-queue-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.03);
  cursor: pointer;
}

.review-queue-row-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex-wrap: wrap;
}

.review-queue-row-actions,
.review-subhead {
  display: flex;
  gap: 8px;
  align-items: center;
}

.review-queue-row-actions {
  justify-content: flex-end;
}

.review-queue-row-actions--status-only {
  min-width: 180px;
  justify-content: flex-end;
}

.review-reject-modal {
  width: min(560px, calc(100vw - 32px));
}

.review-reject-field {
  display: grid;
  gap: 8px;
}

.review-reject-field textarea {
  min-height: 120px;
  resize: vertical;
}

.review-reject-actions {
  justify-content: flex-end;
}

.review-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 0.92rem;
  font-weight: 700;
  border: 1px solid rgba(255,255,255,0.12);
}

.review-status-chip--submitted {
  color: #f3e7a5;
  background: rgba(181, 140, 16, 0.18);
}

.review-status-chip--approved {
  color: #9cf0c8;
  background: rgba(34, 122, 82, 0.22);
}

.review-comment-input {
  width: 320px;
  max-width: 100%;
}

.review-details-modal {
  width: min(760px, calc(100vw - 24px));
}

.review-player-list {
  display: grid;
  gap: 12px;
}

.review-player-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(220px, 1fr) minmax(180px, 0.8fr);
  gap: 16px;
  align-items: center;
  padding: 10px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
}

@media (max-width: 960px) {
  .review-queue-row {
    grid-template-columns: 1fr;
  }

  .review-queue-row-actions {
    justify-content: stretch;
    flex-wrap: wrap;
  }

  .review-comment-input {
    width: 100%;
  }

  .review-player-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}

@media (max-width: 640px) {
  .review-season-select {
    min-width: 0;
    width: 100%;
  }

  .review-queue-row {
    gap: 12px;
    padding: 12px;
  }

  .review-queue-row-actions,
  .review-reject-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .review-queue-row-actions > *,
  .review-reject-actions > * {
    width: 100%;
  }

  .review-details-modal,
  .review-reject-modal {
    width: calc(100vw - 20px);
  }
}
</style>