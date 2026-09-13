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
          <button class="btn-ghost btn-sm" type="button" :disabled="match.protocolStatus === 'VERIFIED' || scheduleSaving" :title="match.protocolStatus === 'VERIFIED' ? 'Сначала суперадминистратор должен открыть протокол' : 'Изменить расписание'" @click="$emit('edit-schedule', match)">Расписание</button>
          <button
            class="btn-danger btn-sm"
            type="button"
            :disabled="!canDelete(match)"
            :title="deleteTitle(match)"
            @click="$emit('delete', match.id)"
          >Удалить</button>
        </div>
        <p v-if="match.protocolStatus === 'VERIFIED'" class="tour-schedule-lock">Расписание закрыто для изменений. Открыть протокол может только суперадминистратор.</p>
        <form v-if="match.protocolStatus !== 'VERIFIED' && String(scheduleEditingId) === String(match.id)" class="tour-schedule-editor" @submit.prevent="$emit('save-schedule', match.id)">
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
    <dialog ref="errorDialog" class="tour-schedule-error-dialog" aria-labelledby="schedule-error-title" @close="$emit('dismiss-schedule-error')">
      <h3 id="schedule-error-title">Расписание не сохранено</h3><p>{{ scheduleError }}</p><button class="btn-primary" type="button" @click="errorDialog.close()">Понятно</button>
    </dialog>
    <p class="muted-text tour-publish-note">Публично на сайт попадут только опубликованные туры.</p>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
const props = defineProps({
  canDelete: { type: Function, required: true },
  canPublish: { type: Boolean, default: false },
  deleteTitle: { type: Function, required: true },
  formatDateTime: { type: Function, required: true },
  matches: { type: Array, required: true },
  scheduleEditingId: { type: [String, Number], default: '' },
  scheduleForm: { type: Object, required: true },
  scheduleSaving: { type: Boolean, default: false },
  scheduleError: { type: String, default: '' },
  scheduleStatusLabel: { type: Function, required: true },
  scoreLabel: { type: Function, required: true },
  statusBadgeClass: { type: Function, required: true },
  statusLabel: { type: Function, required: true },
  tour: { type: Object, required: true },
  venues: { type: Array, default: () => [] },
})

const errorDialog = ref(null)
watch(() => props.scheduleError, async message => {
  await nextTick()
  if (message && !errorDialog.value?.open) errorDialog.value?.showModal()
  else if (!message && errorDialog.value?.open) errorDialog.value?.close()
})
defineEmits(['dismiss-schedule-error', 'cancel-schedule', 'delete', 'edit-schedule', 'publish', 'save-schedule'])
</script>

<style scoped>
.tour-match-actions { display: flex; align-items: center; gap: 8px; }
.tour-schedule-editor { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; width: 100%; padding-top: 14px; border-top: 1px solid var(--line); }
.tour-schedule-editor label { display: grid; gap: 6px; color: var(--muted); font-size: .75rem; }
.tour-schedule-reason { grid-column: 1 / -1; }
.tour-schedule-buttons { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 720px) { .tour-schedule-editor { grid-template-columns: 1fr; } .tour-schedule-reason, .tour-schedule-buttons { grid-column: auto; } }
</style>

<style scoped>
.admin-list-items .tour-match-item{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:16px;align-items:start;padding:20px}.tour-match-copy{min-width:0}.tour-match-copy>strong{font-size:1rem;line-height:1.4}.tour-match-copy>.muted-text{font-size:.82rem}.tour-match-actions{justify-self:end;flex-wrap:wrap}.tour-schedule-editor{grid-column:1/-1;grid-template-columns:minmax(140px,.7fr) minmax(240px,1fr) minmax(170px,1fr);gap:16px;padding:20px;margin-top:4px;border:1px solid var(--line);border-radius:10px;background:rgba(7,13,31,.55);box-sizing:border-box;min-width:0}.tour-schedule-editor label{min-width:0;gap:8px;font-size:.8rem}.tour-schedule-editor input,.tour-schedule-editor select{width:100%;min-width:0;max-width:100%;height:44px;box-sizing:border-box;margin:0;font-size:.85rem}.tour-schedule-buttons{margin:0}.tour-schedule-buttons button{height:40px;margin:0;padding:0 16px}.tour-schedule-lock{grid-column:1/-1;margin:0;padding:12px 14px;border:1px solid var(--line);border-radius:8px;background:rgba(124,163,255,.04);color:var(--muted);font-size:.78rem;line-height:1.5}.tour-schedule-error-dialog{max-width:460px;width:calc(100% - 40px);padding:24px;border:1px solid var(--line);border-radius:14px;background:#10182e;color:var(--text);box-sizing:border-box;box-shadow:0 24px 80px rgba(0,0,0,.5)}.tour-schedule-error-dialog::backdrop{background:rgba(3,7,18,.75)}.tour-schedule-error-dialog h3{font-size:1.1rem;margin:0 0 14px}.tour-schedule-error-dialog p{font-size:.9rem;line-height:1.6;margin-bottom:20px}.tour-schedule-error-dialog button{float:right}
@media(max-width:1000px){.tour-schedule-editor{grid-template-columns:1fr 1fr}.tour-schedule-editor label:nth-child(3){grid-column:1/-1}}
@media(max-width:640px){.admin-list-items .tour-match-item{grid-template-columns:1fr;padding:16px}.tour-match-actions{justify-self:start}.tour-schedule-editor{grid-template-columns:1fr;padding:16px}.tour-schedule-editor label:nth-child(3),.tour-schedule-reason,.tour-schedule-buttons{grid-column:1}}
</style>
<style scoped>
.tour-match-item{container-type:inline-size}.tour-schedule-editor{grid-template-columns:repeat(3,minmax(0,1fr))}
@container(max-width:650px){.tour-schedule-editor{grid-template-columns:minmax(0,1fr)}.tour-schedule-editor label,.tour-schedule-reason,.tour-schedule-buttons{grid-column:1}}
</style>
<style scoped>
.tour-match-actions button:disabled{opacity:.4;cursor:not-allowed;filter:grayscale(1);box-shadow:none;transform:none;background:rgba(124,163,255,.06);border:1px solid var(--line);color:var(--muted)}
.tour-match-actions button:disabled:hover{transform:none;box-shadow:none;background:rgba(124,163,255,.06);color:var(--muted)}
</style>
