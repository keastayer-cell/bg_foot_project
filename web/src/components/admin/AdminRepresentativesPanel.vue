<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Представители команд</h3>
      <p class="muted-text">Привязка пользователей к командам и управление доступом представителя.</p>
    </div>

    <div class="admin-form admin-surface">
      <label>
        Поиск по email
        <input v-model.trim="search" type="text" placeholder="Начните вводить email представителя..." />
      </label>
      <label>
        Выберите представителя
        <select v-model="selectedEmail">
          <option value="">— выберите —</option>
          <option v-for="user in users" :key="user.email" :value="String(user.email || '').toLowerCase()">
            {{ user.email }}
          </option>
        </select>
      </label>
      <p v-if="!hasUsers" class="muted-text">Представители команды не найдены.</p>
      <div class="actions-row">
        <button class="btn-primary" type="button" @click="$emit('find')">Найти</button>
      </div>
    </div>

    <div v-if="foundUser" class="admin-found-user">
      <p class="admin-found-email">{{ foundUser.email }}</p>
      <p v-if="foundUser.name" class="muted-text">{{ foundUser.name }}</p>
      <p class="muted-text">Текущая команда: {{ currentTeamScope?.teamName || 'не назначена' }}</p>
      <p v-if="hasMultipleTeamScopes" class="muted-text">
        Найдено несколько активных привязок. При сохранении старые привязки будут сняты.
      </p>

      <label>
        Назначить команду
        <select v-model="selectedTeamId">
          <option value="">— выберите —</option>
          <option v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
        </select>
      </label>

      <div class="actions-row">
        <button class="btn-primary" type="button" @click="$emit('save-team')">{{ primaryActionLabel }}</button>
        <button
          class="btn-danger"
          type="button"
          :disabled="!currentTeamScope"
          @click="$emit('unassign-team')"
        >
          Открепить команду
        </button>
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
  currentTeamScope: {
    type: Object,
    default: null,
  },
  hasMultipleTeamScopes: {
    type: Boolean,
    default: false,
  },
  teams: {
    type: Array,
    required: true,
  },
  primaryActionLabel: {
    type: String,
    required: true,
  },
})

defineEmits(['find', 'save-team', 'unassign-team'])

const search = defineModel('search', { type: String, required: true })
const selectedEmail = defineModel('selectedEmail', { type: String, required: true })
const selectedTeamId = defineModel('selectedTeamId', { type: String, required: true })
</script>
