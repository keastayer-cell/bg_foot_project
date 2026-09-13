<template>
  <section class="section-wrap account-page">
    <article class="card account-hero">
      <div>
        <span class="account-kicker">Личный кабинет</span>
        <h1>{{ user?.name }}</h1>
        <p class="muted-text">Профиль, права доступа и безопасность аккаунта.</p>
      </div>
      <div class="account-role-chips">
        <span v-for="role in roleAnnotations" :key="role.code" class="account-role-chip">
          {{ role.title }}
        </span>
      </div>
    </article>

    <div v-if="loading" class="card account-state">Загрузка профиля...</div>
    <p v-else-if="loadError" class="card error-text account-state">{{ loadError }}</p>

    <div v-else class="account-layout">
      <div class="account-main">
        <article class="card account-panel">
          <div class="account-panel-head">
            <div>
              <span class="account-kicker">Основные данные</span>
              <h2 class="section-title">Профиль</h2>
            </div>
          </div>
          <form class="account-form" @submit.prevent="saveProfile">
            <label>
              Имя
              <input v-model.trim="profileForm.name" minlength="2" maxlength="120" autocomplete="name" required />
            </label>
            <label>
              Email
              <input v-model.trim="profileForm.email" type="email" maxlength="255" autocomplete="email" required />
            </label>
            <p v-if="profileError" class="error-text">{{ profileError }}</p>
            <p v-if="profileOk" class="success-text">{{ profileOk }}</p>
            <div class="actions-row">
              <button class="btn-primary" type="submit" :disabled="profileSaving">
                {{ profileSaving ? 'Сохраняем...' : 'Сохранить изменения' }}
              </button>
            </div>
          </form>
        </article>

        <article class="card account-panel">
          <div class="account-panel-head">
            <div>
              <span class="account-kicker">Каналы связи</span>
              <h2 class="section-title">Уведомления</h2>
            </div>
          </div>
          <p class="muted-text">
            Выберите обычные события для колокольчика и почты. Сообщения, которые требуют
            решения или подтверждения, а также письма безопасности доставляются всегда.
          </p>
          <div class="notification-settings">
            <div class="notification-setting-head">
              <span>Категория</span><span>Колокольчик</span><span>Email</span>
            </div>
            <label v-for="setting in notificationSettings" :key="setting.category" class="notification-setting-row">
              <span>
                <strong>{{ notificationCategory(setting.category).title }}</strong>
                <small>{{ notificationCategory(setting.category).description }}</small>
              </span>
              <input v-model="setting.bellEnabled" type="checkbox" :aria-label="`${notificationCategory(setting.category).title}: колокольчик`" />
              <input v-model="setting.emailEnabled" type="checkbox" :aria-label="`${notificationCategory(setting.category).title}: email`" />
            </label>
          </div>
          <p v-if="notificationError" class="error-text">{{ notificationError }}</p>
          <p v-if="notificationOk" class="success-text">{{ notificationOk }}</p>
          <div class="actions-row">
            <button class="btn-primary" type="button" :disabled="notificationSaving" @click="saveNotificationSettings">
              {{ notificationSaving ? 'Сохраняем...' : 'Сохранить настройки' }}
            </button>
          </div>
        </article>

        <article class="card account-panel">
          <div class="account-panel-head">
            <div>
              <span class="account-kicker">Безопасность</span>
              <h2 class="section-title">Смена пароля</h2>
            </div>
          </div>
          <form class="account-form" @submit.prevent="savePassword">
            <label>
              Текущий пароль
              <input v-model="passwordForm.currentPassword" type="password" minlength="6" maxlength="120" autocomplete="current-password" required />
            </label>
            <label>
              Новый пароль
              <input v-model="passwordForm.newPassword" type="password" minlength="6" maxlength="120" autocomplete="new-password" required />
            </label>
            <label>
              Повторите новый пароль
              <input v-model="passwordForm.confirmPassword" type="password" minlength="6" maxlength="120" autocomplete="new-password" required />
            </label>
            <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
            <p v-if="passwordOk" class="success-text">{{ passwordOk }}</p>
            <div class="actions-row">
              <button class="btn-primary" type="submit" :disabled="passwordSaving">
                {{ passwordSaving ? 'Обновляем...' : 'Обновить пароль' }}
              </button>
            </div>
          </form>
        </article>
      </div>

      <aside class="account-side">
        <article class="card account-panel">
          <span class="account-kicker">Моя лига</span>
          <h2 class="section-title">Избранное</h2>
          <div v-if="favorites.length" class="account-favorites">
            <div v-for="item in favorites" :key="item.type + ':' + item.targetId">
              <RouterLink :to="favoriteLink(item)"><small>{{ item.type === 'TEAM' ? 'Команда' : 'Игрок' }}</small><strong>{{ item.name }}</strong></RouterLink>
              <button type="button" aria-label="Удалить из избранного" @click="deleteFavorite(item)">×</button>
            </div>
          </div>
          <p v-else class="muted-text">Добавляйте команды и игроков из их профилей.</p>
        </article>

        <article class="card account-panel">
          <span class="account-kicker">Доступ</span>
          <h2 class="section-title">Роли и команды</h2>
          <div class="account-access-list">
            <div v-for="role in roleAnnotations" :key="role.code" class="account-access-item">
              <strong>{{ role.title }}</strong>
              <p>{{ role.description }}</p>
            </div>
            <div v-if="!roleAnnotations.length" class="account-access-item">
              <p>Дополнительные роли не назначены.</p>
            </div>
          </div>
          <div v-if="teamScopes.length" class="account-team-list">
            <article v-for="scope in teamScopes" :key="scope.teamId" class="account-team">
              <strong>{{ scope.teamName }}</strong>
              <span>{{ permissionText(scope) }}</span>
            </article>
          </div>
          <div class="account-shortcuts">
            <router-link v-if="canOpenTeamDashboard" class="btn-primary" to="/team-rep-dashboard">Кабинет команды</router-link>
            <router-link v-if="canOpenTransfers" class="btn-ghost" to="/team-rep-transfers">Трансферы команды</router-link>
          </div>
        </article>

        <article class="card account-panel account-danger">
          <h2 class="section-title">Активные сеансы</h2>
          <p class="muted-text">Завершит сеансы на всех устройствах, включая текущее.</p>
          <p v-if="sessionError" class="error-text">{{ sessionError }}</p>
          <button class="btn-danger" type="button" :disabled="sessionClosing" @click="closeAllSessions">
            {{ sessionClosing ? 'Завершаем...' : 'Выйти на всех устройствах' }}
          </button>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'

const router = useRouter()
const {
  user, loadAccount, updateProfile, changePassword, logoutAllDevices,
  loadNotificationSettings, updateNotificationSettings, hasRole, hasTeamAccess,
  loadFavorites, removeFavorite,
} = useAuth()
const loading = ref(true)
const loadError = ref('')
const profileSaving = ref(false)
const profileError = ref('')
const profileOk = ref('')
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordOk = ref('')
const sessionClosing = ref(false)
const sessionError = ref('')
const notificationSettings = ref([])
const notificationSaving = ref(false)
const notificationError = ref('')
const notificationOk = ref('')
const favorites = ref([])

const NOTIFICATION_CATEGORIES = {
  MATCHES: { title: 'Матчи', description: 'Расписание, результаты и изменения матчей' },
  TOURNAMENTS: { title: 'Турниры', description: 'Сезоны, туры и заявки на соревнования' },
  TRANSFERS: { title: 'Трансферы', description: 'Заявки и решения по переходам' },
  DISCIPLINE: { title: 'Дисциплина', description: 'Карточки и дисквалификации' },
  NEWS: { title: 'Новости лиги', description: 'Объявления организаторов' },
}

const profileForm = reactive({ name: '', email: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const roleAnnotations = computed(() => user.value?.roleAnnotations || [])
const teamScopes = computed(() => user.value?.teamScopes || [])
const canOpenTeamDashboard = computed(() => hasRole('SUPER_ADMIN') || hasTeamAccess())
const canOpenTransfers = computed(() => hasRole('SUPER_ADMIN') || hasTeamAccess('canEditApplication'))

function syncForm() {
  profileForm.name = user.value?.name || ''
  profileForm.email = user.value?.email || ''
}

function permissionText(scope) {
  const permissions = []
  if (scope.canEditRoster) permissions.push('состав')
  if (scope.canEditApplication) permissions.push('заявки и трансферы')
  return permissions.length ? `Доступ: ${permissions.join(', ')}` : 'Только просмотр'
}

function notificationCategory(category) {
  return NOTIFICATION_CATEGORIES[category] || { title: category, description: '' }
}

function favoriteLink(item) {
  return item.type === 'TEAM' ? '/teams/' + item.targetId : '/players/' + item.targetId
}

async function deleteFavorite(item) {
  await removeFavorite(item.type, item.targetId)
  favorites.value = favorites.value.filter((value) => (
    value.type !== item.type || value.targetId !== item.targetId
  ))
}

async function saveNotificationSettings() {
  notificationSaving.value = true
  notificationError.value = ''
  notificationOk.value = ''
  try {
    notificationSettings.value = await updateNotificationSettings(notificationSettings.value)
    notificationOk.value = 'Настройки уведомлений сохранены.'
  } catch (error) {
    notificationError.value = error.message || 'Не удалось сохранить настройки уведомлений.'
  } finally {
    notificationSaving.value = false
  }
}

async function saveProfile() {
  profileSaving.value = true
  profileError.value = ''
  profileOk.value = ''
  try {
    await updateProfile(profileForm)
    syncForm()
    profileOk.value = 'Данные профиля обновлены.'
  } catch (error) {
    profileError.value = error.message || 'Не удалось обновить профиль.'
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  passwordError.value = ''
  passwordOk.value = ''
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = 'Новый пароль и его повтор должны совпадать.'
    return
  }
  passwordSaving.value = true
  try {
    await changePassword(passwordForm)
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordOk.value = 'Пароль обновлён. Остальные сеансы завершены.'
  } catch (error) {
    passwordError.value = error.message || 'Не удалось обновить пароль.'
  } finally {
    passwordSaving.value = false
  }
}

async function closeAllSessions() {
  sessionClosing.value = true
  sessionError.value = ''
  try {
    await logoutAllDevices()
    await router.replace('/')
  } catch (error) {
    sessionError.value = error.message || 'Не удалось завершить сеансы.'
  } finally {
    sessionClosing.value = false
  }
}

onMounted(async () => {
  try {
    const [, settings, favoriteItems] = await Promise.all([
      loadAccount(), loadNotificationSettings(), loadFavorites(),
    ])
    notificationSettings.value = settings
    favorites.value = favoriteItems
    syncForm()
  } catch (error) {
    loadError.value = error.message || 'Не удалось загрузить профиль.'
  } finally {
    loading.value = false
  }
})
</script>
