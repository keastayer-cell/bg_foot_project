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
          <span class="muted-text">Расписание: {{ scheduleStatusLabel(match.scheduleStatus) }}</span>
          <span v-if="match.venueName" class="muted-text">{{ match.venueName }}</span>
          <span v-if="match.scheduleChangeReason" class="muted-text">Причина: {{ match.scheduleChangeReason }}</span>
        </div>
        <div class="tour-match-actions">
          <button class="btn-ghost btn-sm" type="button" @click="$emit('edit-schedule', match)">Расписание</button>
          <button
            class="btn-danger btn-sm"
            type="button"
            :disabled="!canDelete(match)"
            :title="deleteTitle(match)"
            @click="$emit('delete', match.id)"
          >Удалить</button>
        </div>
        <form v-if="String(scheduleEditingId) === String(match.id)" class="tour-schedule-editor" @submit.prevent="$emit('save-schedule', match.id)">
          <label>Статус
            <select v-model="scheduleForm.status">
              <option value="SCHEDULED">Запланирован</option>
              <option value="RESCHEDULED">Перенесён</option>
              <option value="CANCELLED">Отменён</option>
            </select>
          </label>
          <label>Дата и время
            <input v-model="scheduleForm.kickoffAt" type="datetime-local" step="60" required />
          </label>
          <label>Площадка
            <select v-model="scheduleForm.venueId">
              <option value="">— не указана —</option>
              <option v-for="venue in venues" :key="venue.id" :value="String(venue.id)">{{ venue.name }}</option>
            </select>
          </label>
          <label class="tour-schedule-reason">Причина
            <input v-model="scheduleForm.reason" type="text" maxlength="500" :required="['RESCHEDULED', 'CANCELLED'].includes(scheduleForm.status)" placeholder="Причина переноса или отмены" />
          </label>
          <div class="tour-schedule-buttons">
            <button class="btn-ghost btn-sm" type="button" :disabled="scheduleSaving" @click="$emit('cancel-schedule')">Отмена</button>
            <button class="btn-primary btn-sm" type="submit" :disabled="scheduleSaving">{{ scheduleSaving ? 'Сохранение…' : 'Сохранить' }}</button>
          </div>
        </form>
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
  scheduleEditingId: { type: [String, Number], default: '' },
  scheduleForm: { type: Object, required: true },
  scheduleSaving: { type: Boolean, default: false },
  scheduleStatusLabel: { type: Function, required: true },
  scoreLabel: { type: Function, required: true },
  statusBadgeClass: { type: Function, required: true },
  statusLabel: { type: Function, required: true },
  tour: { type: Object, required: true },
  venues: { type: Array, default: () => [] },
})

defineEmits(['cancel-schedule', 'delete', 'edit-schedule', 'publish', 'save-schedule'])
</script>

<style scoped>
.tour-match-actions { display: flex; align-items: center; gap: 8px; }
.tour-schedule-editor { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; width: 100%; padding-top: 14px; border-top: 1px solid var(--line); }
.tour-schedule-editor label { display: grid; gap: 6px; color: var(--muted); font-size: .75rem; }
.tour-schedule-reason { grid-column: 1 / -1; }
.tour-schedule-buttons { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 720px) { .tour-schedule-editor { grid-template-columns: 1fr; } .tour-schedule-reason, .tour-schedule-buttons { grid-column: auto; } }
</style>
