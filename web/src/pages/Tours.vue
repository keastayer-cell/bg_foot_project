<template>
  <section class="section-wrap home-page">
    <article class="card home-hero">
      <div class="home-hero-head">
        <div>
          <p class="eyebrow">Сезонный обзор</p>
          <h1 class="section-title home-title">Туры и таблица сезона</h1>
          <p class="muted-text home-subtitle">
            Выберите сезон, чтобы посмотреть турнирную таблицу и структуру туров.
          </p>
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

    <div class="home-main-grid" :class="{ 'home-main-grid-matrix': seasonViewMode === 'matrix' }">
      <article class="card standings-card">
        <div class="section-head standings-head">
          <div>
            <h2 class="section-title">Таблица сезона</h2>
          </div>
          <div class="standings-toolbar">
            <div class="standings-toolbar-actions" v-if="selectedSeason">
              <button
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
              </div>
            </div>
            <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
            <span class="muted-text" v-else-if="standingsConfig?.lastCalculatedAt">Обновлено: {{ formatMatchDateTime(standingsConfig.lastCalculatedAt) }}</span>
          </div>
        </div>

        <table class="stats-table standings-table-desktop" v-if="seasonViewMode === 'table' && seasonStandings.length">
          <thead>
            <tr>
              <th>Место</th>
              <th>Команда</th>
              <th>И</th>
              <th>ЗМ</th>
              <th>ПМ</th>
              <th>РМ</th>
              <th>О</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in seasonStandings" :key="row.teamId">
              <td>{{ row.position }}</td>
              <td>
                <div class="team-cell">
                  <img v-if="row.teamLogoDataUrl" :src="row.teamLogoDataUrl" :alt="row.teamName" class="team-logo" />
                  <span>{{ row.teamName }}</span>
                </div>
              </td>
              <td>{{ row.matchesPlayed }}</td>
              <td>{{ row.goalsFor }}</td>
              <td>{{ row.goalsAgainst }}</td>
              <td>{{ signedGoalDifference(row.goalDifference) }}</td>
              <td><strong>{{ row.points }}</strong></td>
            </tr>
          </tbody>
        </table>

        <table class="stats-table standings-table-mobile" v-if="seasonViewMode === 'table' && seasonStandings.length">
          <thead>
            <tr>
              <th>Место</th>
              <th>Команда</th>
              <th>И</th>
              <th>ЗМ</th>
              <th>ПМ</th>
              <th>РМ</th>
              <th>О</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in seasonStandings" :key="`mobile-${row.teamId}`">
              <td>{{ row.position }}</td>
              <td>
                <div class="team-cell team-cell-mobile">
                  <img v-if="row.teamLogoDataUrl" :src="row.teamLogoDataUrl" :alt="row.teamName" class="team-logo" />
                  <span>{{ row.teamName }}</span>
                </div>
              </td>
              <td>{{ row.matchesPlayed }}</td>
              <td>{{ row.goalsFor }}</td>
              <td>{{ row.goalsAgainst }}</td>
              <td>{{ signedGoalDifference(row.goalDifference) }}</td>
              <td><strong>{{ row.points }}</strong></td>
            </tr>
          </tbody>
        </table>

        <div class="matrix-wrap matrix-desktop" v-else-if="seasonViewMode === 'matrix' && matrixTeams.length">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="matrix-team-head">Команда</th>
                <th v-for="team in matrixTeams" :key="`matrix-col-${team.id}`" class="matrix-col-head">
                  {{ team.positionLabel }}
                </th>
                <th>И</th>
                <th>В</th>
                <th>Н</th>
                <th>П</th>
                <th>М</th>
                <th>О</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in matrixRows" :key="`matrix-row-${row.team.id}`">
                <td class="matrix-team-cell">
                  <div class="matrix-team-copy">
                    <strong>{{ row.team.name }}</strong>
                  </div>
                </td>
                <td
                  v-for="cell in row.cells"
                  :key="`matrix-cell-${row.team.id}-${cell.opponentTeamId}`"
                  class="matrix-score-cell"
                  :class="{
                    'matrix-score-self': cell.isSelf,
                    'matrix-score-empty': !cell.isSelf && !cell.results.length,
                  }"
                >
                  <template v-if="cell.isSelf">
                    <span class="matrix-ball" aria-hidden="true">⚽</span>
                  </template>
                  <template v-else-if="cell.results.length">
                    <component
                      v-for="result in cell.results"
                      :key="result.key"
                      :is="result.matchId ? 'router-link' : 'span'"
                      :to="result.matchId ? `/match/${result.matchId}` : undefined"
                      class="matrix-score-pill"
                      :class="{
                        'matrix-score-link': Boolean(result.matchId),
                        'matrix-score-pending': result.pending,
                      }"
                    >
                      {{ result.label }}
                    </component>
                  </template>
                  <template v-else>—</template>
                </td>
                <td>{{ row.summary.played }}</td>
                <td>{{ row.summary.wins }}</td>
                <td>{{ row.summary.draws }}</td>
                <td>{{ row.summary.losses }}</td>
                <td>{{ row.summary.goalsFor }}-{{ row.summary.goalsAgainst }}</td>
                <td><strong>{{ row.summary.points }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="matrix-mobile-list" v-else-if="seasonViewMode === 'matrix' && matrixRows.length">
          <article class="matrix-mobile-card" v-for="row in matrixRows" :key="`matrix-mobile-${row.team.id}`">
            <div class="matrix-mobile-card-head">
              <div class="matrix-mobile-team-copy">
                <strong>{{ row.team.name }}</strong>
                <span class="muted-text">И: {{ row.summary.played }} · О: {{ row.summary.points }}</span>
              </div>
              <span class="matrix-mobile-goals">{{ row.summary.goalsFor }}-{{ row.summary.goalsAgainst }}</span>
            </div>

            <div class="matrix-mobile-summary">
              <span>В: <strong>{{ row.summary.wins }}</strong></span>
              <span>Н: <strong>{{ row.summary.draws }}</strong></span>
              <span>П: <strong>{{ row.summary.losses }}</strong></span>
            </div>

            <div class="matrix-mobile-opponents">
              <article
                class="matrix-mobile-opponent"
                v-for="cell in row.cells.filter((item) => !item.isSelf)"
                :key="`matrix-mobile-opp-${row.team.id}-${cell.opponentTeamId}`"
              >
                <div class="matrix-mobile-opponent-head">
                  <strong>{{ cell.opponentName }}</strong>
                </div>
                <div class="matrix-mobile-results">
                  <component
                    v-for="result in cell.results"
                    :key="result.key"
                    :is="result.matchId ? 'router-link' : 'span'"
                    :to="result.matchId ? `/match/${result.matchId}` : undefined"
                    class="matrix-mobile-result-pill"
                    :class="{
                      'matrix-score-link': Boolean(result.matchId),
                      'matrix-score-pending': result.pending,
                    }"
                  >
                    {{ result.label }}
                  </component>
                </div>
              </article>
            </div>
          </article>
        </div>

        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'table'">Подтвержденных матчей в опубликованных турах пока нет.</p>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData && seasonViewMode === 'matrix'">В выбранном сезоне пока нет данных для шахматки.</p>
        <p class="empty-text" v-else>Выберите сезон, чтобы посмотреть таблицу.</p>
      </article>

      <article class="card tours-card" v-if="seasonViewMode !== 'matrix' && sidePanelMode === 'tours'">
        <div class="section-head">
          <h2 class="section-title">Туры сезона</h2>
          <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
        </div>

        <div class="tour-list" v-if="formattedTours.length">
          <article class="tour-block" v-for="tour in formattedTours" :key="tour.id">
            <button class="tour-card tour-card-button" type="button" @click="toggleTour(tour.id)">
              <div class="tour-main">
                <h3>{{ tour.name }}</h3>
                <p class="muted-text">{{ stageLabel(tour) }}</p>
                <p class="tour-date">{{ tourDateLabel(tour) }}</p>
              </div>
              <div class="tour-side">
                <span class="tour-match-count">{{ tour.matchesCount }} матч{{ matchesWord(tour.matchesCount) }}</span>
                <span class="tour-badge">{{ tourBadge(tour) }}</span>
              </div>
            </button>
            <div class="tour-match-list" v-if="String(expandedTourId) === String(tour.id)">
              <router-link
                v-for="match in tour.matches"
                :key="match.id"
                :to="`/match/${match.id}`"
                class="tour-match-link"
              >
                <div class="tour-match-copy">
                  <strong>{{ match.homeTeamName }} - {{ match.awayTeamName }}</strong>
                  <span class="muted-text">{{ matchStatusLabel(match.status) }}</span>
                </div>
                <div class="tour-match-meta">
                  <span>{{ formatMatchDateTime(match.kickoffAt) }}</span>
                  <span class="tour-match-score">{{ matchScoreLabel(match) }}</span>
                </div>
              </router-link>
            </div>
          </article>
        </div>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData">Для выбранного сезона пока нет туров с назначенными матчами.</p>
        <p class="empty-text" v-else>Выберите сезон, чтобы посмотреть туры.</p>
      </article>

      <article class="card player-stats-card" v-else-if="sidePanelMode === 'stats'">
        <div class="section-head player-stats-head">
          <div>
            <h2 class="section-title">Статистика игроков сезона</h2>
            <p class="muted-text player-stats-subtitle">Учитываются только подтвержденные протоколы опубликованных туров.</p>
          </div>
          <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
        </div>

        <div class="player-stats-tabs" v-if="selectedSeason">
          <button
            class="btn-ghost player-stats-tab"
            type="button"
            :class="{ 'is-active': statsMode === 'scorers' }"
            @click="statsMode = 'scorers'"
          >
            Бомбардиры
          </button>
          <button
            class="btn-ghost player-stats-tab"
            type="button"
            :class="{ 'is-active': statsMode === 'discipline' }"
            @click="statsMode = 'discipline'"
          >
            Дисциплина
          </button>
        </div>

        <div class="player-stats-table-wrap" v-if="topStatsRows.length">
          <table class="stats-table player-stats-table player-stats-table-scorers" v-if="statsMode === 'scorers'">
            <thead>
              <tr>
                <th>№</th>
                <th>Игрок</th>
                <th>Команда</th>
                <th>Голы</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in topStatsRows" :key="`scorers-${row.playerId}`">
                <td class="player-stats-rank">{{ index + 1 }}</td>
                <td><strong class="player-stats-name">{{ row.fullName }}</strong></td>
                <td><span class="player-stats-team">{{ row.teamName || '—' }}</span></td>
                <td><strong>{{ row.goals }}</strong></td>
              </tr>
            </tbody>
          </table>

          <table class="stats-table player-stats-table player-stats-table-discipline" v-else>
            <thead>
              <tr>
                <th>№</th>
                <th>Игрок</th>
                <th>Команда</th>
                <th>ЖК</th>
                <th>КК</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in topStatsRows" :key="`discipline-${row.playerId}`">
                <td class="player-stats-rank">{{ index + 1 }}</td>
                <td><strong class="player-stats-name">{{ row.fullName }}</strong></td>
                <td><span class="player-stats-team">{{ row.teamName || '—' }}</span></td>
                <td class="player-stats-yellow">{{ row.yellowCards }}</td>
                <td class="player-stats-red">{{ row.redCards }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData">{{ statsEmptyText }}</p>
        <p class="empty-text" v-else>Выберите сезон, чтобы посмотреть статистику игроков.</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'

const { optionalAuthApiRequest } = useAuth()

const seasons = ref([])
const selectedSeasonId = ref('')
const seasonTeams = ref([])
const seasonTours = ref([])
const seasonStandings = ref([])
const seasonPlayerStats = ref([])
const standingsConfig = ref(null)
const expandedTourId = ref('')
const seasonViewMode = ref('table')
const sidePanelMode = ref('tours')
const statsMode = ref('scorers')
const loadingSeasons = ref(false)
const loadingSeasonData = ref(false)
const pageError = ref('')

const selectedSeason = computed(() => {
  return seasons.value.find((item) => String(item.id) === String(selectedSeasonId.value)) || null
})

const playoffLabel = computed(() => {
  if (!selectedSeason.value?.playoffEnabled) return 'Нет'
  return `${selectedSeason.value.playoffTeamCount || '—'} команд`
})

const scorersStats = computed(() => {
  return [...seasonPlayerStats.value]
    .filter((item) => Number(item?.goals || 0) > 0)
    .sort((left, right) => {
      const goalsDiff = Number(right?.goals || 0) - Number(left?.goals || 0)
      if (goalsDiff !== 0) return goalsDiff
      return String(left?.fullName || '').localeCompare(String(right?.fullName || ''), 'ru', { sensitivity: 'base' })
    })
})

const disciplineStats = computed(() => {
  return [...seasonPlayerStats.value]
    .filter((item) => Number(item?.yellowCards || 0) > 0 || Number(item?.redCards || 0) > 0)
    .sort((left, right) => {
      const redDiff = Number(right?.redCards || 0) - Number(left?.redCards || 0)
      if (redDiff !== 0) return redDiff
      const yellowDiff = Number(right?.yellowCards || 0) - Number(left?.yellowCards || 0)
      if (yellowDiff !== 0) return yellowDiff
      return String(left?.fullName || '').localeCompare(String(right?.fullName || ''), 'ru', { sensitivity: 'base' })
    })
})

const activeStatsRows = computed(() => {
  return statsMode.value === 'discipline' ? disciplineStats.value : scorersStats.value
})

const topStatsRows = computed(() => {
  return activeStatsRows.value.slice(0, 10)
})

const statsEmptyText = computed(() => {
  if (statsMode.value === 'discipline') {
    return 'В подтвержденных матчах выбранного сезона пока нет желтых или красных карточек.'
  }
  return 'В подтвержденных матчах выбранного сезона пока нет голов.'
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

const regularSeasonMatches = computed(() => {
  return seasonTours.value
    .filter((tour) => String(tour.stageType || '').toUpperCase() !== 'PLAYOFF')
    .flatMap((tour) => (Array.isArray(tour.matches) ? tour.matches : []))
    .sort((left, right) => {
      const leftTime = new Date(left.kickoffAt || 0).getTime()
      const rightTime = new Date(right.kickoffAt || 0).getTime()
      return leftTime - rightTime || Number(left.id) - Number(right.id)
    })
})

const standingsMap = computed(() => {
  return new Map(seasonStandings.value.map((row) => [String(row.teamId), row]))
})

const matrixSummaryMap = computed(() => {
  const summary = new Map()

  function ensure(teamId) {
    const key = String(teamId)
    if (!summary.has(key)) {
      const standingsRow = standingsMap.value.get(key)
      summary.set(key, {
        played: Number(standingsRow?.matchesPlayed || 0),
        wins: 0,
        draws: 0,
        losses: 0,
        goalsFor: Number(standingsRow?.goalsFor || 0),
        goalsAgainst: Number(standingsRow?.goalsAgainst || 0),
        points: Number(standingsRow?.points || 0),
      })
    }
    return summary.get(key)
  }

  for (const team of seasonTeams.value) {
    ensure(team.id)
  }

  for (const match of regularSeasonMatches.value) {
    if (!Number.isInteger(match.homeScore) || !Number.isInteger(match.awayScore)) {
      continue
    }

    const home = ensure(match.homeTeamId)
    const away = ensure(match.awayTeamId)

    if (match.homeScore > match.awayScore) {
      home.wins += 1
      away.losses += 1
    } else if (match.homeScore < match.awayScore) {
      away.wins += 1
      home.losses += 1
    } else {
      home.draws += 1
      away.draws += 1
    }
  }

  return summary
})

const matrixTeams = computed(() => {
  const ordered = []
  const used = new Set()

  for (const row of seasonStandings.value) {
    ordered.push({
      id: row.teamId,
      name: row.teamName,
      positionLabel: row.position,
    })
    used.add(String(row.teamId))
  }

  const remainder = seasonTeams.value
    .filter((team) => !used.has(String(team.id)))
    .slice()
    .sort((left, right) => String(left.name || '').localeCompare(String(right.name || ''), 'ru', { sensitivity: 'base' }))

  remainder.forEach((team) => {
    ordered.push({
      id: team.id,
      name: team.name,
      positionLabel: ordered.length + 1,
    })
  })

  return ordered
})

const expectedMatrixMatchCount = computed(() => {
  const roundsCount = Number(selectedSeason.value?.roundsCount || 1)
  return Number.isFinite(roundsCount) && roundsCount > 0 ? roundsCount : 1
})

const matrixRows = computed(() => {
  const matchMap = new Map()

  for (const match of regularSeasonMatches.value) {
    const key = [String(match.homeTeamId), String(match.awayTeamId)].sort().join(':')
    if (!matchMap.has(key)) {
      matchMap.set(key, [])
    }
    matchMap.get(key).push(match)
  }

  return matrixTeams.value.map((team) => ({
    team,
    summary: matrixSummaryMap.value.get(String(team.id)) || {
      played: 0,
      wins: 0,
      draws: 0,
      losses: 0,
      goalsFor: 0,
      goalsAgainst: 0,
      points: 0,
    },
    cells: matrixTeams.value.map((opponent) => {
      if (String(team.id) === String(opponent.id)) {
        return {
          opponentTeamId: opponent.id,
          opponentName: opponent.name,
          isSelf: true,
          results: [],
        }
      }

      const key = [String(team.id), String(opponent.id)].sort().join(':')
      const matches = matchMap.get(key) || []
      const results = matches.map((match) => {
        const hasScore = Number.isInteger(match.homeScore) && Number.isInteger(match.awayScore)
        let label = '—'
        if (hasScore) {
          label = String(match.homeTeamId) === String(team.id)
            ? `${match.homeScore}:${match.awayScore}`
            : `${match.awayScore}:${match.homeScore}`
        }
        return {
          key: match.id,
          matchId: match.id,
          label,
          pending: !hasScore,
        }
      })

      while (results.length < expectedMatrixMatchCount.value) {
        results.push({
          key: `placeholder-${team.id}-${opponent.id}-${results.length}`,
          matchId: null,
          label: '—',
          pending: true,
        })
      }

      return {
        opponentTeamId: opponent.id,
        opponentName: opponent.name,
        isSelf: false,
        results,
      }
    }),
  }))
})

watch(selectedSeasonId, async (seasonId) => {
  expandedTourId.value = ''
  sidePanelMode.value = 'tours'
  statsMode.value = 'scorers'
  if (!seasonId) {
    seasonTeams.value = []
    seasonTours.value = []
    seasonStandings.value = []
    seasonPlayerStats.value = []
    standingsConfig.value = null
    return
  }

  await loadSeasonData(seasonId)
})

function resetError() {
  pageError.value = ''
}

function toggleTour(tourId) {
  expandedTourId.value = String(expandedTourId.value) === String(tourId) ? '' : String(tourId)
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
    const payload = await optionalAuthApiRequest('/api/seasons?active_flag=1', { method: 'GET' })
    seasons.value = Array.isArray(payload) ? payload : []
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
  loadingSeasonData.value = true
  resetError()

  try {
    const [overviewPayload, playerStatsPayload] = await Promise.all([
      optionalAuthApiRequest(`/api/seasons/${encodeURIComponent(seasonId)}/overview`, { method: 'GET' }),
      optionalAuthApiRequest(`/api/seasons/${encodeURIComponent(seasonId)}/player-stats`, { method: 'GET' }),
    ])

    seasonTeams.value = Array.isArray(overviewPayload?.teams) ? overviewPayload.teams : []
    seasonTours.value = Array.isArray(overviewPayload?.tours) ? overviewPayload.tours : []
    seasonStandings.value = Array.isArray(overviewPayload?.standings) ? overviewPayload.standings : []
    seasonPlayerStats.value = Array.isArray(playerStatsPayload) ? playerStatsPayload : []
    standingsConfig.value = overviewPayload?.standingsConfig || null
  } catch (error) {
    seasonTeams.value = []
    seasonTours.value = []
    seasonStandings.value = []
    seasonPlayerStats.value = []
    standingsConfig.value = null
    pageError.value = error.message || 'Не удалось загрузить данные выбранного сезона.'
  } finally {
    loadingSeasonData.value = false
  }
}

function stageLabel(tour) {
  if (String(tour.stageType || '').toUpperCase() === 'PLAYOFF') {
    return 'Стадия плей-офф'
  }
  if (tour.roundNumber) {
    return `Регулярный этап, тур ${tour.roundNumber}`
  }
  return 'Регулярный этап'
}

function tourBadge(tour) {
  if (String(tour.stageType || '').toUpperCase() === 'PLAYOFF') {
    return 'Плей-офф'
  }
  return tour.roundNumber ? `Тур ${tour.roundNumber}` : 'Регулярка'
}

function tourDateLabel(tour) {
  const firstMatch = Array.isArray(tour.matches) && tour.matches.length ? tour.matches[0] : null
  if (!firstMatch?.kickoffAt) return 'Дата тура будет назначена позже'
  return `Дата тура: ${formatDateOnly(firstMatch.kickoffAt)}`
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

function matchScoreLabel(match) {
  if (Number.isInteger(match.homeScore) && Number.isInteger(match.awayScore)) {
    return `${match.homeScore} : ${match.awayScore}`
  }
  return '— : —'
}

function signedGoalDifference(value) {
  const normalized = Number(value || 0)
  return normalized > 0 ? `+${normalized}` : `${normalized}`
}

function matchStatusLabel(status) {
  if (status === 'LIVE') return 'Матч идет'
  if (status === 'FINISHED') return 'Матч завершен'
  if (status === 'VERIFIED') return 'Протокол подтвержден'
  if (status === 'LINEUPS_SUBMITTED') return 'Заявки поданы'
  return 'Матч запланирован'
}

function matchesWord(count) {
  const normalized = Math.abs(Number(count || 0))
  const lastTwo = normalized % 100
  const last = normalized % 10

  if (lastTwo >= 11 && lastTwo <= 14) return 'ей'
  if (last === 1) return ''
  if (last >= 2 && last <= 4) return 'а'
  return 'ей'
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
}

.player-stats-card {
  display: grid;
  gap: 14px;
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
  align-items: start;
}

.standings-toolbar {
  display: grid;
  gap: 10px;
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
  grid-template-columns: minmax(0, 1.08fr) minmax(340px, 0.92fr);
  align-items: start;
  gap: 24px;
}

.home-main-grid-matrix {
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

.team-logo {
  width: 28px;
  height: 28px;
  padding: 3px;
  border-radius: 50%;
  object-fit: contain;
  background: rgba(245, 237, 232, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.08);
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
    flex-direction: column;
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

  .team-cell-mobile .team-logo {
    width: 22px;
    height: 22px;
    padding: 2px;
  }
}
</style>