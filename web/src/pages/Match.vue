<template>
  <section class="section-wrap">
    <article class="card" v-if="loading">
      <p class="muted-text">Загрузка матча...</p>
    </article>

    <article class="card" v-else-if="pageError">
      <h2 class="section-title">Матч недоступен</h2>
      <p class="error-text">{{ pageError }}</p>
      <router-link class="btn-ghost" to="/">Вернуться на главную</router-link>
    </article>

    <article class="card match-screen" v-else-if="match">
      <div class="match-topbar">
        <router-link class="btn-ghost" to="/">← На главную</router-link>
        <span class="match-status-badge">{{ matchStatusLabel(match.protocol?.status) }}</span>
      </div>

      <div class="match-hero">
        <div class="match-team-card">
          <img v-if="match.homeTeam.logoDataUrl" :src="match.homeTeam.logoDataUrl" :alt="match.homeTeam.name" class="match-team-logo" />
          <h2>{{ match.homeTeam.name }}</h2>
        </div>

        <div class="match-score-card">
          <p class="match-date">{{ formatDateTime(match.kickoffAt) }}</p>
          <div class="match-score">{{ matchScoreLabel(match.protocol) }}</div>
          <p class="muted-text">{{ match.seasonName }} · {{ match.tourName }}</p>
        </div>

        <div class="match-team-card">
          <img v-if="match.awayTeam.logoDataUrl" :src="match.awayTeam.logoDataUrl" :alt="match.awayTeam.name" class="match-team-logo" />
          <h2>{{ match.awayTeam.name }}</h2>
        </div>
      </div>

      <div class="match-meta-grid">
        <article class="match-meta-card">
          <span class="muted-text">Статус</span>
          <strong>{{ matchStatusLabel(match.protocol?.status) }}</strong>
        </article>
        <article class="match-meta-card">
          <span class="muted-text">Лучший игрок</span>
          <strong>{{ match.protocol?.bestPlayerName || '—' }}</strong>
        </article>
        <article class="match-meta-card">
          <span class="muted-text">Начало</span>
          <strong>{{ formatDateTime(match.protocol?.startedAt || match.kickoffAt) }}</strong>
        </article>
        <article class="match-meta-card">
          <span class="muted-text">Окончание</span>
          <strong>{{ formatDateTime(match.protocol?.finishedAt) }}</strong>
        </article>
      </div>

      <article class="match-section">
        <div class="section-head match-section-head">
          <h3 class="section-title">События матча</h3>
          <span class="muted-text" v-if="match.protocol?.events?.length">{{ match.protocol.events.length }} событий</span>
        </div>

        <div class="event-list" v-if="match.protocol?.events?.length">
          <article class="event-item" v-for="event in match.protocol.events" :key="event.id">
            <div class="event-minute">{{ eventMinuteLabel(event) }}</div>
            <div class="event-body">
              <strong>{{ eventTitle(event) }}</strong>
              <p class="muted-text">{{ eventDescription(event) }}</p>
            </div>
          </article>
        </div>
        <p class="empty-text" v-else>Протокол матча пока не заполнен.</p>
      </article>

      <article class="match-section">
        <h3 class="section-title">Примечание судьи / администратора</h3>
        <p class="match-notes" v-if="match.protocol?.notes">{{ match.protocol.notes }}</p>
        <p class="empty-text" v-else>Дополнительные заметки по матчу пока не заполнены.</p>
      </article>

      <section class="lineup-grid">
        <article class="match-section lineup-card" v-for="lineup in lineupCards" :key="lineup.teamId">
          <div class="section-head match-section-head">
            <div>
              <h3 class="section-title">Состав: {{ lineup.teamName }}</h3>
              <p class="muted-text">Доступны только игроки команды, заявленные на сезон {{ match.seasonName }}.</p>
            </div>
            <span class="muted-text">{{ lineupSubmittedLabel(lineup) }}</span>
          </div>

          <ol class="lineup-list" v-if="lineup.players?.length">
            <li class="lineup-item" v-for="player in lineup.players" :key="player.playerId">
              <span class="lineup-order">{{ player.sortOrder }}</span>
              <span>{{ player.playerName }}</span>
            </li>
          </ol>
          <p class="empty-text" v-else>Заявка этой команды пока не подана.</p>

          <div v-if="canEditLineup(lineup.teamId)" class="lineup-editor">
            <div class="lineup-editor-head">
              <strong>Редактирование заявки</strong>
              <span class="muted-text">Выбрано: {{ selectedCount(lineup.teamId) }}</span>
            </div>

            <div class="lineup-checklist" v-if="lineup.availablePlayers?.length">
              <label class="lineup-choice" v-for="player in lineup.availablePlayers" :key="player.playerId">
                <input
                  type="checkbox"
                  :checked="isPlayerSelected(lineup.teamId, player.playerId)"
                  @change="toggleLineupPlayer(lineup.teamId, player.playerId)"
                  :disabled="Boolean(lineupSaving[lineup.teamId])"
                />
                <span>{{ player.playerName }}</span>
                <span class="lineup-chip" v-if="selectionOrder(lineup.teamId, player.playerId) > 0">
                  #{{ selectionOrder(lineup.teamId, player.playerId) }}
                </span>
              </label>
            </div>
            <p class="empty-text" v-else>Для этой команды нет игроков, одновременно входящих в состав и заявленных на сезон.</p>

            <p class="error-text" v-if="lineupErrors[lineup.teamId]">{{ lineupErrors[lineup.teamId] }}</p>
            <p class="muted-text" v-else-if="lineupNotices[lineup.teamId]">{{ lineupNotices[lineup.teamId] }}</p>

            <div class="lineup-actions">
              <button class="btn-primary" type="button" @click="saveLineup(lineup.teamId)" :disabled="Boolean(lineupSaving[lineup.teamId])">
                {{ lineupSaving[lineup.teamId] ? 'Сохранение...' : 'Сохранить заявку' }}
              </button>
              <button class="btn-ghost" type="button" @click="clearLineup(lineup.teamId)" :disabled="Boolean(lineupSaving[lineup.teamId])">
                Очистить
              </button>
            </div>
          </div>
        </article>
      </section>
    </article>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../store/auth'

const route = useRoute()
const { optionalAuthApiRequest, authorizedApiRequest, user, hasRole } = useAuth()

const match = ref(null)
const loading = ref(false)
const pageError = ref('')
const lineupDrafts = ref({})
const lineupSaving = ref({})
const lineupErrors = ref({})
const lineupNotices = ref({})

const lineupCards = computed(() => {
  if (!match.value) return []
  return [match.value.homeLineup, match.value.awayLineup].filter(Boolean)
})

watch(
  () => route.params.id,
  async (matchId) => {
    if (!matchId) {
      match.value = null
      return
    }
    await loadMatch(matchId)
  },
  { immediate: true }
)

async function loadMatch(matchId) {
  loading.value = true
  pageError.value = ''

  try {
    const payload = await optionalAuthApiRequest(`/api/matches/${encodeURIComponent(matchId)}`, {
      method: 'GET',
    })
    match.value = payload
    syncLineupDrafts(payload)
  } catch (error) {
    match.value = null
    pageError.value = error.message || 'Не удалось загрузить матч.'
  } finally {
    loading.value = false
  }
}

function syncLineupDrafts(payload) {
  const nextDrafts = {}
  for (const lineup of [payload?.homeLineup, payload?.awayLineup].filter(Boolean)) {
    nextDrafts[lineup.teamId] = Array.isArray(lineup.players) ? lineup.players.map((player) => player.playerId) : []
  }
  lineupDrafts.value = nextDrafts
  lineupErrors.value = {}
}

function canEditLineup(teamId) {
  if (!user.value) return false
  if (hasRole('SUPER_ADMIN')) return true
  if (!hasRole('TEAM_REP')) return false
  return String(user.value.teamId || '') === String(teamId) && Boolean(user.value.teamScope?.canEditRoster)
}

function selectedPlayers(teamId) {
  return Array.isArray(lineupDrafts.value[teamId]) ? lineupDrafts.value[teamId] : []
}

function selectedCount(teamId) {
  return selectedPlayers(teamId).length
}

function isPlayerSelected(teamId, playerId) {
  return selectedPlayers(teamId).includes(playerId)
}

function selectionOrder(teamId, playerId) {
  return selectedPlayers(teamId).findIndex((value) => value === playerId) + 1
}

function toggleLineupPlayer(teamId, playerId) {
  const current = [...selectedPlayers(teamId)]
  const existingIndex = current.findIndex((value) => value === playerId)
  if (existingIndex >= 0) {
    current.splice(existingIndex, 1)
  } else {
    current.push(playerId)
  }
  lineupDrafts.value = {
    ...lineupDrafts.value,
    [teamId]: current,
  }
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }
}

function clearLineup(teamId) {
  lineupDrafts.value = {
    ...lineupDrafts.value,
    [teamId]: [],
  }
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }
}

async function saveLineup(teamId) {
  if (!match.value) return

  lineupSaving.value = {
    ...lineupSaving.value,
    [teamId]: true,
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }

  try {
    const payload = await authorizedApiRequest(
      `/api/matches/${encodeURIComponent(match.value.id)}/lineups/${encodeURIComponent(teamId)}`,
      {
        method: 'PUT',
        body: JSON.stringify({ playerIds: selectedPlayers(teamId) }),
      }
    )
    match.value = payload
    syncLineupDrafts(payload)
    lineupNotices.value = {
      ...lineupNotices.value,
      [teamId]: selectedPlayers(teamId).length ? 'Заявка сохранена.' : 'Заявка очищена.',
    }
  } catch (error) {
    lineupErrors.value = {
      ...lineupErrors.value,
      [teamId]: error.message || 'Не удалось сохранить заявку.',
    }
  } finally {
    lineupSaving.value = {
      ...lineupSaving.value,
      [teamId]: false,
    }
  }
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function matchStatusLabel(status) {
  if (status === 'LIVE') return 'Матч идет'
  if (status === 'FINISHED') return 'Матч завершен'
  if (status === 'VERIFIED') return 'Протокол подтвержден'
  if (status === 'LINEUPS_SUBMITTED') return 'Заявки поданы'
  return 'Матч запланирован'
}

function matchScoreLabel(protocol) {
  if (Number.isInteger(protocol?.homeScore) && Number.isInteger(protocol?.awayScore)) {
    return `${protocol.homeScore} : ${protocol.awayScore}`
  }
  return '— : —'
}

function eventMinuteLabel(event) {
  if (!Number.isInteger(event?.minute)) return '—'
  if (Number.isInteger(event.extraMinute) && event.extraMinute > 0) {
    return `${event.minute}+${event.extraMinute}'`
  }
  return `${event.minute}'`
}

function eventTitle(event) {
  if (event.eventType === 'GOAL') return 'Гол'
  if (event.eventType === 'OWN_GOAL') return 'Автогол'
  if (event.eventType === 'PENALTY_GOAL') return 'Гол с пенальти'
  if (event.eventType === 'MISSED_PENALTY') return 'Нереализованный пенальти'
  if (event.eventType === 'YELLOW_CARD') return 'Желтая карточка'
  if (event.eventType === 'RED_CARD') return 'Красная карточка'
  if (event.eventType === 'SECOND_YELLOW_RED') return 'Вторая желтая и удаление'
  if (event.eventType === 'SUBSTITUTION') return 'Замена'
  if (event.eventType === 'START') return 'Начало матча'
  if (event.eventType === 'END') return 'Окончание матча'
  return event.eventType || 'Событие'
}

function eventDescription(event) {
  const parts = []
  if (event.teamName) parts.push(event.teamName)
  if (event.playerName) parts.push(event.playerName)
  if (event.relatedPlayerName) parts.push(`→ ${event.relatedPlayerName}`)
  if (event.valueText) parts.push(event.valueText)
  return parts.join(' · ') || 'Без дополнительных деталей'
}

function lineupSubmittedLabel(lineup) {
  if (!lineup?.submittedAt) return 'Не подана'
  return `Подана ${formatDateTime(lineup.submittedAt)}`
}
</script>

<style scoped>
.match-screen {
  display: grid;
  gap: 22px;
}

.match-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.match-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  border: 1px solid rgba(97, 232, 162, 0.2);
  font-size: 0.82rem;
  font-weight: 700;
}

.match-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.match-team-card,
.match-score-card,
.match-meta-card,
.event-item,
.match-section {
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
}

.match-team-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  text-align: center;
}

.match-team-card h2 {
  margin: 0;
  font-size: 1.2rem;
}

.match-team-logo {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5ede8;
}

.match-score-card {
  min-width: 220px;
  text-align: center;
}

.match-date {
  margin: 0 0 8px;
  color: var(--muted);
}

.match-score {
  font-size: 2.3rem;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 10px;
}

.match-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.match-meta-card {
  display: grid;
  gap: 6px;
}

.match-meta-card strong {
  font-size: 1rem;
}

.match-section {
  display: grid;
  gap: 14px;
}

.match-section-head {
  margin-bottom: 0;
}

.event-list {
  display: grid;
  gap: 10px;
}

.event-item {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.event-minute {
  font-weight: 700;
  color: var(--brand);
}

.event-body {
  display: grid;
  gap: 4px;
}

.event-body strong,
.match-notes {
  margin: 0;
}

.match-placeholder {
  background: rgba(255, 255, 255, 0.02);
}

.lineup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.lineup-card {
  align-content: start;
}

.lineup-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.lineup-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.lineup-order,
.lineup-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  font-size: 0.8rem;
  font-weight: 700;
}

.lineup-editor {
  display: grid;
  gap: 12px;
  padding-top: 4px;
}

.lineup-editor-head,
.lineup-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.lineup-checklist {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.lineup-choice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

@media (max-width: 960px) {
  .match-hero {
    grid-template-columns: 1fr;
  }

  .match-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .lineup-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .match-topbar {
    align-items: start;
    flex-direction: column;
  }

  .match-meta-grid {
    grid-template-columns: 1fr;
  }

  .event-item {
    grid-template-columns: 1fr;
  }
}
</style>