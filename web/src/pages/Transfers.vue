<template>
  <section class="section-wrap catalog-page">
    <PublicCatalogHeader title="Трансферы" description="Переходы игроков между командами лиги." :count="!loadingSeasonData && !pageError ? totalElements : null" count-label="переходов" />
    <div class="catalog-toolbar">
      <label class="catalog-search transfer-season">
        <span>Сезон</span>
        <select v-model="selectedSeasonId" :disabled="loadingSeasons || !seasons.length">
          <option value="" v-if="!seasons.length">— сезоны не найдены —</option>
          <option v-for="item in seasons" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <span class="catalog-toolbar-note">Все статусы переходов</span>
    </div>

    <UiState
      v-if="pageError"
      tone="error"
      title="Не удалось загрузить трансферы"
      :message="pageError"
      action-label="Повторить"
      @action="loadSeasons"
    />

    <article class="catalog-surface transfer-surface">
      <div class="catalog-list-head">
        <div>
          <h2>Переходы за сезон</h2>
        </div>
        <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
      </div>

      <div class="transfer-table-head" v-if="transfers.length">
        <span>Игрок</span>
        <span>Откуда</span>
        <span>Куда</span>
        <span>Дата заявки</span>
        <span>Статус</span>
      </div>

      <div class="season-transfer-list" v-if="transfers.length">
        <article class="season-transfer-item" v-for="transfer in transfers" :key="transfer.id">
          <span class="transfer-cell transfer-player">{{ transfer.playerName }}<span v-if="transfer.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></span>
          <span class="transfer-cell transfer-team" data-label="Откуда">{{ transfer.fromTeamName }}</span>
          <span class="transfer-cell transfer-team transfer-team-target" data-label="Куда">{{ transfer.toTeamName }}</span>
          <span class="transfer-cell transfer-request-date">{{ formatDateOnly(transfer.requestedDate || transfer.requestedAt) }}</span>
          <span class="transfer-cell transfer-status-wrap">
            <span class="transfer-status-badge" :class="statusClass(transfer.status)">{{ formatTransferStatus(transfer.status) }}</span>
          </span>
        </article>
      </div>

      <div class="catalog-pagination" v-if="totalPages > 1">
        <button class="btn-ghost" type="button" @click="changePage(currentPage - 1)" :disabled="loadingSeasonData || currentPage <= 0">Назад</button>
        <span class="muted-text">Страница {{ currentPage + 1 }} из {{ totalPages }} · всего {{ totalElements }}</span>
        <button class="btn-ghost" type="button" @click="changePage(currentPage + 1)" :disabled="loadingSeasonData || currentPage + 1 >= totalPages">Вперёд</button>
      </div>

      <UiState
        v-if="!loadingSeasonData && !pageError && !transfers.length && selectedSeasonId"
        title="Трансферов пока нет"
        message="Подтвержденные и ожидающие решения переходы этого сезона появятся здесь."
      />
      <UiState
        v-else-if="!loadingSeasonData && !pageError && !selectedSeasonId"
        title="Нет активного сезона"
        message="Список трансферов станет доступен после открытия сезона."
      />
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import UiState from '../components/UiState.vue'
import PublicCatalogHeader from '../components/PublicCatalogHeader.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
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
    const payload = await catalogApi.getSeasons(1)
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
    const payload = await catalogApi.getSeasonTransfers(seasonId, pageNum, pageSize)
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
  if (status === 'PENDING') return 'На рассмотрении'
  if (status === 'APPROVED') return 'Одобрен'
  if (status === 'REJECTED') return 'Отклонён'
  if (status === 'REVOKED') return 'Отозван'
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
.transfer-season { flex: 0 1 300px; }
.transfer-table-head, .season-transfer-item { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr) 95px 130px; align-items: center; gap: 16px; padding: 0 20px; }
.transfer-table-head { min-height: 38px; background: rgba(124, 163, 255, .035); color: var(--muted); font-size: .62rem; border-bottom: 1px solid rgba(124, 163, 255, .15); }
.season-transfer-item { min-height: 62px; border-bottom: 1px solid rgba(124, 163, 255, .1); font-size: .75rem; }
.season-transfer-item:last-child { border-bottom: 0; }
.season-transfer-item:hover { background: rgba(97, 232, 162, .035); }
.transfer-cell { min-width: 0; overflow-wrap: anywhere; }
.transfer-player { font-weight: 700; }
.transfer-team { color: var(--muted); }
.transfer-team-target { color: var(--text); }
.transfer-request-date { color: var(--muted); font-size: .65rem; font-variant-numeric: tabular-nums; }
.transfer-status-badge { display: inline-flex; padding: 5px 8px; border: 1px solid rgba(124, 163, 255, .2); border-radius: 6px; font-size: .62rem; font-weight: 700; }
.transfer-status-badge.is-pending { background: rgba(243, 201, 105, .07); border-color: rgba(243, 201, 105, .23); color: #f3c969; }
.transfer-status-badge.is-approved { background: rgba(97, 232, 162, .07); border-color: rgba(97, 232, 162, .23); color: var(--brand); }
.transfer-status-badge.is-rejected { background: rgba(255, 156, 156, .07); border-color: rgba(255, 156, 156, .23); color: #ff9c9c; }
.transfer-status-badge.is-revoked { color: var(--muted); }
.transfer-surface > .catalog-pagination { padding: 14px 20px; border-top: 1px solid rgba(124, 163, 255, .12); }
@media (max-width: 860px) {
  .transfer-table-head { display: none; }
  .season-transfer-item { grid-template-columns: minmax(0, 1fr) auto; gap: 8px 12px; padding: 14px; }
  .transfer-player { grid-column: 1; grid-row: 1; }
  .transfer-status-wrap { grid-column: 2; grid-row: 1; }
  .transfer-team { grid-column: 1 / -1; display: grid; grid-template-columns: 48px minmax(0, 1fr); gap: 8px; }
  .transfer-team::before { content: attr(data-label); color: #7890bd; font-size: .63rem; }
  .transfer-request-date { grid-column: 1 / -1; }
  .transfer-season { flex: 1 1 100%; }
}
</style>
