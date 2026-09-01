<template>
  <section class="cup-matches-workspace">
    <AdminCupDrawPanel
      v-if="cup.drawStatus !== 'CONFIRMED'"
      :busy="drawBusy"
      :cup="cup"
      :draw-order="drawOrder"
      :rounds="rounds"
      @confirm="$emit('confirm-draw')"
      @draw-manual="$emit('draw-manual')"
      @draw-random="$emit('draw-random')"
      @move-team="(index, delta) => $emit('move-team', index, delta)"
    />

    <div v-else class="admin-form admin-surface cup-pair-picker">
      <label>
        Кубковая пара
        <select v-model="selectedTieId" @change="onTieChange">
          <option value="">— выберите пару —</option>
          <optgroup v-for="round in rounds" :key="round.code" :label="round.label">
            <option v-for="tie in round.ties" :key="tie.id" :value="String(tie.id)">
              {{ tieLabel(tie) }}
            </option>
          </optgroup>
        </select>
      </label>
      <p v-if="!rounds.length" class="muted-text">В утверждённой сетке пока нет пар.</p>
    </div>

    <div v-if="cup.drawStatus === 'CONFIRMED' && selectedTie" class="admin-grid">
      <form class="admin-form admin-surface" @submit.prevent="$emit('create')">
        <div>
          <span class="muted-text">{{ roundName(selectedTie) }}</span>
          <h4 class="admin-list-title">{{ participantName(selectedTie.homeTeam) }} — {{ participantName(selectedTie.awayTeam) }}</h4>
        </div>
        <p class="muted-text">
          {{ selectedTie.legCount }} {{ selectedTie.legCount === 1 ? 'матч' : 'матча' }} в паре.
        </p>

        <template v-if="selectedTie.matches?.length">
          <p class="muted-text">Матчи этой пары уже сформированы.</p>
        </template>
        <template v-else-if="selectedTie.homeTeam && selectedTie.awayTeam && cup.drawStatus === 'CONFIRMED'">
          <label v-for="leg in selectedTie.legCount" :key="leg">
            {{ selectedTie.legCount === 1 ? 'Дата и время матча' : `Дата и время матча ${leg}` }}
            <input
              v-model="kickoffDates[leg - 1]"
              type="datetime-local"
              class="admin-temporal-input admin-temporal-input-wide"
              step="60"
            />
          </label>
          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="!canCreate">Создать матчи пары</button>
          </div>
        </template>
        <p v-else class="muted-text">Участники этой пары определятся после завершения предыдущего раунда.</p>
      </form>

      <div class="admin-list admin-surface">
        <h4 class="admin-list-title">Матчи пары</h4>
        <p v-if="!selectedTie.matches?.length" class="muted-text">Матчи ещё не созданы.</p>
        <div v-else class="admin-list-items">
          <router-link
            v-for="match in selectedTie.matches"
            :key="match.id"
            :to="`/matches/${match.id}`"
            class="admin-list-item cup-match-item"
          >
            <div class="cup-match-copy">
              <strong>{{ participantName(match.homeTeam) }} — {{ participantName(match.awayTeam) }}</strong>
              <span class="muted-text">{{ formatDateTime(match.kickoffAt) }}</span>
              <span v-if="match.homeScore != null" class="muted-text">Счёт: {{ match.homeScore }}:{{ match.awayScore }}</span>
              <span class="tour-match-status-badge" :class="statusBadgeClass(match.protocolStatus)">
                {{ statusLabel(match.protocolStatus) }}
              </span>
            </div>
            <span class="cup-match-open">Открыть матч</span>
          </router-link>
        </div>
        <form v-if="needsWinner" class="cup-penalty-form" @submit.prevent="$emit('save-winner')">
          <strong>Результат серии пенальти</strong>
          <div class="cup-penalty-fields">
            <label>
              {{ participantName(selectedTie.homeTeam) }}
              <input v-model.number="penaltyForm.home" type="number" min="0" />
            </label>
            <label>
              {{ participantName(selectedTie.awayTeam) }}
              <input v-model.number="penaltyForm.away" type="number" min="0" />
            </label>
          </div>
          <button class="btn-primary" type="submit" :disabled="!canSaveWinner">Сохранить победителя пары</button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import AdminCupDrawPanel from './AdminCupDrawPanel.vue'

defineProps({
  canCreate: { type: Boolean, default: false },
  canSaveWinner: { type: Boolean, default: false },
  cup: { type: Object, required: true },
  drawBusy: { type: Boolean, default: false },
  drawOrder: { type: Array, required: true },
  formatDateTime: { type: Function, required: true },
  kickoffDates: { type: Array, required: true },
  onTieChange: { type: Function, required: true },
  penaltyForm: { type: Object, required: true },
  rounds: { type: Array, required: true },
  selectedTie: { type: Object, default: null },
  needsWinner: { type: Boolean, default: false },
  statusBadgeClass: { type: Function, required: true },
  statusLabel: { type: Function, required: true },
})

const selectedTieId = defineModel('selectedTieId', { type: String, default: '' })
defineEmits(['confirm-draw', 'create', 'draw-manual', 'draw-random', 'move-team', 'save-winner'])

function participantName(team) {
  return team?.shortName || team?.name || 'Участник не определён'
}

function roundName(tie) {
  return String(tie?.title || '').split(' · ')[0] || 'Кубковый раунд'
}

function tieLabel(tie) {
  const pair = `Пара ${tie.slotOrder}: ${participantName(tie.homeTeam)} — ${participantName(tie.awayTeam)}`
  return tie.matches?.length ? `${pair} · матчи созданы` : pair
}
</script>

<style scoped>
.cup-matches-workspace{display:grid;gap:18px}.cup-pair-picker{max-width:none}.cup-pair-picker label{max-width:720px}.cup-match-item{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:16px;color:inherit;text-decoration:none}.cup-match-copy{display:grid;gap:6px;min-width:0}.cup-match-open{color:var(--brand);font-weight:700;white-space:nowrap}.cup-penalty-form{display:grid;gap:12px;margin-top:18px;padding-top:18px;border-top:1px solid var(--line)}.cup-penalty-fields{display:grid;grid-template-columns:1fr 1fr;gap:12px}.cup-penalty-fields label{display:grid;gap:6px}@media(max-width:600px){.cup-match-item,.cup-penalty-fields{grid-template-columns:1fr}.cup-match-open{justify-self:start}}
</style>
