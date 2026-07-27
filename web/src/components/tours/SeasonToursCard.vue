<template>
  <article class="card tours-card">
    <div class="section-head">
      <h2 class="section-title">Туры сезона</h2>
      <span v-if="loading" class="muted-text">Загрузка...</span>
    </div>

    <div v-if="tours.length" class="tour-list">
      <article v-for="tour in tours" :key="tour.id" class="tour-block">
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
        <div v-if="String(expandedTourId) === String(tour.id)" class="tour-match-list">
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
    <p v-else-if="selectedSeason && !loading" class="empty-text">
      Для выбранного сезона пока нет туров с назначенными матчами.
    </p>
  </article>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  tours: {
    type: Array,
    required: true,
  },
  selectedSeason: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const expandedTourId = ref('')

function toggleTour(tourId) {
  expandedTourId.value = String(expandedTourId.value) === String(tourId) ? '' : String(tourId)
}

function stageLabel(tour) {
  if (String(tour.stageType || '').toUpperCase() === 'PLAYOFF') return 'Стадия плей-офф'
  if (tour.roundNumber) return `Регулярный этап, тур ${tour.roundNumber}`
  return 'Регулярный этап'
}

function tourBadge(tour) {
  return String(tour.stageType || '').toUpperCase() === 'PLAYOFF'
    ? 'Плей-офф'
    : tour.roundNumber ? `Тур ${tour.roundNumber}` : 'Регулярка'
}

function tourDateLabel(tour) {
  const firstMatch = Array.isArray(tour.matches) && tour.matches.length ? tour.matches[0] : null
  if (!firstMatch?.kickoffAt) return 'Дата тура будет назначена позже'
  return `Дата тура: ${formatDateOnly(firstMatch.kickoffAt)}`
}

function formatDateOnly(value) {
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
</script>
