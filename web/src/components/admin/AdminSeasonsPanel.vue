<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Сезоны</h3>
      <p class="muted-text">Сезон объединяет команды, чемпионат и Кубки. Регламенты настраиваются внутри соревнований.</p>
    </div>
    <div v-if="messageError || messageOk" class="admin-inline-message">
      <p v-if="messageError" class="error-text">{{ messageError }}</p>
      <p v-if="messageOk" class="success-text">{{ messageOk }}</p>
    </div>
    <div class="admin-subnav">
      <button
        class="btn-ghost admin-subnav-btn"
        :class="{ 'admin-subnav-active': seasonSubMode === 'create' }"
        type="button"
        @click="showCreate"
      >Создать сезон</button>
      <button
        class="btn-ghost admin-subnav-btn"
        :class="{ 'admin-subnav-active': seasonSubMode === 'edit' }"
        type="button"
        @click="seasonSubMode = 'edit'"
      >Редактировать</button>
    </div>

    <div class="admin-form admin-surface">
      <div v-if="seasonSubMode === 'edit'" class="admin-season-edit-toolbar">
        <label class="admin-season-edit-picker">
          Выберите сезон
          <select v-model="seasonEditSelectId" @change="onSeasonSelectChange">
            <option value="">— выберите —</option>
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
      </div>

      <form
        v-if="seasonSubMode === 'create' || editingSeasonId"
        class="admin-form admin-season-form"
        @submit.prevent="submitSeason"
      >
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
        <div class="actions-row admin-season-actions">
          <button
            v-if="seasonSubMode === 'create'"
            class="btn-primary"
            type="submit"
            :disabled="isSeasonCreateDisabled"
          >Создать сезон</button>
          <template v-else>
            <button class="btn-primary" type="submit">Сохранить изменения</button>
            <button class="btn-danger" type="button" @click="deactivateSeason(editingSeasonId)">Удалить сезон</button>
            <button class="btn-ghost" type="button" @click="cancelSelection">Отмена</button>
          </template>
        </div>
      </form>
    </div>
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
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

function showCreate() {
  seasonSubMode.value = 'create'
  cancelEditSeason.value()
  seasonEditSelectId.value = ''
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
