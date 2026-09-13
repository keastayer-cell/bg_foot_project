<template>
  <section class="section-wrap calendar-page">
    <article class="card calendar-hero">
      <div><p class="eyebrow">Расписание лиги</p><h1 class="section-title">Календарь матчей</h1><p class="muted-text">Найдите матч и добавьте его в календарь телефона или компьютера.</p></div>
      <a v-if="selectedCompetitionId" class="btn-primary" :href="icsUrl({ competitionId: selectedCompetitionId })">Скачать календарь турнира</a>
    </article>

    <article class="card calendar-filters">
      <CompetitionContextPicker v-model:season-id="selectedSeasonId" v-model:competition-id="selectedCompetitionId" :seasons="seasons" :competitions="competitions" :disabled="loading" />
      <div class="calendar-filter-grid">
        <label><span>Команда</span><select v-model="teamId"><option value="">Все команды</option><option v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option></select></label>
        <label><span>Площадка</span><select v-model="venueId"><option value="">Все площадки</option><option v-for="venue in venues" :key="venue.id" :value="String(venue.id)">{{ venue.name }}</option></select></label>
        <label><span>Дата от</span><input v-model="from" type="date" /></label>
        <label><span>Дата до</span><input v-model="to" type="date" /></label>
      </div>
      <div class="calendar-filter-actions">
        <a v-if="teamId" class="btn-ghost" :href="icsUrl({ teamId })">ICS выбранной команды</a>
        <button class="btn-ghost" type="button" @click="resetDates">Сбросить даты</button>
      </div>
    </article>

    <UiState v-if="loading" tone="loading" title="Загружаем календарь" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить календарь" :message="error" action-label="Повторить" @action="loadCalendar" />
    <template v-else>
      <article v-for="group in visibleGroups" :key="group.key" class="card calendar-group">
        <header><div><p class="eyebrow">{{ group.kicker }}</p><h2>{{ group.title }}</h2></div><b>{{ group.items.length }}</b></header>
        <div class="calendar-match-list">
          <RouterLink v-for="match in group.items" :key="match.id" class="calendar-match" :to="`/matches/${match.id}`">
            <time><strong>{{ formatDay(match.kickoff_at) }}</strong><span>{{ formatTime(match.kickoff_at) }}</span></time>
            <div class="calendar-match-teams"><strong>{{ match.home_team_name }}</strong><span>—</span><strong>{{ match.away_team_name }}</strong></div>
            <div class="calendar-match-meta"><span>{{ match.competition_name || match.season_name }}</span><small>{{ match.venue_name || 'Площадка уточняется' }}</small><em v-if="match.schedule_status === 'RESCHEDULED'">Перенесён</em><em v-if="match.schedule_status === 'CANCELLED'" class="is-cancelled">Отменён</em></div>
            <span class="calendar-score" v-if="match.protocol_status === 'VERIFIED'">{{ match.home_score }}:{{ match.away_score }}</span>
            <a class="calendar-ics" :href="icsUrl({ matchId: match.id })" title="Добавить матч в календарь" @click.stop>ICS</a>
          </RouterLink>
        </div>
      </article>
      <UiState v-if="!visibleGroups.length" tone="empty" title="Матчи не найдены" message="Измените сезон, турнир или диапазон дат." />
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompetitionContextPicker from '../components/CompetitionContextPicker.vue'
import UiState from '../components/UiState.vue'
import { useAuth } from '../store/auth'
import { useCompetitionContext } from '../store/competitionContext'
import { createCatalogApi } from '../api/catalog'
import { createCompetitionsApi } from '../api/competitions'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
const competitionsApi = createCompetitionsApi(optionalAuthApiRequest)
const route = useRoute(); const router = useRouter()
const { seasonId: selectedSeasonId, competitionId: selectedCompetitionId, selectAvailable, syncUrl } = useCompetitionContext(route, router)
const seasons = ref([]); const competitions = ref([]); const teams = ref([]); const venues = ref([]); const matches = ref([])
const teamId = ref(''); const venueId = ref(''); const from = ref(''); const to = ref('')
const loading = ref(true); const error = ref('')
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const completed = (match) => match.protocol_status === 'VERIFIED'
const groups = computed(() => [
  { key: 'rescheduled', kicker: 'Изменения', title: 'Перенесённые и отменённые', items: matches.value.filter((m) => ['RESCHEDULED', 'CANCELLED'].includes(m.schedule_status)) },
  { key: 'upcoming', kicker: 'Впереди', title: 'Будущие матчи', items: matches.value.filter((m) => !completed(m) && !['RESCHEDULED', 'CANCELLED'].includes(m.schedule_status)) },
  { key: 'completed', kicker: 'Архив', title: 'Завершённые матчи', items: matches.value.filter(completed).reverse() },
])
const visibleGroups = computed(() => groups.value.filter((group) => group.items.length))

watch(selectedSeasonId, loadSeason)
watch([selectedCompetitionId, teamId, venueId, from, to], () => { syncUrl(); loadCalendar() })

async function loadSeason() {
  if (!selectedSeasonId.value) return
  try {
    const [competitionData, overview] = await Promise.all([competitionsApi.list(selectedSeasonId.value), catalogApi.getSeasonOverview(selectedSeasonId.value)])
    competitions.value = competitionData || []; teams.value = overview?.teams || []
    selectAvailable(seasons.value, competitions.value); syncUrl(); await loadCalendar()
  } catch (requestError) { error.value = requestError.message || 'Не удалось загрузить выбранный сезон.' }
}
async function loadCalendar() {
  if (!selectedSeasonId.value) return
  loading.value = true; error.value = ''
  try {
    const data = await catalogApi.getCalendar({ seasonId: selectedSeasonId.value, competitionId: selectedCompetitionId.value, teamId: teamId.value, venueId: venueId.value, from: from.value, to: to.value })
    matches.value = data?.matches || []; venues.value = data?.venues || []
  } catch (requestError) { error.value = requestError.message || 'Не удалось получить матчи.' }
  finally { loading.value = false }
}
function resetDates() { from.value = ''; to.value = '' }
function icsUrl(params) { const query = new URLSearchParams(params); return `${apiBaseUrl}/api/calendar.ics?${query}` }
function formatDay(value) { return new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value)) }
function formatTime(value) { return new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
onMounted(async () => {
  try { seasons.value = await catalogApi.getSeasons(1); selectAvailable(seasons.value, competitions.value); await loadSeason() }
  catch (requestError) { error.value = requestError.message || 'Не удалось загрузить сезоны.'; loading.value = false }
})
</script>

<style scoped>
.calendar-page{display:grid;gap:18px}.calendar-hero{display:flex;align-items:end;justify-content:space-between;gap:24px;padding:28px}.calendar-hero h1{margin:5px 0 8px}.calendar-filters{display:grid;gap:16px;padding:20px}.calendar-filter-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.calendar-filter-grid label{display:grid;gap:6px}.calendar-filter-grid label>span{color:var(--muted);font-size:.62rem;text-transform:uppercase}.calendar-filter-actions{display:flex;justify-content:flex-end;gap:8px}.calendar-group{overflow:hidden}.calendar-group>header{display:flex;align-items:center;justify-content:space-between;padding:20px 22px;border-bottom:1px solid var(--line)}.calendar-group h2{margin:4px 0 0}.calendar-group>header>b{display:grid;place-items:center;min-width:38px;height:38px;border-radius:12px;background:rgba(97,232,162,.1);color:var(--brand)}.calendar-match{display:grid;grid-template-columns:130px minmax(280px,1fr) minmax(190px,.7fr) auto auto;align-items:center;gap:18px;padding:16px 22px;border-bottom:1px solid var(--line)}.calendar-match:hover{background:rgba(97,232,162,.045)}.calendar-match time{display:grid;gap:3px}.calendar-match time span,.calendar-match-meta small{color:var(--muted);font-size:.7rem}.calendar-match-teams{display:grid;grid-template-columns:1fr auto 1fr;gap:10px}.calendar-match-teams strong:last-child{text-align:right}.calendar-match-teams span{color:var(--muted)}.calendar-match-meta{display:grid;gap:3px;font-size:.7rem}.calendar-match-meta em{width:max-content;padding:3px 7px;border-radius:6px;background:rgba(255,199,88,.12);color:#ffd168;font-style:normal}.calendar-match-meta em.is-cancelled{background:rgba(255,99,113,.12);color:#ff8792}.calendar-score{font-weight:900}.calendar-ics{padding:7px 9px;border:1px solid rgba(97,232,162,.28);border-radius:8px;color:var(--brand);font-size:.66rem;font-weight:800}
@media(max-width:900px){.calendar-filter-grid{grid-template-columns:repeat(2,1fr)}.calendar-match{grid-template-columns:100px 1fr auto}.calendar-match-meta{grid-column:2}.calendar-score{grid-column:3;grid-row:1}.calendar-ics{grid-column:3;grid-row:2}}
@media(max-width:620px){.calendar-hero{align-items:flex-start;flex-direction:column}.calendar-filter-grid{grid-template-columns:1fr}.calendar-match{grid-template-columns:1fr auto;gap:10px;padding:14px}.calendar-match time{grid-template-columns:auto auto;gap:7px}.calendar-match-teams{grid-column:1/-1}.calendar-match-meta{grid-column:1}.calendar-score{grid-column:2;grid-row:1}.calendar-ics{grid-column:2;grid-row:3}.calendar-filter-actions{justify-content:flex-start;flex-wrap:wrap}}
</style>
