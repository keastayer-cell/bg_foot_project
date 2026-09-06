<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Доступ</p>
      <h3 class="section-title">Представители команд</h3>
      <p class="muted-text">Назначение ответственного пользователя на конкретную команду.</p>
    </header>

    <form class="admin-step-section" @submit.prevent="$emit('find')">
      <div class="admin-step-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Найдите представителя</h4><p>В списке доступны пользователи с ролью TEAM_REP.</p></div>
      </div>
      <div class="representative-search-grid">
        <label>
          Email
          <input v-model.trim="search" type="search" inputmode="email" autocomplete="off" placeholder="representative@example.com" />
        </label>
        <label>
          Результаты · {{ users.length }}
          <select v-model="selectedEmail">
            <option value="">— выберите представителя —</option>
            <option v-for="user in users" :key="user.email" :value="String(user.email || '').toLowerCase()">
              {{ user.email }}{{ user.name ? ` — ${user.name}` : '' }}
            </option>
          </select>
        </label>
        <button class="btn-primary representative-open-button" type="submit">Открыть</button>
      </div>
      <p v-if="!hasUsers" class="admin-record-placeholder">Представители по вашему запросу не найдены.</p>
    </form>

    <section v-if="foundUser" class="representative-card">
      <header class="representative-user-header">
        <span class="representative-avatar">{{ userInitials }}</span>
        <div><strong>{{ foundUser.name || 'Имя не указано' }}</strong><small>{{ foundUser.email }}</small></div>
        <span class="representative-role">TEAM_REP</span>
      </header>

      <div class="representative-layout">
        <section class="representative-section">
          <div class="admin-step-heading">
            <span class="admin-step-number">2</span>
            <div><h4>Текущее назначение</h4><p>Команда, которой представитель может управлять сейчас.</p></div>
          </div>
          <div class="representative-current-team" :class="{ empty: !currentTeamScope }">
            <span>Команда</span>
            <strong>{{ currentTeamScope?.teamName || 'Не назначена' }}</strong>
          </div>
          <p v-if="hasMultipleTeamScopes" class="representative-warning">
            Обнаружено несколько активных привязок. Новое сохранение оставит только выбранную команду.
          </p>
        </section>

        <section class="representative-section representative-assignment">
          <div class="admin-step-heading">
            <span class="admin-step-number">3</span>
            <div><h4>{{ currentTeamScope ? 'Изменить команду' : 'Назначить команду' }}</h4><p>Выберите одну команду и подтвердите назначение.</p></div>
          </div>
          <label>
            Новая команда
            <select v-model="selectedTeamId">
              <option value="">— выберите команду —</option>
              <option v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <button class="btn-primary" type="button" :disabled="!selectedTeamId" @click="$emit('save-team')">{{ primaryActionLabel }}</button>
        </section>
      </div>

      <div v-if="currentTeamScope" class="admin-danger-zone representative-danger">
        <div><strong>Снять назначение</strong><p>Представитель потеряет доступ к управлению текущей командой.</p></div>
        <button class="btn-danger btn-sm" type="button" @click="$emit('unassign-team')">Открепить команду</button>
      </div>
    </section>

    <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите представителя, чтобы увидеть его текущее назначение.</span></div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  users: { type: Array, required: true },
  hasUsers: { type: Boolean, required: true },
  foundUser: { type: Object, default: null },
  currentTeamScope: { type: Object, default: null },
  hasMultipleTeamScopes: { type: Boolean, default: false },
  teams: { type: Array, required: true },
  primaryActionLabel: { type: String, required: true },
})

defineEmits(['find', 'save-team', 'unassign-team'])
const search = defineModel('search', { type: String, required: true })
const selectedEmail = defineModel('selectedEmail', { type: String, required: true })
const selectedTeamId = defineModel('selectedTeamId', { type: String, required: true })
const userInitials = computed(() => {
  const source = String(props.foundUser?.name || props.foundUser?.email || '?').trim()
  return source.split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '?'
})
</script>

<style scoped>
.representative-search-grid { display: grid; grid-template-columns: minmax(240px, .8fr) minmax(320px, 1.2fr) auto; align-items: end; gap: 12px; }
.representative-search-grid label, .representative-assignment label { display: grid; gap: 7px; }
.representative-open-button { min-height: 46px; min-width: 104px; }
.representative-card { overflow: hidden; border: 1px solid rgba(124, 163, 255, .18); border-radius: 14px; background: rgba(10, 16, 37, .62); }
.representative-user-header { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 18px 20px; border-bottom: 1px solid rgba(124, 163, 255, .16); background: rgba(24, 36, 73, .42); }
.representative-avatar { display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid rgba(124, 163, 255, .35); border-radius: 10px; background: rgba(124, 163, 255, .12); font-weight: 800; }
.representative-user-header strong, .representative-user-header small { display: block; }
.representative-user-header small { margin-top: 3px; color: var(--muted); }
.representative-role { padding: 4px 8px; border: 1px solid rgba(97, 232, 162, .3); border-radius: 7px; color: var(--brand); font-size: .72rem; font-weight: 800; }
.representative-layout { display: grid; grid-template-columns: 1fr 1fr; }
.representative-section { display: grid; align-content: start; gap: 16px; min-width: 0; padding: 20px; }
.representative-assignment { border-left: 1px solid rgba(124, 163, 255, .16); background: rgba(8, 13, 31, .42); }
.representative-current-team { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px; border: 1px solid rgba(97, 232, 162, .2); border-radius: 10px; background: rgba(97, 232, 162, .05); }
.representative-current-team span { color: var(--muted); font-size: .8rem; }
.representative-current-team.empty { border-style: dashed; border-color: rgba(124, 163, 255, .2); background: transparent; }
.representative-warning { margin: 0; padding: 11px; border-radius: 8px; background: rgba(255, 190, 92, .08); color: #ffd28a; font-size: .78rem; }
.representative-danger { margin: 0 20px 20px; }
@media (max-width: 900px) {
  .representative-search-grid, .representative-layout { grid-template-columns: 1fr; }
  .representative-assignment { border-top: 1px solid rgba(124, 163, 255, .16); border-left: 0; }
}
@media (max-width: 600px) {
  .representative-user-header { grid-template-columns: 1fr; }
  .representative-avatar { display: none; }
  .representative-role { justify-self: start; }
}
</style>
