<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Участники</p>
      <h3 class="section-title">Команды и составы</h3>
      <p class="muted-text">Карточки команд, постоянные составы и заявки на сезон.</p>
    </header>
    <div v-if="messageError || messageOk" class="admin-inline-message">
      <p v-if="messageError" class="error-text">{{ messageError }}</p>
      <p v-if="messageOk" class="success-text">{{ messageOk }}</p>
    </div>
    <AdminEntityModeSwitch v-model="subMode" :options="modeOptions" @update:model-value="changeMode" />

    <form v-if="subMode === 'create'" class="admin-step-section admin-form" @submit.prevent="create">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Новая команда</h4><p>Заполните основные данные команды и добавьте логотип.</p></div>
      </div>
      <AdminTeamFields compact :form="form" @logo-selected="onLogoSelected" />
      <div class="admin-primary-actions">
        <button class="btn-primary" type="submit">Создать команду</button>
      </div>
    </form>

    <div v-else class="admin-entity-flow">
      <section class="admin-step-section">
        <div class="admin-step-heading">
          <span class="admin-step-number">1</span>
          <div><h4>Выберите команду</h4><p>После выбора откроются данные, состав и заявки команды.</p></div>
          <span class="admin-step-count">{{ teamsList.length }}</span>
        </div>
        <select v-model="editSelectId" @change="onSelectChange">
          <option value="">— выберите команду —</option>
          <option v-for="team in teamsList" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
        </select>
      </section>
      <template v-if="editingId">
        <div class="admin-sticky-actions-spacer"></div>
        <div class="admin-team-editor-shell">
          <section class="admin-step-section admin-team-identity-card">
            <div class="admin-team-identity-head">
              <div class="admin-step-heading admin-team-card-heading">
                <span class="admin-step-number">2</span>
                <div><h4>Карточка команды</h4><p>Основные данные и показатели выбранной команды.</p></div>
              </div>
              <div class="admin-team-summary-grid">
                <article class="admin-team-summary-card">
                  <span class="admin-team-summary-label">В составе</span>
                  <strong>{{ roster.length }}</strong>
                </article>
                <article class="admin-team-summary-card">
                  <span class="admin-team-summary-label">Сезонов</span>
                  <strong>{{ seasonOptions.length }}</strong>
                </article>
                <article class="admin-team-summary-card" :class="{ 'is-accent': selectedSeasonId }">
                  <span class="admin-team-summary-label">В заявке</span>
                  <strong>{{ selectedSeasonId ? seasonSelectedPlayers.length : '—' }}</strong>
                </article>
              </div>
            </div>
            <AdminTeamFields :form="form" @logo-selected="onLogoSelected" />
          </section>

          <div class="admin-team-management-grid">
            <AdminTeamRosterSection
              v-model:selected-ids="rosterToAddIds"
              :busy="rosterBusy"
              :format-date-only="formatDateOnly"
              :options="rosterAddOptions"
              :roster="roster"
              :team-id="editingId"
              :visible="isRosterVisible"
              @add="addPlayersToRoster"
              @refresh="refreshContext"
              @remove="removePlayerFromRoster"
              @toggle-visibility="toggleRosterVisibility"
            />
            <AdminTeamSeasonSection
              v-model:add-ids="seasonToAddIds"
              v-model:remove-ids="seasonToRemoveIds"
              v-model:selected-season-id="selectedSeasonId"
              :add-options="seasonAddOptions"
              :at-limit="isSeasonAtLimit"
              :available-players-count="seasonAvailablePlayers.length"
              :busy="seasonBusy"
              :exceeds-limit="willSelectedPlayersExceedSeasonLimit"
              :max-roster-size="seasonMaxRosterSize"
              :players="seasonPlayers"
              :remaining-slots="seasonRemainingSlots"
              :remove-options="seasonRemoveOptions"
              :seasons="seasonOptions"
              :selected-players-count="seasonSelectedPlayers.length"
              @add="addPlayersToSeason"
              @remove="removePlayersFromSeason"
              @season-change="onSeasonChange"
            />
          </div>
        </div>

        <div class="admin-primary-actions admin-sticky-actions">
          <button class="btn-primary" type="button" :disabled="saving" @click="saveEdit">
            {{ saving ? 'Сохраняем...' : 'Сохранить изменения' }}
          </button>
          <button class="btn-ghost" type="button" @click="cancelSelection">Отмена</button>
        </div>
        <div class="admin-danger-zone">
          <div><strong>Удаление команды</strong><p>Команда будет деактивирована и станет недоступна для новых соревнований.</p></div>
          <button class="btn-danger btn-sm" type="button" @click="deactivate(editingId)">Удалить команду</button>
        </div>
      </template>
      <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите команду, чтобы открыть управление составом и заявками.</span></div>
    </div>
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
import AdminEntityModeSwitch from './AdminEntityModeSwitch.vue'
import AdminTeamFields from './AdminTeamFields.vue'
import AdminTeamRosterSection from './AdminTeamRosterSection.vue'
import AdminTeamSeasonSection from './AdminTeamSeasonSection.vue'

const props = defineProps({
  panel: { type: Object, required: true },
})

const {
  addPlayersToRoster,
  addPlayersToSeason,
  cancelEdit,
  create,
  deactivate,
  editingId,
  editSelectId,
  form,
  formatDateOnly,
  isRosterVisible,
  isSeasonAtLimit,
  messageError,
  messageOk,
  onLogoSelected,
  onSeasonChange,
  onSelectChange,
  refreshContext,
  removePlayerFromRoster,
  removePlayersFromSeason,
  roster,
  rosterAddOptions,
  rosterBusy,
  rosterToAddIds,
  saveEdit,
  saving,
  seasonAddOptions,
  seasonAvailablePlayers,
  seasonBusy,
  seasonMaxRosterSize,
  seasonOptions,
  seasonPlayers,
  seasonRemainingSlots,
  seasonRemoveOptions,
  seasonSelectedPlayers,
  seasonToAddIds,
  seasonToRemoveIds,
  selectedSeasonId,
  subMode,
  teamsList,
  toggleRosterVisibility,
  willSelectedPlayersExceedSeasonLimit,
} = toRefs(props.panel)

const modeOptions = [
  { value: 'create', label: 'Создать', description: 'Новая карточка команды' },
  { value: 'edit', label: 'Редактировать', description: 'Составы и заявки' },
]

function showCreate() {
  subMode.value = 'create'
  cancelEdit.value()
  editSelectId.value = ''
}

function changeMode(mode) {
  if (mode === 'create') showCreate()
}

function cancelSelection() {
  cancelEdit.value()
  editSelectId.value = ''
}
</script>
