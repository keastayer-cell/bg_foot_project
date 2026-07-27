<template>
  <table
    v-for="variant in variants"
    :key="variant"
    class="stats-table"
    :class="`standings-table-${variant}`"
  >
    <thead>
      <tr>
        <th>Место</th>
        <th>Команда</th>
        <th>И</th>
        <th>ЗМ</th>
        <th>ПМ</th>
        <th>РМ</th>
        <th>О</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="row in standings" :key="`${variant}-${row.teamId}`">
        <td>{{ row.position }}</td>
        <td>
          <div class="team-cell" :class="{ 'team-cell-mobile': variant === 'mobile' }">
            <RouterLink
              class="team-link"
              :to="{
                path: `/teams/${row.teamId}`,
                query: { seasonId: String(seasonId) },
              }"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ row.teamName }}
            </RouterLink>
          </div>
        </td>
        <td>{{ row.matchesPlayed }}</td>
        <td>{{ row.goalsFor }}</td>
        <td>{{ row.goalsAgainst }}</td>
        <td>{{ signedGoalDifference(row.goalDifference) }}</td>
        <td><strong>{{ row.points }}</strong></td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  standings: {
    type: Array,
    required: true,
  },
  seasonId: {
    type: [String, Number],
    required: true,
  },
})

const variants = ['desktop', 'mobile']

function signedGoalDifference(value) {
  const normalized = Number(value || 0)
  return normalized > 0 ? `+${normalized}` : `${normalized}`
}
</script>
