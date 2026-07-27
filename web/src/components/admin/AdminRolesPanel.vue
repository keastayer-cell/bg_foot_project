<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Роли и доступ</h3>
      <p class="muted-text">Поиск пользователя, управление ролями и сброс временного пароля.</p>
    </div>

    <div class="admin-form admin-surface">
      <label>
        Поиск по email
        <input v-model.trim="search" type="text" placeholder="Начните вводить email..." />
      </label>
      <label>
        Выберите пользователя
        <select v-model="selectedEmail">
          <option value="">— выберите —</option>
          <option v-for="user in users" :key="user.email" :value="user.email">{{ user.email }}</option>
        </select>
      </label>
      <p v-if="!hasUsers" class="muted-text">Пользователи не найдены.</p>
      <div class="actions-row">
        <button class="btn-primary" type="button" @click="$emit('find')">Найти</button>
      </div>
    </div>

    <div v-if="foundUser" class="admin-found-user">
      <p class="admin-found-email">{{ foundUser.email }}</p>
      <p v-if="foundUser.name" class="muted-text">{{ foundUser.name }}</p>
      <p class="muted-text">
        Требуется смена пароля: {{ foundUser.mustChangePassword ? 'да' : 'нет' }}
      </p>
      <p v-if="!foundUser.roles.length" class="muted-text">Ролей нет.</p>

      <div class="actions-row">
        <button class="btn-danger btn-sm" type="button" @click="$emit('reset-password')">Сбросить пароль</button>
      </div>

      <article v-if="passwordResetResult" class="card admin-reset-password-card">
        <p><strong>Одноразовая ссылка:</strong></p>
        <p class="admin-reset-password-link">{{ passwordResetLink }}</p>
        <div class="actions-row">
          <button class="btn-ghost btn-sm" type="button" @click="$emit('copy-reset-link')">
            Скопировать ссылку
          </button>
        </div>
        <p class="muted-text">
          Ссылка действует до {{ formatDateTime(passwordResetResult.expiresAt) }}. Передайте ее пользователю
          по безопасному каналу. После установки нового пароля ссылка станет недействительной.
        </p>
      </article>

      <div v-for="role in foundUser.roles" :key="role" class="admin-role-manage-row">
        <span class="admin-role-badge">{{ role }}</span>
        <template v-if="replaceTarget === role">
          <select v-model="replaceCode" class="admin-role-select-inline">
            <option v-for="code in roleCodes" :key="code" :value="code">{{ code }}</option>
          </select>
          <button class="btn-primary btn-sm" type="button" @click="$emit('confirm-replace')">Подтвердить</button>
          <button class="btn-ghost btn-sm" type="button" @click="replaceTarget = ''">Отмена</button>
        </template>
        <template v-else>
          <button class="btn-ghost btn-sm" type="button" @click="$emit('start-replace', role)">Заменить</button>
          <button class="btn-danger btn-sm" type="button" @click="$emit('remove-role', role)">Снять</button>
        </template>
      </div>

      <div class="admin-add-role-row">
        <select v-model="assignCode" class="admin-role-select-inline">
          <option v-for="code in roleCodes" :key="code" :value="code">{{ code }}</option>
        </select>
        <button class="btn-ghost btn-sm" type="button" @click="$emit('assign-role')">+ Добавить роль</button>
      </div>
    </div>
  </article>
</template>

<script setup>
defineProps({
  users: {
    type: Array,
    required: true,
  },
  hasUsers: {
    type: Boolean,
    required: true,
  },
  foundUser: {
    type: Object,
    default: null,
  },
  passwordResetResult: {
    type: Object,
    default: null,
  },
  passwordResetLink: {
    type: String,
    default: '',
  },
  formatDateTime: {
    type: Function,
    required: true,
  },
})

defineEmits([
  'assign-role',
  'confirm-replace',
  'copy-reset-link',
  'find',
  'remove-role',
  'reset-password',
  'start-replace',
])

const roleCodes = ['USER', 'REFEREE', 'TEAM_REP', 'SUPER_ADMIN']
const search = defineModel('search', { type: String, required: true })
const selectedEmail = defineModel('selectedEmail', { type: String, required: true })
const replaceTarget = defineModel('replaceTarget', { type: String, required: true })
const replaceCode = defineModel('replaceCode', { type: String, required: true })
const assignCode = defineModel('assignCode', { type: String, required: true })
</script>
