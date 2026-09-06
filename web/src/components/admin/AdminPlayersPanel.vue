<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Участники</p>
      <h3 class="section-title">Игроки</h3>
      <p class="muted-text">Создание и ведение единого реестра игроков.</p>
    </header>

    <AdminEntityModeSwitch v-model="subMode" :options="modeOptions" @update:model-value="changeMode" />

    <form v-if="subMode === 'create'" class="admin-step-section" @submit.prevent="$emit('create')">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Новый игрок</h4><p>Заполните обязательные данные и при необходимости добавьте фотографию.</p></div>
      </div>
      <PlayerFields :form="form" required-fields @photo-selected="$emit('photo-selected', $event)" />
      <div class="admin-primary-actions"><button class="btn-primary" type="submit">Создать игрока</button></div>
    </form>

    <div v-else class="admin-entity-flow">
      <section class="admin-step-section">
        <div class="admin-step-heading">
          <span class="admin-step-number">1</span>
          <div><h4>Выберите игрока</h4><p>Найдите карточку по ФИО в едином реестре.</p></div>
        </div>
        <SearchableSelect
          v-model="editSelectId"
          :options="editOptions"
          placeholder="— выберите игрока —"
          search-placeholder="Начните вводить ФИО игрока"
          empty-text="Игрок по такому ФИО не найден"
        />
      </section>

      <section v-if="editingId" class="admin-step-section">
        <div class="admin-step-heading">
          <span class="admin-step-number">2</span>
          <div><h4>Карточка игрока</h4><p>Изменения применятся после сохранения.</p></div>
        </div>
        <PlayerFields :form="form" @photo-selected="$emit('photo-selected', $event)" />
        <div class="admin-primary-actions">
          <button class="btn-ghost" type="button" @click="cancelEdit">Отмена</button>
          <button class="btn-primary" type="button" @click="$emit('save')">Сохранить изменения</button>
        </div>
        <div class="admin-danger-zone">
          <div><strong>Удаление игрока</strong><p>Карточка будет деактивирована и исчезнет из доступного реестра.</p></div>
          <button class="btn-danger btn-sm" type="button" @click="$emit('deactivate', editingId)">Удалить игрока</button>
        </div>
      </section>

      <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите игрока, чтобы открыть его карточку.</span></div>
    </div>
  </article>
</template>

<script setup>
import AdminEntityModeSwitch from './AdminEntityModeSwitch.vue'
import PlayerFields from './PlayerFields.vue'
import SearchableSelect from '../SearchableSelect.vue'

defineProps({
  form: { type: Object, required: true },
  editOptions: { type: Array, required: true },
  editingId: { type: [String, Number], default: null },
})

const emit = defineEmits(['cancel', 'create', 'deactivate', 'photo-selected', 'save'])
const subMode = defineModel('subMode', { type: String, required: true })
const editSelectId = defineModel('editSelectId', { type: String, required: true })
const modeOptions = [
  { value: 'create', label: 'Создать', description: 'Новая карточка игрока' },
  { value: 'edit', label: 'Редактировать', description: 'Работа с реестром' },
]

function cancelEdit() {
  editSelectId.value = ''
  emit('cancel')
}

function changeMode(mode) {
  if (mode === 'create') cancelEdit()
}
</script>
