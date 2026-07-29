<template>
  <component
    :is="card.matchId ? 'router-link' : 'article'"
    :to="matchTarget"
    class="playoff-match-card"
    :class="{
      'playoff-match-card-center': center,
      'is-placeholder': !card.matchId,
      'is-clickable': Boolean(card.matchId),
      'is-third-place': card.roundKey === 'THIRD_PLACE',
    }"
    :aria-label="card.matchId ? `Открыть матч: ${card.homeTeamName} — ${card.awayTeamName}` : undefined"
    :title="card.matchId ? 'Открыть страницу матча' : undefined"
  >
    <div class="playoff-match-card-head">
      <span class="playoff-match-card-badge">{{ card.badge }}</span>
      <span v-if="card.dateLabel" class="playoff-match-card-date">{{ card.dateLabel }}</span>
    </div>
    <div class="playoff-match-card-body">
      <div class="playoff-team-slot">
        <strong>{{ card.homeTeamName }}</strong>
        <span class="playoff-team-score">{{ card.homeScoreLabel }}</span>
      </div>
      <div class="playoff-team-slot">
        <strong>{{ card.awayTeamName }}</strong>
        <span class="playoff-team-score">{{ card.awayScoreLabel }}</span>
      </div>
    </div>
    <div v-if="card.statusLabel || card.tourLabel" class="playoff-match-card-footer">
      <span class="muted-text">{{ card.statusLabel }}</span>
      <span v-if="card.tourLabel" class="muted-text">{{ card.tourLabel }}</span>
    </div>
    <span v-if="card.matchId" class="playoff-match-card-open" aria-hidden="true">Открыть матч →</span>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import { matchPageLocation } from '../../utils/publicUrls'

const props = defineProps({
  card: {
    type: Object,
    required: true,
  },
  center: {
    type: Boolean,
    default: false,
  },
  seasonId: {
    type: [String, Number],
    default: '',
  },
})

const matchTarget = computed(() => {
  if (!props.card.matchId) return undefined
  return matchPageLocation(props.card.matchId, {
    returnContext: 'playoff',
    ...(props.seasonId ? { seasonId: String(props.seasonId) } : {}),
  })
})
</script>
