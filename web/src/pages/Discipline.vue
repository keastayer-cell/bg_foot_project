<template>
  <section class="section-wrap discipline-page">
    <article class="card discipline-hero">
      <div><p class="eyebrow">Честная игра</p><h1 class="section-title">Дисциплинарный центр</h1><p class="muted-text">Карточки, пороги и действующие наказания по каждому турниру.</p></div>
      <CompetitionContextPicker v-model:season-id="seasonId" v-model:competition-id="competitionId" :seasons="seasons" :competitions="competitions" :disabled="loading" />
    </article>
    <UiState v-if="loading" tone="loading" title="Считаем дисциплину" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить данные" :message="error" action-label="Повторить" @action="load" />
    <template v-else>
      <article class="card discipline-summary"><div><span>Турнир</span><strong>{{ data.competition?.name }}</strong></div><div><span>Порог ЖК</span><strong>{{ data.competition?.yellowThreshold || 'Не задан' }}</strong></div><div><span>Под наблюдением</span><strong>{{ rows.length }}</strong></div><div><span>Дисквалифицированы</span><strong>{{ suspended.length }}</strong></div></article>
      <article class="card discipline-table-card">
        <header><div><p class="eyebrow">Текущие данные</p><h2>Игроки с карточками</h2></div></header>
        <div v-if="rows.length" class="discipline-table-wrap"><table><thead><tr><th>Игрок</th><th>Команда</th><th>ЖК</th><th>КК</th><th>Состояние</th><th>Источник</th><th v-if="canManage">Действие</th></tr></thead><tbody>
          <tr v-for="row in rows" :key="row.player_id"><td><RouterLink :to="`/players/${row.player_id}`">{{ row.player_name }}</RouterLink></td><td><RouterLink :to="`/teams/${row.team_id}`">{{ row.team_name }}</RouterLink></td><td><b class="card-count yellow">{{ row.yellow_cards }}</b></td><td><b class="card-count red">{{ row.red_cards }}</b></td><td><span v-if="row.remaining_matches > 0" class="status suspended">Пропустит: {{ row.remaining_matches }}</span><span v-else-if="row.near_threshold" class="status threshold">На пороге</span><span v-else class="muted-text">Без наказания</span><small v-if="row.adjustment_reason">Решение: {{ row.adjustment_reason }}</small></td><td><RouterLink v-if="row.source_match_id" :to="`/matches/${row.source_match_id}`">Матч →</RouterLink></td><td v-if="canManage"><button class="btn-ghost" type="button" @click="openAdjustment(row)">Изменить</button></td></tr>
        </tbody></table></div><UiState v-else tone="empty" title="Карточек пока нет" message="Подтверждённые протоколы появятся здесь автоматически." />
      </article>
      <article v-if="canManage && data.adjustments?.length" class="card adjustment-history"><header><p class="eyebrow">Журнал решений</p><h2>Ручные корректировки</h2></header><div v-for="item in data.adjustments" :key="item.id" class="adjustment-row"><strong>{{ item.player_name }}</strong><span>{{ item.old_remaining_matches }} → {{ item.new_remaining_matches }} матч.</span><p>{{ item.reason }}</p><small>{{ item.author_name }} · {{ formatDate(item.created_at) }}</small></div></article>
    </template>
    <div v-if="adjusting" class="modal-backdrop" @click.self="adjusting=null"><form class="card adjustment-modal" @submit.prevent="saveAdjustment"><header><div><p class="eyebrow">Решение организатора</p><h2>{{ adjusting.player_name }}</h2></div><button class="btn-ghost" type="button" @click="adjusting=null">Закрыть</button></header><label>Осталось матчей<input v-model.number="adjustment.remaining" type="number" min="0" required /></label><label>Причина<textarea v-model.trim="adjustment.reason" minlength="5" maxlength="500" rows="4" required /></label><p v-if="adjustmentError" class="error-text">{{ adjustmentError }}</p><button class="btn-primary" type="submit" :disabled="saving">{{ saving ? 'Сохраняем…' : 'Зафиксировать решение' }}</button></form></div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompetitionContextPicker from '../components/CompetitionContextPicker.vue'
import UiState from '../components/UiState.vue'
import { useAuth } from '../store/auth'
import { useCompetitionContext } from '../store/competitionContext'
import { createCatalogApi } from '../api/catalog'
import { createCompetitionsApi } from '../api/competitions'
const { optionalAuthApiRequest, hasRole } = useAuth(); const api=createCatalogApi(optionalAuthApiRequest); const competitionsApi=createCompetitionsApi(optionalAuthApiRequest)
const route=useRoute(); const router=useRouter(); const { seasonId, competitionId, selectAvailable, syncUrl }=useCompetitionContext(route,router)
const seasons=ref([]); const competitions=ref([]); const data=ref({}); const loading=ref(true); const error=ref(''); const adjusting=ref(null); const saving=ref(false); const adjustmentError=ref(''); const adjustment=reactive({remaining:0,reason:''})
const canManage=computed(()=>hasRole('SUPER_ADMIN')||hasRole('REFEREE')); const rows=computed(()=>data.value.players||[]); const suspended=computed(()=>rows.value.filter((row)=>Number(row.remaining_matches)>0))
watch(seasonId, loadCompetitions); watch(competitionId,()=>{syncUrl();load()})
async function loadCompetitions(){if(!seasonId.value)return;try{competitions.value=await competitionsApi.list(seasonId.value);selectAvailable(seasons.value,competitions.value);syncUrl();await load()}catch(e){error.value=e.message}}
async function load(){if(!competitionId.value)return;loading.value=true;error.value='';try{data.value=await api.getDiscipline(competitionId.value)}catch(e){error.value=e.message||'Ошибка загрузки.'}finally{loading.value=false}}
function openAdjustment(row){adjusting.value=row;adjustment.remaining=Number(row.remaining_matches||0);adjustment.reason='';adjustmentError.value=''}
async function saveAdjustment(){saving.value=true;adjustmentError.value='';try{await api.adjustDiscipline({competitionId:Number(competitionId.value),playerId:Number(adjusting.value.player_id),teamId:Number(adjusting.value.team_id),oldRemainingMatches:Number(adjusting.value.remaining_matches||0),newRemainingMatches:Number(adjustment.remaining),reason:adjustment.reason});adjusting.value=null;await load()}catch(e){adjustmentError.value=e.message||'Не удалось сохранить решение.'}finally{saving.value=false}}
function formatDate(value){return new Intl.DateTimeFormat('ru-RU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(value))}
onMounted(async()=>{try{seasons.value=await api.getSeasons(1);selectAvailable(seasons.value,competitions.value);await loadCompetitions()}catch(e){error.value=e.message;loading.value=false}})
</script>

<style scoped>
.discipline-page{display:grid;gap:18px}.discipline-hero{display:grid;grid-template-columns:1fr minmax(420px,.8fr);align-items:end;gap:30px;padding:28px}.discipline-hero h1{margin:5px 0 8px}.discipline-summary{display:grid;grid-template-columns:repeat(4,1fr);overflow:hidden}.discipline-summary>div{display:grid;gap:6px;padding:20px;border-right:1px solid var(--line)}.discipline-summary span{color:var(--muted);font-size:.65rem;text-transform:uppercase}.discipline-summary strong{font-size:1.25rem}.discipline-table-card>header,.adjustment-history>header{padding:20px 22px;border-bottom:1px solid var(--line)}.discipline-table-card h2,.adjustment-history h2{margin:4px 0}.discipline-table-wrap{overflow:auto}.discipline-table-wrap table{width:100%;border-collapse:collapse}.discipline-table-wrap th,.discipline-table-wrap td{padding:14px 16px;border-bottom:1px solid var(--line);text-align:left;font-size:.74rem}.discipline-table-wrap th{color:var(--muted);font-size:.59rem;text-transform:uppercase}.discipline-table-wrap td small{display:block;margin-top:6px;color:var(--muted)}.card-count{display:inline-grid;place-items:center;width:28px;height:34px;border-radius:4px;color:#071025}.card-count.yellow{background:#ffd34e}.card-count.red{background:#ff5d69}.status{display:inline-flex;padding:5px 8px;border-radius:7px;font-size:.65rem;font-weight:800}.status.suspended{background:rgba(255,93,105,.12);color:#ff8792}.status.threshold{background:rgba(255,211,78,.12);color:#ffd34e}.adjustment-row{display:grid;grid-template-columns:1fr auto;gap:6px;padding:14px 20px;border-bottom:1px solid var(--line)}.adjustment-row p{grid-column:1/-1;margin:0}.adjustment-row small{grid-column:1/-1;color:var(--muted)}.adjustment-modal{width:min(520px,calc(100vw - 24px));display:grid;gap:16px;padding:22px}.adjustment-modal header{display:flex;justify-content:space-between}.adjustment-modal h2{margin:4px 0}.adjustment-modal label{display:grid;gap:7px}
@media(max-width:800px){.discipline-hero{grid-template-columns:1fr}.discipline-summary{grid-template-columns:repeat(2,1fr)}}
</style>
