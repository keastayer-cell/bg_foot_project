<template>
  <article class="card admin-panel dashboard-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Рабочий стол</p>
      <h3 class="section-title">Сводка</h3>
      <p class="muted-text">Задачи организатора по выбранному турниру.</p>
    </header>
    <div class="dashboard-context">
      <CompetitionContextPicker v-model:season-id="seasonId" v-model:competition-id="competitionId" :seasons="seasons" :competitions="competitions" :disabled="loadingContext || loading" />
    </div>
    <UiState v-if="loading" tone="loading" title="Собираем сводку" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить сводку" :message="error" action-label="Повторить" @action="retry" />
    <UiState v-else-if="!selectedCompetition" tone="empty" title="Выберите соревнование" message="Сводка появится после выбора сезона и турнира." />
    <div v-else class="dashboard-workspace">
      <section v-for="group in groups" :key="group.scope" class="dashboard-group" :class="`dashboard-group-${group.scope.toLowerCase()}`">
        <header class="dashboard-group-head"><p>{{ group.kicker }}</p><h4>{{ group.title }}</h4><span>{{ group.description }}</span></header>
        <div class="dashboard-grid">
          <component :is="task.path ? 'a' : 'button'" v-for="task in group.tasks" :key="task.key" class="dashboard-task" :class="{ 'is-clear': task.count === 0, 'has-tasks': task.count > 0 }" :href="task.path || undefined" type="button" @click="!task.path && $emit('open-task', { tab: task.adminTab, seasonId, competitionId })">
            <span>{{ taskLabels[task.key] || task.label }}</span><strong>{{ task.count }}</strong><small aria-hidden="true">→</small>
          </component>
        </div>
      </section>
    </div>
  </article>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiState from '../UiState.vue'
import CompetitionContextPicker from '../CompetitionContextPicker.vue'
import { useAuth } from '../../store/auth'
import { useCompetitionContext } from '../../store/competitionContext'
import { createCatalogApi } from '../../api/catalog'
import { createCompetitionsApi } from '../../api/competitions'

defineEmits(['open-task'])
const { authorizedApiRequest } = useAuth()
const catalog = createCatalogApi(authorizedApiRequest)
const competitionApi = createCompetitionsApi(authorizedApiRequest)
const { seasonId, competitionId, selectAvailable, syncUrl } = useCompetitionContext(useRoute(), useRouter())
const seasons = ref([])
const competitions = ref([])
const tasks = ref([])
const loading = ref(true)
const loadingContext = ref(true)
const error = ref('')
let initialized = false
let requestNumber = 0
const selectedCompetition = computed(() => competitions.value.find((item) => String(item.id) === competitionId.value))
const selectedSeason = computed(() => seasons.value.find((item) => String(item.id) === seasonId.value))
const taskLabels = { protocols: 'Протоколы на подтверждение', lineups: 'Матчи без двух составов', unpublished: 'Туры на публикацию', rosters: 'Неполные заявки команд', suspensions: 'Действующие дисквалификации', applications: 'Заявки на проверке', transfers: 'Ожидающие трансферы', acknowledgements: 'Оповещения без ознакомления' }
const groups = computed(() => [
  { scope: 'COMPETITION', kicker: 'Турнир', title: selectedCompetition.value?.name || '', description: 'Матчи, составы и дисциплина' },
  { scope: 'SEASON', kicker: 'Сезон', title: selectedSeason.value?.name || '', description: 'Заявки и переходы всех команд сезона' },
  { scope: 'LEAGUE', kicker: 'Вся лига', title: 'Оповещения всей лиги', description: 'Обязательное ознакомление пользователей' },
].map(group => ({ ...group, tasks: tasks.value.filter(task => task.scope === group.scope) })).filter(group => group.tasks.length))

watch(seasonId, () => { if (initialized) loadCompetitions() })
watch(competitionId, () => { syncUrl(); if (initialized && !loadingContext.value) load() }, { flush: 'sync' })
async function loadCompetitions() {
  ++requestNumber
  loadingContext.value = true; loading.value = true; tasks.value = []; error.value = ''
  try {
    competitions.value = seasonId.value ? await competitionApi.list(seasonId.value) : []
    if (!competitions.value.length) competitionId.value = ''
    selectAvailable(seasons.value, competitions.value)
  } catch (requestError) { competitions.value = []; error.value = requestError.message || 'Не удалось загрузить соревнования.' }
  finally { loadingContext.value = false; loading.value = false }
  if (!error.value) await load()
}
async function load() {
  if (!selectedCompetition.value) { tasks.value = []; loading.value = false; return }
  loading.value = true; error.value = ''; tasks.value = []
  const request = ++requestNumber
  const params = new URLSearchParams({ seasonId: seasonId.value, competitionId: competitionId.value })
  try {
    const payload = await authorizedApiRequest(`/api/admin/dashboard?${params}`, { method: 'GET' })
    if (!Array.isArray(payload) || payload.some((task) => !['COMPETITION', 'SEASON', 'LEAGUE'].includes(task.scope))) {
      throw new Error('Backend вернул сводку старого формата. Необходимо обновить локальный backend.');
    }
    if (request === requestNumber) tasks.value = payload
  } catch (requestError) { if (request === requestNumber) error.value = requestError.message || 'Внутренняя ошибка сервера.' }
  finally { if (request === requestNumber) loading.value = false }
}
async function initialize() {
  try {
    seasons.value = await catalog.getSeasons(1)
    selectAvailable(seasons.value)
    initialized = true
    await loadCompetitions()
  } catch (requestError) { error.value = requestError.message || 'Не удалось загрузить сезоны.'; loading.value = false; loadingContext.value = false }
}
async function retry() { if (!seasons.value.length) await initialize(); else await loadCompetitions() }
onMounted(initialize)
</script>

<style scoped>
.dashboard-panel{padding:0;overflow:hidden}.dashboard-panel .admin-panel-head{padding:22px 24px;margin:0;border-bottom:1px solid var(--line)}.dashboard-panel .admin-panel-head .section-title{font-size:1.35rem;margin:7px 0}.dashboard-panel .admin-panel-head .muted-text{font-size:.85rem;margin:0;color:var(--muted)}.dashboard-context{padding:22px 24px;border-bottom:1px solid var(--line);background:rgba(7,13,31,.25)}.dashboard-context :deep(.competition-context-picker){max-width:950px;gap:16px}.dashboard-context :deep(select){height:46px;min-height:46px;font-size:.88rem;font-weight:400}.dashboard-workspace{display:grid;grid-template-columns:minmax(0,1.4fr) minmax(300px,1fr);gap:20px;padding:24px;align-items:start;max-width:1250px}.dashboard-group{border:1px solid var(--line);border-radius:12px;background:rgba(7,13,31,.6);overflow:hidden;min-width:0}.dashboard-group-competition{grid-column:1;grid-row:1/3;border-top:3px solid var(--brand)}.dashboard-group-season{grid-column:2;grid-row:1;border-top:3px solid #8fa7eb}.dashboard-group-league{grid-column:2;grid-row:2}.dashboard-group-head{padding:18px 20px;border-bottom:1px solid var(--line);background:rgba(124,163,255,.05)}.dashboard-group-head p{margin:0 0 7px;color:var(--brand);font-size:.65rem;text-transform:uppercase;letter-spacing:.07em;font-weight:800}.dashboard-group-season .dashboard-group-head p{color:#9db3ef}.dashboard-group-league .dashboard-group-head p{color:var(--muted)}.dashboard-group-head h4{font-size:1.05rem;margin:0;line-height:1.4}.dashboard-group-head>span{display:block;color:var(--muted);font-size:.75rem;margin-top:8px;line-height:1.5}.dashboard-grid{display:grid;padding:6px 20px}.dashboard-task{display:grid;grid-template-columns:minmax(0,1fr) 38px 16px;align-items:center;gap:14px;min-height:64px;padding:14px 0;border:0;border-bottom:1px solid var(--line);background:transparent;color:inherit;text-align:left;text-decoration:none;cursor:pointer}.dashboard-task:last-child{border-bottom:0}.dashboard-task span{font-size:.85rem;font-weight:600;line-height:1.5}.dashboard-task strong{display:grid;place-items:center;min-height:32px;border-radius:7px;font-size:1rem;background:rgba(124,163,255,.07);color:var(--muted)}.dashboard-task small{font-size:1rem;color:var(--muted)}.dashboard-task.has-tasks strong{background:rgba(255,209,104,.12);color:#ffd168;border:1px solid rgba(255,209,104,.25)}.dashboard-task.has-tasks small{color:var(--brand)}.dashboard-task:hover span{color:var(--brand)}.dashboard-task.is-clear span{color:var(--muted)}.dashboard-task:focus-visible{outline:2px solid var(--brand);outline-offset:3px;border-radius:4px}
@media(max-width:1000px){.dashboard-workspace{grid-template-columns:1fr}.dashboard-group-competition,.dashboard-group-season,.dashboard-group-league{grid-column:1;grid-row:auto}}
@media(max-width:640px){.dashboard-panel .admin-panel-head,.dashboard-context{padding:20px}.dashboard-workspace{padding:20px;gap:16px}.dashboard-grid{padding:4px 16px}.dashboard-group-head{padding:16px}}
</style>
