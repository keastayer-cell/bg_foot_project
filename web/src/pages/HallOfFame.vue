<template>
  <section class="section-wrap catalog-page hall-page">
    <PublicCatalogHeader title="Зал славы" description="Чемпионы, призёры, лучшие игроки и символические сборные турниров." :count="!loading && !error ? entries.length : null" count-label="турниров" />

    <div v-if="entries.length" class="hall-filters">
      <div class="hall-filter-heading"><span>Архив турниров</span><strong>Выберите сезон и соревнование</strong></div>
      <label><span>Сезон</span><select v-model="seasonId"><option v-for="season in seasonOptions" :key="season.id" :value="String(season.id)">{{ season.name }}</option></select></label>
      <label><span>Турнир</span><select v-model="competitionId"><option v-for="competition in competitionOptions" :key="competition.id" :value="String(competition.id)">{{ competition.type === 'CUP' ? 'Кубок' : 'Чемпионат' }} · {{ competition.name }}</option></select></label>
    </div>

    <UiState v-if="loading" tone="loading" title="Загружаем Зал славы" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить Зал славы" :message="error" action-label="Повторить" @action="load" />
    <UiState v-else-if="!visibleEntries.length" title="Награды пока не опубликованы" message="После подведения итогов Чемпионата или Кубка они появятся здесь." />

    <article v-for="entry in visibleEntries" v-else :key="entry.competitionId" class="hall-edition catalog-surface">
      <header class="hall-edition-head">
        <div class="hall-edition-mark">{{ entry.competitionType === 'CUP' ? 'К' : 'Ч' }}</div>
        <div><p class="catalog-kicker">{{ entry.competitionType === 'CUP' ? 'Кубок' : 'Чемпионат' }}</p><h2>{{ entry.competitionName }}</h2><span>{{ entry.seasonName }}</span></div>
      </header>

      <div class="hall-edition-body">
        <section class="hall-honors">
          <div class="hall-section-title"><p class="catalog-kicker">Итоги турнира</p><h3>Призёры и лауреаты</h3></div>
          <div v-if="podium(entry).length" class="hall-podium">
            <article v-for="award in podium(entry)" :key="award.id" :class="`hall-podium-place hall-podium-${award.code?.toLowerCase()}`">
              <span>{{ awardIcon(award.code) }}</span><div><small>{{ award.title }}</small><RouterLink v-if="award.teamId" :to="teamLink(award.teamId)">{{ award.winnerName }}</RouterLink><strong v-else>{{ award.winnerName }}</strong></div>
            </article>
          </div>

          <div v-if="individualAwards(entry).length" class="hall-awards">
            <article v-for="award in individualAwards(entry)" :key="award.id" class="hall-award-card" :class="{ 'hall-award-top-scorer': award.code === 'TOP_SCORER' }">
              <RouterLink v-if="award.playerId" class="hall-award-avatar" :to="playerLink(award.playerId)" :aria-label="`Профиль игрока ${award.winnerName}`"><img v-if="award.photoDataUrl" :src="award.photoDataUrl" :alt="award.winnerName" /><span v-else>{{ initials(award.winnerName) }}</span></RouterLink>
              <div><small>{{ award.title }}</small><RouterLink v-if="award.playerId" class="hall-person-link" :to="playerLink(award.playerId)">{{ playerPitchName(award.winnerName) }}</RouterLink><strong v-else>{{ award.winnerName }}</strong><RouterLink v-if="award.teamId" class="hall-team-link" :to="teamLink(award.teamId)">{{ award.teamShortName || award.teamName }}</RouterLink><span v-else>{{ award.teamShortName || award.teamName }}</span></div>
              <b v-if="award.statValue != null" class="hall-award-stat">{{ award.statValue }}<small>голов</small></b>
            </article>
          </div>
        </section>

        <section v-if="entry.slots?.length" class="hall-selection">
          <div class="hall-selection-title"><div><p class="catalog-kicker">Выбор организаторов</p><h3>Символическая <span>сборная турнира</span></h3></div><div class="hall-formation"><strong>{{ entry.formation }}</strong><span>{{ entry.slots.length }} игроков</span></div></div>
          <div class="hall-pitch" :aria-label="`Символическая сборная: ${entry.competitionName}`">
            <div class="hall-pitch-markings"><span class="hall-centre-line"></span><span class="hall-centre-circle"></span><span class="hall-area hall-area-top"></span><span class="hall-area hall-area-bottom"></span><span class="hall-goal hall-goal-top"></span><span class="hall-goal hall-goal-bottom"></span></div>
            <article v-for="slot in entry.slots" :key="slot.id" class="hall-player" :style="{ left: `${slot.xPercent}%`, top: `${slot.yPercent}%` }">
              <RouterLink class="hall-player-photo" :to="playerLink(slot.playerId)" :aria-label="`Профиль игрока ${slot.playerName}`"><img v-if="slot.photoDataUrl" :src="slot.photoDataUrl" :alt="slot.playerName" /><span v-else>{{ initials(slot.playerName) }}</span></RouterLink>
              <div class="hall-player-label"><RouterLink :to="playerLink(slot.playerId)">{{ playerPitchName(slot.playerName) }}</RouterLink><small>{{ slot.positionLabel }}</small><RouterLink v-if="slot.teamId" class="hall-player-team" :to="teamLink(slot.teamId)">{{ slot.teamShortName || slot.teamName }}</RouterLink><span v-else>{{ slot.teamShortName || slot.teamName }}</span></div>
            </article>
          </div>
        </section>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PublicCatalogHeader from '../components/PublicCatalogHeader.vue'
import UiState from '../components/UiState.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'

const { optionalAuthApiRequest } = useAuth()
const api = createCatalogApi(optionalAuthApiRequest)
const entries = ref([])
const loading = ref(false)
const error = ref('')
const seasonId = ref('')
const competitionId = ref('')
const podiumCodes = new Set(['CHAMPION', 'SILVER', 'BRONZE'])
const seasonOptions = computed(() => [...new Map(entries.value.map((item) => [item.seasonId, { id: item.seasonId, name: item.seasonName }])).values()])
const competitionOptions = computed(() => entries.value.filter((item) => String(item.seasonId) === seasonId.value).map((item) => ({ id: item.competitionId, name: item.competitionName, type: item.competitionType })))
const visibleEntries = computed(() => entries.value.filter((item) => String(item.seasonId) === seasonId.value && String(item.competitionId) === competitionId.value))
const podium = (entry) => (entry.awards || []).filter((award) => podiumCodes.has(award.code))
const individualAwards = (entry) => (entry.awards || []).filter((award) => !podiumCodes.has(award.code))
const awardIcon = (code) => ({ CHAMPION: '1', SILVER: '2', BRONZE: '3' })[code] || '•'
const initials = (name) => String(name || '').trim().split(/\s+/).slice(0, 2).map((part) => part[0] || '').join('').toUpperCase()
const playerLink = (playerId) => ({ name: 'player-profile', params: { playerId }, query: { from: 'hall-of-fame' } })
const teamLink = (teamId) => ({ name: 'team-profile', params: { slug: teamId }, query: { from: 'hall-of-fame' } })
function playerPitchName(name) {
  const cleanName = String(name || '').replace(/\s*\([^)]*\)\s*$/, '').trim()
  const parts = cleanName.split(/\s+/).filter(Boolean)
  if (parts.length < 2) return parts[0] || ''
  if (parts.length >= 3 && /(вич|вна|ична)$/i.test(parts.at(-1))) return `${parts[0]} ${parts[1][0]}.`
  if (parts.length >= 3 && /(вич|вна|ична)$/i.test(parts[1])) return `${parts.at(-1)} ${parts[0][0]}.`
  return `${parts.at(-1)} ${parts[0][0]}.`
}
async function load() {
  loading.value = true; error.value = ''
  try {
    const payload = await api.getHallOfFame()
    entries.value = Array.isArray(payload) ? payload : []
    if (entries.value.length) {
      seasonId.value = String(entries.value[0].seasonId)
      competitionId.value = String(entries.value[0].competitionId)
    }
  }
  catch (err) { error.value = err.message || 'Не удалось получить опубликованные награды.' }
  finally { loading.value = false }
}
watch(seasonId, () => {
  if (!competitionOptions.value.some((item) => String(item.id) === competitionId.value)) {
    competitionId.value = competitionOptions.value.length ? String(competitionOptions.value[0].id) : ''
  }
})
onMounted(load)
</script>

<style scoped>
.hall-page { align-content: start; width: min(calc(100% - 32px), 1240px); margin-inline: auto; }
.hall-filters { display: grid; grid-template-columns: minmax(210px,.72fr) repeat(2,minmax(240px,1fr)); align-items: end; gap: 14px; padding: 18px 20px; border: 1px solid rgba(97,232,162,.22); border-radius: 13px; background: linear-gradient(120deg, rgba(18,37,54,.94), rgba(12,20,43,.96)); box-shadow: 0 14px 34px rgba(2,7,20,.18); }
.hall-filter-heading { display: grid; align-self: center; gap: 5px; }
.hall-filter-heading span, .hall-filters label > span { color: var(--brand); font-size: .59rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.hall-filter-heading strong { font-size: .76rem; }
.hall-filters label { display: grid; gap: 7px; min-width: 0; }
.hall-filters select { width: 100%; min-width: 0; border-color: rgba(97,232,162,.22); background-color: rgba(7,15,34,.94); }
.hall-edition { display: grid; background: linear-gradient(145deg, rgba(13,22,47,.98), rgba(7,13,30,.98)); box-shadow: 0 24px 70px rgba(1,5,18,.24); }
.hall-edition-head { display: flex; align-items: center; gap: 14px; padding: 20px 24px; border-bottom: 1px solid rgba(124,163,255,.13); background: radial-gradient(circle at 0 0, rgba(97,232,162,.11), transparent 36%), rgba(18,29,61,.54); }
.hall-edition-head h2, .hall-edition-head p { margin: 0; }
.hall-edition-head h2 { font-size: 1.25rem; }
.hall-edition-head > div:last-child > span { display: block; margin-top: 5px; color: var(--muted); font-size: .68rem; }
.hall-edition-mark { display: grid; place-items: center; flex: 0 0 48px; width: 48px; height: 48px; border: 1px solid rgba(97,232,162,.34); border-radius: 14px; background: rgba(97,232,162,.08); color: var(--brand); font-size: 1rem; font-weight: 900; }
.hall-edition-body { display: grid; }
.hall-honors { min-width: 0; padding: 24px; background: radial-gradient(circle at 12% 16%, rgba(68,105,172,.1), transparent 38%), rgba(10,18,39,.72); }
.hall-section-title, .hall-selection-title { margin-bottom: 18px; }
.hall-section-title h3, .hall-section-title p, .hall-selection-title h3, .hall-selection-title p { margin: 0; }
.hall-section-title h3, .hall-selection-title h3 { font-size: .95rem; }
.hall-podium { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); align-items: end; gap: 9px; padding-top: 10px; }
.hall-podium-place { position: relative; display: grid; justify-items: center; align-content: center; gap: 11px; min-width: 0; min-height: 118px; padding: 18px 10px; overflow: hidden; border: 1px solid rgba(124,163,255,.24); border-radius: 13px 13px 7px 7px; background: linear-gradient(155deg, rgba(32,47,84,.96), rgba(17,27,55,.96)); box-shadow: inset 0 1px 0 rgba(255,255,255,.04), 0 12px 25px rgba(1,5,18,.19); text-align: center; }
.hall-podium-place::after { content: ''; position: absolute; right: -24px; bottom: -38px; width: 90px; height: 90px; border-radius: 50%; background: currentColor; opacity: .035; }
.hall-podium-silver { order: 1; color: #c8d4ef; }
.hall-podium-champion { order: 2; min-height: 142px; border-color: rgba(255,205,84,.44); background: linear-gradient(155deg, rgba(81,66,37,.9), rgba(28,31,53,.98) 68%); color: #ffd867; }
.hall-podium-bronze { order: 3; color: #e6a779; }
.hall-podium-place > span { display: grid; place-items: center; width: 42px; height: 42px; border: 1px solid currentColor; border-radius: 50%; background: rgba(255,255,255,.045); color: inherit; font-size: .9rem; font-weight: 900; box-shadow: 0 0 0 5px rgba(255,255,255,.025); }
.hall-podium-champion > span { width: 50px; height: 50px; color: #ffd867; }
.hall-podium-place div, .hall-award-card > div:nth-child(2) { display: grid; gap: 4px; min-width: 0; }
.hall-podium-place small { color: #9faecb; font-size: .56rem; letter-spacing: .04em; text-transform: uppercase; }
.hall-podium-place :is(strong,a) { overflow: hidden; max-width: 100%; color: #f3f6ff; font-size: .7rem; font-weight: 800; text-decoration: none; text-overflow: ellipsis; white-space: nowrap; }
.hall-podium-place a:hover { color: var(--brand); }
.hall-podium-champion :is(strong,a) { font-size: .78rem; }
.hall-awards { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 10px; margin-top: 22px; }
.hall-award-card { position: relative; display: grid; grid-template-columns: 48px minmax(0,1fr); align-items: center; gap: 11px; min-width: 0; min-height: 76px; padding: 13px 14px 13px 17px; overflow: hidden; border: 1px solid rgba(82,127,205,.3); border-radius: 11px; background: linear-gradient(130deg, rgba(28,43,77,.94), rgba(14,24,51,.98)); box-shadow: inset 3px 0 0 rgba(97,232,162,.72), 0 8px 20px rgba(1,6,20,.14); }
.hall-award-card::after { content: ''; position: absolute; right: -20px; top: -28px; width: 80px; height: 80px; border-radius: 50%; background: rgba(97,232,162,.055); }
.hall-award-top-scorer { grid-template-columns: 48px minmax(0,1fr) 38px; border-color: rgba(255,197,76,.34); background: linear-gradient(130deg, rgba(68,54,31,.9), rgba(19,27,52,.98)); box-shadow: inset 3px 0 0 #ffc94f, 0 8px 20px rgba(1,6,20,.14); }
.hall-award-avatar { z-index: 1; display: grid; place-items: center; width: 48px; height: 48px; overflow: hidden; border: 1px solid rgba(97,232,162,.45); border-radius: 50%; background: rgba(97,232,162,.11); color: var(--brand); font-size: .62rem; font-weight: 900; text-decoration: none; }
.hall-award-avatar img { width: 100%; height: 100%; object-fit: cover; }
.hall-award-card small { color: var(--brand); font-size: .55rem; font-weight: 800; letter-spacing: .025em; text-transform: uppercase; }
.hall-award-card :is(strong,span,.hall-person-link,.hall-team-link) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hall-person-link { color: #f3f6ff; font-size: .7rem; font-weight: 800; text-decoration: none; }
.hall-team-link { color: var(--muted); font-size: .59rem; text-decoration: none; }
.hall-person-link:hover, .hall-team-link:hover { color: var(--brand); }
.hall-award-card > div > strong { color: #f3f6ff; font-size: .7rem; }
.hall-award-card > div > span { color: var(--muted); font-size: .59rem; }
.hall-award-stat { position: relative; z-index: 1; display: grid; justify-items: center; color: #ffd263; font-size: 1rem; line-height: 1; }
.hall-award-stat small { margin-top: 3px; color: #cbb98e; font-size: .42rem; }
.hall-selection { min-width: 0; padding: 30px 24px 38px; border-top: 1px solid rgba(210,168,78,.22); background: radial-gradient(circle at 50% 18%, rgba(193,148,57,.1), transparent 42%), linear-gradient(180deg, rgba(5,10,20,.8), rgba(3,8,17,.96)); }
.hall-selection-title { display: flex; align-items: end; justify-content: space-between; width: min(100%, 760px); margin-inline: auto; gap: 14px; }
.hall-selection-title > div:first-child { display: grid; gap: 2px; }
.hall-selection-title h3 { color: #f4f5f3; font-size: clamp(1.4rem, 3vw, 2.35rem); font-weight: 900; letter-spacing: .025em; line-height: .94; text-transform: uppercase; text-shadow: 0 3px 16px rgba(0,0,0,.55); }
.hall-selection-title h3 span { display: block; margin-top: 7px; color: #e5bb62; }
.hall-formation { display: grid; justify-items: end; gap: 3px; }
.hall-formation strong { color: #e5bb62; font-size: .8rem; }
.hall-formation span { color: var(--muted); font-size: .58rem; }
.hall-pitch { position: relative; width: min(100%, 760px); aspect-ratio: 4 / 5; margin: 0 auto; overflow: hidden; border: 1px solid rgba(218,174,83,.58); border-radius: 6px; background-image: linear-gradient(rgba(2,6,8,.04), rgba(2,6,8,.16)), url('../assets/hall-of-fame/pitch-poster-v2.png'); background-position: center; background-size: cover; box-shadow: inset 0 0 90px rgba(0,0,0,.34), 0 30px 65px rgba(0,0,0,.42); }
.hall-pitch-markings { display: none; }
.hall-pitch::before { content: ''; position: absolute; inset: 0; background: radial-gradient(circle at 50% 42%, transparent 30%, rgba(1,4,6,.18) 76%, rgba(1,4,6,.42)); pointer-events: none; }
.hall-centre-line { position: absolute; left: 4%; right: 4%; top: 50%; border-top: 2px solid rgba(224,229,200,.34); }
.hall-centre-circle { position: absolute; left: 50%; top: 50%; width: 25%; aspect-ratio: 1; border: 2px solid rgba(224,229,200,.34); border-radius: 50%; transform: translate(-50%,-50%); }
.hall-area { position: absolute; left: 29%; width: 42%; height: 14%; border: 2px solid rgba(224,229,200,.34); }
.hall-area-top { top: 5%; border-top: 0; }
.hall-area-bottom { bottom: 5%; border-bottom: 0; }
.hall-goal { position: absolute; left: 41%; width: 18%; height: 3%; border: 2px solid rgba(224,229,200,.34); }
.hall-goal-top { top: 2%; border-bottom: 0; }
.hall-goal-bottom { bottom: 2%; border-top: 0; }
.hall-player { position: absolute; z-index: 2; display: grid; justify-items: center; width: 148px; transform: translate(-50%,-50%); }
.hall-player-photo { display: grid; place-items: center; width: 92px; height: 108px; overflow: hidden; border: 1px solid rgba(245,226,184,.84); border-radius: 8px 8px 2px 2px; background: radial-gradient(circle at 50% 25%, #424b55, #11171d 70%); color: #e2e5e8; font-size: .86rem; font-weight: 900; text-decoration: none; box-shadow: -5px 7px 0 rgba(193,139,38,.86), 0 12px 26px rgba(0,0,0,.55); }
.hall-player-photo img { width: 100%; height: 100%; object-fit: cover; object-position: center top; }
.hall-player-label { display: grid; justify-items: stretch; width: 140px; margin-top: -7px; text-align: center; filter: drop-shadow(0 6px 10px rgba(0,0,0,.5)); }
.hall-player-label > :is(a,span) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hall-player-label > a:first-of-type { z-index: 1; padding: 6px 8px; border: 1px solid rgba(229,187,98,.82); border-radius: 5px; background: linear-gradient(180deg, #151b22, #05080c); color: #fff; font-size: .66rem; font-weight: 900; text-decoration: none; }
.hall-player-label small { margin: -1px 9px 0; padding: 5px 5px 4px; border-radius: 0 0 4px 4px; background: linear-gradient(90deg, #9b681e, #efca70 48%, #a56e20); color: #171106; font-size: .45rem; font-weight: 900; text-transform: uppercase; box-shadow: inset 0 1px rgba(255,255,255,.25); }
.hall-player-label > :is(.hall-player-team,span) { margin-top: 5px; color: #f5dfa4; font-size: .64rem; font-weight: 900; letter-spacing: .015em; text-decoration: none; text-shadow: 0 2px 6px #000; }
.hall-player-label a:hover { color: #f1c96d; }
@media(max-width: 1020px) { .hall-filters { grid-template-columns: repeat(2,minmax(0,1fr)); } .hall-filter-heading { grid-column: 1 / -1; } }
@media(max-width: 640px) { .hall-page { width: min(calc(100% - 20px), 1240px); } .hall-filters { grid-template-columns: 1fr; padding: 15px; } .hall-filter-heading { grid-column: auto; } .hall-edition-head { padding: 16px; } .hall-edition-mark { width: 42px; height: 42px; flex-basis: 42px; } .hall-honors { padding: 18px 14px; } .hall-podium { gap: 6px; } .hall-podium-place { min-height: 106px; padding: 13px 5px; } .hall-podium-champion { min-height: 122px; } .hall-podium-place > span { width: 34px; height: 34px; } .hall-podium-champion > span { width: 40px; height: 40px; } .hall-podium-place small { font-size: .45rem; } .hall-podium-place :is(strong,a), .hall-podium-champion :is(strong,a) { font-size: .54rem; } .hall-awards { grid-template-columns: 1fr; gap: 8px; margin-top: 16px; } .hall-selection { padding: 18px 6px 20px; } .hall-selection-title { padding: 0 8px; } .hall-selection-title h3 { font-size: 1.2rem; } .hall-pitch { width: 100%; aspect-ratio: 4 / 5; } .hall-player { width: 82px; } .hall-player-photo { width: 46px; height: 54px; border-radius: 4px 4px 1px 1px; box-shadow: -3px 4px 0 rgba(193,139,38,.86), 0 7px 14px rgba(0,0,0,.5); } .hall-player-label { width: 80px; margin-top: -4px; } .hall-player-label > a:first-of-type { padding: 3px; font-size: .45rem; } .hall-player-label small { margin-inline: 5px; padding: 3px 2px 2px; font-size: .32rem; } .hall-player-label > :is(.hall-player-team,span) { margin-top: 2px; font-size: .4rem; } }
</style>
