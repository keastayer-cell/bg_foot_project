<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand-wrap brand-link" to="/" aria-label="На главную">
        <div class="brand-row">
          <div class="brand-emblem" aria-hidden="true">
            <img
              class="brand-emblem-img"
              :src="bogorodskCoat"
              alt="Герб Богородска"
            />
          </div>
          <div>
            <div class="brand">Футбол Богородск</div>
            <div class="brand-sub">Богородск, Нижегородская область</div>
          </div>
        </div>
      </router-link>
      <div class="topbar-right">
        <nav class="topnav">
          <router-link to="/">Туры</router-link>
          <router-link to="/players">Игроки</router-link>
          <router-link to="/teams">Команды</router-link>
          <router-link v-if="canSeeAdmin()" to="/admin">Админ-панель</router-link>
          <router-link v-if="canSeeAdmin()" to="/api-explorer">API Explorer</router-link>
        </nav>

        <div class="auth-strip" v-if="isAuthenticated && user">
          <button
            v-if="isTeamRep"
            class="btn-ghost auth-profile-btn"
            type="button"
            @click="openProfile"
          >
            {{ teamRepLabel }}
          </button>
          <span v-else class="auth-name">{{ user.name }}</span>
          <button class="btn-ghost" type="button" @click="handleLogout">Выйти</button>
        </div>

        <button v-else class="btn-ghost" type="button" @click="openAuthModal('login')">Войти / Регистрация</button>
      </div>
    </header>

    <main class="content-wrap">
      <router-view />
    </main>

    <div v-if="authModalOpen" class="modal-backdrop" @click.self="closeAuthModal">
      <article class="card auth-modal">
        <div class="toolbar auth-modal-head">
          <h3 class="section-title">Авторизация</h3>
          <button class="btn-ghost" type="button" @click="closeAuthModal">Закрыть</button>
        </div>

        <div class="auth-tabs">
          <button
            class="btn-ghost"
            :class="{ 'auth-tab-active': authMode === 'login' }"
            type="button"
            @click="authMode = 'login'"
          >
            Вход
          </button>
          <button
            class="btn-ghost"
            :class="{ 'auth-tab-active': authMode === 'register' }"
            type="button"
            @click="authMode = 'register'"
          >
            Регистрация
          </button>
        </div>

        <form class="auth-form" @submit.prevent="submitAuth">
          <label>
            Email
            <input v-model.trim="authForm.email" type="email" required />
          </label>

          <label v-if="authMode === 'register'">
            Имя
            <input v-model.trim="authForm.name" type="text" minlength="2" maxlength="120" required />
          </label>

          <label>
            Пароль
            <div class="password-input-wrap">
              <input
                v-model="authForm.password"
                :type="showPassword ? 'text' : 'password'"
                minlength="6"
                maxlength="120"
                required
              />
              <button
                class="btn-ghost password-toggle"
                type="button"
                :aria-label="showPassword ? 'Скрыть пароль' : 'Показать пароль'"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '🙈' : '👁' }}
              </button>
            </div>
          </label>

          <p class="error-text" v-if="authError">{{ authError }}</p>
          <p class="success-text" v-if="authOk">{{ authOk }}</p>

          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="authSubmitting">
              {{ authSubmitting ? 'Подождите...' : authMode === 'register' ? 'Зарегистрироваться' : 'Войти' }}
            </button>
          </div>
        </form>
      </article>
    </div>

    <div v-if="passwordChangeModalOpen" class="modal-backdrop password-change-backdrop">
      <article class="card auth-modal password-change-modal">
        <div class="toolbar auth-modal-head">
          <h3 class="section-title">Смена пароля обязательна</h3>
          <button v-if="!passwordChangeSubmitting" class="btn-ghost" type="button" @click="handleLogout">Выйти</button>
        </div>

        <form class="auth-form" @submit.prevent="submitPasswordChange">
          <p class="muted-text">
            Администратор сбросил ваш пароль на временный. Для продолжения работы задайте новый личный пароль.
          </p>

          <label>
            Текущий временный пароль
            <input v-model="passwordChangeForm.currentPassword" type="password" minlength="6" maxlength="120" required />
          </label>

          <label>
            Новый пароль
            <input v-model="passwordChangeForm.newPassword" type="password" minlength="6" maxlength="120" required />
          </label>

          <label>
            Повторите новый пароль
            <input v-model="passwordChangeForm.confirmPassword" type="password" minlength="6" maxlength="120" required />
          </label>

          <p class="error-text" v-if="passwordChangeError">{{ passwordChangeError }}</p>
          <p class="success-text" v-if="passwordChangeOk">{{ passwordChangeOk }}</p>

          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="passwordChangeSubmitting">
              {{ passwordChangeSubmitting ? 'Подождите...' : 'Сменить пароль' }}
            </button>
          </div>
        </form>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from './store/auth'
import bogorodskCoat from './assets/bogorodsk-coat.png'

const { user, isAuthenticated, register, login, logout, changePassword, ensureSession, hasRole } = useAuth()
const router = useRouter()

const authModalOpen = ref(false)
const authMode = ref('login')
const authSubmitting = ref(false)
const authError = ref('')
const authOk = ref('')
const showPassword = ref(false)
const passwordChangeModalOpen = ref(false)
const passwordChangeSubmitting = ref(false)
const passwordChangeError = ref('')
const passwordChangeOk = ref('')

const authForm = reactive({
  email: '',
  name: '',
  password: '',
})

const passwordChangeForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const isTeamRep = computed(() => isAuthenticated.value && hasRole('TEAM_REP'))
const teamRepLabel = computed(() => {
  const teamName = String(user.value?.teamName || '').trim() || 'не назначена'
  return `Представитель команды "${teamName}"`
})

function resetMessages() {
  authError.value = ''
  authOk.value = ''
}

function openAuthModal(mode) {
  authMode.value = mode
  resetMessages()
  authModalOpen.value = true
}

function closeAuthModal() {
  authModalOpen.value = false
  authSubmitting.value = false
  showPassword.value = false
  resetMessages()
}

function resetPasswordChangeMessages() {
  passwordChangeError.value = ''
  passwordChangeOk.value = ''
}

function syncPasswordChangeModal() {
  const required = Boolean(isAuthenticated.value && user.value?.mustChangePassword)
  passwordChangeModalOpen.value = required
  if (!required) {
    passwordChangeSubmitting.value = false
    passwordChangeForm.currentPassword = ''
    passwordChangeForm.newPassword = ''
    passwordChangeForm.confirmPassword = ''
    resetPasswordChangeMessages()
  }
}

async function submitAuth() {
  authSubmitting.value = true
  resetMessages()

  try {
    if (authMode.value === 'register') {
      await register(authForm)
      authOk.value = 'Регистрация успешна. Вы вошли в систему.'
    } else {
      await login(authForm)
      authOk.value = user.value?.mustChangePassword
        ? 'Вход выполнен. Требуется сменить временный пароль.'
        : 'Вход выполнен.'
    }

    authForm.password = ''
    setTimeout(() => {
      closeAuthModal()
      syncPasswordChangeModal()
    }, 300)
  } catch (error) {
    authError.value = error.message || 'Не удалось выполнить запрос.'
  } finally {
    authSubmitting.value = false
  }
}

async function submitPasswordChange() {
  resetPasswordChangeMessages()

  if (passwordChangeForm.newPassword !== passwordChangeForm.confirmPassword) {
    passwordChangeError.value = 'Новый пароль и его повтор должны совпадать.'
    return
  }

  passwordChangeSubmitting.value = true
  try {
    await changePassword({
      currentPassword: passwordChangeForm.currentPassword,
      newPassword: passwordChangeForm.newPassword,
    })
    passwordChangeOk.value = 'Пароль успешно обновлен.'
    passwordChangeForm.currentPassword = ''
    passwordChangeForm.newPassword = ''
    passwordChangeForm.confirmPassword = ''
    syncPasswordChangeModal()
  } catch (error) {
    passwordChangeError.value = error.message || 'Не удалось сменить пароль.'
  } finally {
    passwordChangeSubmitting.value = false
  }
}

async function handleLogout() {
  await logout({ remote: true, suppressErrors: true })
  router.replace('/')
}

function openProfile() {
  router.push('/team-rep-dashboard')
}

function canSeeAdmin() {
  return isAuthenticated.value && hasRole('SUPER_ADMIN')
}

onMounted(async () => {
  try {
    await ensureSession({ forceRefresh: isAuthenticated.value })
    syncPasswordChangeModal()
  } catch {
    await logout({ remote: true, suppressErrors: true })
  }
})

watch(
  () => user.value?.mustChangePassword,
  () => {
    syncPasswordChangeModal()
  }
)
</script>
