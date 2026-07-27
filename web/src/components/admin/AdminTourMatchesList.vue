<template>
  <div class="admin-list admin-surface">
    <div class="tour-matches-header">
      <h4 class="admin-list-title">Матчи тура</h4>
      <button class="btn-ghost tour-publish-button" type="button" :disabled="!canPublish" @click="$emit('publish')">
        {{ tour.published ? 'Опубликован' : 'Опубликовать тур' }}
      </button>
    </div>
    <p v-if="!matches.length" class="muted-text">В этом туре пока нет матчей.</p>
    <div v-else class="admin-list-items">
      <article v-for="match in matches" :key="match.id" class="admin-list-item tour-match-item">
        <div class="tour-match-copy">
          <strong>{{ match.homeTeamName }} - {{ match.awayTeamName }}</strong>
          <span class="muted-text">{{ formatDateTime(match.kickoffAt) }}</span>
          <span v-if="scoreLabel(match)" class="muted-text">{{ scoreLabel(match) }}</span>
          <span class="tour-match-status-badge" :class="statusBadgeClass(match.protocolStatus)">
            {{ statusLabel(match.protocolStatus) }}
          </span>
        </div>
        <button
          class="btn-danger btn-sm"
          type="button"
          :disabled="!canDelete(match)"
          :title="deleteTitle(match)"
          @click="$emit('delete', match.id)"
        >Удалить</button>
      </article>
    </div>
    <p class="muted-text tour-publish-note">Публично на сайт попадут только опубликованные туры.</p>
  </div>
</template>

<script setup>
defineProps({
  canDelete: { type: Function, required: true },
  canPublish: { type: Boolean, default: false },
  deleteTitle: { type: Function, required: true },
  formatDateTime: { type: Function, required: true },
  matches: { type: Array, required: true },
  scoreLabel: { type: Function, required: true },
  statusBadgeClass: { type: Function, required: true },
  statusLabel: { type: Function, required: true },
  tour: { type: Object, required: true },
})

defineEmits(['delete', 'publish'])
</script>
