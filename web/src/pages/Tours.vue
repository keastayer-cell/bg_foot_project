<template>
  <section class="section-wrap home-page">
    <article class="card home-hero">
      <div class="home-hero-head">
        <div>
          <p class="eyebrow">Сезонный обзор</p>
          <h1 class="section-title home-title">Туры и таблица сезона</h1>
        </div>

        <label class="season-box season-box-wide">
          <span>Сезон</span>
          <select v-model="selectedSeasonId" :disabled="loadingSeasons || !seasons.length">
            <option value="" v-if="!seasons.length">— сезоны не найдены —</option>
            <option v-for="item in seasons" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <div class="season-meta-grid" v-if="selectedSeason">
        <article class="season-meta-card">
          <span class="season-meta-label">Кругов</span>
          <strong>{{ selectedSeason.roundsCount }}</strong>
        </article>
        <article class="season-meta-card">
          <span class="season-meta-label">Команд в сезоне</span>
          <strong>{{ seasonTeams.length }}</strong>
        </article>
        <article class="season-meta-card">
          <span class="season-meta-label">Туров в регулярке</span>
          <strong>{{ selectedSeason.regularToursCount }}</strong>
        </article>
        <article class="season-meta-card">
          <span class="season-meta-label">Плей-офф</span>
          <strong>{{ playoffLabel }}</strong>
        </article>
      </div>

      <p v-if="pageError" class="error-text">{{ pageError }}</p>
    </article>

    <div class="home-main-grid" :class="{ 'home-main-grid-wide': seasonViewMode === 'matrix' || seasonViewMode === 'playoff' }">
      <article class="card standings-card">
        <div class="section-head standings-head">
          <div>
            <h2 class="section-title">{{ mainPanelTitle }}</h2>
          </div>
          <div class="standings-toolbar">
            <div class="standings-toolbar-actions" v-if="selectedSeason">
              <button
                v-if="seasonViewMode !== 'playoff'"
                class="btn-ghost toolbar-stats-button"
                type="button"
                :class="{ 'is-active': sidePanelMode === 'stats' && seasonViewMode !== 'matrix' }"
                @click="openStatsPanel"
              >
                {{ sidePanelMode === 'stats' && seasonViewMode !== 'matrix' ? 'Туры' : 'Статистика' }}
              </button>
              <div class="view-switcher">
                <button
                  class="btn-ghost"
                  type="button"
                  :class="{ 'is-active': seasonViewMode === 'table' }"
                  @click="seasonViewMode = 'table'"
                >
                  Таблица
                </button>
                <button
                  class="btn-ghost"
                  type="button"
                  :class="{ 'is-active': seasonViewMode === 'matrix' }"
                  @click="seasonViewMode = 'matrix'"
                >
                  Шахматка
                </button>
                <button
                  class="btn-ghost"
                  type="button"
                  :class="{ 'is-active': seasonViewMode === 'playoff' }"
                  :disabled="!selectedSeason?.playoffEnabled"
                  @click="seasonViewMode = 'playoff'"
                >
                  Сетка
                </button>
              </div>
            </div>
            <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
            <span class="muted-text" v-else-if="standingsConfig?.lastCalculatedAt">Обновлено: {{ formatMatchDateTime(standingsConfig.lastCalculatedAt) }}</span>
          </div>
        </div>

        <SeasonPlayoffBracket
          v-if="seasonViewMode === 'playoff' && selectedSeason?.playoffEnabled && playoffBracketColumns.length"
          :left-columns="playoffLeftColumns"
          :right-columns="playoffRightColumns"
          :center-cards="playoffCenterCards"
          :season-id="selectedSeasonId"
        />

        <SeasonStandingsTables
          v-else-if="seasonViewMode === 'table' && seasonStandings.length"
          :standings="seasonStandings"
          :season-id="selectedSeasonId"
        />

        <SeasonMatrix
          v-else-if="seasonViewMode === 'matrix' && matrixTeams.length"
          :teams="matrixTeams"
          :rows="matrixRows"
          :season-id="selectedSeasonId"
        />

        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'playoff' && !selectedSeason?.playoffEnabled">Для этого сезона плей-офф выключен.</p>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'playoff'">Сетка плей-офф пока не заполнена матчами, но формат сезона уже учтен.</p>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'table'">Подтвержденных матчей в опубликованных турах пока нет.</p>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'matrix'">В выбранном сезоне пока нет данных для шахматки.</p>
      </article>

      <SeasonToursCard
        v-if="seasonViewMode !== 'matrix' && seasonViewMode !== 'playoff' && sidePanelMode === 'tours'"
        :tours="formattedTours"
        :selected-season="selectedSeason"
        :loading="loadingSeasonData"
        :initial-tour-id="requestedTourId"
      />

      <SeasonPlayerStatsCard
        v-else-if="seasonViewMode !== 'playoff' && sidePanelMode === 'stats'"
        v-model:mode="statsMode"
        :selected-season="selectedSeason"
        :loading="loadingSeasonData"
        :rows="topStatsRows"
        :empty-text="statsEmptyText"
      />

    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SeasonMatrix from '../components/tours/SeasonMatrix.vue'
import SeasonPlayerStatsCard from '../components/tours/SeasonPlayerStatsCard.vue'
import SeasonPlayoffBracket from '../components/tours/SeasonPlayoffBracket.vue'
import SeasonStandingsTables from '../components/tours/SeasonStandingsTables.vue'
import SeasonToursCard from '../components/tours/SeasonToursCard.vue'
import { useSeasonMatrix } from '../composables/useSeasonMatrix'
import { useSeasonPlayerStats } from '../composables/useSeasonPlayerStats'
import { useSeasonPlayoff } from '../composables/useSeasonPlayoff'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
const route = useRoute()
const navigationState = window.history.state || {}
const requestedViewValue = String(navigationState.view || route.query.view || '')
const requestedView = ['table', 'matrix', 'playoff'].includes(requestedViewValue)
  ? requestedViewValue
  : 'table'
const requestedSeasonId = String(navigationState.seasonId || route.query.season || '')
const requestedTourId = String(navigationState.tourId || route.query.tour || '')

const seasons = ref([])
const selectedSeasonId = ref('')
const seasonTeams = ref([])
const seasonTours = ref([])
const seasonStandings = ref([])
const seasonPlayerStats = ref([])
const playoffBracket = ref(null)
const standingsConfig = ref(null)
const seasonViewMode = ref(requestedView)
const sidePanelMode = ref('tours')
const statsMode = ref('scorers')
const loadingSeasons = ref(false)
const loadingSeasonData = ref(false)
const pageError = ref('')
const { topStatsRows, statsEmptyText } = useSeasonPlayerStats(seasonPlayerStats, statsMode)

const selectedSeason = computed(() => {
  return seasons.value.find((item) => String(item.id) === String(selectedSeasonId.value)) || null
})
const { matrixRows, matrixTeams, teamPositionMap } = useSeasonMatrix({
  season: selectedSeason,
  teams: seasonTeams,
  tours: seasonTours,
  standings: seasonStandings,
})
const {
  bracketColumns: playoffBracketColumns,
  centerCards: playoffCenterCards,
  leftColumns: playoffLeftColumns,
  rightColumns: playoffRightColumns,
} = useSeasonPlayoff({
  bracket: playoffBracket,
  season: selectedSeason,
  tours: seasonTours,
  teamPositionMap,
})

const mainPanelTitle = computed(() => {
  if (seasonViewMode.value === 'matrix') return 'Шахматка сезона'
  if (seasonViewMode.value === 'playoff') return 'Сетка'
  return 'Таблица сезона'
})

const playoffLabel = computed(() => {
  if (!selectedSeason.value?.playoffEnabled) return 'Нет'
  return `${selectedSeason.value.playoffTeamCount || '—'} команд${selectedSeason.value.thirdPlaceEnabled ? ' · 3 место' : ''}`
})

const formattedTours = computed(() => {
  return [...seasonTours.value]
    .map((tour) => ({
      ...tour,
      matches: [...(Array.isArray(tour.matches) ? tour.matches : [])].sort((left, right) => {
        const leftTime = new Date(left.kickoffAt || 0).getTime()
        const rightTime = new Date(right.kickoffAt || 0).getTime()
        return leftTime - rightTime || Number(left.id) - Number(right.id)
      }),
    }))
    .map((tour) => ({
      ...tour,
      matchesCount: tour.matches.length,
    }))
    .sort((left, right) => {
      const leftOrder = Number(left.sortOrder || 0)
      const rightOrder = Number(right.sortOrder || 0)
      if (leftOrder !== rightOrder) return leftOrder - rightOrder

      const leftRound = Number(left.roundNumber || 0)
      const rightRound = Number(right.roundNumber || 0)
      if (leftRound !== rightRound) return leftRound - rightRound

      return Number(left.id) - Number(right.id)
    })
})

watch(selectedSeason, (season) => {
  if (!season?.playoffEnabled && seasonViewMode.value === 'playoff') {
    seasonViewMode.value = 'table'
  }
})

watch(selectedSeasonId, async (seasonId) => {
  sidePanelMode.value = 'tours'
  statsMode.value = 'scorers'
  if (!seasonId) {
    seasonTeams.value = []
    seasonTours.value = []
    seasonStandings.value = []
    seasonPlayerStats.value = []
    playoffBracket.value = null
    standingsConfig.value = null
    return
  }

  await loadSeasonData(seasonId)
})

function resetError() {
  pageError.value = ''
}

function openStatsPanel() {
  if (seasonViewMode.value === 'matrix') {
    seasonViewMode.value = 'table'
  }
  sidePanelMode.value = sidePanelMode.value === 'stats' ? 'tours' : 'stats'
}

async function loadSeasons() {
  loadingSeasons.value = true
  resetError()

  try {
    const payload = await catalogApi.getSeasons(1)
    seasons.value = Array.isArray(payload)
      ? payload.filter((item) => String(item?.status || '') === 'ACTIVE')
      : []
    if (seasons.value.length) {
      const requestedSeason = seasons.value.find(
        (season) => String(season.id) === requestedSeasonId,
      )
      selectedSeasonId.value = String(requestedSeason?.id || seasons.value[0].id)
    }
  } catch (error) {
    seasons.value = []
    pageError.value = error.message || 'Не удалось загрузить список сезонов.'
  } finally {
    loadingSeasons.value = false
  }
}

async function loadSeasonData(seasonId) {
  loadingSeasonData.value = true
  resetError()

  try {
    const [overviewPayload, playerStatsPayload] = await Promise.all([
      catalogApi.getSeasonOverview(seasonId),
      catalogApi.getSeasonPlayerStats(seasonId),
    ])

    seasonTeams.value = Array.isArray(overviewPayload?.teams) ? overviewPayload.teams : []
    seasonTours.value = Array.isArray(overviewPayload?.tours) ? overviewPayload.tours : []
    seasonStandings.value = Array.isArray(overviewPayload?.standings) ? overviewPayload.standings : []
    seasonPlayerStats.value = Array.isArray(playerStatsPayload) ? playerStatsPayload : []
    playoffBracket.value = overviewPayload?.playoffBracket || null
    standingsConfig.value = overviewPayload?.standingsConfig || null
  } catch (error) {
    seasonTeams.value = []
    seasonTours.value = []
    seasonStandings.value = []
    seasonPlayerStats.value = []
    playoffBracket.value = null
    standingsConfig.value = null
    pageError.value = error.message || 'Не удалось загрузить данные выбранного сезона.'
  } finally {
    loadingSeasonData.value = false
  }
}

function formatMatchDateTime(value) {
  if (!value) return 'Дата не указана'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Дата не указана'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}


onMounted(async () => {
  await loadSeasons()
})
</script>

<style>
.home-page {
  width: 100%;
  max-width: 1680px;
  margin: 0 auto;
}

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

.eyebrow {
  margin: 0 0 8px;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
  opacity: 0.82;
}

.home-title {
  margin-bottom: 8px;
}

.home-subtitle {
  max-width: 620px;
}

.season-box-wide {
  min-width: 260px;
}

.standings-table-desktop {
  display: table;
}

.standings-table-mobile {
  display: none;
}

.season-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.standings-card {
  display: grid;
  gap: 14px;
  align-self: start;
  height: fit-content;
}

.tours-card,
.player-stats-card {
  display: grid;
  gap: 14px;
  align-self: start;
  height: fit-content;
}

.player-stats-head {
  margin-bottom: 0;
}

.player-stats-tabs {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.player-stats-tab {
  min-width: 132px;
}

.player-stats-subtitle {
  margin: 6px 0 0;
}

.player-stats-table-wrap {
  width: 100%;
}


.player-stats-table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.player-stats-rank {
  width: 44px;
  text-align: center;
}

.player-stats-name,
.player-stats-team {
  display: block;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-stats-table-scorers th:nth-child(1),
.player-stats-table-scorers td:nth-child(1) {
  width: 44px;
}

.player-stats-table-scorers th:nth-child(3),
.player-stats-table-scorers td:nth-child(3) {
  width: 28%;
}

.player-stats-table-scorers th:nth-child(4),
.player-stats-table-scorers td:nth-child(4) {
  width: 64px;
  text-align: center;
}

.player-stats-table-discipline th:nth-child(1),
.player-stats-table-discipline td:nth-child(1) {
  width: 44px;
}

.player-stats-table-discipline th:nth-child(3),
.player-stats-table-discipline td:nth-child(3) {
  width: 24%;
}

.player-stats-table-discipline th:nth-child(4),
.player-stats-table-discipline td:nth-child(4),
.player-stats-table-discipline th:nth-child(5),
.player-stats-table-discipline td:nth-child(5) {
  width: 56px;
  text-align: center;
}

.player-stats-yellow {
  color: #f3c34d;
  font-weight: 700;
}

.player-stats-red {
  color: #ff7d7d;
  font-weight: 700;
}

.standings-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  column-gap: 18px;
  row-gap: 12px;
}

.standings-head > div:first-child {
  min-width: 0;
}

.standings-rules-text {
  margin: 8px 0 0;
}

.standings-toolbar {
  display: grid;
  gap: 10px;
  min-width: max-content;
  justify-items: end;
}

.standings-toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.view-switcher {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.view-switcher .btn-ghost.is-active {
  border-color: rgba(97, 232, 162, 0.28);
  background: rgba(97, 232, 162, 0.12);
  color: var(--text);
}

.view-switcher .btn-ghost:disabled {
  opacity: 0.46;
  cursor: not-allowed;
}

.toolbar-stats-button.is-active,
.player-stats-tab.is-active {
  border-color: rgba(97, 232, 162, 0.28);
  background: rgba(97, 232, 162, 0.12);
  color: var(--text);
}

.season-meta-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
  display: grid;
  gap: 4px;
}

.season-meta-label {
  font-size: 0.76rem;
  color: var(--muted);
}

.season-meta-card strong {
  font-size: 1.45rem;
  line-height: 1.1;
}

.home-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  align-items: start;
  gap: 24px;
}

.home-main-grid-wide {
  grid-template-columns: minmax(0, 1fr);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.team-link {
  color: inherit;
  text-decoration: none;
  font-weight: 700;
}

.team-link:hover {
  color: rgba(151, 233, 194, 0.96);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.tour-list {
  display: grid;
  gap: 10px;
  align-content: start;
}

.tour-block {
  display: grid;
  gap: 10px;
}

.tour-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
}

.tour-card-button {
  width: 100%;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.tour-date {
  margin: 8px 0 0;
  font-size: 0.82rem;
  color: var(--muted);
}

.tour-main h3 {
  margin: 0 0 6px;
  font-size: 0.98rem;
}

.tour-main p {
  margin: 0;
}

.tour-side {
  display: grid;
  justify-items: end;
  gap: 8px;
}

.tour-match-count {
  font-size: 0.78rem;
  color: var(--muted);
}

.tour-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  border: 1px solid rgba(97, 232, 162, 0.2);
  font-size: 0.78rem;
  font-weight: 700;
}

.tours-card-head {
  margin-bottom: 0;
}

.tours-card-subtitle {
  margin: 6px 0 0;
}

.tour-navigator {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 12px;
}

.tour-select-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.tour-select-field > span {
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 700;
}

.tour-select-field select {
  width: 100%;
  min-width: 0;
}

.tour-stepper {
  display: grid;
  grid-template-columns: 38px minmax(64px, auto) 38px;
  align-items: center;
  gap: 6px;
}

.tour-stepper strong {
  text-align: center;
  white-space: nowrap;
  font-size: 0.78rem;
}

.tour-step-button {
  min-width: 38px;
  padding: 8px;
}

.tour-step-button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.selected-tour-card {
  display: grid;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(126, 191, 255, 0.18);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(66, 94, 157, 0.13), rgba(255, 255, 255, 0.025));
}

.selected-tour-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.selected-tour-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.selected-tour-head h3 {
  margin: 10px 0 5px;
  font-size: 1.15rem;
}

.selected-tour-head p {
  margin: 0;
}

.selected-tour-progress {
  display: grid;
  justify-items: end;
  gap: 3px;
  min-width: max-content;
}

.selected-tour-progress strong {
  font-size: 1.12rem;
}

.selected-tour-progress span {
  font-size: 0.72rem;
}

.tour-state-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
}

.tour-state-badge.is-complete {
  color: #c8d1e8;
  border-color: rgba(200, 209, 232, 0.16);
  background: rgba(200, 209, 232, 0.08);
}

.tour-state-badge.is-upcoming {
  color: #8ee8ba;
  border-color: rgba(97, 232, 162, 0.22);
  background: rgba(97, 232, 162, 0.1);
}

.tour-state-badge.is-live {
  color: #ffcf74;
  border-color: rgba(255, 184, 76, 0.25);
  background: rgba(255, 184, 76, 0.1);
}

.selected-tour-match-list {
  max-height: 430px;
  overflow-y: auto;
  padding-right: 3px;
}

.tour-match-list {
  display: grid;
  gap: 8px;
}

.tour-match-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.025);
  color: inherit;
  text-decoration: none;
}

.tour-match-link:hover {
  border-color: rgba(97, 232, 162, 0.25);
  background: rgba(97, 232, 162, 0.06);
}

.tour-match-copy {
  display: grid;
  gap: 4px;
}

.tour-match-meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  font-size: 0.86rem;
}

.tour-match-score {
  font-weight: 700;
  color: var(--text);
}

.matrix-desktop {
  display: block;
}

.matrix-mobile-list {
  display: none;
}

.matrix-wrap {
  overflow: hidden;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.matrix-table th,
.matrix-table td {
  border: 1px solid var(--line);
  padding: 8px 6px;
  text-align: center;
  vertical-align: middle;
}

.matrix-team-head,
.matrix-team-cell {
  position: sticky;
  left: 0;
  z-index: 1;
  width: 30%;
  text-align: left;
  background: var(--panel, #131a34);
}

.matrix-team-head {
  z-index: 2;
}

.matrix-col-head,
.matrix-score-cell {
  width: auto;
  min-width: 0;
}

.matrix-team-copy {
  display: grid;
  gap: 2px;
  justify-items: start;
  text-align: left;
  min-width: 0;
}

.matrix-team-copy strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix-score-self {
  color: var(--muted);
  background: rgba(255, 255, 255, 0.03);
}

.matrix-score-empty {
  color: var(--muted);
}

.matrix-ball {
  font-size: 0.72rem;
  line-height: 1;
}

.matrix-score-pill {
  display: block;
  white-space: nowrap;
  font-size: 0.82rem;
  overflow: hidden;
  text-overflow: ellipsis;
}

.matrix-score-pill + .matrix-score-pill {
  margin-top: 2px;
}

.matrix-score-link {
  color: var(--text);
  text-decoration: none;
}

.matrix-score-link:hover {
  color: var(--brand);
}

.matrix-score-pending {
  color: var(--muted);
}

.matrix-mobile-list {
  gap: 10px;
}

.matrix-mobile-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
}

.matrix-mobile-card-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.matrix-mobile-team-copy {
  display: grid;
  gap: 3px;
}

.matrix-mobile-goals {
  color: var(--muted);
  white-space: nowrap;
}

.matrix-mobile-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.matrix-mobile-opponents {
  display: grid;
  gap: 8px;
}

.matrix-mobile-opponent {
  display: grid;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.matrix-mobile-results {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.matrix-mobile-result-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 6px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  text-decoration: none;
  color: var(--text);
}

.playoff-bracket-wrap {
  position: relative;
  width: 100%;
  overflow-x: auto;
  padding: 16px 8px 22px;
}

.playoff-stage-shell {
  position: relative;
  display: grid;
  grid-template-columns: max-content 210px max-content;
  justify-content: center;
  gap: 56px;
  align-items: start;
  min-width: max-content;
}

.playoff-connectors {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  pointer-events: none;
}

.playoff-connectors path {
  fill: none;
  stroke: rgba(145, 191, 235, 0.52);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
  filter: drop-shadow(0 0 5px rgba(72, 137, 206, 0.18));
}

.playoff-stage-side {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 190px;
  gap: 56px;
  align-items: start;
}

.playoff-side-column {
  display: grid;
  grid-template-rows: 40px var(--playoff-stage-height);
  gap: 14px;
  width: 190px;
}

.playoff-side-cards {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: stretch;
  height: var(--playoff-stage-height);
  position: relative;
}

.playoff-card-anchor {
  position: relative;
  z-index: 1;
  width: 100%;
}

.playoff-round-head {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 100%;
  min-height: 40px;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(126, 191, 255, 0.25);
  background: linear-gradient(180deg, rgba(72, 107, 191, 0.42), rgba(25, 34, 74, 0.82));
  backdrop-filter: blur(10px);
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.playoff-round-head h3 {
  margin: 0;
  font-size: 0.9rem;
}

.playoff-match-card {
  position: relative;
  display: grid;
  gap: 8px;
  width: 100%;
  min-height: 106px;
  padding: 10px 11px 12px;
  border-radius: 14px;
  border: 1px solid rgba(192, 216, 255, 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.13), rgba(255, 255, 255, 0.045)),
    linear-gradient(135deg, rgba(75, 113, 184, 0.28), rgba(12, 19, 46, 0.94));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 16px 34px rgba(3, 8, 24, 0.24);
  text-decoration: none;
  color: inherit;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease,
    background 160ms ease;
}

.playoff-match-card.is-clickable {
  cursor: pointer;
  padding-bottom: 22px;
}

.playoff-match-card.is-clickable:hover,
.playoff-match-card.is-clickable:focus-visible {
  z-index: 2;
  outline: none;
  border-color: rgba(97, 232, 162, 0.7);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.17), rgba(255, 255, 255, 0.06)),
    linear-gradient(135deg, rgba(65, 151, 145, 0.35), rgba(12, 24, 48, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 18px 44px rgba(3, 8, 24, 0.34),
    0 0 0 3px rgba(97, 232, 162, 0.08);
  transform: translateY(-3px);
}

.playoff-match-card.is-placeholder {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.03)),
    linear-gradient(135deg, rgba(59, 83, 137, 0.18), rgba(12, 17, 42, 0.82));
}

.playoff-match-card.is-third-place {
  border-color: rgba(245, 189, 92, 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.05)),
    linear-gradient(135deg, rgba(155, 110, 56, 0.26), rgba(20, 18, 36, 0.88));
}

.playoff-stage-center {
  position: relative;
  z-index: 1;
  width: 210px;
}

.playoff-center-stack {
  position: relative;
  width: 100%;
  height: calc(var(--playoff-stage-height) + 54px);
}

.playoff-center-card-wrap {
  position: absolute;
  left: 0;
  width: 100%;
  display: grid;
  gap: 12px;
  justify-items: stretch;
}

.playoff-center-card-wrap.is-final {
  top: 0;
  height: 100%;
  grid-template-rows: 40px 1fr;
}

.playoff-center-card-wrap.is-final .playoff-card-anchor {
  align-self: center;
  transform: translateY(-20px);
}

.playoff-center-card-wrap.is-third-place {
  bottom: 0;
}

.playoff-round-head-center {
  width: 100%;
}

.playoff-match-card-center {
  min-height: 112px;
}

.playoff-match-card-head,
.playoff-match-card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
}

.playoff-match-card-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #eef5ff;
  font-size: 0.62rem;
  font-weight: 700;
}

.playoff-match-card-date {
  font-size: 0.64rem;
  color: rgba(239, 245, 255, 0.82);
  white-space: nowrap;
}

.playoff-match-card-body {
  display: grid;
  gap: 5px;
}

.playoff-team-slot {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px;
  align-items: center;
  gap: 6px;
  padding: 5px 7px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #17203d;
}

.playoff-team-slot strong {
  min-width: 0;
  max-width: 132px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.72rem;
}

.playoff-team-score {
  font-weight: 900;
  font-size: 0.82rem;
  color: #1f315e;
}

.playoff-match-card-footer {
  align-items: end;
  min-height: 0;
  font-size: 0.62rem;
}

.playoff-match-card-open {
  position: absolute;
  right: 11px;
  bottom: 6px;
  color: rgba(181, 255, 218, 0.82);
  font-size: 0.61rem;
  font-weight: 800;
  letter-spacing: 0.015em;
  opacity: 0;
  transform: translateX(-5px);
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.playoff-match-card.is-clickable:hover .playoff-match-card-open,
.playoff-match-card.is-clickable:focus-visible .playoff-match-card-open {
  opacity: 1;
  transform: translateX(0);
}

.playoff-stage-center .playoff-team-slot {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 255, 0.96));
}

.playoff-stage-center .playoff-match-card-badge {
  background: rgba(97, 232, 162, 0.18);
  color: #d9ffec;
}

.team-cell-mobile {
  gap: 8px;
  min-width: 0;
}

.team-cell-mobile span {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1100px) {
  .season-meta-grid {
    font-size: 0.86rem;
  }

  .home-main-grid {
    grid-template-columns: 1fr;
    font-size: 0.8rem;
  }

  .section-head,
  .standings-head {
    align-items: start;
    grid-template-columns: 1fr;
  }

  .playoff-bracket-wrap {
    overflow-x: visible;
    padding: 6px 0 10px;
  }

  .playoff-stage-shell {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 18px;
  }

  .playoff-connectors {
    display: none;
  }

  .playoff-stage-side,
  .playoff-stage-center,
  .playoff-center-stack {
    display: contents;
  }

  .playoff-side-column {
    width: 100%;
    grid-template-rows: auto auto;
    gap: 10px;
  }

  .playoff-side-column.is-round-of-16 {
    order: 1;
  }

  .playoff-side-column.is-quarterfinal {
    order: 2;
  }

  .playoff-side-column.is-semifinal {
    order: 3;
  }

  .playoff-center-card-wrap.is-final {
    order: 4;
  }

  .playoff-center-card-wrap.is-third-place {
    order: 5;
  }

  .playoff-center-card-wrap,
  .playoff-center-card-wrap.is-final,
  .playoff-center-card-wrap.is-third-place {
    position: static;
    display: grid;
    grid-template-rows: auto auto;
    width: 100%;
    height: auto;
    gap: 10px;
  }

  .playoff-center-card-wrap.is-final .playoff-card-anchor {
    transform: none;
  }

  .playoff-side-cards {
    height: auto;
    justify-content: flex-start;
    gap: 10px;
  }

  .playoff-round-head,
  .playoff-round-head-center,
  .playoff-match-card,
  .playoff-match-card-center {
    width: 100%;
    max-width: none;
  }

  .playoff-round-head,
  .playoff-round-head-center {
    justify-self: stretch;
  }

  .playoff-match-card,
  .playoff-match-card-center {
    min-height: 0;
    padding: 10px 12px;
  }

  .playoff-match-card.is-clickable {
    padding-bottom: 24px;
  }

  .playoff-match-card-head,
  .playoff-match-card-footer {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .playoff-match-card-body {
    gap: 6px;
  }

  .playoff-team-slot {
    grid-template-columns: minmax(0, 1fr) 22px;
    padding: 7px 9px;
  }

  .playoff-team-slot strong {
    max-width: none;
    font-size: 0.84rem;
  }

  .playoff-team-score {
    font-size: 0.9rem;
  }

  .playoff-match-card-badge {
    font-size: 0.68rem;
  }

  .playoff-match-card-date,
  .playoff-match-card-footer {
    font-size: 0.7rem;
  }

  .playoff-round-head-center {
    min-width: 0;
  }

  .standings-toolbar {
    width: 100%;
    justify-items: start;
  }

  .standings-toolbar-actions {
    justify-content: flex-start;
  }

  .standings-table-desktop {
    display: none;
  }

  .standings-table-mobile {
    display: table;
  }

  .standings-table-mobile th,
  .standings-table-mobile td {
    padding: 10px 8px;
    font-size: 0.92rem;
  }

  .standings-table-mobile th:nth-child(1),
  .standings-table-mobile td:nth-child(1) {
    width: 44px;
  }

  .standings-table-mobile th:nth-child(2),
  .standings-table-mobile td:nth-child(2) {
    min-width: 0;
  }
  .standings-table-mobile td:nth-child(4) {
    width: 40px;
    text-align: center;
  }

  .standings-table-mobile th:nth-child(5),

  .tours-card {
    flex: 1 1 auto;
  }

  .standings-table-mobile td:nth-child(5),
  .standings-table-mobile th:nth-child(6),
  .standings-table-mobile td:nth-child(6),
  .standings-table-mobile th:nth-child(7),
  .standings-table-mobile td:nth-child(7) {
    width: 42px;
    text-align: center;
  }

}

@media (max-width: 860px) {
  .season-box-wide {
    width: 100%;
    min-width: 0;
  }

  .season-box-wide select {
    width: 100%;
  }

  .matrix-desktop {
    display: none;
  }

  .matrix-mobile-list {
    display: grid;
  }

  .tour-navigator {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .tour-stepper {
    justify-content: space-between;
  }

  .selected-tour-head {
    flex-direction: column;
  }

  .selected-tour-progress {
    justify-items: start;
  }

  .tour-card,
  .home-hero-head,
  .section-head {
    align-items: start;
  }

  .playoff-tour-head,
  .playoff-match-meta,
  .playoff-match-copy {
    align-items: start;
    flex-direction: column;
  }

  .playoff-match-card-head {
    align-items: center;
    flex-direction: row;
  }

  .playoff-match-card-footer {
    align-items: start;
    flex-direction: column;
  }

  .tour-card {
    flex-direction: column;
  }

  .tour-side {
    width: 100%;
    justify-items: start;
  }

  .tour-match-link {
    align-items: start;
    flex-direction: column;
  }

  .tour-match-meta {
    width: 100%;
    grid-template-columns: 1fr;
    justify-items: start;
  }
  .matrix-mobile-card-head {
    align-items: start;
    flex-direction: column;
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

  .season-meta-grid {
    grid-template-columns: 1fr;
  }

  .standings-toolbar {
    justify-items: start;
  }

  .playoff-stage-shell {
    gap: 12px;
  }

  .playoff-round-head,
  .playoff-round-head-center {
    padding: 5px 9px;
  }

  .playoff-round-head h3 {
    font-size: 0.8rem;
  }

  .playoff-match-card,
  .playoff-match-card-center {
    padding: 8px 10px;
    border-radius: 10px;
  }

  .playoff-match-card.is-clickable {
    padding-bottom: 22px;
  }

  .playoff-match-card-head {
    gap: 4px;
  }

  .playoff-match-card-badge {
    padding: 2px 6px;
    font-size: 0.62rem;
  }

  .playoff-team-slot {
    grid-template-columns: minmax(0, 1fr) 18px;
    gap: 5px;
    padding: 6px 8px;
  }

  .playoff-team-slot strong {
    font-size: 0.76rem;
  }

  .playoff-team-score {
    font-size: 0.82rem;
  }

  .standings-table-mobile th,
  .standings-table-mobile td {
    padding: 9px 6px;
    font-size: 0.86rem;
  }

  .player-stats-table {
    font-size: 0.88rem;
  }

  .player-stats-table th,
  .player-stats-table td {
    padding: 9px 6px;
  }

  .player-stats-table-scorers th:nth-child(3),
  .player-stats-table-scorers td:nth-child(3) {
    width: 30%;
  }

  .player-stats-table-discipline th:nth-child(3),
  .player-stats-table-discipline td:nth-child(3) {
    width: 22%;
  }

  .tour-card,
  .home-hero-head,
  .section-head {
    align-items: start;
  }

  .tour-card {
    flex-direction: column;
  }

  .tour-side {
    width: 100%;
    justify-items: start;
  }

  .tour-match-link {
    align-items: start;
    flex-direction: column;
  }

  .tour-match-meta {
    width: 100%;
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .matrix-mobile-card-head {
    align-items: start;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .standings-table-mobile th,
  .standings-table-mobile td {
    padding: 8px 4px;
    font-size: 0.8rem;
  }

  .team-cell-mobile {
    gap: 6px;
  }
}
</style>
