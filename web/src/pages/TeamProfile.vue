<template>
  <section class="section-wrap team-profile-page catalog-page">
    <div class="team-profile-backline">
      <RouterLink class="btn-ghost team-profile-back" :to="backLocation">← {{ backLabel }}</RouterLink>
    </div>

    <article v-if="loading" class="card team-profile-state">
      <p class="muted">Загружаем профиль команды...</p>
    </article>

    <article v-else-if="errorText" class="card team-profile-state">
      <p class="error-text">{{ errorText }}</p>
      <div class="actions-row">
        <button class="btn-primary" type="button" @click="loadTeamProfile">Повторить</button>
      </div>
    </article>

    <template v-else-if="teamProfile">
      <article class="catalog-header team-profile-hero">
        <div class="team-profile-hero-main">
          <div class="team-profile-logo-shell" :class="{ 'is-empty': !teamProfile.logoDataUrl }">
            <img
              v-if="teamProfile.logoDataUrl"
              :src="teamProfile.logoDataUrl"
              :alt="`Эмблема ${teamProfile.name}`"
              class="team-profile-logo"
            />
            <span v-else>{{ initials(teamProfile.name) }}</span>
          </div>

          <div class="team-profile-copy">
            <div class="team-profile-copy-top">
              <p class="catalog-kicker">Профиль команды</p>
              <span class="team-profile-status" :class="{ 'is-active': teamProfile.active }">
                {{ teamProfile.active ? 'Активна' : 'Неактивна' }}
              </span>
            </div>
            <h1 class="section-title team-profile-title">{{ teamProfile.name }}</h1>
            <p v-if="teamProfile.shortName || teamProfile.city" class="team-profile-subtitle">
              <span v-if="teamProfile.shortName">{{ teamProfile.shortName }}</span>
              <span v-if="teamProfile.shortName && teamProfile.city"> · </span>
              <span v-if="teamProfile.city">{{ teamProfile.city }}</span>
            </p>
            <FavoriteButton type="TEAM" :target-id="teamProfile.id" />
          </div>
        </div>
      </article>

      <article class="catalog-toolbar team-profile-controls">
        <div class="team-profile-controls-actions team-profile-controls-actions-wide">
          <div class="team-profile-controls-primary">
            <button
              v-if="selectedSeason && seasonRosterStatus === 'APPROVED' && seasonRoster.length"
              class="team-profile-roster-btn"
              type="button"
              @click="openSeasonRosterModal"
            >
              Показать заявку
            </button>
            <label class="catalog-search team-profile-select-wrap">
              <span>Сезон</span>
              <select v-model="selectedSeasonKey">
                <option value="all">Все сезоны</option>
                <option v-for="season in teamProfile.seasons" :key="season.id" :value="String(season.id)">
                  {{ season.name }}
                </option>
              </select>
            </label>
          </div>
          <div v-if="selectedSeason" class="team-profile-season-contact">
            <span class="team-profile-season-contact-label">Представитель</span>
            <strong>{{ seasonRepresentativeName || 'Не указан' }}</strong>
          </div>
        </div>
      </article>

      <div class="catalog-surface team-profile-stats-grid">
        <article class="team-profile-stat-card">
          <span class="team-profile-stat-label">Матчи</span>
          <strong>{{ selectedSummary.matchesPlayed }}</strong>
          <span class="muted">{{ selectedSeason ? 'В выбранном сезоне' : 'По всем сезонам' }}</span>
        </article>
        <article class="team-profile-stat-card">
          <span class="team-profile-stat-label">Победы</span>
          <strong>{{ selectedSummary.wins }}</strong>
          <span class="muted">Ничьи: {{ selectedSummary.draws }} · Поражения: {{ selectedSummary.losses }}</span>
        </article>
        <article class="team-profile-stat-card">
          <span class="team-profile-stat-label">Голы</span>
          <strong>{{ selectedSummary.goalsFor }}</strong>
          <span class="muted">Пропущено: {{ selectedSummary.goalsAgainst }}</span>
        </article>
        <article class="team-profile-stat-card">
          <span class="team-profile-stat-label">Форма</span>
          <div v-if="selectedForm.length" class="team-profile-form-strip">
            <span
              v-for="(result, index) in selectedForm"
              :key="`form-${index}`"
              class="team-profile-form-pill"
              :class="resultClass(result)"
              :title="result === 'W' ? 'Победа' : result === 'L' ? 'Поражение' : 'Ничья'"
            >
              {{ resultShortLabel(result) }}
            </span>
          </div>
          <span v-else class="muted">Нет завершенных матчей</span>
          <span v-if="selectedForm.length" class="muted">Последние {{ selectedForm.length }} · новые слева</span>
        </article>
      </div>

      <article class="catalog-surface team-profile-section team-profile-matches">
        <div class="catalog-list-head team-profile-section-head">
          <div class="team-profile-match-head-line">
            <h2 class="section-title team-profile-match-title">Последние матчи</h2>
          </div>
        </div>

        <div v-if="paginatedMatches.length" class="team-profile-history-table">
          <div class="team-profile-history-head muted">
            <span>Когда</span>
            <span>Соперник</span>
            <span>Счёт</span>
            <span>Результат</span>
          </div>
          <RouterLink
            v-for="match in paginatedMatches"
            :key="match.matchId"
            :to="matchLocation(match)"
            class="team-profile-history-row"
          >
            <span class="team-profile-history-meta">
              <span class="team-profile-history-date">{{ formatHistoryDate(match.kickoffAt) }}</span>
              <span class="team-profile-history-tournament">{{ match.seasonName }}</span>
            </span>
            <span class="team-profile-history-matchup">
              <strong>{{ match.opponentName }}</strong>
            </span>
            <span class="team-profile-history-score">{{ match.teamScore }}:{{ match.opponentScore }}</span>
            <span class="team-profile-history-statusline">
              <span class="team-profile-result-pill" :class="resultClass(match.resultCode)">{{ match.resultLabel }}</span>
              <span class="muted">{{ match.home ? 'Дом' : 'Выезд' }}</span>
            </span>
          </RouterLink>
        </div>
        <p v-else class="empty-text">У команды пока нет завершенных матчей.</p>

        <div v-if="totalPages > 1" class="catalog-pagination team-profile-pagination">
          <button class="btn-ghost" type="button" :disabled="currentPage === 1" @click="currentPage -= 1">Назад</button>
          <span>Страница {{ currentPage }} из {{ totalPages }} · матчей {{ selectedMatches.length }}</span>
          <button class="btn-ghost" type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">Вперёд</button>
        </div>
      </article>

      <div v-if="seasonRosterModalOpen" class="team-profile-modal-backdrop" @click.self="closeSeasonRosterModal">
        <article class="team-profile-modal" role="dialog" aria-modal="true" aria-labelledby="team-roster-title">
          <div class="team-profile-modal-head">
            <div>
              <p class="eyebrow">Состав на сезон</p>
              <h3 id="team-roster-title" class="section-title">{{ selectedSeason?.name }}</h3>
            </div>
            <button class="btn-ghost" type="button" @click="closeSeasonRosterModal">Закрыть</button>
          </div>

          <div v-if="seasonRoster.length" class="team-profile-modal-list">
            <div class="team-profile-season-roster-headline muted">
              <span>ФИО</span>
              <span>Дата рождения</span>
              <span>Город</span>
            </div>
            <article v-for="player in seasonRoster" :key="player.id" class="team-profile-season-roster-row">
              <strong>{{ player.fullName }}</strong>
              <span class="muted">{{ formatBirthDate(player.birthDate) }}</span>
              <span class="muted">{{ player.residence || '—' }}</span>
            </article>
          </div>
          <p v-else class="muted-text">Состав пока недоступен.</p>
        </article>
      </div>
    </template>

  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'
import { matchPageLocation, publicSlug } from '../utils/publicUrls'
import FavoriteButton from '../components/FavoriteButton.vue'

const PAGE_SIZE = 5

const route = useRoute()
const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)

const loading = ref(false)
const errorText = ref('')
const teamProfile = ref(null)
const selectedSeasonKey = ref('all')
const currentPage = ref(1)
const seasonRosterModalOpen = ref(false)
const seasonRosterLoading = ref(false)
const seasonRosterError = ref('')
const seasonRoster = ref([])
const seasonRosterStatus = ref('WAITING_FILL')
const seasonRepresentativeName = ref('')
const activeTeamId = ref(null)
const returnToHall = computed(() => route.query.from === 'hall-of-fame')
const backLocation = computed(() => returnToHall.value
  ? { name: 'hall-of-fame', query: { season: route.query.season, competition: route.query.competition } }
  : { name: 'teams', query: { season: route.query.season, competition: route.query.competition } })
const backLabel = computed(() => returnToHall.value ? 'Назад в Зал славы' : 'К списку команд')

const selectedSeason = computed(() => {
  if (selectedSeasonKey.value === 'all') return null
  return (teamProfile.value?.seasons || []).find((season) => String(season.id) === selectedSeasonKey.value) || null
})

const chronologyMatches = computed(() => teamProfile.value?.recentMatches || [])

const selectedMatches = computed(() => {
  if (!selectedSeason.value) return chronologyMatches.value
  return chronologyMatches.value.filter((match) => match.seasonId === selectedSeason.value.id)
})

const selectedSummary = computed(() => {
  const summary = { matchesPlayed: 0, wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0 }
  for (const match of selectedMatches.value) {
    summary.matchesPlayed += 1
    summary.goalsFor += match.teamScore
    summary.goalsAgainst += match.opponentScore
    if (match.resultCode === 'W') summary.wins += 1
    else if (match.resultCode === 'L') summary.losses += 1
    else summary.draws += 1
  }
  return summary
})

const selectedForm = computed(() => selectedMatches.value.slice(0, 6).map((match) => match.resultCode))
const totalPages = computed(() => Math.max(1, Math.ceil(selectedMatches.value.length / PAGE_SIZE)))
const paginatedMatches = computed(() => {
  const startIndex = (currentPage.value - 1) * PAGE_SIZE
  return selectedMatches.value.slice(startIndex, startIndex + PAGE_SIZE)
})

async function loadTeamProfile() {
  loading.value = true
  errorText.value = ''
  activeTeamId.value = null
  selectedSeasonKey.value = 'all'
  currentPage.value = 1
  resetSeasonRoster()

  try {
    const teamId = await resolveTeamId()
    activeTeamId.value = teamId
    teamProfile.value = await catalogApi.getTeam(teamId)
    applySeasonSelectionFromNavigation()
  } catch (error) {
    errorText.value = error.message || 'Не удалось загрузить профиль команды.'
    teamProfile.value = null
  } finally {
    loading.value = false
  }
}

async function resolveTeamId() {
  const routeValue = String(route.params.slug || '').trim()
  const legacyTeamId = Number(routeValue)
  if (Number.isFinite(legacyTeamId) && legacyTeamId > 0) {
    return legacyTeamId
  }

  const navigationTeamId = Number(window.history.state?.teamId)
  const navigationTeamSlug = String(window.history.state?.teamSlug || '')
  if (
    navigationTeamSlug === routeValue
    && Number.isFinite(navigationTeamId)
    && navigationTeamId > 0
  ) {
    return navigationTeamId
  }

  const payload = await catalogApi.getActiveTeams()
  const teams = Array.isArray(payload) ? payload : []
  const matchingTeam = teams.find((team) => publicSlug(team?.name) === routeValue)
  if (!matchingTeam?.id) {
    throw new Error('Команда по указанному адресу не найдена.')
  }
  return Number(matchingTeam.id)
}

function applySeasonSelectionFromNavigation() {
  const requestedSeasonId = String(
    window.history.state?.seasonId || route.query.seasonId || '',
  ).trim()
  if (!requestedSeasonId) {
    selectedSeasonKey.value = 'all'
    return
  }

  const hasRequestedSeason = (teamProfile.value?.seasons || []).some(
    (season) => String(season.id) === requestedSeasonId
  )
  selectedSeasonKey.value = hasRequestedSeason ? requestedSeasonId : 'all'
}

function matchLocation(match) {
  const teamSlug = String(route.params.slug)
  return matchPageLocation(match.matchId, {
    returnContext: 'team-profile',
    teamSlug,
    teamId: activeTeamId.value,
    ...(selectedSeasonKey.value !== 'all' ? { seasonId: selectedSeasonKey.value } : {}),
  })
}

async function loadSeasonRoster() {
  if (!selectedSeason.value) {
    resetSeasonRoster()
    return
  }

  seasonRosterLoading.value = true
  seasonRosterError.value = ''
  seasonRoster.value = []
  seasonRosterStatus.value = 'WAITING_FILL'
  seasonRepresentativeName.value = ''

  try {
    const payload = await catalogApi.getTeamSeasonRoster(activeTeamId.value, selectedSeason.value.id)
    seasonRoster.value = Array.isArray(payload?.players) ? payload.players : []
    seasonRosterStatus.value = payload?.status || 'WAITING_FILL'
    seasonRepresentativeName.value = String(payload?.representativeName || '').trim()
  } catch (error) {
    seasonRosterError.value = error.message || 'Не удалось загрузить заявку сезона.'
  } finally {
    seasonRosterLoading.value = false
  }
}

function openSeasonRosterModal() {
  if (seasonRosterStatus.value !== 'APPROVED' || !seasonRoster.value.length) {
    return
  }
  seasonRosterModalOpen.value = true
}

function resetSeasonRoster() {
  seasonRosterModalOpen.value = false
  seasonRosterLoading.value = false
  seasonRosterError.value = ''
  seasonRoster.value = []
  seasonRosterStatus.value = 'WAITING_FILL'
  seasonRepresentativeName.value = ''
}

function closeSeasonRosterModal() {
  seasonRosterModalOpen.value = false
}

function initials(value) {
  const parts = String(value || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'FC'
  return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function formatBirthDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatHistoryDate(value) {
  if (!value) return 'Дата не указана'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Дата не указана'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function resultClass(result) {
  return {
    'is-win': result === 'W',
    'is-loss': result === 'L',
    'is-draw': result === 'D',
  }
}

function resultShortLabel(result) {
  if (result === 'W') return 'В'
  if (result === 'L') return 'П'
  return 'Н'
}

watch(() => route.params.slug, loadTeamProfile, { immediate: true })
watch(() => route.query.seasonId, () => {
  applySeasonSelectionFromNavigation()
})
watch(selectedSeasonKey, async () => {
  currentPage.value = 1
  await loadSeasonRoster()
})
</script>

<style scoped>
.team-profile-backline { display: flex; }
.team-profile-back { min-height: 40px; padding: 8px 0; border: 0; background: transparent; color: #8da7df; font-size: .7rem; box-shadow: none; }
.team-profile-hero { display: block; }
.team-profile-hero-main { display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: 20px; align-items: center; }
.team-profile-logo-shell { width: 72px; height: 72px; display: grid; place-items: center; overflow: hidden; border: 1px solid rgba(124, 163, 255, .2); border-radius: 12px; background: rgba(124, 163, 255, .05); color: #8da7df; font-size: 1.25rem; font-weight: 700; }
.team-profile-logo { width: 100%; height: 100%; object-fit: contain; }
.team-profile-copy { min-width: 0; }
.team-profile-copy-top { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 8px; }
.team-profile-copy-top .catalog-kicker { margin: 0; }
.team-profile-title { overflow-wrap: anywhere; line-height: 1.25; }
.team-profile-subtitle { margin: 8px 0 0; color: var(--muted); font-size: .72rem; }
.team-profile-status { color: var(--muted); font-size: .59rem; }
.team-profile-status.is-active { color: var(--brand); }
.team-profile-status::before { content: ''; display: inline-block; width: 5px; height: 5px; margin-right: 5px; border-radius: 50%; background: currentColor; vertical-align: middle; }
.team-profile-controls-actions { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; width: 100%; min-width: 0; }
.team-profile-controls-primary { display: flex; align-items: end; gap: 12px; flex-wrap: wrap; min-width: 0; }
.team-profile-select-wrap { order: -1; width: 300px; flex: 0 1 300px; }
.team-profile-season-contact { display: grid; gap: 5px; text-align: right; }
.team-profile-season-contact-label { color: var(--muted); font-size: .62rem; }
.team-profile-season-contact strong { font-size: .74rem; font-weight: 600; overflow-wrap: anywhere; }
.team-profile-roster-btn { min-height: 40px; padding: 8px 14px; border: 1px solid rgba(97, 232, 162, .25); border-radius: 8px; background: rgba(97, 232, 162, .06); color: var(--brand); font-size: .7rem; cursor: pointer; }
.team-profile-stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.team-profile-stat-card { display: grid; align-content: start; gap: 10px; min-width: 0; padding: 18px 20px; border-right: 1px solid rgba(124, 163, 255, .13); }
.team-profile-stat-card:last-child { border-right: 0; }
.team-profile-stat-label { color: var(--muted); font-size: .64rem; }
.team-profile-stat-card strong { font-size: 1.5rem; line-height: 1; font-variant-numeric: tabular-nums; }
.team-profile-stat-card > .muted { color: #8c9abb; font-size: .6rem; line-height: 1.5; }
.team-profile-form-strip { display: flex; flex-wrap: wrap; gap: 4px; }
.team-profile-form-pill, .team-profile-result-pill { display: inline-flex; align-items: center; justify-content: center; border: 1px solid rgba(124, 163, 255, .2); border-radius: 5px; font-size: .59rem; font-weight: 700; }
.team-profile-form-pill { width: 23px; height: 24px; }
.team-profile-result-pill { min-height: 25px; padding: 4px 8px; }
.is-win { color: var(--brand); background: rgba(97, 232, 162, .06); border-color: rgba(97, 232, 162, .2); }
.is-loss { color: #e58d98; background: rgba(229, 141, 152, .06); border-color: rgba(229, 141, 152, .2); }
.is-draw { color: #9caedd; background: rgba(156, 174, 221, .06); border-color: rgba(156, 174, 221, .2); }
.team-profile-section-head .section-title { margin: 0; font-size: .85rem; }
.team-profile-history-head, .team-profile-history-row { display: grid; grid-template-columns: minmax(140px, .9fr) minmax(140px, 1.2fr) 70px 160px; align-items: center; gap: 18px; padding: 0 20px; }
.team-profile-history-head { min-height: 38px; border-block: 1px solid rgba(124, 163, 255, .12); background: rgba(124, 163, 255, .035); color: var(--muted); font-size: .62rem; }
.team-profile-history-row { min-height: 64px; border-bottom: 1px solid rgba(124, 163, 255, .1); }
.team-profile-history-row:last-child { border-bottom: 0; }
.team-profile-history-row:hover { background: rgba(97, 232, 162, .035); }
.team-profile-history-meta { display: grid; gap: 5px; min-width: 0; }
.team-profile-history-date { font-size: .7rem; }
.team-profile-history-tournament { color: #8c9abb; font-size: .61rem; overflow-wrap: anywhere; }
.team-profile-history-matchup { min-width: 0; font-size: .76rem; overflow-wrap: anywhere; }
.team-profile-history-score { font-size: 1rem; font-weight: 700; font-variant-numeric: tabular-nums; }
.team-profile-history-statusline { display: grid; grid-template-columns: 86px 1fr; align-items: center; gap: 12px; }
.team-profile-history-statusline .team-profile-result-pill { width: 100%; white-space: nowrap; }
.team-profile-history-statusline > .muted { color: var(--muted); font-size: .61rem; }
.team-profile-pagination { border-top: 1px solid rgba(124, 163, 255, .12); padding: 12px 20px; }
.team-profile-matches > .empty-text { padding: 0 20px 16px; font-size: .75rem; }
.team-profile-modal-backdrop { position: fixed; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(3, 7, 20, .78); backdrop-filter: blur(5px); z-index: 200; }
.team-profile-modal { width: min(720px, 100%); max-height: calc(100dvh - 40px); overflow: auto; padding: 22px; border: 1px solid rgba(124, 163, 255, .24); border-radius: 14px; background: #0d1730; }
.team-profile-modal-head { display: flex; align-items: start; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.team-profile-modal-head > div { min-width: 0; }
.team-profile-modal-head .eyebrow { margin: 0 0 8px; color: var(--brand); font-size: .6rem; }
.team-profile-modal-head .section-title { margin: 0; font-size: 1rem; overflow-wrap: anywhere; }
.team-profile-modal-head button { min-height: 44px; font-size: .7rem; }
.team-profile-season-roster-headline, .team-profile-season-roster-row { display: grid; grid-template-columns: minmax(0, 1.5fr) 100px minmax(0, 1fr); align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid rgba(124, 163, 255, .12); }
.team-profile-season-roster-headline { color: var(--muted); font-size: .6rem; }
.team-profile-season-roster-row { font-size: .7rem; overflow-wrap: anywhere; }
.team-profile-season-roster-row .muted { color: var(--muted); font-size: .65rem; }
@media (max-width: 900px) {
  .team-profile-stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .team-profile-stat-card:nth-child(2) { border-right: 0; }
  .team-profile-stat-card:nth-child(-n+2) { border-bottom: 1px solid rgba(124, 163, 255, .13); }
  .team-profile-history-head { display: none; }
  .team-profile-history-row { grid-template-columns: minmax(0, 1fr) auto; gap: 10px; padding: 14px; }
  .team-profile-history-meta { grid-column: 1 / -1; grid-row: 1; }
  .team-profile-history-matchup { grid-column: 1; grid-row: 2; }
  .team-profile-history-score { grid-column: 2; grid-row: 2; text-align: right; }
  .team-profile-history-statusline { grid-column: 1 / -1; grid-row: 3; }
}
@media (max-width: 540px) {
  .team-profile-hero-main { grid-template-columns: 54px minmax(0, 1fr); gap: 12px; }
  .team-profile-logo-shell { width: 54px; height: 60px; font-size: 1rem; }
  .team-profile-controls-primary, .team-profile-select-wrap { width: 100%; }
  .team-profile-select-wrap { flex-basis: 100%; }
  .team-profile-season-contact { text-align: left; }
  .team-profile-stat-card { padding: 14px; }
  .team-profile-form-strip { gap: 3px; }
  .team-profile-form-pill { width: 16px; height: 20px; font-size: .53rem; }
  .team-profile-pagination { display: grid; grid-template-columns: 1fr 1fr; }
  .team-profile-pagination > span { grid-column: 1 / -1; grid-row: 1; text-align: center; }
  .team-profile-pagination > button { grid-row: 2; }
  .team-profile-modal-backdrop { padding: 10px; }
  .team-profile-modal { padding: 16px; max-height: calc(100dvh - 20px); }
  .team-profile-season-roster-headline { display: none; }
  .team-profile-season-roster-row { grid-template-columns: 1fr 1fr; gap: 6px; }
  .team-profile-season-roster-row strong { grid-column: 1 / -1; }
}
</style>
