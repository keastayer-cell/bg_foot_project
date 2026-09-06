<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Турнир</p>
      <h3 class="section-title">Сезоны</h3>
      <p class="muted-text">Сезон объединяет команды, чемпионат и Кубки. Регламенты настраиваются внутри соревнований.</p>
    </header>
    <div v-if="messageError || messageOk" class="admin-inline-message">
      <p v-if="messageError" class="error-text">{{ messageError }}</p>
      <p v-if="messageOk" class="success-text">{{ messageOk }}</p>
    </div>
    <AdminEntityModeSwitch v-model="seasonSubMode" :options="modeOptions" @update:model-value="changeMode" />

    <div class="admin-entity-flow">
      <section v-if="seasonSubMode === 'edit'" class="admin-step-section admin-season-edit-toolbar">
        <div class="admin-step-heading admin-field-wide">
          <span class="admin-step-number">1</span>
          <div><h4>Выберите сезон</h4><p>Откройте настройки существующего сезона или выгрузите его протоколы.</p></div>
          <span class="admin-step-count">{{ seasonsList.length }}</span>
        </div>
        <label class="admin-season-edit-picker">
          Сезон
          <select v-model="seasonEditSelectId" @change="onSeasonSelectChange">
            <option value="">— выберите сезон —</option>
            <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
        <div v-if="editingSeasonId" class="admin-season-export-wrap">
          <button class="btn-ghost" type="button" :disabled="downloadingSeasonProtocols" @click="toggleSeasonProtocolMenu">
            {{ downloadingSeasonProtocols ? seasonProtocolProgressText || 'Подготовка архива...' : 'Скачать протоколы' }}
          </button>
          <div v-if="seasonProtocolMenuOpen" class="admin-season-export-menu">
            <button
              class="btn-ghost admin-season-export-action"
              type="button"
              :disabled="downloadingSeasonProtocols"
              @click="downloadSeasonProtocolsArchive"
            >Скачать все подтвержденные (.zip)</button>
          </div>
        </div>
      </section>

      <form
        v-if="seasonSubMode === 'create' || editingSeasonId"
        class="admin-form admin-step-section admin-season-form"
        @submit.prevent="submitSeason"
      >
        <div class="admin-step-heading">
          <span class="admin-step-number">{{ seasonSubMode === 'create' ? '1' : '2' }}</span>
          <div><h4>{{ seasonSubMode === 'create' ? 'Новый сезон' : 'Настройки сезона' }}</h4><p>Основные параметры, участники и календарные ограничения.</p></div>
        </div>
        <AdminSeasonFields
          :form="seasonForm"
          :is-create="seasonSubMode === 'create'"
          :playoff-team-options="playoffTeamOptions"
        />
        <AdminSeasonParticipants
          v-model:team-to-add-id="seasonTeamToAddId"
          v-model:referee-to-add-id="seasonRefereeToAddId"
          :available-referees="seasonAvailableReferees"
          :available-teams="seasonAvailableTeams"
          :form="seasonForm"
          :format-date-only="formatDateOnly"
          :regular-tours-count="seasonRegularToursCount"
          :selected-referees="seasonSelectedReferees"
          :selected-teams="seasonSelectedTeams"
          @add-referee="addSeasonRefereeToForm"
          @add-team="addSeasonTeamToForm"
          @remove-referee="removeSeasonRefereeFromForm"
          @remove-team="removeSeasonTeamFromForm"
        />
        <div class="admin-primary-actions admin-season-actions">
          <button
            v-if="seasonSubMode === 'create'"
            class="btn-primary"
            type="submit"
            :disabled="isSeasonCreateDisabled"
          >Создать сезон</button>
          <template v-else>
            <button class="btn-ghost" type="button" @click="cancelSelection">Отмена</button>
            <button class="btn-primary" type="submit">Сохранить изменения</button>
          </template>
        </div>
        <div v-if="seasonSubMode === 'edit'" class="admin-danger-zone">
          <div><strong>Удаление сезона</strong><p>Сезон будет деактивирован вместе с доступом к его настройкам.</p></div>
          <button class="btn-danger btn-sm" type="button" @click="deactivateSeason(editingSeasonId)">Удалить сезон</button>
        </div>
      </form>
      <div v-else-if="seasonSubMode === 'edit'" class="admin-record-placeholder"><span>↳</span><span>Выберите сезон, чтобы открыть его настройки.</span></div>
    </div>
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
import AdminEntityModeSwitch from './AdminEntityModeSwitch.vue'
import AdminSeasonFields from './AdminSeasonFields.vue'
import AdminSeasonParticipants from './AdminSeasonParticipants.vue'

const props = defineProps({
  panel: { type: Object, required: true },
})

const {
  addSeasonRefereeToForm,
  addSeasonTeamToForm,
  cancelEditSeason,
  createSeason,
  deactivateSeason,
  downloadSeasonProtocolsArchive,
  downloadingSeasonProtocols,
  editingSeasonId,
  formatDateOnly,
  isSeasonCreateDisabled,
  messageError,
  messageOk,
  onSeasonSelectChange,
  playoffTeamOptions,
  removeSeasonRefereeFromForm,
  removeSeasonTeamFromForm,
  saveEditSeason,
  seasonAvailableReferees,
  seasonAvailableTeams,
  seasonEditSelectId,
  seasonForm,
  seasonProtocolMenuOpen,
  seasonProtocolProgressText,
  seasonRefereeToAddId,
  seasonRegularToursCount,
  seasonsList,
  seasonSelectedReferees,
  seasonSelectedTeams,
  seasonSubMode,
  seasonTeamToAddId,
  toggleSeasonProtocolMenu,
} = toRefs(props.panel)

const modeOptions = [
  { value: 'create', label: 'Создать', description: 'Новый турнирный сезон' },
  { value: 'edit', label: 'Редактировать', description: 'Настройки и участники' },
]

function showCreate() {
  seasonSubMode.value = 'create'
  cancelEditSeason.value()
  seasonEditSelectId.value = ''
}

function changeMode(mode) {
  if (mode === 'create') showCreate()
}

function cancelSelection() {
  cancelEditSeason.value()
  seasonEditSelectId.value = ''
}

function submitSeason() {
  return seasonSubMode.value === 'create'
    ? createSeason.value()
    : saveEditSeason.value()
}
</script>

<style scoped>
.admin-season-edit-toolbar {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) auto;
  align-items: end;
}

.admin-season-edit-toolbar .admin-step-heading {
  grid-column: 1 / -1;
  width: 100%;
}

@media (max-width: 720px) {
  .admin-season-edit-toolbar { grid-template-columns: 1fr; }
  .admin-season-export-wrap, .admin-season-export-wrap button { width: 100%; }
}
</style>
