<template>
  <article class="card admin-panel representatives-panel">
    <header class="admin-panel-head"><p class="admin-panel-kicker">Доступ</p><h3 class="section-title">Представители команд</h3><p class="muted-text">Выберите пользователя и назначьте ему команду.</p></header>
    <form class="representative-search" @submit.prevent="$emit('find')">
      <div class="representative-search-grid">
        <label><span>Поиск по email</span><input v-model.trim="search" type="search" inputmode="email" autocomplete="off" placeholder="Введите email" /></label>
        <label><span>Представитель <small>{{ users.length }}</small></span><select v-model="selectedEmail"><option value="">— выберите представителя —</option><option v-for="user in users" :key="user.email" :value="String(user.email || '').toLowerCase()">{{ user.name || user.email }}{{ user.name ? ` · ${user.email}` : '' }}</option></select></label>
        <button class="btn-primary representative-open-button" type="submit">Открыть</button>
      </div>
      <p v-if="!hasUsers" class="representative-empty">Представители не найдены.</p>
    </form>
    <section v-if="foundUser" class="representative-card">
      <header class="representative-user-header"><span class="representative-avatar" aria-hidden="true">{{ userInitials }}</span><div><strong>{{ foundUser.name || 'Имя не указано' }}</strong><small>{{ foundUser.email }}</small></div><span class="representative-role">Представитель команды</span></header>
      <div class="representative-body">
        <div class="representative-current-team" :class="{ empty: !currentTeamScope }"><div><span>Текущая команда</span><strong>{{ currentTeamScope?.teamName || 'Команда не назначена' }}</strong></div><button v-if="currentTeamScope" class="btn-ghost representative-unassign" type="button" @click="$emit('unassign-team')">Снять назначение</button></div>
        <p v-if="hasMultipleTeamScopes" class="representative-warning">Есть несколько назначений. Сохранение оставит только выбранную команду.</p>
        <form class="representative-assignment" @submit.prevent="$emit('save-team')"><label><span>{{ currentTeamScope ? 'Новая команда' : 'Назначить команду' }}</span><select v-model="selectedTeamId"><option value="">— выберите команду —</option><option v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option></select></label><button class="btn-primary" type="submit" :disabled="!selectedTeamId">{{ primaryActionLabel }}</button></form>
      </div>
    </section>
    <div v-else class="representative-empty representative-empty-card">Откройте представителя для управления назначением.</div>
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
.representatives-panel{padding:0;overflow:hidden}.representatives-panel .admin-panel-head{padding:24px;margin:0;border-bottom:1px solid var(--line)}.representatives-panel .admin-panel-head .section-title{font-size:1.35rem;margin:8px 0}.representatives-panel .admin-panel-head .muted-text{font-size:.85rem;margin:8px 0 0}.representative-search{padding:24px;border-bottom:1px solid var(--line)}.representative-search-grid{display:grid;grid-template-columns:minmax(180px,.7fr) minmax(240px,1fr) auto;gap:16px;align-items:end}.representative-search-grid label,.representative-assignment label{display:grid;gap:9px;font-size:.8rem;color:var(--muted);min-width:0}.representative-search-grid label>span{display:flex;align-items:center;gap:8px}.representative-search-grid label small{padding:2px 6px;border-radius:5px;background:rgba(124,163,255,.1);font-size:.7rem}.representative-search-grid input,.representative-search-grid select,.representative-assignment select{height:46px;min-height:46px;margin:0;font-size:.88rem;font-weight:400}.representative-search-grid button,.representative-assignment>.btn-primary{height:46px;min-height:46px;padding:0 18px;margin:0;white-space:nowrap}.representative-card{margin:24px;border:1px solid var(--line);border-radius:12px;overflow:hidden;background:rgba(7,13,31,.45)}.representative-user-header{display:flex;align-items:center;gap:14px;padding:20px 24px;border-bottom:1px solid var(--line);background:rgba(124,163,255,.05)}.representative-avatar{display:grid;place-items:center;width:44px;height:44px;flex-shrink:0;border-radius:10px;border:1px solid rgba(97,232,162,.25);background:rgba(97,232,162,.08);color:var(--brand);font-weight:800}.representative-user-header>div{min-width:0}.representative-user-header strong{display:block;font-size:1.05rem;overflow-wrap:anywhere}.representative-user-header small{display:block;margin-top:5px;font-size:.8rem;color:var(--muted);overflow-wrap:anywhere}.representative-role{margin-left:auto;padding:6px 10px;border:1px solid rgba(97,232,162,.22);border-radius:7px;color:var(--brand);font-size:.7rem;white-space:nowrap}.representative-body{padding:24px;display:grid;gap:24px}.representative-current-team{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:18px 20px;border:1px solid rgba(97,232,162,.2);border-radius:10px;background:rgba(97,232,162,.04)}.representative-current-team>div{display:grid;gap:8px}.representative-current-team span{font-size:.75rem;color:var(--muted)}.representative-current-team strong{font-size:1.05rem}.representative-current-team.empty{border-color:var(--line);background:rgba(124,163,255,.03)}.representative-unassign{color:#ff8792;border-color:rgba(255,99,113,.25);font-size:.8rem;white-space:nowrap}.representative-assignment{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:16px;max-width:800px}.representative-warning{margin:0;padding:12px;border:1px solid rgba(255,190,92,.2);border-radius:8px;color:#ffd28a;font-size:.8rem;line-height:1.5}.representative-empty{font-size:.85rem;color:var(--muted);margin:16px 0 0}.representative-empty-card{padding:32px 24px;margin:0}
@media(max-width:1000px){.representative-search-grid{grid-template-columns:1fr 1fr}.representative-search-grid button{grid-column:1/-1;justify-self:start}}
@media(max-width:640px){.representatives-panel .admin-panel-head,.representative-search{padding:20px}.representative-search-grid,.representative-assignment{grid-template-columns:1fr}.representative-search-grid button{width:100%}.representative-card{margin:20px}.representative-user-header{padding:18px;flex-wrap:wrap}.representative-role{margin-left:0}.representative-body{padding:18px;gap:20px}.representative-current-team{align-items:flex-start;flex-direction:column}.representative-assignment>.btn-primary{width:100%}}
</style>
