<template>
  <article class="card admin-panel access-panel">
    <header class="admin-panel-head access-panel-heading">
      <div>
        <p class="admin-panel-kicker">Доступ</p>
        <h3 class="section-title">Роли и доступ</h3>
        <p class="muted-text">Управление правами пользователей.</p>
      </div>
    </header>

    <form class="access-search" @submit.prevent="$emit('find')">
      <div class="access-search-grid">
        <label class="access-field">
          <span>Поиск по email</span>
          <input v-model.trim="search" type="search" inputmode="email" autocomplete="off" placeholder="Введите email" />
        </label>
        <label class="access-field access-results-field">
          <span>Пользователь <small>{{ users.length }}</small></span>
          <select v-model="selectedEmail">
            <option value="">— выберите пользователя —</option>
            <option v-for="user in users" :key="user.email" :value="user.email">
              {{ user.name || user.email }}{{ user.name ? ` · ${user.email}` : '' }}
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

            <div><h4>Роли пользователя</h4></div>
            <span class="access-section-count">{{ foundUser.roles.length }}</span>
          </div>

          <p v-if="!foundUser.roles.length" class="access-empty-state">У пользователя пока нет назначенных ролей.</p>
          <div v-else class="access-role-list">
            <div v-for="role in foundUser.roles" :key="role" class="access-role-row">
              <div class="access-role-info">

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
                    >{{ item.label }}</option>
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
            <label for="access-assign-role">Добавить роль</label>
            <select v-model="assignCode" id="access-assign-role" class="admin-role-select-inline" aria-label="Назначить роль">
              <option
                v-for="item in rolesCatalog"
                :key="item.code"
                :value="item.code"
                :disabled="foundUser.roles.includes(item.code)"
              >{{ item.label }}</option>
            </select>
            <button class="btn-primary btn-sm" type="button" :disabled="!canAssignRole" @click="$emit('assign-role')">Добавить</button>
          </div>
        </section>

        <aside class="access-section access-security-section">
          <div class="access-section-heading access-section-heading-lined">

            <div><h4>Безопасность</h4></div>
          </div>
          <div class="access-security-status"><span>Пароль</span><strong>{{ foundUser.mustChangePassword ? 'Ожидает смены' : 'Установлен' }}</strong></div>
          <p class="access-security-note">Создаёт одноразовую ссылку для нового пароля. Текущий пароль перестанет действовать.</p>
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
.access-panel{padding:0;gap:0;overflow:hidden}.access-panel-heading{padding:22px 24px;margin:0;border-bottom:1px solid var(--line)}.access-panel-heading .section-title{font-size:1.35rem;margin:7px 0}.access-panel-heading .muted-text{font-size:.85rem;margin:0;color:var(--muted)}.access-search{padding:22px 24px;background:rgba(7,13,31,.3);border-bottom:1px solid var(--line)}.access-search-grid{display:grid;grid-template-columns:minmax(180px,280px) minmax(240px,420px) auto;gap:14px;align-items:end;max-width:850px}.access-field{display:grid;gap:8px;min-width:0}.access-field>span{font-size:.78rem;color:var(--muted)}.access-field small{margin-left:6px}.access-field input,.access-field select,.access-add-role select{height:44px;min-height:44px;margin:0;font-size:.85rem;font-weight:400;min-width:0;width:100%}.access-search-button{height:44px;margin:0;padding:0 18px}.access-search-empty{margin:16px 0 0;font-size:.85rem;color:var(--muted)}.access-user-card{margin:24px;max-width:1100px;border:0;background:transparent}.access-user-header{display:flex;align-items:center;gap:14px;padding:18px 20px;background:rgba(124,163,255,.06);border:1px solid var(--line);border-radius:12px}.access-user-avatar{display:grid;place-items:center;width:44px;height:44px;flex-shrink:0;color:var(--brand);font-weight:800;border:1px solid rgba(97,232,162,.25);border-radius:10px;background:rgba(97,232,162,.08)}.access-user-identity{min-width:0;overflow-wrap:anywhere}.access-user-name{margin:0;font-size:1.05rem;font-weight:800}.access-user-email{margin:5px 0 0;font-size:.8rem;color:var(--muted)}.access-status{margin-left:auto;padding:6px 10px;font-size:.72rem;color:var(--brand);border:1px solid rgba(97,232,162,.22);border-radius:7px;white-space:nowrap}.access-status.warning{color:#ffd28a;border-color:rgba(255,190,92,.3)}.access-user-layout{display:grid;grid-template-columns:minmax(0,1fr) 290px;align-items:start;gap:20px;margin-top:20px}.access-section{padding:20px;border:1px solid var(--line);border-radius:12px;background:rgba(7,13,31,.55);min-width:0}.access-section-heading{display:flex;align-items:center;gap:10px;margin-bottom:18px}.access-section-heading h4{margin:0;font-size:1rem}.access-section-count{margin-left:auto;padding:3px 7px;border-radius:5px;background:rgba(124,163,255,.1);font-size:.72rem;color:var(--muted)}.access-role-list{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,270px),1fr));gap:12px}.access-role-row{display:flex;flex-direction:column;align-items:stretch;gap:16px;padding:16px;border:1px solid rgba(97,232,162,.18);border-left:3px solid var(--brand);border-radius:9px;background:rgba(97,232,162,.035)}.access-role-info strong{display:block;font-size:.95rem}.access-role-info small{display:block;margin-top:7px;color:var(--muted);font-size:.78rem;line-height:1.5;max-width:380px}.access-role-actions{display:flex;align-items:center;gap:14px;margin-top:auto}.access-role-actions .btn-ghost{padding:7px 11px;font-size:.78rem}.access-text-danger{padding:7px 0;border:0;background:transparent;color:#ff8792;font-size:.78rem;cursor:pointer}.access-role-edit{display:flex;align-items:end;flex-wrap:wrap;gap:10px}.access-role-edit label{flex:1;min-width:160px}.access-role-edit button{padding:8px 11px;font-size:.78rem}.access-add-role{display:grid;grid-template-columns:minmax(0,320px) auto;gap:8px 12px;justify-content:start;margin-top:20px;padding-top:18px;border-top:1px solid var(--line)}.access-add-role>label{grid-column:1/-1;font-size:.78rem;color:var(--muted)}.access-add-role button{height:44px;margin:0;padding:0 16px;font-size:.82rem}.access-security-section{background:rgba(124,163,255,.035)}.access-security-status{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:.78rem;padding:12px;border-radius:7px;background:rgba(124,163,255,.05);color:var(--muted)}.access-security-status strong{color:var(--text)}.access-security-note{font-size:.8rem;line-height:1.6;color:var(--muted);margin:16px 0}.access-reset-button{padding:10px 14px;font-size:.8rem;width:100%}.access-reset-result{display:grid;gap:10px;margin-top:16px;padding-top:16px;border-top:1px solid var(--line);overflow-wrap:anywhere}.access-reset-result strong{font-size:.85rem}.access-reset-result p{margin:0;font-size:.78rem}.access-reset-result small{color:var(--muted);font-size:.72rem;line-height:1.5}.access-placeholder{padding:32px 24px;color:var(--muted);font-size:.85rem}.access-placeholder>span{display:none}.access-placeholder p{margin:8px 0 0}.access-empty-state{color:var(--muted);font-size:.85rem}
@media(max-width:1100px){.access-user-layout{grid-template-columns:1fr}.access-security-section{max-width:460px}.access-search-grid{grid-template-columns:minmax(0,1fr) minmax(0,1.3fr) auto}}
@media(max-width:640px){.access-search-grid{grid-template-columns:1fr}.access-search-button{justify-self:start}.access-user-card{margin:20px}.access-user-header{flex-wrap:wrap;padding:16px}.access-status{margin-left:0}.access-panel-heading,.access-search{padding:20px}.access-section{padding:16px}.access-add-role{grid-template-columns:minmax(0,1fr) auto}.access-security-section{max-width:none}}
</style>
