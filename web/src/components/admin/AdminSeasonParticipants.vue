<template>
  <section class="admin-season-section">
    <div class="admin-season-section-head">
      <div class="admin-season-section-copy">
        <h4 class="admin-list-title">Команды сезона</h4>
        <p class="muted-text">Состав сезона и краткая сводка по формату собраны в одном месте.</p>
      </div>
    </div>
    <div class="actions-row admin-season-team-row">
      <select :value="teamToAddId" @change="$emit('update:teamToAddId', $event.target.value)">
        <option value="">— выберите команду —</option>
        <option v-for="team in availableTeams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
      </select>
      <button class="btn-ghost" type="button" @click="$emit('add-team')">Добавить команду</button>
    </div>
    <p class="muted-text admin-season-selected-note">Выбрано: {{ selectedTeams.length }}</p>
    <p v-if="!selectedTeams.length" class="muted-text admin-season-empty-state">Пока не выбрано ни одной команды.</p>
    <div v-else class="admin-list-items admin-season-team-list">
      <article v-for="team in selectedTeams" :key="team.id" class="admin-list-item admin-season-picked-item">
        <div class="admin-season-picked-copy">
          <strong>{{ team.name }}</strong>
          <p class="muted-text">Команда включена в состав сезона.</p>
        </div>
        <button class="btn-danger btn-sm" type="button" @click="$emit('remove-team', team.id)">Убрать</button>
      </article>
    </div>
    <div class="admin-season-meta-grid">
      <article class="admin-season-meta-card">
        <span class="admin-season-meta-label">Регулярный этап</span>
        <strong>{{ regularToursCount }} туров</strong>
        <p class="muted-text">При {{ selectedTeams.length }} командах.</p>
      </article>
      <article class="admin-season-meta-card">
        <span class="admin-season-meta-label">Дисциплина</span>
        <strong>{{ Number(form.yellowCardsForSuspension || 0) || 0 }} ЖК / {{ Number(form.redCardsForSuspension || 0) || 0 }} КК</strong>
        <p class="muted-text">Порог автоматического пропуска.</p>
      </article>
      <article class="admin-season-meta-card">
        <span class="admin-season-meta-label">Регламент состава</span>
        <strong>{{ form.maxRosterSize || 'Без лимита' }}</strong>
        <p class="muted-text">Статус: {{ statusLabel }}.</p>
      </article>
      <article class="admin-season-meta-card">
        <span class="admin-season-meta-label">Трансферы</span>
        <strong>{{ transferWindowLabel }}</strong>
        <p class="muted-text">Пустые даты означают отсутствие ограничений.</p>
      </article>
      <article v-if="form.playoffEnabled && form.playoffTeamCount" class="admin-season-meta-card admin-season-meta-card-accent">
        <span class="admin-season-meta-label">Плей-офф</span>
        <strong>{{ form.playoffTeamCount }} команд</strong>
        <p class="muted-text">{{ form.thirdPlaceEnabled ? 'С матчем за 3 место.' : 'Без матча за 3 место.' }}</p>
      </article>
    </div>
  </section>

  <section class="admin-season-section admin-season-section-compact">
    <div class="admin-season-section-head admin-season-section-head-compact">
      <div class="admin-season-section-copy">
        <h4 class="admin-list-title">Судьи сезона</h4>
        <p class="muted-text">Привязанные судьи будут доступны при заполнении протоколов матчей.</p>
      </div>
    </div>
    <div class="actions-row admin-season-team-row">
      <select :value="refereeToAddId" @change="$emit('update:refereeToAddId', $event.target.value)">
        <option value="">— выберите судью —</option>
        <option v-for="referee in availableReferees" :key="referee.id" :value="String(referee.id)">{{ referee.fullName }}</option>
      </select>
      <button class="btn-ghost" type="button" @click="$emit('add-referee')">Добавить судью</button>
    </div>
    <p class="muted-text admin-season-selected-note">Привязано: {{ selectedReferees.length }}</p>
    <p v-if="!selectedReferees.length" class="muted-text admin-season-empty-state">Судьи к сезону пока не привязаны.</p>
    <div v-else class="admin-list-items admin-season-team-list">
      <article v-for="referee in selectedReferees" :key="referee.id" class="admin-list-item admin-referee-list-item admin-season-picked-item">
        <div class="admin-season-picked-copy">
          <strong>{{ referee.fullName }}</strong>
          <p class="muted-text">{{ referee.city || 'Город не указан' }}<span v-if="referee.birthDate"> · {{ formatDateOnly(referee.birthDate) }}</span></p>
        </div>
        <button class="btn-danger btn-sm" type="button" @click="$emit('remove-referee', referee.id)">Убрать</button>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  availableReferees: { type: Array, required: true },
  availableTeams: { type: Array, required: true },
  form: { type: Object, required: true },
  formatDateOnly: { type: Function, required: true },
  refereeToAddId: { type: String, default: '' },
  regularToursCount: { type: Number, required: true },
  selectedReferees: { type: Array, required: true },
  selectedTeams: { type: Array, required: true },
  teamToAddId: { type: String, default: '' },
})

defineEmits([
  'add-referee',
  'add-team',
  'remove-referee',
  'remove-team',
  'update:refereeToAddId',
  'update:teamToAddId',
])

const statusLabel = computed(() => {
  if (props.form.status === 'ACTIVE') return 'Активный'
  if (props.form.status === 'CLOSED') return 'Закрыт'
  return 'Черновик'
})

const transferWindowLabel = computed(() => {
  if (!props.form.transferWindowStartDate && !props.form.transferWindowEndDate) {
    return 'Без окна'
  }
  return `${props.form.transferWindowStartDate || '...'} - ${props.form.transferWindowEndDate || '...'}`
})
</script>
