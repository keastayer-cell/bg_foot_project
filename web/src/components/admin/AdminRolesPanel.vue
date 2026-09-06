<template>
  <article class="card admin-panel access-panel">
    <header class="admin-panel-head access-panel-heading">
      <div>
        <p class="admin-panel-kicker">Доступ</p>
        <h3 class="section-title">Роли и доступ</h3>
        <p class="muted-text">Найдите пользователя, назначьте права или восстановите доступ к аккаунту.</p>
      </div>
    </header>

    <form class="access-search" @submit.prevent="$emit('find')">
      <div class="access-section-heading">
        <span class="access-step">1</span>
        <div><h4>Найдите пользователя</h4><p>Поиск выполняется по адресу электронной почты.</p></div>
      </div>
      <div class="access-search-grid">
        <label class="access-field">
          <span>Email</span>
          <input v-model.trim="search" type="search" inputmode="email" autocomplete="off" placeholder="user@example.com" />
        </label>
        <label class="access-field access-results-field">
          <span>Результаты <small>{{ users.length }}</small></span>
          <select v-model="selectedEmail">
            <option value="">— выберите пользователя —</option>
            <option v-for="user in users" :key="user.email" :value="user.email">
              {{ user.email }}{{ user.name ? ` — ${user.name}` : '' }}
            </option>
          </select>
        </label>
        <button class="btn-primary access-search-button" type="submit">Открыть</button>
      </div>
      <p v-if="!hasUsers" class="access-search-empty">По вашему запросу пользователи не найдены.</p>
    </form>

    <section v-if="foundUser" class="access-user-card">
      <header class="access-user-header">
        <span class="access-user-avatar" aria-hidden="true">{{ userInitials }}</span>
        <div class="access-user-identity">
          <p class="access-user-name">{{ foundUser.name || 'Имя не указано' }}</p>
          <p class="access-user-email">{{ foundUser.email }}</p>
        </div>
        <span class="access-status" :class="{ warning: foundUser.mustChangePassword }">
          {{ foundUser.mustChangePassword ? 'Требуется смена пароля' : 'Доступ активен' }}
        </span>
      </header>

      <div class="access-user-layout">
        <section class="access-section access-roles-section">
          <div class="access-section-heading access-section-heading-lined">
            <span class="access-step">2</span>
            <div><h4>Роли пользователя</h4><p>Роли определяют доступные пользователю разделы и действия.</p></div>
            <span class="access-section-count">{{ foundUser.roles.length }}</span>
          </div>

          <p v-if="!foundUser.roles.length" class="access-empty-state">У пользователя пока нет назначенных ролей.</p>
          <div v-else class="access-role-list">
            <div v-for="role in foundUser.roles" :key="role" class="access-role-row">
              <div class="access-role-info">
                <span class="admin-role-badge">{{ role }}</span>
                <span><strong>{{ roleInfo(role).label }}</strong><small>{{ roleInfo(role).description }}</small></span>
              </div>

              <div v-if="replaceTarget === role" class="access-role-edit">
                <label class="access-field access-field-compact">
                  <span>Новая роль</span>
                  <select v-model="replaceCode" class="admin-role-select-inline">
                    <option
                      v-for="item in rolesCatalog"
                      :key="item.code"
                      :value="item.code"
                      :disabled="item.code === role || foundUser.roles.includes(item.code)"
                    >{{ item.label }} · {{ item.code }}</option>
                  </select>
                </label>
                <button class="btn-primary btn-sm" type="button" :disabled="!canConfirmReplace(role)" @click="$emit('confirm-replace')">Сохранить</button>
                <button class="btn-ghost btn-sm" type="button" @click="replaceTarget = ''">Отмена</button>
              </div>
              <div v-else class="access-role-actions">
                <button class="btn-ghost btn-sm" type="button" @click="$emit('start-replace', role)">Изменить</button>
                <button class="access-text-danger" type="button" @click="$emit('remove-role', role)">Снять роль</button>
              </div>
            </div>
          </div>

          <div class="access-add-role">
            <div><strong>Назначить роль</strong><p>Пользователь сразу получит соответствующие права.</p></div>
            <select v-model="assignCode" class="admin-role-select-inline">
              <option
                v-for="item in rolesCatalog"
                :key="item.code"
                :value="item.code"
                :disabled="foundUser.roles.includes(item.code)"
              >{{ item.label }} · {{ item.code }}</option>
            </select>
            <button class="btn-primary btn-sm" type="button" :disabled="!canAssignRole" @click="$emit('assign-role')">Добавить</button>
          </div>
        </section>

        <aside class="access-section access-security-section">
          <div class="access-section-heading access-section-heading-lined">
            <span class="access-step">3</span>
            <div><h4>Безопасность</h4><p>Восстановление доступа к аккаунту.</p></div>
          </div>
          <div class="access-security-status"><span>Пароль</span><strong>{{ foundUser.mustChangePassword ? 'Ожидает смены' : 'Установлен' }}</strong></div>
          <p class="access-security-note">Сброс завершит действие текущего пароля и создаст одноразовую ссылку для установки нового.</p>
          <button class="btn-danger access-reset-button" type="button" @click="$emit('reset-password')">Сбросить пароль</button>

          <article v-if="passwordResetResult" class="access-reset-result">
            <strong>Одноразовая ссылка создана</strong>
            <p class="admin-reset-password-link">{{ passwordResetLink }}</p>
            <button class="btn-ghost btn-sm" type="button" @click="$emit('copy-reset-link')">Скопировать ссылку</button>
            <small>Действует до {{ formatDateTime(passwordResetResult.expiresAt) }}. Передайте ссылку пользователю по безопасному каналу.</small>
          </article>
        </aside>
      </div>
    </section>

    <section v-else class="access-placeholder">
      <span aria-hidden="true">↳</span>
      <div><strong>Пользователь не выбран</strong><p>После выбора здесь появятся его роли и настройки безопасности.</p></div>
    </section>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  users: { type: Array, required: true },
  hasUsers: { type: Boolean, required: true },
  foundUser: { type: Object, default: null },
  passwordResetResult: { type: Object, default: null },
  passwordResetLink: { type: String, default: '' },
  formatDateTime: { type: Function, required: true },
})

defineEmits(['assign-role', 'confirm-replace', 'copy-reset-link', 'find', 'remove-role', 'reset-password', 'start-replace'])

const rolesCatalog = [
  { code: 'USER', label: 'Пользователь', description: 'Базовый доступ к общим разделам системы.' },
  { code: 'REFEREE', label: 'Судья', description: 'Работа с назначенными матчами и протоколами.' },
  { code: 'TEAM_REP', label: 'Представитель команды', description: 'Управление заявкой и составом своей команды.' },
  { code: 'SUPER_ADMIN', label: 'Администратор', description: 'Полный доступ к настройкам и данным системы.' },
]

const search = defineModel('search', { type: String, required: true })
const selectedEmail = defineModel('selectedEmail', { type: String, required: true })
const replaceTarget = defineModel('replaceTarget', { type: String, required: true })
const replaceCode = defineModel('replaceCode', { type: String, required: true })
const assignCode = defineModel('assignCode', { type: String, required: true })

const userInitials = computed(() => {
  const source = String(props.foundUser?.name || props.foundUser?.email || '?').trim()
  return source.split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '?'
})
const canAssignRole = computed(() => Boolean(assignCode.value && props.foundUser && !props.foundUser.roles.includes(assignCode.value)))

function roleInfo(code) {
  return rolesCatalog.find((item) => item.code === code) || { label: code, description: 'Дополнительная системная роль.' }
}

function canConfirmReplace(currentRole) {
  return Boolean(replaceCode.value && replaceCode.value !== currentRole && !props.foundUser?.roles.includes(replaceCode.value))
}
</script>

<style scoped>
.access-panel { gap: 20px; }
.access-panel-heading { padding-bottom: 4px; }
.access-search, .access-user-card, .access-placeholder { border: 1px solid rgba(124, 163, 255, .18); border-radius: 14px; background: rgba(10, 16, 37, .62); }
.access-search { display: grid; gap: 16px; padding: 18px; }
.access-section-heading { display: flex; align-items: flex-start; gap: 10px; }
.access-section-heading h4, .access-section-heading p, .access-add-role p, .access-placeholder p { margin: 0; }
.access-section-heading h4 { font-size: .95rem; }
.access-section-heading p, .access-add-role p, .access-placeholder p { margin-top: 3px; color: var(--muted); font-size: .8rem; }
.access-step { display: inline-grid; flex: 0 0 26px; height: 26px; place-items: center; border: 1px solid rgba(97, 232, 162, .35); border-radius: 7px; color: var(--brand); font-size: .76rem; font-weight: 800; }
.access-search-grid { display: grid; grid-template-columns: minmax(240px, .8fr) minmax(320px, 1.2fr) auto; align-items: end; gap: 12px; }
.access-field { display: grid; gap: 7px; }
.access-field > span { color: #dbe4ff; font-size: .78rem; font-weight: 700; }
.access-field small { margin-left: 4px; color: var(--muted); font-weight: 600; }
.access-search-button { min-height: 46px; min-width: 104px; }
.access-search-empty, .access-empty-state { margin: 0; padding: 12px 14px; border: 1px dashed rgba(124, 163, 255, .2); border-radius: 10px; color: var(--muted); font-size: .84rem; }
.access-user-card { overflow: hidden; }
.access-user-header { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 18px 20px; border-bottom: 1px solid rgba(124, 163, 255, .16); background: rgba(24, 36, 73, .42); }
.access-user-avatar { display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid rgba(124, 163, 255, .35); border-radius: 10px; background: rgba(124, 163, 255, .12); color: #e8edff; font-weight: 800; }
.access-user-name, .access-user-email { margin: 0; }
.access-user-name { font-weight: 800; }
.access-user-email { margin-top: 3px; color: var(--muted); font-size: .82rem; }
.access-status { padding: 6px 10px; border: 1px solid rgba(97, 232, 162, .3); border-radius: 7px; background: rgba(97, 232, 162, .08); color: #9bf0c4; font-size: .74rem; font-weight: 750; }
.access-status.warning { border-color: rgba(255, 190, 92, .35); background: rgba(255, 190, 92, .09); color: #ffd28a; }
.access-user-layout { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(280px, .75fr); }
.access-section { min-width: 0; padding: 20px; }
.access-security-section { border-left: 1px solid rgba(124, 163, 255, .16); background: rgba(8, 13, 31, .42); }
.access-section-heading-lined { padding-bottom: 16px; border-bottom: 1px solid rgba(124, 163, 255, .14); }
.access-section-count { margin-left: auto; padding: 3px 8px; border-radius: 6px; background: rgba(124, 163, 255, .12); color: #cbd8ff; font-size: .76rem; font-weight: 800; }
.access-role-list { display: grid; }
.access-role-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 14px; min-height: 74px; padding: 13px 0; border-bottom: 1px solid rgba(124, 163, 255, .12); }
.access-role-info { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: 12px; }
.access-role-info strong, .access-role-info small { display: block; }
.access-role-info strong { font-size: .88rem; }
.access-role-info small { margin-top: 3px; color: var(--muted); font-size: .76rem; }
.access-role-actions, .access-role-edit { display: flex; align-items: center; gap: 8px; }
.access-role-edit { padding: 9px; border: 1px solid rgba(124, 163, 255, .16); border-radius: 10px; background: rgba(124, 163, 255, .06); }
.access-field-compact { min-width: 205px; }
.access-field-compact > span { font-size: .68rem; }
.access-text-danger { border: 0; background: transparent; color: #f3a0ad; font-size: .78rem; font-weight: 700; cursor: pointer; }
.access-text-danger:hover { color: #ffbdc6; text-decoration: underline; }
.access-add-role { display: grid; grid-template-columns: minmax(180px, 1fr) minmax(200px, .8fr) auto; align-items: end; gap: 10px; margin-top: 18px; padding: 14px; border: 1px solid rgba(97, 232, 162, .2); border-radius: 10px; background: rgba(97, 232, 162, .05); }
.access-add-role strong { font-size: .84rem; }
.access-security-status { display: flex; justify-content: space-between; gap: 12px; margin-top: 18px; padding: 12px 0; border-bottom: 1px solid rgba(124, 163, 255, .12); color: var(--muted); font-size: .82rem; }
.access-security-status strong { color: var(--text); }
.access-security-note { margin: 16px 0; color: var(--muted); font-size: .8rem; line-height: 1.55; }
.access-reset-button { width: 100%; }
.access-reset-result { display: grid; gap: 9px; margin-top: 16px; padding: 12px; border: 1px solid rgba(255, 190, 92, .25); border-radius: 10px; background: rgba(255, 190, 92, .06); }
.access-reset-result > strong { font-size: .82rem; }
.access-reset-result small { color: var(--muted); line-height: 1.45; }
.access-placeholder { display: flex; align-items: center; gap: 12px; padding: 18px; color: var(--muted); }
.access-placeholder > span { font-size: 1.3rem; color: rgba(124, 163, 255, .65); }

@media (max-width: 980px) {
  .access-search-grid, .access-user-layout { grid-template-columns: 1fr; }
  .access-security-section { border-top: 1px solid rgba(124, 163, 255, .16); border-left: 0; }
}

@media (max-width: 680px) {
  .access-search, .access-section { padding: 15px; }
  .access-user-header, .access-role-row, .access-add-role { grid-template-columns: 1fr; }
  .access-user-avatar { display: none; }
  .access-status { justify-self: start; }
  .access-role-actions, .access-role-edit { align-items: stretch; flex-direction: column; }
  .access-role-actions > *, .access-role-edit > *, .access-add-role > * { width: 100%; }
}
</style>
