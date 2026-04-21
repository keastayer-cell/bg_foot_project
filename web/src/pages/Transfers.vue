<template>
  <section class="section-wrap home-page">
    <article class="card home-hero">
      <div class="home-hero-head">
        <h1 class="section-title home-title">Трансферы</h1>
        <label class="season-box season-box-wide">
          <span>Сезон</span>
          <select v-model="selectedSeasonId" :disabled="loadingSeasons || !seasons.length">
            <option value="" v-if="!seasons.length">— сезоны не найдены —</option>
            <option v-for="item in seasons" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <p v-if="pageError" class="error-text">{{ pageError }}</p>
    </article>

    <article class="card player-stats-card">
      <div class="section-head player-stats-head">
        <div>
          <h2 class="section-title">Список трансферов сезона</h2>
        </div>
        <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
      </div>

      <div class="transfer-table-head" v-if="transfers.length">
        <span>Куда переходит</span>
        <span>ФИО</span>
        <span>Клуб откуда</span>
        <span>Дата перехода</span>
        <span>Статус трансфера</span>
      </div>

      <div class="season-transfer-list" v-if="transfers.length">
        <article class="season-transfer-item" v-for="transfer in transfers" :key="transfer.id">
          <span class="transfer-cell transfer-team transfer-team-target">{{ transfer.toTeamName }}</span>
          <span class="transfer-cell transfer-player">{{ transfer.playerName }}<span v-if="transfer.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></span>
          <span class="transfer-cell transfer-team">{{ transfer.fromTeamName }}</span>
          <span class="transfer-cell transfer-request-date">{{ formatDateOnly(transfer.requestedDate || transfer.requestedAt) }}</span>
          <span class="transfer-cell transfer-status-wrap">
            <span class="transfer-status-badge" :class="statusClass(transfer.status)">{{ formatTransferStatus(transfer.status) }}</span>
          </span>
        </article>
      </div>

      <div class="pagination-bar" v-if="totalPages > 1">
        <button class="btn-ghost" type="button" @click="changePage(currentPage - 1)" :disabled="loadingSeasonData || currentPage <= 0">Назад</button>
        <span class="muted-text">Страница {{ currentPage + 1 }} из {{ totalPages }} · всего {{ totalElements }}</span>
        <button class="btn-ghost" type="button" @click="changePage(currentPage + 1)" :disabled="loadingSeasonData || currentPage + 1 >= totalPages">Вперёд</button>
      </div>

      <div v-else-if="!loadingSeasonData" class="transfer-list-empty" aria-hidden="true"></div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'

const { optionalAuthApiRequest } = useAuth()
const pageSize = 20

const seasons = ref([])
const selectedSeasonId = ref('')
const transfers = ref([])
const loadingSeasons = ref(false)
const loadingSeasonData = ref(false)
const pageError = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

watch(selectedSeasonId, async (seasonId) => {
  if (!seasonId) {
    transfers.value = []
    currentPage.value = 0
    totalPages.value = 0
    totalElements.value = 0
    return
  }

  currentPage.value = 0
  await loadSeasonData(seasonId, 0)
})

async function loadSeasons() {
  loadingSeasons.value = true
  pageError.value = ''

  try {
    const payload = await optionalAuthApiRequest('/api/seasons?active_flag=1', { method: 'GET' })
    seasons.value = Array.isArray(payload)
      ? payload.filter((item) => String(item?.status || '') === 'ACTIVE')
      : []
    if (!selectedSeasonId.value && seasons.value.length) {
      selectedSeasonId.value = String(seasons.value[0].id)
    }
  } catch (error) {
    seasons.value = []
    pageError.value = error.message || 'Не удалось загрузить список сезонов.'
  } finally {
    loadingSeasons.value = false
  }
}

async function loadSeasonData(seasonId) {
  return loadSeasonDataPage(seasonId, currentPage.value)
}

async function loadSeasonDataPage(seasonId, pageNum) {
  loadingSeasonData.value = true
  pageError.value = ''

  try {
    const payload = await optionalAuthApiRequest(
      `/api/seasons/${encodeURIComponent(seasonId)}/transfers?pagenum=${pageNum}&pagesize=${pageSize}`,
      { method: 'GET' }
    )
    transfers.value = Array.isArray(payload?.content) ? payload.content : []
    currentPage.value = Number(payload?.number || 0)
    totalPages.value = Number(payload?.totalPages || 0)
    totalElements.value = Number(payload?.totalElements || 0)
  } catch (error) {
    transfers.value = []
    currentPage.value = 0
    totalPages.value = 0
    totalElements.value = 0
    pageError.value = error.message || 'Не удалось загрузить трансферы выбранного сезона.'
  } finally {
    loadingSeasonData.value = false
  }
}

async function changePage(pageNum) {
  if (!selectedSeasonId.value) return
  if (pageNum < 0) return
  if (totalPages.value && pageNum >= totalPages.value) return
  await loadSeasonDataPage(selectedSeasonId.value, pageNum)
}

function formatTransferStatus(status) {
  if (status === 'PENDING') return 'Трансфер запрошен'
  if (status === 'APPROVED') return 'Трансфер одобрен'
  if (status === 'REJECTED') return 'Трансфер отклонен'
  if (status === 'REVOKED') return 'Трансфер отозван'
  return status || '—'
}

function statusClass(status) {
  if (status === 'PENDING') return 'is-pending'
  if (status === 'APPROVED') return 'is-approved'
  if (status === 'REJECTED') return 'is-rejected'
  if (status === 'REVOKED') return 'is-revoked'
  return ''
}

function formatDateOnly(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

onMounted(async () => {
  await loadSeasons()
})
</script>

<style scoped>
.home-hero {
  display: grid;
  gap: 18px;
}

.home-hero-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.home-title {
  margin-bottom: 0;
}

.season-box-wide {
  min-width: 260px;
}

.season-transfer-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transfer-table-head,
.season-transfer-item {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1.2fr) minmax(0, 1.15fr) 140px 180px;
  gap: 12px;
  align-items: center;
}

.transfer-table-head {
  margin-top: 18px;
  padding: 0 14px 6px;
  color: rgba(241, 244, 255, 0.88);
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 1.2;
}

.season-transfer-item {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.transfer-cell {
  min-width: 0;
}

.transfer-team {
  color: var(--text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-player {
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-status-wrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.transfer-request-date {
  color: rgba(231, 236, 255, 0.78);
  font-size: 0.9rem;
  white-space: nowrap;
}

.transfer-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  font-size: 0.8rem;
  font-weight: 700;
  white-space: nowrap;
}

.transfer-status-badge.is-pending {
  background: rgba(218, 165, 32, 0.15);
  border-color: rgba(218, 165, 32, 0.45);
  color: #f3c969;
}

.transfer-status-badge.is-approved {
  background: rgba(62, 166, 106, 0.15);
  border-color: rgba(62, 166, 106, 0.45);
  color: #7be0a0;
}

.transfer-status-badge.is-rejected {
  background: rgba(196, 74, 74, 0.15);
  border-color: rgba(196, 74, 74, 0.45);
  color: #ff9c9c;
}

.transfer-status-badge.is-revoked {
  background: rgba(135, 145, 170, 0.16);
  border-color: rgba(135, 145, 170, 0.42);
  color: rgba(231, 236, 255, 0.82);
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.transfer-list-empty {
  min-height: 8px;
}

@media (max-width: 860px) {
  .season-box-wide {
    width: 100%;
    min-width: 0;
  }

  .season-box-wide select {
    width: 100%;
  }

  .home-hero-head {
    align-items: start;
  }

  .transfer-table-head {
    display: none;
  }

  .season-transfer-item {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 12px;
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
    flex-wrap: wrap;
  }
}

@media (max-width: 560px) {
  .pagination-bar > * {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .season-box-wide {
    width: 100%;
    min-width: 0;
  }

  .season-box-wide select {
    width: 100%;
  }
}
</style>