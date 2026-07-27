<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Блокировки пользователей</h3>
      <p class="muted-text">Локальный реестр блокировок и ручное управление статусом пользователя.</p>
    </div>
    <div class="admin-grid">
      <form class="admin-form admin-surface" @submit.prevent="$emit('ban')">
        <label>
          Email пользователя
          <input v-model.trim="form.email" type="email" required />
        </label>
        <label>
          Причина
          <input v-model.trim="form.reason" type="text" placeholder="Нарушение правил" required />
        </label>
        <div class="actions-row">
          <button class="btn-danger" type="submit">Забанить</button>
        </div>
      </form>

      <div class="admin-list admin-surface">
        <h4 class="admin-list-title">Статус пользователей</h4>
        <p v-if="!users.length" class="muted-text">Пока пусто.</p>
        <div v-else class="admin-list-items">
          <article v-for="item in users" :key="`ban-${item.email}`" class="admin-list-item">
            <strong>{{ item.email }}</strong>
            <span v-if="item.banned" class="muted-text">Заблокирован: {{ item.banReason || '-' }}</span>
            <span v-else class="success-text">Активен</span>
            <div v-if="item.banned" class="actions-row">
              <button class="btn-ghost" type="button" @click="$emit('unban', item.email)">Разбанить</button>
            </div>
          </article>
        </div>
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
  users: {
    type: Array,
    required: true,
  },
})

defineEmits(['ban', 'unban'])
</script>
