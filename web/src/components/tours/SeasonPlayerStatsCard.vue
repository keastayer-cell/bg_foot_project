<template>
  <article class="card player-stats-card">
    <div class="section-head player-stats-head">
      <div>
        <h2 class="section-title">Статистика игроков сезона</h2>
        <p class="muted-text player-stats-subtitle">
          Учитываются только подтвержденные протоколы опубликованных туров.
        </p>
      </div>
      <span v-if="loading" class="muted-text">Загрузка...</span>
    </div>

    <div v-if="selectedSeason" class="player-stats-tabs">
      <button
        class="btn-ghost player-stats-tab"
        type="button"
        :class="{ 'is-active': mode === 'scorers' }"
        @click="$emit('update:mode', 'scorers')"
      >
        Бомбардиры
      </button>
      <button
        class="btn-ghost player-stats-tab"
        type="button"
        :class="{ 'is-active': mode === 'discipline' }"
        @click="$emit('update:mode', 'discipline')"
      >
        Дисциплина
      </button>
    </div>

    <div v-if="rows.length" class="player-stats-table-wrap">
      <table v-if="mode === 'scorers'" class="stats-table player-stats-table player-stats-table-scorers">
        <thead>
          <tr>
            <th>№</th>
            <th>Игрок</th>
            <th>Команда</th>
            <th>Голы</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="`scorers-${row.playerId}`">
            <td class="player-stats-rank">{{ index + 1 }}</td>
            <td><strong class="player-stats-name">{{ row.fullName }}</strong></td>
            <td><span class="player-stats-team">{{ row.teamName || '—' }}</span></td>
            <td><strong>{{ row.goals }}</strong></td>
          </tr>
        </tbody>
      </table>

      <table v-else class="stats-table player-stats-table player-stats-table-discipline">
        <thead>
          <tr>
            <th>№</th>
            <th>Игрок</th>
            <th>Команда</th>
            <th>ЖК</th>
            <th>КК</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="`discipline-${row.playerId}`">
            <td class="player-stats-rank">{{ index + 1 }}</td>
            <td><strong class="player-stats-name">{{ row.fullName }}</strong></td>
            <td><span class="player-stats-team">{{ row.teamName || '—' }}</span></td>
            <td class="player-stats-yellow">{{ row.yellowCards }}</td>
            <td class="player-stats-red">{{ row.redCards }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-else-if="selectedSeason && !loading" class="empty-text">{{ emptyText }}</p>
    <p v-else class="empty-text">Выберите сезон, чтобы посмотреть статистику игроков.</p>
  </article>
</template>

<script setup>
defineProps({
  selectedSeason: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  mode: {
    type: String,
    required: true,
  },
  rows: {
    type: Array,
    required: true,
  },
  emptyText: {
    type: String,
    required: true,
  },
})

defineEmits(['update:mode'])
</script>
