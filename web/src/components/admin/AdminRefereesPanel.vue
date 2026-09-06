<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Участники</p>
      <h3 class="section-title">Судьи</h3>
      <p class="muted-text">Создание и ведение реестра арбитров.</p>
    </header>

    <AdminEntityModeSwitch v-model="subMode" :options="modeOptions" @update:model-value="changeMode" />

    <form v-if="subMode === 'create'" class="admin-step-section" @submit.prevent="$emit('create')">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Новый судья</h4><p>Создайте карточку арбитра для назначения на матчи.</p></div>
      </div>
      <RefereeFields :form="form" required-name @photo-selected="$emit('photo-selected', $event)" />
      <div class="admin-primary-actions"><button class="btn-primary" type="submit">Создать судью</button></div>
    </form>

    <div v-else class="admin-entity-flow">
      <section class="admin-step-section">
        <div class="admin-step-heading">
          <span class="admin-step-number">1</span>
          <div><h4>Выберите судью</h4><p>Откройте существующую карточку из реестра.</p></div>
          <span class="admin-step-count">{{ referees.length }}</span>
        </div>
        <select v-model="editSelectId" @change="$emit('selection-change')">
          <option value="">— выберите судью —</option>
          <option v-for="referee in referees" :key="referee.id" :value="String(referee.id)">{{ referee.fullName }}</option>
        </select>
      </section>

      <section v-if="editingId" class="admin-step-section">
        <div class="admin-step-heading">
          <span class="admin-step-number">2</span>
          <div><h4>Карточка судьи</h4><p>Проверьте данные перед сохранением.</p></div>
        </div>
        <RefereeFields :form="form" @photo-selected="$emit('photo-selected', $event)" />
        <div class="admin-primary-actions">
          <button class="btn-ghost" type="button" @click="cancelEdit">Отмена</button>
          <button class="btn-primary" type="button" @click="$emit('save')">Сохранить изменения</button>
        </div>
        <div class="admin-danger-zone">
          <div><strong>Удаление судьи</strong><p>Арбитр будет деактивирован и станет недоступен для новых назначений.</p></div>
          <button class="btn-danger btn-sm" type="button" @click="$emit('deactivate', editingId)">Удалить судью</button>
        </div>
      </section>

      <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите судью, чтобы открыть его карточку.</span></div>
    </div>
  </article>
</template>

<script setup>
import AdminEntityModeSwitch from './AdminEntityModeSwitch.vue'
import RefereeFields from './RefereeFields.vue'

defineProps({
  form: { type: Object, required: true },
  referees: { type: Array, required: true },
  editingId: { type: [String, Number], default: null },
})

const emit = defineEmits(['cancel', 'create', 'deactivate', 'photo-selected', 'save', 'selection-change'])
const subMode = defineModel('subMode', { type: String, required: true })
const editSelectId = defineModel('editSelectId', { type: String, required: true })
const modeOptions = [
  { value: 'create', label: 'Создать', description: 'Новая карточка судьи' },
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
