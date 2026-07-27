<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Игроки</h3>
      <p class="muted-text">Единый реестр игроков с быстрым созданием и редактированием карточек.</p>
    </div>
    <div class="admin-subnav">
      <button
        class="btn-ghost admin-subnav-btn"
        :class="{ 'admin-subnav-active': subMode === 'create' }"
        type="button"
        @click="openCreateMode"
      >
        Создать игрока
      </button>
      <button
        class="btn-ghost admin-subnav-btn"
        :class="{ 'admin-subnav-active': subMode === 'edit' }"
        type="button"
        @click="subMode = 'edit'"
      >
        Редактировать
      </button>
    </div>

    <div class="admin-grid">
      <form v-if="subMode === 'create'" class="admin-form admin-surface" @submit.prevent="$emit('create')">
        <label>
          ФИО
          <input v-model.trim="form.fullName" type="text" required />
        </label>
        <label>
          Дата рождения
          <input v-model="form.birthDate" type="date" class="admin-temporal-input" required />
        </label>
        <label>
          Прописка
          <input v-model.trim="form.residence" type="text" placeholder="Город/деревня" required />
        </label>
        <label class="admin-checkbox-row">
          <input v-model="form.isGoalkeeper" type="checkbox" />
          <span>Вратарь</span>
        </label>
        <label>
          Фото игрока
          <input type="file" accept="image/*" @change="$emit('photo-selected', $event)" />
        </label>
        <img
          v-if="form.photoDataUrl"
          :src="form.photoDataUrl"
          alt="Превью фото игрока"
          class="team-rep-player-photo-preview"
        />
        <div class="actions-row">
          <button class="btn-primary" type="submit">Создать игрока</button>
        </div>
      </form>

      <div v-else class="admin-form admin-surface">
        <label>
          Выберите игрока
          <SearchableSelect
            v-model="editSelectId"
            :options="editOptions"
            placeholder="— выберите —"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="Игрок по такому ФИО не найден"
          />
        </label>
        <template v-if="editingId">
          <label>
            ФИО
            <input v-model.trim="form.fullName" type="text" />
          </label>
          <label>
            Дата рождения
            <input v-model="form.birthDate" type="date" class="admin-temporal-input" />
          </label>
          <label>
            Прописка
            <input v-model.trim="form.residence" type="text" placeholder="Город/деревня" />
          </label>
          <label class="admin-checkbox-row">
            <input v-model="form.isGoalkeeper" type="checkbox" />
            <span>Вратарь</span>
          </label>
          <label>
            Фото игрока
            <input type="file" accept="image/*" @change="$emit('photo-selected', $event)" />
          </label>
          <img
            v-if="form.photoDataUrl"
            :src="form.photoDataUrl"
            alt="Превью фото игрока"
            class="team-rep-player-photo-preview"
          />
          <div class="actions-row">
            <button class="btn-primary" type="button" @click="$emit('save')">Сохранить изменения</button>
            <button class="btn-danger" type="button" @click="$emit('deactivate', editingId)">Удалить игрока</button>
            <button class="btn-ghost" type="button" @click="cancelEdit">Отмена</button>
          </div>
        </template>
      </div>
    </div>
  </article>
</template>

<script setup>
import SearchableSelect from '../SearchableSelect.vue'

defineProps({
  form: {
    type: Object,
    required: true,
  },
  editOptions: {
    type: Array,
    required: true,
  },
  editingId: {
    type: [String, Number],
    default: null,
  },
})

const emit = defineEmits(['cancel', 'create', 'deactivate', 'photo-selected', 'save'])
const subMode = defineModel('subMode', { type: String, required: true })
const editSelectId = defineModel('editSelectId', { type: String, required: true })

function cancelEdit() {
  editSelectId.value = ''
  emit('cancel')
}

function openCreateMode() {
  subMode.value = 'create'
  cancelEdit()
}
</script>
