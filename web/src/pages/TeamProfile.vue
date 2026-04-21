<template>
  <section class="section-wrap team-profile-page">
    <div class="team-profile-backline">
      <RouterLink class="btn-ghost team-profile-back" to="/teams">← К списку команд</RouterLink>
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
      <article class="card team-profile-hero">
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
              <p class="eyebrow">Профиль команды</p>
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
          </div>
        </div>
      </article>

      <article class="card team-profile-controls">
        <div class="team-profile-controls-copy">
          <p class="eyebrow team-profile-controls-kicker">Статистика</p>
        </div>

        <div class="team-profile-controls-actions">
          <label class="team-profile-select-wrap">
            <select v-model="selectedSeasonKey">
              <option value="all">Все сезоны</option>
              <option v-for="season in teamProfile.seasons" :key="season.id" :value="String(season.id)">
                {{ season.name }}
              </option>
            </select>
          </label>
        </div>
      </article>

      <article class="card team-profile-section team-profile-matches">
        <div class="section-head team-profile-section-head team-profile-match-head">
          <div class="team-profile-match-head-line">
            <h2 class="section-title team-profile-match-title">Последние матчи</h2>
          </div>
        </div>

        <div v-if="paginatedMatches.length" class="team-profile-history-table">
          <div class="team-profile-history-head muted">
            <span>Когда</span>
            <span>Матч</span>
            <span>Счёт</span>
            <span>Статус</span>
          </div>
          <RouterLink
            v-for="match in paginatedMatches"
            :key="match.matchId"
            :to="{
              path: `/match/${match.matchId}`,
              query: {
                from: 'team-profile',
                teamId: String(teamProfile.id),
              },
            }"
            class="team-profile-history-row"
          >
            <span class="team-profile-history-meta">
              <span class="team-profile-history-date">{{ formatHistoryDate(match.kickoffAt) }}</span>
              <span class="team-profile-history-tournament">{{ match.seasonName }}</span>
            </span>
            <span class="team-profile-history-matchup">
              <strong>{{ teamProfile.shortName || teamProfile.name }}</strong>
              <span class="muted">vs</span>
              <span>{{ match.opponentName }}</span>
            </span>
            <span class="team-profile-history-score">{{ match.teamScore }}:{{ match.opponentScore }}</span>
            <span class="team-profile-history-statusline">
              <span class="team-profile-result-pill" :class="resultClass(match.resultCode)">{{ match.resultLabel }}</span>
              <span class="muted">{{ match.home ? 'Дом' : 'Выезд' }}</span>
            </span>
          </RouterLink>
        </div>
        <p v-else class="empty-text">У команды пока нет завершенных матчей.</p>

        <div v-if="totalPages > 1" class="team-profile-pagination">
          <button class="btn-ghost" type="button" :disabled="currentPage === 1" @click="currentPage -= 1">Назад</button>
          <button class="btn-ghost" type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">Вперёд</button>
        </div>
      </article>

      <div class="team-profile-stats-grid">
        <article class="card team-profile-stat-card">
          <span class="team-profile-stat-label">Матчи</span>
          <strong>{{ selectedSummary.matchesPlayed }}</strong>
          <span class="muted">{{ selectedSeason ? 'В выбранном сезоне' : 'По всем сезонам' }}</span>
        </article>
        <article class="card team-profile-stat-card">
          <span class="team-profile-stat-label">Победы</span>
          <strong>{{ selectedSummary.wins }}</strong>
          <span class="muted">Ничьи: {{ selectedSummary.draws }} · Поражения: {{ selectedSummary.losses }}</span>
        </article>
        <article class="card team-profile-stat-card">
          <span class="team-profile-stat-label">Голы</span>
          <strong>{{ selectedSummary.goalsFor }}</strong>
          <span class="muted">Пропущено: {{ selectedSummary.goalsAgainst }}</span>
        </article>
        <article class="card team-profile-stat-card">
          <span class="team-profile-stat-label">Форма</span>
          <div v-if="selectedForm.length" class="team-profile-form-strip">
            <span
              v-for="(result, index) in selectedForm"
              :key="`form-${index}`"
              class="team-profile-form-pill"
              :class="resultClass(result)"
            >
              {{ resultShortLabel(result) }}
            </span>
          </div>
          <span v-else class="muted">Нет завершенных матчей</span>
        </article>
      </div>
    </template>

    <div v-if="seasonRosterModalOpen" class="team-profile-modal-backdrop" @click.self="closeSeasonRosterModal">
      <article class="card team-profile-modal">
        <div class="team-profile-modal-head">
          <div>
            <p class="eyebrow">Заявка на сезон</p>
            <h3 class="section-title">{{ selectedSeason?.name }}</h3>
          </div>
          <button class="btn-ghost" type="button" @click="closeSeasonRosterModal">Закрыть</button>
        </div>

        <div v-if="seasonRosterLoading" class="team-profile-modal-state muted">Загружаем состав...</div>
        <div v-else-if="seasonRosterError" class="team-profile-modal-state error-text">{{ seasonRosterError }}</div>
        <div v-else-if="seasonRoster.length" class="team-profile-modal-list">
          <div class="team-profile-modal-row" v-for="player in seasonRoster" :key="player.id">
            <div class="team-profile-modal-player">
              <div class="team-profile-modal-avatar" :class="{ 'is-empty': !player.photoDataUrl }">
                <img v-if="player.photoDataUrl" :src="player.photoDataUrl" :alt="player.fullName" />
                <span v-else>{{ initials(player.fullName) }}</span>
              </div>
              <div>
                <strong>{{ player.fullName }}</strong>
                <div v-if="player.birthDate" class="muted">{{ formatDate(player.birthDate) }}</div>
              </div>
            </div>
            <span v-if="player.goalkeeper" class="team-profile-role-badge">Вратарь</span>
          </div>
        </div>
        <p v-else class="empty-text">В выбранном сезоне нет активной заявки.</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuth } from '../store/auth'

const PAGE_SIZE = 5

const route = useRoute()
const { optionalAuthApiRequest } = useAuth()

const loading = ref(false)
const errorText = ref('')
const teamProfile = ref(null)
const selectedSeasonKey = ref('all')
const currentPage = ref(1)
const seasonRosterModalOpen = ref(false)
const seasonRosterLoading = ref(false)
const seasonRosterError = ref('')
const seasonRoster = ref([])

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
const totalPages = computed(() => Math.max(1, Math.ceil(chronologyMatches.value.length / PAGE_SIZE)))
const paginatedMatches = computed(() => {
  const startIndex = (currentPage.value - 1) * PAGE_SIZE
  return chronologyMatches.value.slice(startIndex, startIndex + PAGE_SIZE)
})

async function loadTeamProfile() {
  const teamId = Number(route.params.id)
  if (!Number.isFinite(teamId) || teamId <= 0) {
    errorText.value = 'Некорректный идентификатор команды.'
    teamProfile.value = null
    return
  }

  loading.value = true
  errorText.value = ''
  selectedSeasonKey.value = 'all'
  currentPage.value = 1
  closeSeasonRosterModal()

  try {
    teamProfile.value = await optionalAuthApiRequest(`/api/teams/${encodeURIComponent(teamId)}`, { method: 'GET' })
  } catch (error) {
    errorText.value = error.message || 'Не удалось загрузить профиль команды.'
    teamProfile.value = null
  } finally {
    loading.value = false
  }
}

async function openSeasonRosterModal() {
  if (!selectedSeason.value) return

  seasonRosterModalOpen.value = true
  seasonRosterLoading.value = true
  seasonRosterError.value = ''
  seasonRoster.value = []

  try {
    seasonRoster.value = await optionalAuthApiRequest(
      `/api/teams/${encodeURIComponent(route.params.id)}/seasons/${encodeURIComponent(selectedSeason.value.id)}/roster`,
      { method: 'GET' }
    )
  } catch (error) {
    seasonRosterError.value = error.message || 'Не удалось загрузить заявку сезона.'
  } finally {
    seasonRosterLoading.value = false
  }
}

function closeSeasonRosterModal() {
  seasonRosterModalOpen.value = false
  seasonRosterLoading.value = false
  seasonRosterError.value = ''
  seasonRoster.value = []
}

function initials(value) {
  const parts = String(value || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'FC'
  return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function formatDate(value) {
  if (!value) return 'дата не указана'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'дата не указана'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  if (!value) return 'Дата не указана'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Дата не указана'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
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

watch(() => route.params.id, loadTeamProfile, { immediate: true })
watch(selectedSeasonKey, () => {
  closeSeasonRosterModal()
})
</script>

<style scoped>
.team-profile-page {
  display: grid;
  gap: 18px;
}

.team-profile-backline {
  display: flex;
  justify-content: flex-start;
}

.team-profile-back {
  min-height: 42px;
}

.team-profile-state {
  min-height: 180px;
  display: grid;
  place-items: center;
}

.team-profile-hero,
.team-profile-controls,
.team-profile-stat-card,
.team-profile-matches,
.team-profile-modal {
  background:
    linear-gradient(112deg, rgba(97, 232, 162, 0.06), rgba(97, 232, 162, 0) 30%),
    linear-gradient(180deg, rgba(20, 31, 69, 0.98), rgba(13, 20, 44, 1));
}

.team-profile-hero {
  display: grid;
  gap: 16px;
}

.team-profile-hero-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.team-profile-logo-shell {
  width: 104px;
  height: 104px;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, 0.24);
  background: linear-gradient(180deg, rgba(23, 34, 71, 0.98), rgba(13, 21, 48, 1));
  display: grid;
  place-items: center;
  color: rgba(151, 176, 255, 0.9);
  font-size: 1.7rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.team-profile-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.team-profile-copy {
  display: grid;
  gap: 8px;
}

.team-profile-copy-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.team-profile-title {
  margin: 0;
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  line-height: 1.04;
}

.team-profile-subtitle {
  margin: 0;
  color: var(--muted);
}

.team-profile-status,
.team-profile-role-badge,
.team-profile-result-pill,
.team-profile-form-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 800;
}

.team-profile-status,
.team-profile-role-badge {
  border: 1px solid rgba(124, 163, 255, 0.18);
  background: rgba(124, 163, 255, 0.08);
  color: rgba(229, 235, 255, 0.9);
}

.team-profile-status.is-active {
  border-color: rgba(97, 232, 162, 0.24);
  background: rgba(97, 232, 162, 0.12);
  color: rgba(223, 255, 238, 0.95);
}

.team-profile-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.team-profile-controls-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.team-profile-controls-kicker {
  margin: 0;
}

.team-profile-roster-btn {
  min-height: 52px;
  padding: 0 22px;
  border: 1px solid rgba(97, 232, 162, 0.24);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(22, 34, 72, 0.98), rgba(14, 22, 48, 1));
  color: rgba(236, 244, 255, 0.96);
  font: inherit;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 14px 28px rgba(4, 10, 28, 0.2);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.team-profile-roster-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(97, 232, 162, 0.42);
  background:
    linear-gradient(180deg, rgba(25, 40, 82, 0.98), rgba(16, 27, 58, 1));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.07),
    0 18px 32px rgba(4, 10, 28, 0.24);
}

.team-profile-roster-btn:active {
  transform: translateY(0);
}

.team-profile-select-wrap {
  display: grid;
  gap: 0;
}

.team-profile-select-wrap select {
  min-width: 260px;
}

.team-profile-section {
  display: grid;
  gap: 12px;
}

.team-profile-match-head {
  align-items: center;
  margin-bottom: -6px;
}

.team-profile-match-head-line {
  display: flex;
  align-items: baseline;
  gap: 0;
  flex-wrap: wrap;
}

.team-profile-match-title {
  margin: 0;
}

.team-profile-history-table {
  display: grid;
  gap: 8px;
}

.team-profile-history-head,
.team-profile-history-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.1fr) minmax(260px, 1.25fr) 88px 180px;
  gap: 14px;
  align-items: center;
}

.team-profile-history-head {
  padding: 0 12px 8px;
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.team-profile-history-row {
  padding: 14px 12px;
  border: 1px solid rgba(124, 163, 255, 0.15);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(17, 25, 55, 0.92), rgba(11, 18, 39, 0.98));
  color: inherit;
  text-decoration: none;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.team-profile-history-row:hover {
  transform: translateY(-1px);
  border-color: rgba(97, 232, 162, 0.28);
  box-shadow: 0 16px 28px rgba(3, 8, 24, 0.22);
}

.team-profile-history-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.team-profile-history-date {
  font-weight: 700;
  line-height: 1.2;
}

.team-profile-history-tournament {
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.2;
}

.team-profile-history-matchup,
.team-profile-history-statusline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.team-profile-history-statusline {
  display: grid;
  grid-template-columns: 132px auto;
  justify-items: start;
  gap: 12px;
}

.team-profile-history-statusline .team-profile-result-pill {
  min-width: 132px;
  white-space: nowrap;
}

.team-profile-history-score {
  font-size: 1.3rem;
  font-weight: 800;
}

.team-profile-pagination {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.team-profile-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.team-profile-stat-card {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 140px;
}

.team-profile-stat-label {
  color: rgba(151, 176, 255, 0.84);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.team-profile-stat-card strong {
  font-size: clamp(1.8rem, 2.8vw, 2.5rem);
  line-height: 1;
}

.team-profile-form-strip {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.team-profile-form-pill.is-win,
.team-profile-result-pill.is-win {
  background: rgba(97, 232, 162, 0.16);
  color: rgba(223, 255, 238, 0.95);
}

.team-profile-form-pill.is-loss,
.team-profile-result-pill.is-loss {
  background: rgba(255, 96, 96, 0.18);
  color: rgba(255, 228, 228, 0.95);
}

.team-profile-form-pill.is-draw,
.team-profile-result-pill.is-draw {
  background: rgba(151, 176, 255, 0.18);
  color: rgba(239, 243, 255, 0.95);
}

.team-profile-modal-backdrop {
  position: fixed;
  inset: 0;
  padding: 32px 16px;
  background: rgba(5, 9, 20, 0.72);
  backdrop-filter: blur(10px);
  display: grid;
  place-items: center;
  z-index: 40;
}

.team-profile-modal {
  width: min(720px, 100%);
  max-height: min(82vh, 760px);
  overflow: auto;
}

.team-profile-modal-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
  margin-bottom: 16px;
}

.team-profile-modal-state {
  min-height: 120px;
  display: grid;
  place-items: center;
}

.team-profile-modal-list {
  display: grid;
  gap: 10px;
}

.team-profile-modal-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid rgba(124, 163, 255, 0.15);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(17, 25, 55, 0.92), rgba(11, 18, 39, 0.98));
}

.team-profile-modal-player {
  display: flex;
  align-items: center;
  gap: 12px;
}

.team-profile-modal-avatar {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, 0.15);
  background: linear-gradient(180deg, rgba(23, 34, 71, 0.98), rgba(13, 21, 48, 1));
  display: grid;
  place-items: center;
  color: rgba(151, 176, 255, 0.9);
  font-size: 0.9rem;
  font-weight: 800;
}

.team-profile-modal-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 1100px) {
  .team-profile-history-head,
  .team-profile-history-row {
    grid-template-columns: minmax(188px, 1fr) minmax(200px, 1.1fr) 72px 148px;
  }
}

@media (max-width: 900px) {
  .team-profile-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .team-profile-stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .team-profile-history-head {
    display: none;
  }

  .team-profile-history-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

@media (max-width: 720px) {
  .team-profile-hero-main {
    grid-template-columns: 1fr;
  }

  .team-profile-logo-shell {
    width: 88px;
    height: 88px;
  }

  .team-profile-stats-grid {
    grid-template-columns: 1fr;
  }

  .team-profile-select-wrap,
  .team-profile-select-wrap select {
    width: 100%;
    min-width: 0;
  }

  .team-profile-modal-row,
  .team-profile-modal-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>