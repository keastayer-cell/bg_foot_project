<template>
  <section class="section-wrap reset-password-page">
    <article class="card reset-password-card">
      <div class="reset-password-head">
        <span class="reset-password-kicker">Безопасность</span>
        <h2 class="section-title">Установка нового пароля</h2>
        <p class="muted-text">Одноразовая ссылка позволяет задать новый пароль без входа в систему.</p>
      </div>

      <p v-if="!token" class="error-text">Ссылка недействительна: отсутствует токен сброса пароля.</p>

      <form v-else class="form-card reset-password-form" @submit.prevent="submitReset">
        <label>
          Новый пароль
          <input v-model="newPassword" :type="showPassword ? 'text' : 'password'" minlength="8" maxlength="120" required />
        </label>
        <label>
          Повторите новый пароль
          <input v-model="confirmPassword" :type="showPassword ? 'text' : 'password'" minlength="8" maxlength="120" required />
        </label>

        <div class="actions-row reset-password-actions">
          <button class="btn-ghost" type="button" @click="showPassword = !showPassword">
            {{ showPassword ? 'Скрыть пароль' : 'Показать пароль' }}
          </button>
          <button class="btn-primary" type="submit" :disabled="submitting">
            {{ submitting ? 'Сохраняем...' : 'Сохранить пароль' }}
          </button>
        </div>
      </form>

      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <p v-if="successText" class="success-text">{{ successText }}</p>

      <div class="actions-row" v-if="successText">
        <router-link class="btn-ghost" to="/">На главную</router-link>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { completePasswordReset } from '../api/auth'

const route = useRoute()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const errorText = ref('')
const successText = ref('')

const token = computed(() => String(route.query.token || '').trim())

async function submitReset() {
  errorText.value = ''
  successText.value = ''

  if (!token.value) {
    errorText.value = 'Ссылка недействительна: отсутствует токен сброса.'
    return
  }

  if (newPassword.value.length < 8) {
    errorText.value = 'Новый пароль должен содержать не менее 8 символов.'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    errorText.value = 'Пароль и его повтор должны совпадать.'
    return
  }

  submitting.value = true
  try {
    await completePasswordReset(apiBaseUrl, token.value, newPassword.value)

    newPassword.value = ''
    confirmPassword.value = ''
    successText.value = 'Пароль успешно установлен. Теперь можно войти в систему под новым паролем.'
  } catch (error) {
    errorText.value = error.message || 'Не удалось сохранить новый пароль.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reset-password-page {
  display: grid;
  justify-items: center;
}

.reset-password-card {
  width: min(620px, 100%);
  display: grid;
  gap: 16px;
}

.reset-password-head,
.reset-password-form {
  display: grid;
  gap: 12px;
}

.reset-password-kicker {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(151, 176, 255, 0.78);
}

.reset-password-form label {
  display: grid;
  gap: 8px;
}

@media (max-width: 640px) {
  .reset-password-card {
    gap: 14px;
  }

  .reset-password-actions,
  .actions-row {
    align-items: stretch;
    flex-direction: column;
  }

  .reset-password-actions > *,
  .actions-row > * {
    width: 100%;
  }
}
</style>
