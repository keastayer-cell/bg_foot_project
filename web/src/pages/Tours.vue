<template>
  <section class="section-wrap home-page">
    <article class="card home-hero">
      <div class="home-hero-head">
        <div>
          <p class="eyebrow">Сезонный обзор</p>
          <h1 class="section-title home-title">Туры и команды сезона</h1>
          <p class="muted-text home-subtitle">
            Выберите сезон, чтобы посмотреть его состав участников и структуру туров.
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

    <div class="home-main-grid">
      <article class="card teams-card">
        <div class="section-head">
          <h2 class="section-title">Команды сезона</h2>
          <span class="muted-text" v-if="loadingSeasonData">Загрузка...</span>
        </div>

        <table class="stats-table" v-if="seasonTeams.length">
          <thead>
            <tr>
              <th>#</th>
              <th>Команда</th>
              <th>Короткое</th>
              <th>Город</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(team, index) in seasonTeams" :key="team.id">
              <td>{{ index + 1 }}</td>
              <td>
                <div class="team-cell">
                  <img v-if="team.logoDataUrl" :src="team.logoDataUrl" :alt="team.name" class="team-logo" />
                  <span>{{ team.name }}</span>
                </div>
              </td>
              <td>{{ team.shortName || '—' }}</td>
              <td>{{ team.city || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <p class="empty-text" v-else-if="selectedSeason && !loadingSeasonData">В выбранном сезоне пока нет команд.</p>
        <p class="empty-text" v-else>Выберите сезон, чтобы посмотреть состав участников.</p>
      </article>

      <article class="card tours-card">
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
const expandedTourId = ref('')
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

watch(selectedSeasonId, async (seasonId) => {
  expandedTourId.value = ''
  if (!seasonId) {
    seasonTeams.value = []
    seasonTours.value = []
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
    const payload = await optionalAuthApiRequest(`/api/seasons/${encodeURIComponent(seasonId)}/overview`, { method: 'GET' })
    seasonTeams.value = Array.isArray(payload?.teams) ? payload.teams : []
    seasonTours.value = Array.isArray(payload?.tours) ? payload.tours : []
  } catch (error) {
    seasonTeams.value = []
    seasonTours.value = []
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

.season-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
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
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 24px;
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5ede8;
}

.tour-list {
  display: grid;
  gap: 10px;
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

@media (max-width: 960px) {
  .season-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .season-meta-grid {
    grid-template-columns: 1fr;
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
    justify-items: start;
  }
}
</style>