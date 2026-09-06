<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Доступ</p>
      <h3 class="section-title">Блокировки</h3>
      <p class="muted-text">Ручное управление локальным статусом доступа пользователей.</p>
    </header>

    <div class="ban-summary">
      <article><span>Всего записей</span><strong>{{ users.length }}</strong></article>
      <article><span>Активны</span><strong>{{ activeUsersCount }}</strong></article>
      <article class="danger"><span>Заблокированы</span><strong>{{ bannedUsers.length }}</strong></article>
    </div>

    <div class="ban-layout">
      <form class="admin-step-section ban-form" @submit.prevent="$emit('ban')">
        <div class="admin-step-heading">
          <span class="admin-step-number">1</span>
          <div><h4>Новая блокировка</h4><p>Укажите пользователя и зафиксируйте понятную причину.</p></div>
        </div>
        <label>Email пользователя<input v-model.trim="form.email" type="email" required placeholder="user@example.com" /></label>
        <label>Причина<textarea v-model.trim="form.reason" rows="4" placeholder="Опишите причину блокировки" required></textarea></label>
        <div class="ban-warning"><strong>Важно</strong><span>Это действие ограничивает доступ пользователя и требует подтверждения.</span></div>
        <div class="admin-primary-actions"><button class="btn-danger" type="submit">Заблокировать пользователя</button></div>
      </form>

      <section class="admin-step-section ban-registry">
        <div class="admin-step-heading">
          <span class="admin-step-number">2</span>
          <div><h4>Заблокированные пользователи</h4><p>Текущие ограничения и причины их установки.</p></div>
          <span class="admin-step-count">{{ bannedUsers.length }}</span>
        </div>

        <div v-if="!bannedUsers.length" class="ban-empty">
          <strong>Активных блокировок нет</strong>
          <p>Все пользователи из локального реестра имеют активный статус.</p>
        </div>
        <div v-else class="ban-user-list">
          <article v-for="item in bannedUsers" :key="`ban-${item.email}`" class="ban-user-row">
            <div class="ban-user-copy">
              <span class="ban-status">Заблокирован</span>
              <strong>{{ item.email }}</strong>
              <p>{{ item.banReason || 'Причина не указана' }}</p>
            </div>
            <button class="btn-ghost btn-sm" type="button" @click="$emit('unban', item.email)">Разблокировать</button>
          </article>
        </div>
      </section>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: { type: Object, required: true },
  users: { type: Array, required: true },
})

defineEmits(['ban', 'unban'])
const bannedUsers = computed(() => props.users.filter((user) => user.banned))
const activeUsersCount = computed(() => props.users.length - bannedUsers.value.length)
</script>

<style scoped>
.ban-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 180px)); gap: 10px; }
.ban-summary article { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; padding: 12px 14px; border: 1px solid rgba(124, 163, 255, .16); border-radius: 9px; background: rgba(10, 16, 37, .62); }
.ban-summary span { color: var(--muted); font-size: .75rem; }
.ban-summary strong { font-size: 1.1rem; }
.ban-summary .danger { border-color: rgba(255, 115, 115, .2); }
.ban-summary .danger strong { color: #f3a0ad; }
.ban-layout { display: grid; grid-template-columns: minmax(300px, .75fr) minmax(380px, 1.25fr); gap: 14px; align-items: start; }
.ban-form > label { display: grid; gap: 7px; }
.ban-form textarea { resize: vertical; min-height: 100px; }
.ban-warning { display: grid; gap: 3px; padding: 11px 12px; border-left: 3px solid rgba(255, 190, 92, .6); background: rgba(255, 190, 92, .06); font-size: .76rem; }
.ban-warning span { color: var(--muted); }
.ban-user-list { display: grid; }
.ban-user-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid rgba(124, 163, 255, .12); }
.ban-user-row:last-child { border-bottom: 0; }
.ban-user-copy { display: grid; gap: 4px; min-width: 0; }
.ban-user-copy strong { overflow-wrap: anywhere; }
.ban-user-copy p { margin: 0; color: var(--muted); font-size: .78rem; }
.ban-status { width: fit-content; padding: 3px 7px; border: 1px solid rgba(255, 115, 115, .25); border-radius: 6px; color: #f3a0ad; font-size: .68rem; font-weight: 800; text-transform: uppercase; }
.ban-empty { display: grid; gap: 4px; padding: 24px; border: 1px dashed rgba(97, 232, 162, .22); border-radius: 10px; text-align: center; }
.ban-empty strong { color: #9bf0c4; }
.ban-empty p { margin: 0; color: var(--muted); font-size: .8rem; }
@media (max-width: 960px) { .ban-layout { grid-template-columns: 1fr; } }
@media (max-width: 620px) {
  .ban-summary { grid-template-columns: 1fr; }
  .ban-user-row { grid-template-columns: 1fr; }
  .ban-user-row button { width: 100%; }
}
</style>
