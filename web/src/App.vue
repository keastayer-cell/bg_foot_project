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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from './store/auth'
import bogorodskCoat from './assets/bogorodsk-coat.png'

const { user, isAuthenticated, register, login, logout, loadCurrentUser, hasRole } = useAuth()
const router = useRouter()

const authModalOpen = ref(false)
const authMode = ref('login')
const authSubmitting = ref(false)
const authError = ref('')
const authOk = ref('')
const showPassword = ref(false)

const authForm = reactive({
  email: '',
  name: '',
  password: '',
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

async function submitAuth() {
  authSubmitting.value = true
  resetMessages()

  try {
    if (authMode.value === 'register') {
      await register(authForm)
      authOk.value = 'Регистрация успешна. Вы вошли в систему.'
    } else {
      await login(authForm)
      authOk.value = 'Вход выполнен.'
    }

    authForm.password = ''
    setTimeout(() => {
      closeAuthModal()
    }, 500)
  } catch (error) {
    authError.value = error.message || 'Не удалось выполнить запрос.'
  } finally {
    authSubmitting.value = false
  }
}

function handleLogout() {
  logout()
  router.replace('/')
}

function openProfile() {
  router.push('/team-rep-dashboard')
}

function canSeeAdmin() {
  return isAuthenticated.value && hasRole('SUPER_ADMIN')
}

onMounted(async () => {
  if (!isAuthenticated.value) return

  try {
    await loadCurrentUser()
  } catch {
    logout()
  }
})
</script>
