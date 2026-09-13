<template>
  <div class="competition-context-picker">
    <label>
      <span>Сезон</span>
      <select :value="seasonId" :disabled="disabled || !seasons.length" @change="$emit('update:seasonId', $event.target.value)">
        <option v-if="!seasons.length" value="">— сезоны не найдены —</option>
        <option v-for="season in seasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option>
      </select>
    </label>
    <label v-if="showCompetition">
      <span>Турнир</span>
      <select :value="competitionId" :disabled="disabled || !competitions.length" @change="$emit('update:competitionId', $event.target.value)">
        <option v-if="!competitions.length" value="">— турниры не найдены —</option>
        <option v-for="competition in competitions" :key="competition.id" :value="String(competition.id)">
          {{ competition.type === 'CUP' ? 'Кубок' : 'Чемпионат' }} · {{ competition.name }}
        </option>
      </select>
    </label>
  </div>
</template>

<script setup>
defineProps({
  seasons: { type: Array, default: () => [] },
  competitions: { type: Array, default: () => [] },
  seasonId: { type: String, default: '' },
  competitionId: { type: String, default: '' },
  showCompetition: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})
defineEmits(['update:seasonId', 'update:competitionId'])
</script>

<style scoped>
.competition-context-picker { display: grid; grid-template-columns: repeat(2,minmax(220px,1fr)); gap: 14px; width: 100%; }
.competition-context-picker label { display: grid; gap: 7px; min-width: 0; }
.competition-context-picker label > span { color: var(--brand); font-size: .62rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.competition-context-picker select { width: 100%; min-width: 0; }
@media(max-width:640px) { .competition-context-picker { grid-template-columns: 1fr; } }
</style>
