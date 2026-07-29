<template>
  <div class="matrix-wrap matrix-desktop">
    <table class="matrix-table">
      <thead>
        <tr>
          <th class="matrix-team-head">Команда</th>
          <th v-for="team in teams" :key="`matrix-col-${team.id}`" class="matrix-col-head">
            {{ team.positionLabel }}
          </th>
          <th>И</th>
          <th>В</th>
          <th>Н</th>
          <th>П</th>
          <th>М</th>
          <th>О</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="`matrix-row-${row.team.id}`">
          <td class="matrix-team-cell">
            <div class="matrix-team-copy">
              <strong>{{ row.team.name }}</strong>
            </div>
          </td>
          <td
            v-for="cell in row.cells"
            :key="`matrix-cell-${row.team.id}-${cell.opponentTeamId}`"
            class="matrix-score-cell"
            :class="{
              'matrix-score-self': cell.isSelf,
              'matrix-score-empty': !cell.isSelf && !cell.results.length,
            }"
          >
            <span v-if="cell.isSelf" class="matrix-ball" aria-hidden="true">⚽</span>
            <template v-else-if="cell.results.length">
              <component
                v-for="result in cell.results"
                :key="result.key"
                :is="result.matchId ? 'router-link' : 'span'"
                :to="result.matchId ? matchLocation(result.matchId) : undefined"
                class="matrix-score-pill"
                :class="{
                  'matrix-score-link': Boolean(result.matchId),
                  'matrix-score-pending': result.pending,
                }"
              >
                {{ result.label }}
              </component>
            </template>
            <template v-else>—</template>
          </td>
          <td>{{ row.summary.played }}</td>
          <td>{{ row.summary.wins }}</td>
          <td>{{ row.summary.draws }}</td>
          <td>{{ row.summary.losses }}</td>
          <td>{{ row.summary.goalsFor }}-{{ row.summary.goalsAgainst }}</td>
          <td><strong>{{ row.summary.points }}</strong></td>
        </tr>
      </tbody>
    </table>
  </div>

  <div class="matrix-mobile-list">
    <article v-for="row in rows" :key="`matrix-mobile-${row.team.id}`" class="matrix-mobile-card">
      <div class="matrix-mobile-card-head">
        <div class="matrix-mobile-team-copy">
          <strong>{{ row.team.name }}</strong>
          <span class="muted-text">И: {{ row.summary.played }} · О: {{ row.summary.points }}</span>
        </div>
        <span class="matrix-mobile-goals">{{ row.summary.goalsFor }}-{{ row.summary.goalsAgainst }}</span>
      </div>

      <div class="matrix-mobile-summary">
        <span>В: <strong>{{ row.summary.wins }}</strong></span>
        <span>Н: <strong>{{ row.summary.draws }}</strong></span>
        <span>П: <strong>{{ row.summary.losses }}</strong></span>
      </div>

      <div class="matrix-mobile-opponents">
        <article
          v-for="cell in row.cells.filter((item) => !item.isSelf)"
          :key="`matrix-mobile-opp-${row.team.id}-${cell.opponentTeamId}`"
          class="matrix-mobile-opponent"
        >
          <div class="matrix-mobile-opponent-head">
            <strong>{{ cell.opponentName }}</strong>
          </div>
          <div class="matrix-mobile-results">
            <component
              v-for="result in cell.results"
              :key="result.key"
              :is="result.matchId ? 'router-link' : 'span'"
              :to="result.matchId ? matchLocation(result.matchId) : undefined"
              class="matrix-mobile-result-pill"
              :class="{
                'matrix-score-link': Boolean(result.matchId),
                'matrix-score-pending': result.pending,
              }"
            >
              {{ result.label }}
            </component>
          </div>
        </article>
      </div>
    </article>
  </div>
</template>

<script setup>
import { matchPageLocation } from '../../utils/publicUrls'

const props = defineProps({
  teams: {
    type: Array,
    required: true,
  },
  rows: {
    type: Array,
    required: true,
  },
  seasonId: {
    type: [String, Number],
    default: '',
  },
})

function matchLocation(matchId) {
  return matchPageLocation(matchId, {
    returnContext: 'matrix',
    ...(props.seasonId ? { seasonId: String(props.seasonId) } : {}),
  })
}
</script>
