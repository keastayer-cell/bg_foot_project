<template>
  <section class="admin-list admin-team-management-card">
    <div class="toolbar admin-team-management-head">
      <div>
        <h4 class="admin-list-title">Состав команды</h4>
        <p class="muted-text">Массовое добавление в команду и быстрый контроль текущего состава.</p>
      </div>
      <div class="actions-row admin-team-head-actions">
        <button class="btn-ghost btn-sm" type="button" @click="$emit('toggle-visibility')">
          {{ visible ? 'Скрыть состав' : 'Показать весь состав' }}
        </button>
        <button class="btn-ghost btn-sm" type="button" @click="$emit('refresh')">Обновить</button>
      </div>
    </div>

    <div class="actions-row admin-team-picker-row admin-team-management-toolbar">
      <SearchableSelect
        :key="`team-roster-${teamId}-${options.length}`"
        :model-value="selectedIds"
        :options="options"
        multiple
        multiple-summary-text="Выбрано игроков"
        placeholder="Выберите игроков в состав"
        search-placeholder="Начните вводить ФИО игрока"
        empty-text="Игрок по такому ФИО не найден"
        @update:model-value="$emit('update:selectedIds', $event)"
      />
      <div class="admin-team-picker-side">
        <span class="admin-team-picker-count">Выбрано: {{ selectedIds.length }}</span>
        <button class="btn-primary btn-sm" type="button" :disabled="busy || !selectedIds.length" @click="$emit('add')">
          Добавить выбранных
        </button>
      </div>
    </div>

    <p v-if="!visible" class="muted-text">Состав скрыт. Нажмите «Показать весь состав», если нужен полный список игроков.</p>
    <p v-else-if="!roster.length" class="muted-text">В текущем составе команды пока нет игроков.</p>
    <div v-else class="admin-list-items">
      <article v-for="player in roster" :key="player.id" class="admin-list-item admin-player-manage-item">
        <div class="admin-player-manage-copy">
          <strong>
            {{ player.fullName }}
            <span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span>
          </strong>
          <span class="muted-text">В команде с {{ formatDateOnly(player.inTeamSince) }}</span>
        </div>
        <button class="btn-danger btn-sm" type="button" :disabled="busy" @click="$emit('remove', player.id)">
          Удалить из состава
        </button>
      </article>
    </div>
  </section>
</template>

<script setup>
import SearchableSelect from '../SearchableSelect.vue'

defineProps({
  busy: { type: Boolean, default: false },
  formatDateOnly: { type: Function, required: true },
  options: { type: Array, required: true },
  roster: { type: Array, required: true },
  selectedIds: { type: Array, required: true },
  teamId: { type: [Number, String], required: true },
  visible: { type: Boolean, default: false },
})

defineEmits(['add', 'refresh', 'remove', 'toggle-visibility', 'update:selectedIds'])
</script>
