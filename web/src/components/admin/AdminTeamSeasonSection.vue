<template>
  <section class="admin-list admin-team-management-card">
    <div class="toolbar admin-team-management-head">
      <div>
        <h4 class="admin-list-title">Заявка на сезон</h4>
        <p class="muted-text">Выбери сезон, затем массово добавь или сними игроков без длинного списка.</p>
      </div>
    </div>
    <div class="admin-team-management-toolbar admin-team-management-toolbar-spacer" aria-hidden="true"></div>

    <label class="admin-team-season-select-field">
      Выберите сезон
      <select :value="selectedSeasonId" @change="$emit('update:selectedSeasonId', $event.target.value); $emit('season-change')">
        <option value="">— выберите —</option>
        <option v-for="season in seasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option>
      </select>
    </label>

    <p v-if="!seasons.length" class="muted-text">Эта команда пока не включена ни в один активный сезон.</p>
    <p v-else-if="!selectedSeasonId" class="muted-text">Выберите сезон, чтобы управлять заявкой команды.</p>
    <p v-else-if="!players.length" class="muted-text">В составе команды пока нет игроков для управления заявкой сезона.</p>

    <div v-else class="admin-team-season-tools">
      <div class="admin-team-season-summary">
        <span class="admin-season-player-badge is-selected">В заявке: {{ selectedPlayersCount }}</span>
        <span class="admin-season-player-badge is-not-selected">Доступно: {{ availablePlayersCount }}</span>
        <span v-if="maxRosterSize" class="admin-season-player-badge">Лимит: {{ maxRosterSize }}</span>
        <span
          v-if="maxRosterSize"
          class="admin-season-player-badge"
          :class="remainingSlots > 0 ? 'is-not-selected' : 'is-selected'"
        >Осталось мест: {{ remainingSlots }}</span>
      </div>

      <div class="admin-team-season-action-block">
        <label class="admin-team-season-control">
          Добавить игроков в сезон
          <SearchableSelect
            :key="`team-season-add-${selectedSeasonId}-${addOptions.length}`"
            :model-value="addIds"
            :options="addOptions"
            multiple
            multiple-summary-text="Выбрано игроков"
            placeholder="Выберите игроков для заявки"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="Нет доступных игроков для добавления"
            :disabled="busy || !addOptions.length || atLimit"
            @update:model-value="$emit('update:addIds', $event)"
          />
        </label>
        <div class="admin-team-picker-side admin-team-picker-side-inline">
          <span class="admin-team-picker-count">Выбрано: {{ addIds.length }}</span>
          <button class="btn-primary btn-sm" type="button" :disabled="busy || !addIds.length || exceedsLimit" @click="$emit('add')">
            Добавить выбранных
          </button>
        </div>
        <p v-if="maxRosterSize && atLimit" class="muted-text">Лимит заявки уже достигнут. Сначала уберите кого-то из сезона.</p>
        <p v-else-if="exceedsLimit" class="error-text">Нельзя добавить {{ addIds.length }} игрок(ов): будет превышен лимит {{ maxRosterSize }}.</p>
      </div>

      <div v-if="removeOptions.length" class="admin-team-season-action-block">
        <label class="admin-team-season-control">
          Убрать игроков из сезона
          <SearchableSelect
            :key="`team-season-remove-${selectedSeasonId}-${removeOptions.length}`"
            :model-value="removeIds"
            :options="removeOptions"
            multiple
            multiple-summary-text="Выбрано игроков"
            placeholder="Выберите игроков для удаления"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="В заявке пока нет игроков"
            :disabled="busy"
            @update:model-value="$emit('update:removeIds', $event)"
          />
        </label>
        <div class="admin-team-picker-side admin-team-picker-side-inline">
          <span class="admin-team-picker-count">Выбрано: {{ removeIds.length }}</span>
          <button class="btn-danger btn-sm" type="button" :disabled="busy || !removeIds.length" @click="$emit('remove')">
            Убрать выбранных
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import SearchableSelect from '../SearchableSelect.vue'

defineProps({
  addIds: { type: Array, required: true },
  addOptions: { type: Array, required: true },
  atLimit: { type: Boolean, default: false },
  availablePlayersCount: { type: Number, required: true },
  busy: { type: Boolean, default: false },
  exceedsLimit: { type: Boolean, default: false },
  maxRosterSize: { type: Number, default: null },
  players: { type: Array, required: true },
  remainingSlots: { type: Number, default: null },
  removeIds: { type: Array, required: true },
  removeOptions: { type: Array, required: true },
  seasons: { type: Array, required: true },
  selectedPlayersCount: { type: Number, required: true },
  selectedSeasonId: { type: String, default: '' },
})

defineEmits([
  'add',
  'remove',
  'season-change',
  'update:addIds',
  'update:removeIds',
  'update:selectedSeasonId',
])
</script>
