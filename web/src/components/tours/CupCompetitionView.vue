<template>
  <div class="cup-public-view">
    <div class="cup-public-head">
      <div>
        <h2 class="section-title">{{ competition.name }}</h2>
        <p class="muted-text">{{ rulesLabel }}</p>
      </div>
      <span class="cup-public-status">{{ statusLabel }}</span>
    </div>

    <SeasonPlayoffBracket
      v-if="competition.ties?.length"
      :left-columns="leftColumns"
      :right-columns="rightColumns"
      :center-cards="centerCards"
      :season-id="competition.seasonId"
    />
    <p v-else class="empty-text">Жеребьёвка Кубка пока не проведена.</p>

    <div class="cup-public-stats">
      <h3>Статистика Кубка</h3>
      <div class="table-wrap" v-if="stats.length">
        <table>
          <thead><tr><th>Игрок</th><th>Команда</th><th>Голы</th><th>ЖК</th><th>КК</th></tr></thead>
          <tbody><tr v-for="row in stats" :key="row.playerId"><td>{{ row.playerName }}</td><td>{{ row.teamNames }}</td><td>{{ row.goals }}</td><td>{{ row.yellowCards }}</td><td>{{ row.redCards }}</td></tr></tbody>
        </table>
      </div>
      <p v-else class="empty-text">Подтверждённых матчей Кубка пока нет.</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useSeasonPlayoff } from '../../composables/useSeasonPlayoff'
import SeasonPlayoffBracket from './SeasonPlayoffBracket.vue'

const props = defineProps({ competition: { type: Object, required: true }, stats: { type: Array, default: () => [] } })
const bracket = computed(() => ({
  teamCount: props.competition.teams?.length || 0,
  thirdPlaceEnabled: Boolean(props.competition.thirdPlaceEnabled),
  ties: (props.competition.ties || []).map((tie) => ({
    ...tie,
    homeTeamId: tie.homeTeam?.id || null,
    homeTeamName: tie.homeTeam?.shortName || tie.homeTeam?.name || '',
    awayTeamId: tie.awayTeam?.id || null,
    awayTeamName: tie.awayTeam?.shortName || tie.awayTeam?.name || '',
    matchIds: (tie.matches || []).map((match) => match.id),
  })),
}))
const season = computed(() => ({
  playoffTeamCount: props.competition.teams?.length || 0,
}))
const { centerCards, leftColumns, rightColumns } = useSeasonPlayoff({
  bracket,
  season,
  tours: computed(() => []),
  teamPositionMap: computed(() => new Map()),
})
const statusLabel = computed(() => ({ DRAFT: 'Подготовка', ACTIVE: 'Идёт', FINISHED: 'Завершён' })[props.competition.status] || props.competition.status)
const rulesLabel = computed(() => `${props.competition.regularTieLegs === 1 ? 'Один матч' : 'Два матча'} в паре · ${props.competition.playersOnField} игроков на поле${props.competition.penaltiesEnabled ? ' · пенальти' : ''}`)
</script>

<style scoped>
.cup-public-view{min-width:0}.cup-public-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.cup-public-status{padding:7px 10px;border:1px solid var(--line);color:var(--brand)}.cup-public-stats{margin-top:18px;border-top:1px solid var(--line);padding-top:16px}.cup-public-stats th,.cup-public-stats td{text-align:left;padding:9px 12px;border-bottom:1px solid var(--line)}@media(max-width:600px){.cup-public-head{display:grid}}
</style>
