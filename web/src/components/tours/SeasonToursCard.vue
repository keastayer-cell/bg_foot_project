<template>
  <article class="card tours-card">
    <div class="section-head tours-card-head">
      <div>
        <h2 class="section-title">Туры сезона</h2>
        <p v-if="tours.length" class="muted-text tours-card-subtitle">
          Быстрый переход между этапами сезона · всего {{ tours.length }}
        </p>
      </div>
      <span v-if="loading" class="muted-text">Загрузка...</span>
    </div>

    <template v-if="tours.length && selectedTour">
      <div class="tour-navigator">
        <label class="tour-select-field">
          <span>Выбрать тур</span>
          <select v-model="selectedTourId">
            <option v-for="tour in tours" :key="tour.id" :value="String(tour.id)">
              {{ tourOptionLabel(tour) }}
            </option>
          </select>
        </label>

        <div class="tour-stepper" aria-label="Переключение туров">
          <button
            class="btn-ghost tour-step-button"
            type="button"
            :disabled="!hasPreviousTour"
            aria-label="Предыдущий тур"
            @click="selectRelativeTour(-1)"
          >
            ←
          </button>
          <strong>{{ selectedTourPosition }}</strong>
          <button
            class="btn-ghost tour-step-button"
            type="button"
            :disabled="!hasNextTour"
            aria-label="Следующий тур"
            @click="selectRelativeTour(1)"
          >
            →
          </button>
        </div>
      </div>

      <section class="selected-tour-card">
        <div class="selected-tour-head">
          <div>
            <div class="selected-tour-title-row">
              <span class="tour-badge">{{ tourBadge(selectedTour) }}</span>
              <span :class="['tour-state-badge', tourStateClass(selectedTour)]">
                {{ tourStateLabel(selectedTour) }}
              </span>
            </div>
            <h3>{{ selectedTour.name }}</h3>
            <p class="muted-text">{{ stageLabel(selectedTour) }} · {{ tourDateLabel(selectedTour) }}</p>
          </div>
          <div class="selected-tour-progress">
            <strong>{{ completedMatchesCount(selectedTour) }}/{{ selectedTour.matchesCount }}</strong>
            <span class="muted-text">матчей завершено</span>
          </div>
        </div>

        <div v-if="selectedTour.matches.length" class="tour-match-list selected-tour-match-list">
          <router-link
            v-for="match in selectedTour.matches"
            :key="match.id"
            :to="matchTarget(match)"
            class="tour-match-link"
          >
            <div class="tour-match-copy">
              <strong>{{ match.homeTeamName }} — {{ match.awayTeamName }}</strong>
              <span class="muted-text">{{ matchStatusLabel(match.status) }}</span>
            </div>
            <div class="tour-match-meta">
              <span>{{ formatMatchDateTime(match.kickoffAt) }}</span>
              <span class="tour-match-score">{{ matchScoreLabel(match) }}</span>
            </div>
          </router-link>
        </div>
        <p v-else class="empty-text">В этом туре пока нет назначенных матчей.</p>
      </section>
    </template>

    <p v-else-if="selectedSeason && !loading" class="empty-text">
      Для выбранного сезона пока нет туров с назначенными матчами.
    </p>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { matchPageLocation } from '../../utils/publicUrls'

const props = defineProps({
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
  initialTourId: {
    type: [String, Number],
    default: '',
  },
})

const selectedTourId = ref('')

const selectedTourIndex = computed(() => {
  return props.tours.findIndex((tour) => String(tour.id) === String(selectedTourId.value))
})

const selectedTour = computed(() => {
  return selectedTourIndex.value >= 0 ? props.tours[selectedTourIndex.value] : null
})

const selectedTourPosition = computed(() => {
  if (selectedTourIndex.value < 0) return '—'
  return `${selectedTourIndex.value + 1} из ${props.tours.length}`
})

const hasPreviousTour = computed(() => selectedTourIndex.value > 0)
const hasNextTour = computed(() => {
  return selectedTourIndex.value >= 0 && selectedTourIndex.value < props.tours.length - 1
})

watch(
  () => [props.tours, props.initialTourId],
  () => {
    if (!props.tours.length) {
      selectedTourId.value = ''
      return
    }

    const currentExists = props.tours.some(
      (tour) => String(tour.id) === String(selectedTourId.value),
    )
    if (currentExists) return

    const requestedTour = props.tours.find(
      (tour) => String(tour.id) === String(props.initialTourId),
    )
    selectedTourId.value = String(requestedTour?.id || relevantTour(props.tours).id)
  },
  { immediate: true, deep: true },
)

function relevantTour(tours) {
  const now = Date.now()
  const upcoming = tours
    .filter((tour) => !isTourComplete(tour))
    .map((tour) => ({ tour, timestamp: tourTimestamp(tour) }))
    .sort((left, right) => {
      const leftDistance = left.timestamp >= now ? left.timestamp - now : Number.MAX_SAFE_INTEGER
      const rightDistance = right.timestamp >= now ? right.timestamp - now : Number.MAX_SAFE_INTEGER
      return leftDistance - rightDistance
    })

  return upcoming[0]?.tour || tours[tours.length - 1]
}

function selectRelativeTour(direction) {
  const nextTour = props.tours[selectedTourIndex.value + direction]
  if (nextTour) selectedTourId.value = String(nextTour.id)
}

function tourOptionLabel(tour) {
  return `${tour.name} · ${tourDateShortLabel(tour)}`
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

function tourStateLabel(tour) {
  if (isTourComplete(tour)) return 'Завершён'
  if (tour.matches?.some((match) => match.status === 'LIVE')) return 'Идёт сейчас'
  return 'Предстоящий'
}

function tourStateClass(tour) {
  if (isTourComplete(tour)) return 'is-complete'
  if (tour.matches?.some((match) => match.status === 'LIVE')) return 'is-live'
  return 'is-upcoming'
}

function tourDateLabel(tour) {
  const firstMatch = Array.isArray(tour.matches) && tour.matches.length ? tour.matches[0] : null
  if (!firstMatch?.kickoffAt) return 'дата будет назначена позже'
  return `дата ${formatDateOnly(firstMatch.kickoffAt)}`
}

function tourDateShortLabel(tour) {
  const firstMatch = Array.isArray(tour.matches) && tour.matches.length ? tour.matches[0] : null
  return firstMatch?.kickoffAt ? formatDateOnly(firstMatch.kickoffAt) : 'без даты'
}

function tourTimestamp(tour) {
  const timestamps = (tour.matches || [])
    .map((match) => new Date(match.kickoffAt || 0).getTime())
    .filter((value) => Number.isFinite(value) && value > 0)
  return timestamps.length ? Math.min(...timestamps) : Number.MAX_SAFE_INTEGER
}

function isMatchComplete(match) {
  return ['VERIFIED', 'FINISHED'].includes(String(match?.status || '').toUpperCase())
}

function completedMatchesCount(tour) {
  return (tour.matches || []).filter(isMatchComplete).length
}

function isTourComplete(tour) {
  return Boolean(tour.matches?.length) && completedMatchesCount(tour) === tour.matches.length
}

function matchTarget(match) {
  return matchPageLocation(match.id, {
    returnContext: 'tours',
    ...(props.selectedSeason?.id ? { seasonId: String(props.selectedSeason.id) } : {}),
    tourId: String(selectedTour.value?.id || ''),
  })
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
</script>
