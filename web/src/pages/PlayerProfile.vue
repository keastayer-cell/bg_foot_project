<template>
  <section class="section-wrap entity-page">
    <Breadcrumbs :items="[{ label: 'Игроки', to: '/players' }, { label: player?.fullName || 'Игрок' }]" />
    <UiState v-if="loading" tone="loading" title="Загружаем профиль игрока" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить игрока" :message="error" action-label="Повторить" @action="load" />
    <template v-else-if="player">
      <article class="card entity-hero">
        <div class="entity-avatar"><img v-if="player.photoDataUrl" :src="player.photoDataUrl" :alt="player.fullName" /><span v-else>{{ initials(player.fullName) }}</span></div>
        <div><p class="eyebrow">Профиль игрока</p><h1>{{ player.fullName }}</h1><p>{{ player.currentTeamName || 'Без команды' }}</p><FavoriteButton type="PLAYER" :target-id="player.id" /></div>
      </article>
      <div class="entity-grid">
        <article class="card entity-panel"><h2>Статистика</h2><dl class="entity-stats"><div><dt>Голы</dt><dd>{{ player.goals || 0 }}</dd></div><div><dt>ЖК</dt><dd>{{ player.yellowCards || 0 }}</dd></div><div><dt>КК</dt><dd>{{ player.redCards || 0 }}</dd></div></dl></article>
        <article class="card entity-panel"><h2>Данные</h2><dl class="entity-facts"><div><dt>Дата рождения</dt><dd>{{ formatDate(player.birthDate) }}</dd></div><div><dt>Город</dt><dd>{{ player.residence || 'Не указан' }}</dd></div><div><dt>Амплуа</dt><dd>{{ player.isGoalkeeper ? 'Вратарь' : player.position || 'Полевой игрок' }}</dd></div></dl></article>
      </div>
      <article class="card entity-panel"><h2>История команд</h2><div class="entity-history"><div v-for="item in history" :key="item.teamId + ':' + item.validFrom"><strong>{{ item.teamName }}</strong><span>{{ formatDate(item.validFrom) }} — {{ item.active ? 'сейчас' : formatDate(item.validTo) }}</span></div><p v-if="!history.length" class="muted-text">История пока не заполнена.</p></div></article>
    </template>
  </section>
</template>
<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Breadcrumbs from '../components/Breadcrumbs.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import UiState from '../components/UiState.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'
const route=useRoute(); const {optionalAuthApiRequest}=useAuth(); const api=createCatalogApi(optionalAuthApiRequest)
const player=ref(null);const history=ref([]);const loading=ref(true);const error=ref('')
const initials=(name)=>String(name||'').split(/\s+/).slice(0,2).map(x=>x[0]||'').join('').toUpperCase()
function formatDate(value){if(!value)return 'Не указана';const date=new Date(value);return Number.isNaN(date.getTime())?'Не указана':new Intl.DateTimeFormat('ru-RU').format(date)}
async function load(){loading.value=true;error.value='';try{[player.value,history.value]=await Promise.all([api.getPlayer(route.params.playerId),api.getPlayerHistory(route.params.playerId)])}catch(e){error.value=e.message||'Игрок не найден.'}finally{loading.value=false}}
onMounted(load);watch(()=>route.params.playerId,load)
</script>
<style scoped>
.entity-page{display:grid;gap:18px;max-width:1180px}.entity-hero{display:flex;align-items:center;gap:24px;padding:30px}.entity-hero h1{margin:5px 0;font-size:clamp(2rem,4vw,3.5rem)}.entity-avatar{display:grid;place-items:center;width:150px;height:180px;overflow:hidden;border:1px solid rgba(97,232,162,.35);border-radius:18px;background:rgba(97,232,162,.08);color:var(--brand);font-size:2rem;font-weight:900}.entity-avatar img{width:100%;height:100%;object-fit:cover}.entity-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.entity-panel{padding:24px}.entity-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.entity-stats div,.entity-facts div,.entity-history div{padding:14px;border:1px solid var(--line);border-radius:11px;background:rgba(7,12,30,.4)}.entity-stats dt,.entity-facts dt{color:var(--muted)}.entity-stats dd{margin:7px 0 0;color:var(--brand);font-size:2rem;font-weight:900}.entity-facts,.entity-history{display:grid;gap:9px}.entity-facts div,.entity-history div{display:flex;justify-content:space-between;gap:15px}.entity-facts dd{margin:0}.entity-history span{color:var(--muted)}@media(max-width:650px){.entity-hero{align-items:flex-start;flex-direction:column}.entity-avatar{width:110px;height:132px}.entity-grid{grid-template-columns:1fr}.entity-stats{grid-template-columns:repeat(3,1fr)}}
</style>
