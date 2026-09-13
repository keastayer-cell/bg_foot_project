<template>
  <article class="card admin-panel dashboard-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Сегодня</p>
      <h3 class="section-title">Что требует внимания</h3>
      <p class="muted-text">Рабочие задачи выбранного соревнования и его сезона.</p>
    </header>
    <div class="dashboard-context">
      <CompetitionContextPicker v-model:season-id="seasonId" v-model:competition-id="competitionId" :seasons="seasons" :competitions="competitions" :disabled="loadingContext || loading" />
    </div>
    <UiState v-if="loading" tone="loading" title="Собираем сводку" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить сводку" :message="error" action-label="Повторить" @action="retry" />
    <UiState v-else-if="!selectedCompetition" tone="empty" title="Выберите соревнование" message="Сводка появится после выбора сезона и турнира." />
    <section v-for="group in groups" v-else :key="group.scope" class="dashboard-group">
      <h4>{{ group.title }}</h4>
      <p v-if="group.scope === 'SEASON'" class="muted-text">Заявки и трансферы в нашей модели относятся ко всему сезону.</p>
      <div class="dashboard-grid">
      <component
        :is="task.path ? 'a' : 'button'"
        v-for="task in group.tasks"
        :key="task.key"
        class="dashboard-task"
        :class="{ 'is-clear': task.count === 0 }"
        :href="task.path || undefined"
        type="button"
        @click="!task.path && $emit('open-task', { tab: task.adminTab, seasonId, competitionId })"
      >
        <strong>{{ task.count }}</strong><span>{{ task.label }}</span><small>{{ task.count ? 'Открыть →' : 'Всё в порядке' }}</small>
      </component>
      </div>
    </section>
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
const groups = computed(() => [
  { scope: 'COMPETITION', title: `Соревнование · ${selectedCompetition.value?.name || ''}` },
  { scope: 'SEASON', title: `Сезон · ${selectedSeason.value?.name || ''}` },
  { scope: 'LEAGUE', title: 'Оповещения всей лиги' },
].map((group) => ({ ...group, tasks: tasks.value.filter((task) => task.scope === group.scope) })).filter((group) => group.tasks.length))
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
.dashboard-panel{padding:0}.dashboard-context{padding:20px;border-bottom:1px solid var(--line)}.dashboard-group>h4{margin:20px 20px 0}.dashboard-group>p{margin:8px 20px 0;font-size:.8rem}.dashboard-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;padding:20px}.dashboard-task{display:grid;grid-template-columns:64px 1fr auto;align-items:center;gap:14px;padding:18px;border:1px solid rgba(124,163,255,.18);border-radius:12px;background:rgba(10,16,37,.58);color:inherit;text-align:left}.dashboard-task:hover{border-color:rgba(97,232,162,.42)}.dashboard-task strong{color:#ffd168;font-size:1.8rem}.dashboard-task span{font-weight:800}.dashboard-task small{color:var(--brand)}.dashboard-task.is-clear{opacity:.62}.dashboard-task.is-clear strong{color:var(--brand)}@media(max-width:760px){.dashboard-grid{grid-template-columns:1fr}.dashboard-task{grid-template-columns:50px 1fr}.dashboard-task small{grid-column:2}}
</style>
