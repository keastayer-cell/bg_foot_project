<template>
  <form class="admin-form admin-surface tour-match-form" @submit.prevent="$emit('create')">
    <header class="tour-match-form-head">
      <div class="tour-match-form-index">3</div>
      <div>
        <p class="tour-match-form-kicker">Новый матч</p>
        <h4 class="admin-list-title">{{ availabilityMessage ? 'Добавление недоступно' : 'Соберите пару команд' }}</h4>
      </div>
      <span class="tour-match-form-tour">{{ tour.name }}</span>
    </header>

    <div v-if="availabilityMessage" class="tour-match-unavailable">
      <span class="tour-match-unavailable-mark">✓</span>
      <div>
        <strong>Календарь сезона уже сформирован</strong>
        <p>{{ availabilityMessage }}</p>
      </div>
    </div>

    <template v-else>
      <div class="tour-matchup-builder">
        <label class="tour-team-picker" :class="{ 'is-selected': homeTeam }">
          <span class="tour-team-role"><b>01</b> Хозяева</span>
          <select v-model="form.homeTeamId" aria-label="Команда хозяев">
            <option value="">Выберите команду</option>
            <option v-for="team in homeTeams" :key="`home-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
          </select>
          <span class="tour-team-selection">{{ homeTeam?.name || 'Команда не выбрана' }}</span>
        </label>

        <div class="tour-matchup-divider">
          <span></span>
          <button
            type="button"
            :disabled="!homeTeam || !awayTeam"
            title="Поменять команды местами"
            aria-label="Поменять команды местами"
            @click="swapTeams"
          >⇄</button>
          <small>VS</small>
          <span></span>
        </div>

        <label class="tour-team-picker" :class="{ 'is-selected': awayTeam }">
          <span class="tour-team-role"><b>02</b> Гости</span>
          <select v-model="form.awayTeamId" aria-label="Команда гостей" :disabled="!homeTeam || !awayTeams.length">
            <option value="">{{ homeTeam ? 'Выберите команду' : 'Сначала выберите хозяев' }}</option>
            <option v-for="team in awayTeams" :key="`away-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
          </select>
          <span class="tour-team-selection">{{ awayTeam?.name || 'Команда не выбрана' }}</span>
        </label>
      </div>

      <div class="tour-match-schedule">
        <label>
          <span class="tour-match-schedule-label">Дата и время начала</span>
          <input v-model="form.kickoffAt" type="datetime-local" class="admin-temporal-input" step="60" />
        </label>
        <div class="tour-match-preview" :class="{ 'is-ready': canSubmit }">
          <small>Матч</small>
          <strong>{{ matchupLabel }}</strong>
        </div>
      </div>

      <p v-if="limitMessage" class="error-text tour-match-form-error">{{ limitMessage }}</p>

      <footer class="tour-match-form-footer">
        <p>После добавления матч появится в списке этого тура.</p>
        <button class="btn-primary" type="submit" :disabled="!canSubmit">Добавить матч</button>
      </footer>
    </template>
  </form>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  availabilityMessage: { type: String, default: '' },
  awayTeams: { type: Array, required: true },
  form: { type: Object, required: true },
  homeTeams: { type: Array, required: true },
  limitMessage: { type: String, default: '' },
  teams: { type: Array, required: true },
  tour: { type: Object, required: true },
})

defineEmits(['create'])

const homeTeam = computed(() => props.teams.find(
  (team) => String(team.id) === String(props.form.homeTeamId)
) || null)

const awayTeam = computed(() => props.teams.find(
  (team) => String(team.id) === String(props.form.awayTeamId)
) || null)

const matchupLabel = computed(() => {
  if (!homeTeam.value && !awayTeam.value) return 'Пара пока не выбрана'
  return `${homeTeam.value?.name || 'Хозяева'} — ${awayTeam.value?.name || 'Гости'}`
})

const canSubmit = computed(() => Boolean(
  homeTeam.value
  && awayTeam.value
  && props.form.kickoffAt
  && !props.limitMessage
))

function swapTeams() {
  if (!homeTeam.value || !awayTeam.value) return
  const previousHomeTeamId = props.form.homeTeamId
  props.form.homeTeamId = props.form.awayTeamId
  props.form.awayTeamId = previousHomeTeamId
}
</script>

<style scoped>
.tour-match-form {
  align-self: start;
  gap: 0;
  padding: 0;
  overflow: hidden;
}

.tour-match-form-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 17px 18px;
  border-bottom: 1px solid rgba(124, 163, 255, .14);
}

.tour-match-form-index {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(97, 232, 162, .38);
  border-radius: 9px;
  color: var(--brand);
  font-size: .8rem;
  font-weight: 800;
}

.tour-match-form-kicker {
  margin: 0 0 3px;
  color: var(--brand);
  font-size: .68rem;
  font-weight: 800;
  letter-spacing: .09em;
  text-transform: uppercase;
}

.tour-match-form-tour {
  max-width: 150px;
  padding: 7px 10px;
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, .16);
  border-radius: 8px;
  color: var(--muted);
  font-size: .72rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tour-match-unavailable {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  margin: 18px;
  padding: 17px;
  border: 1px solid rgba(97, 232, 162, .24);
  border-radius: 12px;
  background: rgba(97, 232, 162, .055);
}

.tour-match-unavailable-mark {
  display: grid;
  flex: 0 0 30px;
  height: 30px;
  place-items: center;
  border: 1px solid rgba(97, 232, 162, .36);
  border-radius: 50%;
  color: var(--brand);
  font-size: .75rem;
  font-weight: 800;
}

.tour-match-unavailable strong,
.tour-match-unavailable p { margin: 0; }
.tour-match-unavailable strong { font-size: .82rem; }
.tour-match-unavailable p { margin-top: 5px; color: var(--muted); font-size: .74rem; line-height: 1.5; }

.tour-matchup-builder {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px minmax(0, 1fr);
  align-items: stretch;
  padding: 18px;
}

.tour-team-picker {
  display: grid;
  align-content: start;
  gap: 9px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(124, 163, 255, .17);
  border-radius: 12px;
  background: rgba(7, 13, 32, .42);
  transition: border-color .18s ease, background .18s ease;
}

.tour-team-picker.is-selected {
  border-color: rgba(97, 232, 162, .34);
  background: rgba(97, 232, 162, .055);
}

.tour-team-picker select { width: 100%; }

.tour-team-role {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  font-size: .72rem;
  font-weight: 700;
  text-transform: uppercase;
}

.tour-team-role b {
  color: var(--brand);
  font-size: .66rem;
}

.tour-team-selection {
  min-height: 1.2em;
  overflow: hidden;
  color: var(--muted);
  font-size: .72rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tour-team-picker.is-selected .tour-team-selection { color: var(--text); }

.tour-matchup-divider {
  display: grid;
  grid-template-rows: 1fr auto auto 1fr;
  place-items: center;
  gap: 6px;
  padding: 4px 0;
}

.tour-matchup-divider > span {
  width: 1px;
  height: 100%;
  background: rgba(124, 163, 255, .18);
}

.tour-matchup-divider button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid rgba(97, 232, 162, .3);
  border-radius: 8px;
  background: rgba(97, 232, 162, .08);
  color: var(--brand);
  cursor: pointer;
}

.tour-matchup-divider button:disabled {
  border-color: rgba(124, 163, 255, .12);
  color: var(--muted);
  cursor: default;
  opacity: .5;
}

.tour-matchup-divider small {
  color: var(--muted);
  font-size: .6rem;
  font-weight: 800;
}

.tour-match-schedule {
  display: grid;
  grid-template-columns: minmax(220px, .78fr) minmax(0, 1.22fr);
  gap: 12px;
  padding: 0 18px 18px;
}

.tour-match-schedule label { display: grid; gap: 7px; }

.tour-match-schedule-label,
.tour-match-preview small {
  color: var(--muted);
  font-size: .7rem;
  font-weight: 700;
}

.tour-match-schedule input { width: 100%; }

.tour-match-preview {
  display: grid;
  align-content: center;
  gap: 4px;
  min-width: 0;
  padding: 10px 13px;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 10px;
  background: rgba(7, 13, 32, .34);
}

.tour-match-preview.is-ready { border-color: rgba(97, 232, 162, .26); }

.tour-match-preview strong {
  overflow: hidden;
  font-size: .78rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tour-match-form-error { margin: 0 18px 16px; }

.tour-match-form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-top: 1px solid rgba(124, 163, 255, .14);
  background: rgba(5, 10, 25, .26);
}

.tour-match-form-footer p {
  margin: 0;
  color: var(--muted);
  font-size: .7rem;
}

.tour-match-form-footer .btn-primary { flex: 0 0 auto; }

@media (max-width: 1120px) {
  .tour-matchup-builder { grid-template-columns: 1fr; gap: 10px; }
  .tour-matchup-divider { grid-template-columns: 1fr auto auto 1fr; grid-template-rows: auto; padding: 0 8px; }
  .tour-matchup-divider > span { width: 100%; height: 1px; }
  .tour-match-schedule { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .tour-match-form-head { grid-template-columns: auto minmax(0, 1fr); }
  .tour-match-form-tour { display: none; }
  .tour-match-form-footer { align-items: stretch; flex-direction: column; }
  .tour-match-form-footer .btn-primary { width: 100%; }
}
</style>
