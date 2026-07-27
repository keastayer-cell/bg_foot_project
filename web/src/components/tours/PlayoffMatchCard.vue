<template>
  <component
    :is="card.matchId ? 'router-link' : 'article'"
    :to="card.matchId ? `/match/${card.matchId}` : undefined"
    class="playoff-match-card"
    :class="{
      'playoff-match-card-center': center,
      'is-placeholder': !card.matchId,
      'is-third-place': card.roundKey === 'THIRD_PLACE',
    }"
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
  </component>
</template>

<script setup>
defineProps({
  card: {
    type: Object,
    required: true,
  },
  center: {
    type: Boolean,
    default: false,
  },
})
</script>
