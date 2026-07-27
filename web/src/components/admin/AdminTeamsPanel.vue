<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Команды и составы</h3>
      <p class="muted-text">Карточка команды, текущий состав и сезонная заявка игроков.</p>
    </div>
    <div v-if="messageError || messageOk" class="admin-inline-message">
      <p v-if="messageError" class="error-text">{{ messageError }}</p>
      <p v-if="messageOk" class="success-text">{{ messageOk }}</p>
    </div>
    <div class="admin-subnav">
      <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': subMode === 'create' }" type="button" @click="showCreate">
        Создать команду
      </button>
      <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': subMode === 'edit' }" type="button" @click="subMode = 'edit'">
        Редактировать
      </button>
    </div>

    <form v-if="subMode === 'create'" class="admin-form admin-surface" @submit.prevent="create">
      <AdminTeamFields compact :form="form" @logo-selected="onLogoSelected" />
      <div class="actions-row">
        <button class="btn-primary" type="submit">Создать команду</button>
      </div>
    </form>

    <div v-else class="admin-form admin-surface">
      <label>
        Выберите команду
        <select v-model="editSelectId" @change="onSelectChange">
          <option value="">— выберите —</option>
          <option v-for="team in teamsList" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
        </select>
      </label>
      <template v-if="editingId">
        <div class="admin-sticky-actions-spacer"></div>
        <div class="admin-team-editor-shell">
          <section class="admin-surface admin-team-identity-card">
            <div class="admin-team-identity-head">
              <div>
                <h4 class="admin-list-title">Карточка команды</h4>
                <p class="muted-text">Базовые поля и быстрые показатели в одном компактном блоке.</p>
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

        <div class="actions-row admin-sticky-actions">
          <button class="btn-primary" type="button" :disabled="saving" @click="saveEdit">
            {{ saving ? 'Сохраняем...' : 'Сохранить изменения' }}
          </button>
          <button class="btn-danger" type="button" @click="deactivate(editingId)">Удалить команду</button>
          <button class="btn-ghost" type="button" @click="cancelSelection">Отмена</button>
        </div>
      </template>
    </div>
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
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

function showCreate() {
  subMode.value = 'create'
  cancelEdit.value()
  editSelectId.value = ''
}

function cancelSelection() {
  cancelEdit.value()
  editSelectId.value = ''
}
</script>
