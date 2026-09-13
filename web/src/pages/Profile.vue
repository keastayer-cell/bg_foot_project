<template>
  <section class="section-wrap account-page">
    <article class="catalog-header account-hero">
      <div class="account-identity">
        <div class="account-avatar" aria-hidden="true">{{ initials }}</div>
        <div><p class="catalog-kicker">Личный кабинет</p><h1>{{ user?.name || 'Профиль пользователя' }}</h1><p class="account-email">{{ user?.email }}</p></div>
      </div>
      <div v-if="roleAnnotations.length" class="account-role-chips">
        <span v-for="role in roleAnnotations" :key="role.code" class="account-role-chip">{{ role.userTitle }}</span>
      </div>
    </article>

    <div v-if="loading" class="card account-state">Загрузка профиля...</div>
    <p v-else-if="loadError" class="card error-text account-state">{{ loadError }}</p>

    <div v-else class="account-layout">
      <nav class="account-navigation" aria-label="Разделы личного кабинета">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ 'is-active': activeSection === tab.key }" :aria-current="activeSection === tab.key ? 'page' : undefined" :aria-controls="`account-${tab.key}`" @click="activeSection = tab.key"><span>{{ tab.label }}</span><small>{{ tab.description }}</small></button>
      </nav>
      <div class="account-content">
      <div class="account-main">
        <article v-show="activeSection === 'profile'" id="account-profile" class="card account-panel">
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

        <article v-show="activeSection === 'notifications'" id="account-notifications" class="card account-panel">
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
            <div v-for="setting in notificationSettings" :key="setting.category" class="notification-setting-row">
              <span>
                <strong>{{ notificationCategory(setting.category).title }}</strong>
                <small>{{ notificationCategory(setting.category).description }}</small>
              </span>
              <input v-model="setting.bellEnabled" type="checkbox" :aria-label="`${notificationCategory(setting.category).title}: колокольчик`" />
              <input v-model="setting.emailEnabled" type="checkbox" :aria-label="`${notificationCategory(setting.category).title}: email`" />
            </div>
          </div>
          <p v-if="notificationError" class="error-text">{{ notificationError }}</p>
          <p v-if="notificationOk" class="success-text">{{ notificationOk }}</p>
          <div class="actions-row">
            <button class="btn-primary" type="button" :disabled="notificationSaving" @click="saveNotificationSettings">
              {{ notificationSaving ? 'Сохраняем...' : 'Сохранить настройки' }}
            </button>
          </div>
        </article>

        <article v-show="activeSection === 'security'" id="account-security" class="card account-panel">
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
        <article v-show="activeSection === 'favorites'" id="account-favorites" class="card account-panel">
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

        <article v-show="activeSection === 'access'" id="account-access" class="card account-panel">
          <span class="account-kicker">Доступ</span>
          <h2 class="section-title">Роли и команды</h2>
          <div class="account-access-list">
            <div v-for="role in roleAnnotations" :key="role.code" class="account-access-item">
              <strong>{{ role.userTitle }}</strong>
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

        <article v-show="activeSection === 'security'" class="card account-panel account-danger">
          <h2 class="section-title">Активные сеансы</h2>
          <p class="muted-text">Завершит сеансы на всех устройствах, включая текущее.</p>
          <p v-if="sessionError" class="error-text">{{ sessionError }}</p>
          <button class="btn-danger" type="button" :disabled="sessionClosing" @click="closeAllSessions">
            {{ sessionClosing ? 'Завершаем...' : 'Выйти на всех устройствах' }}
          </button>
        </article>
      </aside>
      </div>
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
const activeSection = ref('profile')
const tabs = [
  { key: 'profile', label: 'Мои данные', description: 'Имя и email' },
  { key: 'access', label: 'Роли и команды', description: 'Права и рабочие разделы' },
  { key: 'notifications', label: 'Уведомления', description: 'Колокольчик и почта' },
  { key: 'favorites', label: 'Избранное', description: 'Команды и игроки' },
  { key: 'security', label: 'Безопасность', description: 'Пароль и сеансы' },
]
const initials = computed(() => (user.value?.name || '').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase())
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

<style scoped>
.account-page{max-width:1280px;margin-inline:auto;gap:20px}.account-hero{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:24px 28px}.account-identity{display:flex;align-items:center;gap:18px;min-width:0}.account-avatar{display:grid;place-items:center;width:64px;height:64px;flex-shrink:0;border:1px solid rgba(97,232,162,.25);border-radius:16px;background:rgba(97,232,162,.08);color:var(--brand);font-size:1.4rem;font-weight:800}.account-hero h1{margin:5px 0;font-size:clamp(1.3rem,2.5vw,1.9rem);line-height:1.2;overflow-wrap:anywhere}.account-email{margin:0;color:var(--muted);font-size:.85rem;overflow-wrap:anywhere}.account-role-chips{justify-content:flex-end}.account-role-chip{font-size:.75rem;padding:7px 11px}.account-layout{display:grid;grid-template-columns:250px minmax(0,1fr);gap:20px;align-items:start}.account-navigation{display:grid;gap:6px;padding:8px;border:1px solid var(--line);border-radius:14px;background:rgba(10,16,37,.6)}.account-navigation button{display:grid;gap:5px;padding:14px;text-align:left;border:1px solid transparent;border-radius:9px;background:transparent;color:var(--muted);cursor:pointer}.account-navigation button span{font-size:.88rem;font-weight:800}.account-navigation button small{font-size:.7rem;font-weight:400}.account-navigation button:hover{background:rgba(124,163,255,.06);color:var(--text)}.account-navigation button.is-active{border-color:rgba(97,232,162,.26);background:rgba(97,232,162,.09);color:var(--brand)}.account-content{display:grid;gap:18px;min-width:0}.account-main,.account-side{display:contents}.account-panel{padding:24px}.account-panel-head{margin-bottom:22px;padding-bottom:16px}.account-panel .section-title{font-size:1.2rem;margin:5px 0 0}.account-kicker{font-size:.65rem}.account-form{max-width:640px;gap:18px}.account-form label{font-size:.82rem;gap:8px}.account-form input{font-weight:400}.account-form .actions-row{justify-content:flex-start;margin-top:6px}.account-panel>.muted-text{font-size:.82rem;line-height:1.6;max-width:700px;margin:16px 0}.notification-setting-head,.notification-setting-row{grid-template-columns:minmax(0,1fr) 110px 80px;gap:12px}.notification-setting-row{padding:16px;border-radius:0;border:0;border-bottom:1px solid var(--line);background:transparent}.notification-setting-row strong{font-size:.85rem}.notification-setting-row small{font-size:.75rem;line-height:1.5;margin-top:4px}.notification-setting-row input{accent-color:var(--brand);width:18px;height:18px;justify-self:center}.notification-settings{border:1px solid var(--line);border-radius:10px;overflow:hidden;gap:0}.notification-setting-head{padding:12px 16px;background:rgba(124,163,255,.05);font-size:.65rem}.account-shortcuts{display:flex;flex-wrap:wrap;margin-top:20px}.account-team{padding:16px}.account-access-item strong{font-size:.9rem}.account-access-item p{line-height:1.6}.account-danger{border-color:rgba(255,99,113,.22)}.account-danger button{margin-top:8px}.account-favorites>div{padding:14px 16px}.account-favorites strong{font-size:.9rem}.account-favorites button{width:32px;height:32px}
@media(max-width:760px){.account-layout{grid-template-columns:1fr}.account-navigation{grid-template-columns:repeat(2,minmax(0,1fr))}.account-navigation button{padding:11px}.account-navigation button small{display:none}.account-hero{padding:20px;align-items:flex-start;flex-direction:column;gap:14px}.account-role-chips{justify-content:flex-start}.account-panel{padding:20px}.notification-setting-head,.notification-setting-row{grid-template-columns:minmax(0,1fr) 85px 55px;gap:8px}.notification-setting-row{padding:12px}.notification-setting-head{padding:12px}.notification-setting-row small{font-size:.68rem}}
</style>
