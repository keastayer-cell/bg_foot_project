<template>
  <form class="admin-form admin-surface" @submit.prevent="$emit('create')">
    <h4 class="admin-list-title">Добавить матч в тур {{ tour.name }}</h4>
    <label>
      Команда 1
      <select v-model="form.homeTeamId">
        <option value="">— выберите —</option>
        <option v-for="team in teams" :key="`home-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
      </select>
    </label>
    <label>
      Команда 2
      <select v-model="form.awayTeamId">
        <option value="">— выберите —</option>
        <option v-for="team in awayTeams" :key="`away-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
      </select>
    </label>
    <label>
      Время матча
      <input v-model="form.kickoffAt" type="datetime-local" class="admin-temporal-input admin-temporal-input-wide" step="60" />
    </label>
    <p v-if="limitMessage" class="error-text">{{ limitMessage }}</p>
    <div class="actions-row">
      <button class="btn-primary" type="submit" :disabled="Boolean(limitMessage)">Добавить матч</button>
    </div>
    <p class="muted-text">Публично на сайт попадут только опубликованные туры.</p>
  </form>
</template>

<script setup>
defineProps({
  awayTeams: { type: Array, required: true },
  form: { type: Object, required: true },
  limitMessage: { type: String, default: '' },
  teams: { type: Array, required: true },
  tour: { type: Object, required: true },
})

defineEmits(['create'])
</script>
