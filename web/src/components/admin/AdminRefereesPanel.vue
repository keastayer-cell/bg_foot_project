<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Судьи</h3>
      <p class="muted-text">Реестр арбитров с быстрым созданием и редактированием карточек.</p>
    </div>
    <div class="admin-subnav">
      <button
        class="btn-ghost admin-subnav-btn"
        :class="{ 'admin-subnav-active': subMode === 'create' }"
        type="button"
        @click="openCreateMode"
      >
        Создать судью
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
          Город
          <input v-model.trim="form.city" type="text" placeholder="Например: Богородск" />
        </label>
        <label>
          Дата рождения
          <input v-model="form.birthDate" type="date" class="admin-temporal-input" />
        </label>
        <label>
          Фото судьи
          <input type="file" accept="image/*" @change="$emit('photo-selected', $event)" />
        </label>
        <img
          v-if="form.photoDataUrl"
          :src="form.photoDataUrl"
          alt="Превью фото судьи"
          class="team-rep-player-photo-preview"
        />
        <div class="actions-row">
          <button class="btn-primary" type="submit">Создать судью</button>
        </div>
      </form>

      <div v-else class="admin-form admin-surface">
        <label>
          Выберите судью
          <select v-model="editSelectId" @change="$emit('selection-change')">
            <option value="">— выберите —</option>
            <option v-for="referee in referees" :key="referee.id" :value="String(referee.id)">
              {{ referee.fullName }}
            </option>
          </select>
        </label>
        <template v-if="editingId">
          <label>
            ФИО
            <input v-model.trim="form.fullName" type="text" />
          </label>
          <label>
            Город
            <input v-model.trim="form.city" type="text" placeholder="Например: Богородск" />
          </label>
          <label>
            Дата рождения
            <input v-model="form.birthDate" type="date" class="admin-temporal-input" />
          </label>
          <label>
            Фото судьи
            <input type="file" accept="image/*" @change="$emit('photo-selected', $event)" />
          </label>
          <img
            v-if="form.photoDataUrl"
            :src="form.photoDataUrl"
            alt="Превью фото судьи"
            class="team-rep-player-photo-preview"
          />
          <div class="actions-row">
            <button class="btn-primary" type="button" @click="$emit('save')">Сохранить изменения</button>
            <button class="btn-danger" type="button" @click="$emit('deactivate', editingId)">Удалить судью</button>
            <button class="btn-ghost" type="button" @click="cancelEdit">Отмена</button>
          </div>
        </template>
      </div>
    </div>
  </article>
</template>

<script setup>
defineProps({
  form: {
    type: Object,
    required: true,
  },
  referees: {
    type: Array,
    required: true,
  },
  editingId: {
    type: [String, Number],
    default: null,
  },
})

const emit = defineEmits([
  'cancel',
  'create',
  'deactivate',
  'photo-selected',
  'save',
  'selection-change',
])
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
