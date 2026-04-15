<template>
  <section class="section-wrap admin-hub">
    <article class="card admin-hub-header">
      <h2 class="section-title">Админ-панель</h2>
      <p class="muted-text">Структурированные разделы администрирования.</p>
    </article>

    <article class="card admin-tabs-wrap">
      <div class="admin-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="btn-ghost admin-tab-btn"
          :class="{ 'admin-tab-active': activeTab === tab.id }"
          type="button"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'seasons'">
      <h3 class="section-title">Управление сезонами</h3>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': seasonSubMode === 'create' }"
          type="button"
          @click="seasonSubMode = 'create'; cancelEditSeason(); seasonEditSelectId = ''"
        >Создать сезон</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': seasonSubMode === 'edit' }"
          type="button"
          @click="seasonSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <div class="admin-form">
        <form v-if="seasonSubMode === 'create'" class="admin-form" @submit.prevent="createSeason">
          <label>
            Название сезона
            <input v-model.trim="seasonForm.name" type="text" placeholder="Например: 2026/27" required />
          </label>
          <label>
            Количество кругов
            <select v-model="seasonForm.roundsCount">
              <option value="1">1 круг</option>
              <option value="2">2 круга</option>
              <option value="3">3 круга</option>
              <option value="4">4 круга</option>
            </select>
          </label>
          <label class="admin-checkbox-row">
            <input v-model="seasonForm.playoffEnabled" type="checkbox" />
            <span>Включить плей-офф</span>
          </label>
          <label v-if="seasonForm.playoffEnabled">
            Команд в плей-офф
            <select v-model="seasonForm.playoffTeamCount">
              <option value="">— выберите —</option>
              <option v-for="count in playoffTeamOptions" :key="`create-playoff-${count}`" :value="String(count)">{{ count }}</option>
            </select>
          </label>
          <div>
            <p class="muted-text">Команды сезона</p>
            <div class="actions-row">
              <select v-model="seasonTeamToAddId">
                <option value="">— выберите команду —</option>
                <option v-for="team in seasonAvailableTeams" :key="`season-create-team-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
              </select>
              <button class="btn-ghost" type="button" @click="addSeasonTeamToForm">Добавить команду</button>
            </div>
            <p v-if="!seasonSelectedTeams.length" class="muted-text">Пока не выбрано ни одной команды.</p>
            <div v-else class="admin-list-items">
              <article v-for="team in seasonSelectedTeams" :key="`season-create-selected-${team.id}`" class="admin-list-item">
                <label class="admin-inline-check">
                  <strong>{{ team.name }}</strong>
                  <input type="checkbox" @change="removeSeasonTeamFromForm(team.id)" />
                </label>
              </article>
            </div>
            <p class="muted-text">Регулярный этап: {{ seasonRegularToursCount }} туров при {{ seasonSelectedTeams.length }} командах.</p>
            <p v-if="seasonForm.playoffEnabled && seasonForm.playoffTeamCount" class="muted-text">Плей-офф включен: {{ seasonForm.playoffTeamCount }} команд. Сетка будет добавлена следующим этапом.</p>
          </div>
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать сезон</button>
          </div>
        </form>

        <div v-else class="admin-form">
          <label>
            Выберите сезон
            <select v-model="seasonEditSelectId" @change="onSeasonSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
            </select>
          </label>
          <template v-if="editingSeasonId">
            <label>
              Название сезона
              <input v-model.trim="seasonForm.name" type="text" />
            </label>
            <label>
              Количество кругов
              <select v-model="seasonForm.roundsCount">
                <option value="1">1 круг</option>
                <option value="2">2 круга</option>
                <option value="3">3 круга</option>
                <option value="4">4 круга</option>
              </select>
            </label>
            <label class="admin-checkbox-row">
              <input v-model="seasonForm.playoffEnabled" type="checkbox" />
              <span>Включить плей-офф</span>
            </label>
            <label v-if="seasonForm.playoffEnabled">
              Команд в плей-офф
              <select v-model="seasonForm.playoffTeamCount">
                <option value="">— выберите —</option>
                <option v-for="count in playoffTeamOptions" :key="`edit-playoff-${count}`" :value="String(count)">{{ count }}</option>
              </select>
            </label>
            <div>
              <p class="muted-text">Команды сезона</p>
              <div class="actions-row">
                <select v-model="seasonTeamToAddId">
                  <option value="">— выберите команду —</option>
                  <option v-for="team in seasonAvailableTeams" :key="`season-edit-team-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
                </select>
                <button class="btn-ghost" type="button" @click="addSeasonTeamToForm">Добавить команду</button>
              </div>
              <p v-if="!seasonSelectedTeams.length" class="muted-text">Пока не выбрано ни одной команды.</p>
              <div v-else class="admin-list-items">
                <article v-for="team in seasonSelectedTeams" :key="`season-edit-selected-${team.id}`" class="admin-list-item">
                  <label class="admin-inline-check">
                    <strong>{{ team.name }}</strong>
                    <input type="checkbox" @change="removeSeasonTeamFromForm(team.id)" />
                  </label>
                </article>
              </div>
            </div>
            <p class="muted-text">Регулярный этап: {{ seasonRegularToursCount }} туров при {{ seasonSelectedTeams.length }} командах.</p>
            <p v-if="seasonForm.playoffEnabled && seasonForm.playoffTeamCount" class="muted-text">Плей-офф включен: {{ seasonForm.playoffTeamCount }} команд. Сетка будет добавлена следующим этапом.</p>
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveEditSeason">Сохранить изменения</button>
              <button class="btn-danger" type="button" @click="deactivateSeason(editingSeasonId)">Удалить сезон</button>
              <button class="btn-ghost" type="button" @click="cancelEditSeason(); seasonEditSelectId = ''">Отмена</button>
            </div>
          </template>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'teams'">
      <h3 class="section-title">Управление командами</h3>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': teamSubMode === 'create' }"
          type="button"
          @click="teamSubMode = 'create'; cancelEditTeam(); teamEditSelectId = ''"
        >Создать команду</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': teamSubMode === 'edit' }"
          type="button"
          @click="teamSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <form v-if="teamSubMode === 'create'" class="admin-form" @submit.prevent="createTeam">
        <label>
          Название команды
          <input v-model.trim="teamForm.name" type="text" required />
        </label>
        <label>
          Короткое название
          <input v-model.trim="teamForm.shortName" type="text" required />
        </label>
        <label>
          Город
          <input v-model.trim="teamForm.city" type="text" required />
        </label>
        <label>
          Лого команды
          <input type="file" accept="image/*" @change="onTeamLogoSelected" />
        </label>
        <img v-if="teamForm.logoDataUrl" :src="teamForm.logoDataUrl" alt="Превью лого команды" class="team-rep-player-photo-preview" />
        <div class="actions-row">
          <button class="btn-primary" type="submit">Создать команду</button>
        </div>
      </form>

      <div v-else class="admin-form">
        <label>
          Выберите команду
          <select v-model="teamEditSelectId" @change="onTeamSelectChange">
            <option value="">— выберите —</option>
            <option v-for="t in teamsList" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
          </select>
        </label>
        <template v-if="editingTeamId">
          <label>
            Название команды
            <input v-model.trim="teamForm.name" type="text" />
          </label>
          <label>
            Короткое название
            <input v-model.trim="teamForm.shortName" type="text" />
          </label>
          <label>
            Город
            <input v-model.trim="teamForm.city" type="text" />
          </label>
          <label>
            Лого команды
            <input type="file" accept="image/*" @change="onTeamLogoSelected" />
          </label>
          <img v-if="teamForm.logoDataUrl" :src="teamForm.logoDataUrl" alt="Превью лого команды" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="button" @click="saveEditTeam">Сохранить изменения</button>
            <button class="btn-danger" type="button" @click="deactivateTeam(editingTeamId)">Удалить команду</button>
            <button class="btn-ghost" type="button" @click="cancelEditTeam(); teamEditSelectId = ''">Отмена</button>
          </div>
        </template>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'tours'">
      <h3 class="section-title">Управление турами</h3>

      <div class="admin-form">
        <label>
          Сезон
          <select v-model="tourSeasonId" @change="onTourSeasonChange">
            <option value="">— выберите —</option>
            <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
        <p v-if="selectedTourSeason" class="muted-text">
          Регулярный этап рассчитан автоматически: {{ selectedTourSeason.regularToursCount }} туров, {{ selectedTourSeason.roundsCount }} круг(а), {{ tourTeamsList.length }} команд.
        </p>
        <p v-if="selectedTourSeason?.playoffEnabled" class="muted-text">
          Плей-офф включен на {{ selectedTourSeason.playoffTeamCount }} команд. Наполнение сетки будет добавлено следующим этапом.
        </p>
      </div>

      <div class="admin-form" v-if="tourSeasonId">
        <label>
          Выберите тур
          <select v-model="selectedTourId" @change="onTourSelectChange">
            <option value="">— выберите —</option>
            <option v-for="tour in toursList" :key="tour.id" :value="String(tour.id)">{{ tour.name }}</option>
          </select>
        </label>
        <p v-if="!toursList.length" class="muted-text">Для выбранного сезона туры еще не сформированы. Проверьте состав команд и количество кругов.</p>
      </div>

      <div v-if="selectedTour" class="admin-grid">
        <form class="admin-form" @submit.prevent="createTourMatch">
          <h4 class="admin-list-title">Добавить матч в тур {{ selectedTour.name }}</h4>
          <label>
            Команда 1
            <select v-model="matchForm.homeTeamId">
              <option value="">— выберите —</option>
              <option v-for="team in tourTeamsList" :key="`home-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <label>
            Команда 2
            <select v-model="matchForm.awayTeamId">
              <option value="">— выберите —</option>
              <option v-for="team in tourTeamsList" :key="`away-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <label>
            Время матча
            <input v-model="matchForm.kickoffAt" type="datetime-local" />
          </label>
          <div class="actions-row">
            <button class="btn-primary" type="submit">Добавить матч</button>
          </div>
          <p class="muted-text">Публично на сайт попадут только опубликованные туры.</p>
        </form>

        <div class="admin-list">
          <div class="tour-matches-header">
            <h4 class="admin-list-title">Матчи тура</h4>
            <button class="btn-ghost tour-publish-button" type="button" :disabled="!canPublishSelectedTour" @click="publishSelectedTour">
              {{ selectedTour.published ? 'Опубликован' : 'Опубликовать тур' }}
            </button>
          </div>
          <p v-if="!tourMatchesList.length" class="muted-text">В этом туре пока нет матчей.</p>
          <div class="admin-list-items" v-else>
            <article class="admin-list-item tour-match-item" v-for="match in tourMatchesList" :key="match.id">
              <div class="tour-match-copy">
                <strong>{{ match.homeTeamName }} - {{ match.awayTeamName }}</strong>
                <span class="muted-text">{{ formatDateTime(match.kickoffAt) }}</span>
              </div>
              <button class="btn-danger btn-sm" type="button" @click="deleteTourMatch(match.id)">Удалить</button>
            </article>
          </div>
          <p class="muted-text tour-publish-note">Публично на сайт попадут только опубликованные туры.</p>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'players'">
      <h3 class="section-title">Управление игроками</h3>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': playerSubMode === 'create' }"
          type="button"
          @click="playerSubMode = 'create'; cancelEditPlayer(); playerEditSelectId = ''"
        >Создать игрока</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': playerSubMode === 'edit' }"
          type="button"
          @click="playerSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <div class="admin-grid">
        <form v-if="playerSubMode === 'create'" class="admin-form" @submit.prevent="createPlayer">
          <label>
            ФИО
            <input v-model.trim="playerForm.fullName" type="text" required />
          </label>
          <label>
            Дата рождения
            <input v-model="playerForm.birthDate" type="date" required />
          </label>
          <label>
            Прописка
            <input v-model.trim="playerForm.residence" type="text" placeholder="Город/деревня" required />
          </label>
          <label>
            Фото игрока
            <input type="file" accept="image/*" @change="onPlayerPhotoSelected" />
          </label>
          <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото игрока" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать игрока</button>
          </div>
        </form>

        <div v-else class="admin-form">
          <label>
            Выберите игрока
            <select v-model="playerEditSelectId" @change="onPlayerSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in playersList" :key="item.id" :value="String(item.id)">{{ item.fullName }}</option>
            </select>
          </label>
          <template v-if="editingPlayerId">
            <label>
              ФИО
              <input v-model.trim="playerForm.fullName" type="text" />
            </label>
            <label>
              Дата рождения
              <input v-model="playerForm.birthDate" type="date" />
            </label>
            <label>
              Прописка
              <input v-model.trim="playerForm.residence" type="text" placeholder="Город/деревня" />
            </label>
            <label>
              Фото игрока
              <input type="file" accept="image/*" @change="onPlayerPhotoSelected" />
            </label>
            <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото игрока" class="team-rep-player-photo-preview" />
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveEditPlayer">Сохранить изменения</button>
              <button class="btn-danger" type="button" @click="deactivatePlayer(editingPlayerId)">Удалить игрока</button>
              <button class="btn-ghost" type="button" @click="cancelEditPlayer(); playerEditSelectId = ''">Отмена</button>
            </div>
          </template>
        </div>

      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'roles'">
      <h3 class="section-title">Управление ролями</h3>

      <div class="admin-form">
        <label>
          Поиск по email
          <input v-model.trim="rolesSearch" type="text" placeholder="Начните вводить email..." />
        </label>
        <label>
          Выберите пользователя
          <select v-model="rolesSelectedEmail">
            <option value="">— выберите —</option>
            <option v-for="u in filteredUsersForSelect" :key="u.email" :value="u.email">{{ u.email }}</option>
          </select>
        </label>
        <p v-if="!roleUsersList.length" class="muted-text">Пользователи не найдены.</p>
        <div class="actions-row">
          <button class="btn-primary" type="button" @click="findUserForRoles">Найти</button>
        </div>
      </div>

      <div v-if="rolesFoundUser" class="admin-found-user">
        <p class="admin-found-email">{{ rolesFoundUser.email }}</p>
        <p class="muted-text" v-if="rolesFoundUser.name">{{ rolesFoundUser.name }}</p>
        <p class="muted-text">
          Требуется смена пароля: {{ rolesFoundUser.mustChangePassword ? 'да' : 'нет' }}
        </p>
        <p v-if="!rolesFoundUser.roles.length" class="muted-text">Ролей нет.</p>

        <div class="actions-row">
          <button class="btn-danger btn-sm" type="button" @click="resetPasswordForFoundUser">Сбросить пароль</button>
        </div>

        <article v-if="passwordResetResult" class="card admin-reset-password-card">
          <p><strong>Временный пароль:</strong> {{ passwordResetResult.temporaryPassword }}</p>
          <p class="muted-text">
            Передайте этот пароль пользователю вручную. После входа система сразу потребует задать новый пароль.
          </p>
        </article>

        <div v-for="role in rolesFoundUser.roles" :key="role" class="admin-role-manage-row">
          <span class="admin-role-badge">{{ role }}</span>
          <template v-if="replaceRoleTarget === role">
            <select v-model="replaceRoleNewCode" class="admin-role-select-inline">
              <option value="USER">USER</option>
              <option value="TEAM_REP">TEAM_REP</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
            </select>
            <button class="btn-primary btn-sm" type="button" @click="confirmReplaceRole">Подтвердить</button>
            <button class="btn-ghost btn-sm" type="button" @click="replaceRoleTarget = ''">Отмена</button>
          </template>
          <template v-else>
            <button class="btn-ghost btn-sm" type="button" @click="startReplaceRole(role)">Заменить</button>
            <button class="btn-danger btn-sm" type="button" @click="removeRoleFromFound(role)">Снять</button>
          </template>
        </div>

        <div class="admin-add-role-row">
          <select v-model="assignRoleCode" class="admin-role-select-inline">
            <option value="USER">USER</option>
            <option value="TEAM_REP">TEAM_REP</option>
            <option value="SUPER_ADMIN">SUPER_ADMIN</option>
          </select>
          <button class="btn-ghost btn-sm" type="button" @click="assignRoleToFound">+ Добавить роль</button>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'representatives'">
      <h3 class="section-title">Управление Представителями</h3>

      <div class="admin-form">
        <label>
          Поиск по email
          <input v-model.trim="repSearch" type="text" placeholder="Начните вводить email представителя..." />
        </label>
        <label>
          Выберите представителя
          <select v-model="repSelectedEmail">
            <option value="">— выберите —</option>
            <option v-for="u in filteredRepresentativeUsersForSelect" :key="u.email" :value="String(u.email || '').toLowerCase()">{{ u.email }}</option>
          </select>
        </label>
        <p v-if="!repUsersList.length" class="muted-text">Представители команды не найдены.</p>
        <div class="actions-row">
          <button class="btn-primary" type="button" @click="findRepresentative">Найти</button>
        </div>
      </div>

      <div v-if="repFoundUser" class="admin-found-user">
        <p class="admin-found-email">{{ repFoundUser.email }}</p>
        <p class="muted-text" v-if="repFoundUser.name">{{ repFoundUser.name }}</p>
        <p class="muted-text">Текущая команда: {{ repCurrentTeamScope?.teamName || 'не назначена' }}</p>
        <p class="muted-text" v-if="repHasMultipleTeamScopes">Найдено несколько активных привязок. При сохранении старые привязки будут сняты.</p>

        <label>
          Назначить команду
          <select v-model="repSelectedTeamId">
            <option value="">— выберите —</option>
            <option v-for="team in teamsList" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
          </select>
        </label>

        <div class="actions-row">
          <button class="btn-primary" type="button" @click="saveRepresentativeTeam">{{ repPrimaryActionLabel }}</button>
          <button class="btn-danger" type="button" :disabled="!repCurrentTeamScope" @click="unassignRepresentativeTeam">Открепить команду</button>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'ban'">
      <h3 class="section-title">Забанить пользователя</h3>
      <div class="admin-grid">
        <form class="admin-form" @submit.prevent="banUser">
          <label>
            Email пользователя
            <input v-model.trim="banForm.email" type="email" required />
          </label>
          <label>
            Причина
            <input v-model.trim="banForm.reason" type="text" placeholder="Нарушение правил" required />
          </label>
          <div class="actions-row">
            <button class="btn-danger" type="submit">Забанить</button>
          </div>
        </form>

        <div class="admin-list">
          <h4 class="admin-list-title">Статус пользователей</h4>
          <p v-if="!usersRegistry.length" class="muted-text">Пока пусто.</p>
          <div class="admin-list-items" v-else>
            <article class="admin-list-item" v-for="item in usersRegistry" :key="`ban-${item.email}`">
              <strong>{{ item.email }}</strong>
              <span class="muted-text" v-if="item.banned">Заблокирован: {{ item.banReason || '-' }}</span>
              <span class="success-text" v-else>Активен</span>
              <div class="actions-row" v-if="item.banned">
                <button class="btn-ghost" type="button" @click="unbanUser(item.email)">Разбанить</button>
              </div>
            </article>
          </div>
        </div>
      </div>
    </article>

    <article class="card" v-if="messageError || messageOk">
      <p class="error-text" v-if="messageError">{{ messageError }}</p>
      <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useAuth } from '../store/auth'
import { useStore } from '../store/store'

const USERS_KEY = 'football_stats_admin_users_registry'

const tabs = [
  { id: 'seasons', label: 'Управление сезонами' },
  { id: 'teams', label: 'Управление командами' },
  { id: 'tours', label: 'Управление турами' },
  { id: 'players', label: 'Управление игроками' },
  { id: 'roles', label: 'Управление ролями' },
  { id: 'representatives', label: 'Управление Представителями' },
  { id: 'ban', label: 'Бан пользователя' },
]

const activeTab = ref('seasons')

const { authorizedApiRequest } = useAuth()
const { loadSeasons } = useStore()

const seasonsList = ref([])
const teamsList = ref([])
const tourTeamsList = ref([])
const toursList = ref([])
const tourMatchesList = ref([])
const playersList = ref([])
const usersRegistry = ref(loadFromStorage(USERS_KEY))
const roleUsersList = ref([])
const repUsersList = ref([])

const messageError = ref('')
const messageOk = ref('')

const playoffTeamOptions = [4, 8, 16]

const seasonForm = reactive({
  name: '',
  roundsCount: '1',
  playoffEnabled: false,
  playoffTeamCount: '',
})

const matchForm = reactive({
  homeTeamId: '',
  awayTeamId: '',
  kickoffAt: '',
})

const teamForm = reactive({
  name: '',
  shortName: '',
  city: '',
  logoDataUrl: '',
})

const playerForm = reactive({
  fullName: '',
  birthDate: '',
  residence: '',
  photoDataUrl: '',
})

const editingSeasonId = ref(null)
const seasonSubMode = ref('create')
const seasonEditSelectId = ref('')
const seasonTeamIds = ref([])
const seasonTeamToAddId = ref('')
const editingTeamId = ref(null)
const teamSubMode = ref('create')
const teamEditSelectId = ref('')
const tourSeasonId = ref('')
const selectedTourId = ref('')
const editingPlayerId = ref(null)
const playerSubMode = ref('create')
const playerEditSelectId = ref('')

const rolesSearch = ref('')
const rolesSelectedEmail = ref('')
const rolesFoundEmail = ref('')
const replaceRoleTarget = ref('')
const replaceRoleNewCode = ref('USER')
const assignRoleCode = ref('USER')
const passwordResetResult = ref(null)
const rolesSearchDebounceMs = 5000
let rolesSearchDebounceTimer = null

const repSearch = ref('')
const repSelectedEmail = ref('')
const repFoundEmail = ref('')
const repSelectedTeamId = ref('')
const repUserAccess = ref(null)
const repSearchDebounceMs = 5000
let repSearchDebounceTimer = null

const roleForm = reactive({
  email: '',
  roleCode: 'USER',
})

const banForm = reactive({
  email: '',
  reason: '',
})

const userByEmail = computed(() => {
  const map = new Map()
  for (const item of usersRegistry.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const roleUserByEmail = computed(() => {
  const map = new Map()
  for (const item of roleUsersList.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const repUserByEmail = computed(() => {
  const map = new Map()
  for (const item of repUsersList.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const filteredUsersForSelect = computed(() => {
  const q = rolesSearch.value.toLowerCase()
  if (!q) return roleUsersList.value
  return roleUsersList.value.filter((u) => String(u.email || '').toLowerCase().includes(q))
})

const rolesFoundUser = computed(() => {
  if (!rolesFoundEmail.value) return null
  return roleUserByEmail.value.get(rolesFoundEmail.value) || null
})

const filteredRepresentativeUsersForSelect = computed(() => {
  const query = String(repSearch.value || '').toLowerCase()
  if (!query) return repUsersList.value
  return repUsersList.value.filter((user) => String(user.email || '').toLowerCase().includes(query))
})

const repFoundUser = computed(() => {
  if (!repFoundEmail.value) return null
  return repUserByEmail.value.get(repFoundEmail.value) || null
})

const repTeamScopes = computed(() => {
  return Array.isArray(repUserAccess.value?.teamScopes) ? repUserAccess.value.teamScopes : []
})

const repCurrentTeamScope = computed(() => {
  return repTeamScopes.value[0] || null
})

const repHasMultipleTeamScopes = computed(() => repTeamScopes.value.length > 1)

const repPrimaryActionLabel = computed(() => {
  return repCurrentTeamScope.value ? 'Изменить команду' : 'Назначить команду'
})

const seasonSelectedTeams = computed(() => {
  const selectedIds = new Set(seasonTeamIds.value.map((id) => Number(id)))
  const teamsById = new Map(teamsList.value.map((team) => [Number(team.id), team]))

  return seasonTeamIds.value
    .map((id) => teamsById.get(Number(id)))
    .filter(Boolean)
    .filter((team, index, array) => array.findIndex((item) => Number(item.id) === Number(team.id)) === index)
    .filter((team) => selectedIds.has(Number(team.id)))
})

const seasonAvailableTeams = computed(() => {
  const selectedIds = new Set(seasonTeamIds.value.map((id) => Number(id)))
  return teamsList.value.filter((team) => !selectedIds.has(Number(team.id)))
})

const seasonRegularToursCount = computed(() => {
  return calculateRegularToursCount(seasonSelectedTeams.value.length, Number(seasonForm.roundsCount || 1))
})

const selectedTourSeason = computed(() => {
  return seasonsList.value.find((season) => String(season.id) === String(tourSeasonId.value)) || null
})

const selectedTour = computed(() => {
  return toursList.value.find((tour) => String(tour.id) === String(selectedTourId.value)) || null
})

const canPublishSelectedTour = computed(() => {
  return Boolean(selectedTour.value) && !selectedTour.value.published && tourMatchesList.value.length > 0
})

watch(rolesSearch, (rawValue) => {
  if (rolesSearchDebounceTimer) {
    clearTimeout(rolesSearchDebounceTimer)
    rolesSearchDebounceTimer = null
  }

  if (activeTab.value !== 'roles') {
    return
  }

  const emailFilter = String(rawValue || '').trim()
  if (!emailFilter) {
    void loadRoleUsers({ pagenum: 0, pagesize: 20 })
    rolesSelectedEmail.value = ''
    rolesFoundEmail.value = ''
    return
  }

  rolesSearchDebounceTimer = setTimeout(async () => {
    await loadRoleUsers({ email: emailFilter, pagenum: 0, pagesize: 50 })
    if (roleUsersList.value.length === 1) {
      const email = String(roleUsersList.value[0].email || '').toLowerCase()
      rolesSelectedEmail.value = email
      rolesFoundEmail.value = email
      replaceRoleTarget.value = ''
    }
  }, rolesSearchDebounceMs)
})

watch(repSearch, (rawValue) => {
  if (repSearchDebounceTimer) {
    clearTimeout(repSearchDebounceTimer)
    repSearchDebounceTimer = null
  }

  if (activeTab.value !== 'representatives') {
    return
  }

  const emailFilter = String(rawValue || '').trim()
  if (!emailFilter) {
    void loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
    repSelectedEmail.value = ''
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  repSearchDebounceTimer = setTimeout(async () => {
    await loadRepresentativeUsers({ email: emailFilter, pagenum: 0, pagesize: 50 })
    if (repUsersList.value.length === 1) {
      const email = String(repUsersList.value[0].email || '').toLowerCase()
      repSelectedEmail.value = email
      await refreshRepresentativeAccessByEmail(email)
    }
  }, repSearchDebounceMs)
})

watch(repSelectedEmail, (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  if (!normalized) {
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  void refreshRepresentativeAccessByEmail(normalized)
})

watch(activeTab, (tabId) => {
  if (tabId === 'representatives' && !repUsersList.value.length) {
    void loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
  }
  if (tabId === 'tours' && !tourSeasonId.value && seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    void onTourSeasonChange()
  }
})

onBeforeUnmount(() => {
  if (rolesSearchDebounceTimer) {
    clearTimeout(rolesSearchDebounceTimer)
    rolesSearchDebounceTimer = null
  }
  if (repSearchDebounceTimer) {
    clearTimeout(repSearchDebounceTimer)
    repSearchDebounceTimer = null
  }
})

function resetMessages() {
  messageError.value = ''
  messageOk.value = ''
  passwordResetResult.value = null
}

function loadFromStorage(key) {
  const raw = localStorage.getItem(key)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function saveToStorage(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

function ensureUser(email) {
  const normalized = String(email || '').trim().toLowerCase()
  if (!normalized) return null

  let record = userByEmail.value.get(normalized)
  if (record) return record

  record = {
    email: normalized,
    roles: [],
    banned: false,
    banReason: '',
    updatedAt: new Date().toISOString(),
  }
  usersRegistry.value.push(record)
  return record
}

async function createSeason() {
  resetMessages()

  if (!seasonForm.name) {
    messageError.value = 'Заполните название сезона.'
    return
  }

  if (!seasonTeamIds.value.length) {
    messageError.value = 'Выберите хотя бы одну команду для сезона.'
    return
  }

  try {
    const createdSeason = await authorizedApiRequest('/api/seasons', {
      method: 'POST',
      body: JSON.stringify(buildSeasonPayload()),
    })
    await authorizedApiRequest(`/api/seasons/${createdSeason.id}/teams`, {
      method: 'PUT',
      body: JSON.stringify({ teamIds: seasonTeamIds.value }),
    })
    await loadSeasonRegistry()
    await loadSeasons()
    if (String(tourSeasonId.value || '') === String(createdSeason.id)) {
      await onTourSeasonChange()
    }
    resetSeasonForm()
    messageOk.value = 'Сезон создан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать сезон.'
  }
}

async function startEditSeason(item) {
  editingSeasonId.value = item.id
  seasonForm.name = item.name
  seasonForm.roundsCount = String(item.roundsCount || 1)
  seasonForm.playoffEnabled = Boolean(item.playoffEnabled)
  seasonForm.playoffTeamCount = item.playoffTeamCount ? String(item.playoffTeamCount) : ''
  seasonTeamIds.value = await loadSeasonTeams(item.id)
  resetMessages()
}

function cancelEditSeason() {
  editingSeasonId.value = null
  resetSeasonForm()
  resetMessages()
}

function addSeasonTeamToForm() {
  resetMessages()

  const teamId = Number(seasonTeamToAddId.value)
  if (!Number.isFinite(teamId) || teamId <= 0) {
    messageError.value = 'Сначала выберите команду из списка.'
    return
  }

  if (seasonTeamIds.value.some((id) => Number(id) === teamId)) {
    messageError.value = 'Эта команда уже добавлена в сезон.'
    return
  }

  seasonTeamIds.value = [...seasonTeamIds.value, teamId]
  seasonTeamToAddId.value = ''
}

function removeSeasonTeamFromForm(teamId) {
  seasonTeamIds.value = seasonTeamIds.value.filter((id) => Number(id) !== Number(teamId))
  if (!seasonAvailableTeams.value.length) {
    seasonTeamToAddId.value = ''
  }
}

async function saveEditSeason() {
  resetMessages()

  if (!seasonForm.name) {
    messageError.value = 'Заполните название сезона.'
    return
  }

  if (!seasonTeamIds.value.length) {
    messageError.value = 'Выберите хотя бы одну команду для сезона.'
    return
  }

  try {
    await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}`, {
      method: 'PUT',
      body: JSON.stringify(buildSeasonPayload()),
    })
    await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}/teams`, {
      method: 'PUT',
      body: JSON.stringify({ teamIds: seasonTeamIds.value }),
    })
    await loadSeasonRegistry()
    await loadSeasons()
    if (String(tourSeasonId.value || '') === String(editingSeasonId.value)) {
      await onTourSeasonChange()
    }
    cancelEditSeason()
    messageOk.value = 'Сезон обновлен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить сезон.'
  }
}

async function onSeasonSelectChange() {
  if (!seasonEditSelectId.value) {
    cancelEditSeason()
    return
  }
  const item = seasonsList.value.find((seasonItem) => String(seasonItem.id) === seasonEditSelectId.value)
  if (item) {
    await startEditSeason(item)
  }
}

async function deactivateSeason(seasonId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/seasons/${seasonId}`, {
      method: 'DELETE',
    })
    if (String(editingSeasonId.value || '') === String(seasonId)) {
      cancelEditSeason()
      seasonEditSelectId.value = ''
    }
    await loadSeasonRegistry()
    await loadSeasons()
    messageOk.value = 'Сезон деактивирован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить сезон.'
  }
}

async function loadSeasonRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/seasons?active_flag=1', {
      method: 'GET',
    })
    seasonsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    seasonsList.value = []
    messageError.value = error.message || 'Не удалось загрузить сезоны.'
  }
}

async function loadSeasonTeams(seasonId) {
  try {
    const payload = await authorizedApiRequest(`/api/seasons/${seasonId}/teams`, {
      method: 'GET',
    })
    return Array.isArray(payload) ? payload.map((team) => team.id) : []
  } catch (error) {
    messageError.value = error.message || 'Не удалось загрузить команды сезона.'
    return []
  }
}

function resetSeasonForm() {
  seasonForm.name = ''
  seasonForm.roundsCount = '1'
  seasonForm.playoffEnabled = false
  seasonForm.playoffTeamCount = ''
  seasonTeamIds.value = []
  seasonTeamToAddId.value = ''
}

async function createTeam() {
  resetMessages()

  if (!teamForm.name || !teamForm.shortName || !teamForm.city) {
    messageError.value = 'Заполните все поля команды.'
    return
  }

  try {
    await authorizedApiRequest('/api/teams', {
      method: 'POST',
      body: JSON.stringify({
        name: teamForm.name,
        shortName: teamForm.shortName,
        city: teamForm.city,
        logoDataUrl: teamForm.logoDataUrl,
      }),
    })
    await loadTeamRegistry()
    resetTeamForm()
    messageOk.value = 'Команда создана.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать команду.'
  }
}

function startEditTeam(item) {
  editingTeamId.value = item.id
  teamForm.name = item.name
  teamForm.shortName = item.shortName
  teamForm.city = item.city
  teamForm.logoDataUrl = item.logoDataUrl || ''
  resetMessages()
}

function cancelEditTeam() {
  editingTeamId.value = null
  resetTeamForm()
  resetMessages()
}

async function saveEditTeam() {
  resetMessages()

  if (!teamForm.name || !teamForm.shortName || !teamForm.city) {
    messageError.value = 'Заполните все поля команды.'
    return
  }

  try {
    await authorizedApiRequest(`/api/teams/${editingTeamId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: teamForm.name,
        shortName: teamForm.shortName,
        city: teamForm.city,
        logoDataUrl: teamForm.logoDataUrl,
      }),
    })
    await loadTeamRegistry()
    cancelEditTeam()
    messageOk.value = 'Команда обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить команду.'
  }
}

function onTeamSelectChange() {
  if (!teamEditSelectId.value) {
    cancelEditTeam()
    return
  }
  const item = teamsList.value.find((t) => String(t.id) === teamEditSelectId.value)
  if (item) startEditTeam(item)
}

async function deactivateTeam(teamId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/teams/${teamId}`, {
      method: 'DELETE',
    })
    if (String(editingTeamId.value || '') === String(teamId)) {
      cancelEditTeam()
      teamEditSelectId.value = ''
    }
    await loadTeamRegistry()
    messageOk.value = 'Команда деактивирована.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить команду.'
  }
}

function onTeamLogoSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    teamForm.logoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function resetTeamForm() {
  teamForm.name = ''
  teamForm.shortName = ''
  teamForm.city = ''
  teamForm.logoDataUrl = ''
}

async function loadTeamRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/teams?active_flag=1', {
      method: 'GET',
    })
    teamsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamsList.value = []
    messageError.value = error.message || 'Не удалось загрузить команды.'
  }
}

async function onTourSeasonChange() {
  resetMessages()
  selectedTourId.value = ''
  tourMatchesList.value = []
  resetMatchForm()

  if (!tourSeasonId.value) {
    toursList.value = []
    tourTeamsList.value = []
    return
  }

  await Promise.all([loadTours(), loadTeamsForTourSeason()])
}

async function loadTours() {
  if (!tourSeasonId.value) {
    toursList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/tours?season_id=${encodeURIComponent(tourSeasonId.value)}&active_flag=1`, {
      method: 'GET',
    })
    toursList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    toursList.value = []
    messageError.value = error.message || 'Не удалось загрузить туры.'
  }
}

async function loadTeamsForTourSeason() {
  if (!tourSeasonId.value) {
    tourTeamsList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/teams?active_flag=1&season_id=${encodeURIComponent(tourSeasonId.value)}`, {
      method: 'GET',
    })
    tourTeamsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    tourTeamsList.value = []
    messageError.value = error.message || 'Не удалось загрузить команды сезона для туров.'
  }
}

async function onTourSelectChange() {
  resetMessages()
  resetMatchForm()

  if (!selectedTourId.value) {
    tourMatchesList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches?active_flag=1`, {
      method: 'GET',
    })
    tourMatchesList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    tourMatchesList.value = []
    messageError.value = error.message || 'Не удалось загрузить матчи тура.'
  }
}

async function publishSelectedTour() {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  if (!tourMatchesList.value.length) {
    messageError.value = 'Нельзя публиковать пустой тур без матчей.'
    return
  }
  if (selectedTour.value?.published) {
    messageError.value = 'Этот тур уже опубликован.'
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/publish`, {
      method: 'PUT',
    })
    await Promise.all([loadTours(), onTourSelectChange()])
    messageOk.value = 'Тур опубликован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось опубликовать тур.'
  }
}

async function createTourMatch() {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  if (!matchForm.homeTeamId || !matchForm.awayTeamId) {
    messageError.value = 'Выберите обе команды матча.'
    return
  }
  if (String(matchForm.homeTeamId) === String(matchForm.awayTeamId)) {
    messageError.value = 'Команды матча должны отличаться.'
    return
  }
  if (!matchForm.kickoffAt) {
    messageError.value = 'Укажите дату и время матча.'
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches`, {
      method: 'POST',
      body: JSON.stringify({
        homeTeamId: Number(matchForm.homeTeamId),
        awayTeamId: Number(matchForm.awayTeamId),
        kickoffAt: new Date(matchForm.kickoffAt).toISOString(),
      }),
    })
    await onTourSelectChange()
    resetMatchForm()
    messageOk.value = 'Матч добавлен в тур.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить матч.'
  }
}

async function deleteTourMatch(matchId) {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  if (!window.confirm('Удалить матч из тура без возможности восстановления?')) {
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches/${matchId}`, {
      method: 'DELETE',
    })
    await Promise.all([loadTours(), onTourSelectChange()])
    messageOk.value = 'Матч удален из тура.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить матч из тура.'
  }
}

function resetMatchForm() {
  matchForm.homeTeamId = ''
  matchForm.awayTeamId = ''
  matchForm.kickoffAt = ''
}

function buildSeasonPayload() {
  const roundsCount = Number(seasonForm.roundsCount || 1)
  const playoffEnabled = Boolean(seasonForm.playoffEnabled)
  const playoffTeamCount = playoffEnabled && seasonForm.playoffTeamCount
    ? Number(seasonForm.playoffTeamCount)
    : null

  return {
    name: seasonForm.name,
    roundsCount,
    playoffEnabled,
    playoffTeamCount,
  }
}

function calculateRegularToursCount(teamCount, roundsCount) {
  const normalizedTeamCount = Number(teamCount || 0)
  const normalizedRoundsCount = Number(roundsCount || 0)
  if (normalizedTeamCount < 2 || normalizedRoundsCount < 1) {
    return 0
  }
  const toursPerRound = normalizedTeamCount % 2 === 0 ? normalizedTeamCount - 1 : normalizedTeamCount
  return toursPerRound * normalizedRoundsCount
}

async function createPlayer() {
  resetMessages()

  if (!playerForm.fullName || !playerForm.birthDate || !playerForm.residence) {
    messageError.value = 'Заполните все поля игрока.'
    return
  }

  try {
    await authorizedApiRequest('/api/players', {
      method: 'POST',
      body: JSON.stringify({
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate,
        residence: playerForm.residence,
        photoDataUrl: playerForm.photoDataUrl,
      }),
    })
    await loadPlayerRegistry()
    resetPlayerForm()
    messageOk.value = 'Игрок создан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать игрока.'
  }
}

function startEditPlayer(item) {
  editingPlayerId.value = item.id
  playerForm.fullName = item.fullName
  playerForm.birthDate = item.birthDate
  playerForm.residence = item.residence
  playerForm.photoDataUrl = item.photoDataUrl || ''
  resetMessages()
}

function cancelEditPlayer() {
  editingPlayerId.value = null
  resetPlayerForm()
  resetMessages()
}

async function saveEditPlayer() {
  resetMessages()

  if (!playerForm.fullName) {
    messageError.value = 'Укажите ФИО игрока.'
    return
  }

  try {
    await authorizedApiRequest(`/api/players/${editingPlayerId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate,
        residence: playerForm.residence,
        photoDataUrl: playerForm.photoDataUrl,
      }),
    })
    await loadPlayerRegistry()
    cancelEditPlayer()
    messageOk.value = 'Игрок обновлен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить игрока.'
  }
}

function onPlayerSelectChange() {
  if (!playerEditSelectId.value) {
    cancelEditPlayer()
    return
  }
  const item = playersList.value.find((playerItem) => String(playerItem.id) === playerEditSelectId.value)
  if (item) startEditPlayer(item)
}

async function deactivatePlayer(playerId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/players/${playerId}`, {
      method: 'DELETE',
    })
    if (String(editingPlayerId.value || '') === String(playerId)) {
      cancelEditPlayer()
      playerEditSelectId.value = ''
    }
    await loadPlayerRegistry()
    messageOk.value = 'Игрок деактивирован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить игрока.'
  }
}

async function loadPlayerRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/players?active_flag=1&pagenum=0&pagesize=500', {
      method: 'GET',
    })
    playersList.value = Array.isArray(payload?.content) ? payload.content : []
  } catch (error) {
    playersList.value = []
    messageError.value = error.message || 'Не удалось загрузить игроков.'
  }
}

function onPlayerPhotoSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    playerForm.photoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function resetPlayerForm() {
  playerForm.fullName = ''
  playerForm.birthDate = ''
  playerForm.residence = ''
  playerForm.photoDataUrl = ''
}

function assignRole() {
  resetMessages()

  const email = String(roleForm.email || '').trim().toLowerCase()
  if (!email) {
    messageError.value = 'Укажите email пользователя.'
    return
  }

  const user = ensureUser(email)
  if (!user) {
    messageError.value = 'Не удалось подготовить профиль пользователя.'
    return
  }

  if (!user.roles.includes(roleForm.roleCode)) {
    user.roles.push(roleForm.roleCode)
  }
  user.updatedAt = new Date().toISOString()

  saveToStorage(USERS_KEY, usersRegistry.value)

  roleForm.email = ''
  roleForm.roleCode = 'USER'
  messageOk.value = 'Роль назначена.'
}

function banUser() {
  resetMessages()

  const email = String(banForm.email || '').trim().toLowerCase()
  const reason = String(banForm.reason || '').trim()

  if (!email || !reason) {
    messageError.value = 'Укажите email и причину блокировки.'
    return
  }

  const user = ensureUser(email)
  if (!user) {
    messageError.value = 'Не удалось подготовить профиль пользователя.'
    return
  }

  user.banned = true
  user.banReason = reason
  user.updatedAt = new Date().toISOString()

  saveToStorage(USERS_KEY, usersRegistry.value)

  banForm.email = ''
  banForm.reason = ''
  messageOk.value = 'Пользователь заблокирован.'
}

function removeRole(email, roleCode) {
  resetMessages()
  const user = userByEmail.value.get(String(email || '').trim().toLowerCase())
  if (!user) return

  user.roles = user.roles.filter((r) => r !== roleCode)
  user.updatedAt = new Date().toISOString()
  saveToStorage(USERS_KEY, usersRegistry.value)
  messageOk.value = `Роль ${roleCode} снята.`
}

function findUserForRoles() {
  resetMessages()
  const emailFilter = String(rolesSearch.value || rolesSelectedEmail.value || '').trim()
  if (!emailFilter) {
    messageError.value = 'Введите email или выберите пользователя.'
    return
  }

  void loadRoleUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(() => {
    const selected = String(rolesSelectedEmail.value || '').trim().toLowerCase()
    if (selected) {
      rolesFoundEmail.value = selected
      replaceRoleTarget.value = ''
      return
    }

    if (roleUsersList.value.length === 1) {
      const onlyEmail = String(roleUsersList.value[0].email || '').toLowerCase()
      rolesSelectedEmail.value = onlyEmail
      rolesFoundEmail.value = onlyEmail
      replaceRoleTarget.value = ''
      return
    }

    if (!roleUsersList.value.length) {
      messageError.value = 'Пользователь не найден.'
      rolesFoundEmail.value = ''
      return
    }

    messageError.value = 'Выберите пользователя из найденного списка.'
  })
}

function startReplaceRole(role) {
  replaceRoleTarget.value = role
  replaceRoleNewCode.value = 'USER'
}

async function confirmReplaceRole() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${replaceRoleTarget.value}`, {
      method: 'DELETE',
    })
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${replaceRoleNewCode.value}`, {
      method: 'POST',
    })
    await refreshFoundUserAccess()
    replaceRoleTarget.value = ''
    messageOk.value = 'Роль заменена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось заменить роль.'
  }
}

async function removeRoleFromFound(role) {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${role}`, {
      method: 'DELETE',
    })
    await refreshFoundUserAccess()
    messageOk.value = 'Роль снята.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось снять роль.'
  }
}

async function assignRoleToFound() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return
  if (user.roles.includes(assignRoleCode.value)) {
    messageError.value = 'Такая роль уже назначена.'
    return
  }

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${assignRoleCode.value}`, {
      method: 'POST',
    })
    await refreshFoundUserAccess()
    messageOk.value = 'Роль добавлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить роль.'
  }
}

async function resetPasswordForFoundUser() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    const payload = await authorizedApiRequest(`/api/admin/access/users/${user.id}/reset-password`, {
      method: 'POST',
    })
    passwordResetResult.value = payload
    await refreshFoundUserAccess()
    messageOk.value = 'Пароль пользователя сброшен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось сбросить пароль пользователя.'
  }
}

async function loadRoleUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
  try {
    const search = new URLSearchParams()
    search.set('pagenum', String(Math.max(0, pagenum)))
    search.set('pagesize', String(Math.min(Math.max(1, pagesize), 100)))
    if (String(name || '').trim()) search.set('name', String(name).trim())
    if (String(email || '').trim()) search.set('email', String(email).trim())

    const payload = await authorizedApiRequest(`/api/admin/access/users?${search.toString()}`, {
      method: 'GET',
    })

    roleUsersList.value = Array.isArray(payload?.content) ? payload.content : []
    if (rolesSelectedEmail.value && !roleUserByEmail.value.has(String(rolesSelectedEmail.value).toLowerCase())) {
      rolesSelectedEmail.value = ''
    }
    if (rolesFoundEmail.value && !roleUserByEmail.value.has(String(rolesFoundEmail.value).toLowerCase())) {
      rolesFoundEmail.value = ''
    }
  } catch (error) {
    roleUsersList.value = []
    messageError.value = error.message || 'Не удалось загрузить список пользователей.'
  }
}

async function loadRepresentativeUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
  try {
    const search = new URLSearchParams()
    search.set('pagenum', String(Math.max(0, pagenum)))
    search.set('pagesize', String(Math.min(Math.max(1, pagesize), 100)))
    search.set('role', 'TEAM_REP')
    if (String(name || '').trim()) search.set('name', String(name).trim())
    if (String(email || '').trim()) search.set('email', String(email).trim())

    const payload = await authorizedApiRequest(`/api/admin/access/users?${search.toString()}`, {
      method: 'GET',
    })

    repUsersList.value = Array.isArray(payload?.content) ? payload.content : []

    const selectedEmail = String(repSelectedEmail.value || '').toLowerCase()
    if (selectedEmail && !repUserByEmail.value.has(selectedEmail)) {
      repSelectedEmail.value = ''
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
    }
  } catch (error) {
    repUsersList.value = []
    messageError.value = error.message || 'Не удалось загрузить представителей команд.'
  }
}

async function refreshRepresentativeAccessByEmail(email) {
  const user = repUserByEmail.value.get(String(email || '').toLowerCase())
  if (!user) {
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  await refreshRepresentativeAccessById(user.id)
}

async function refreshRepresentativeAccessById(userId) {
  const payload = await authorizedApiRequest(`/api/admin/access/users/${userId}`, {
    method: 'GET',
  })

  repUserAccess.value = payload
  const normalizedEmail = String(payload?.email || '').toLowerCase()
  repFoundEmail.value = normalizedEmail
  repSelectedEmail.value = normalizedEmail
  repSelectedTeamId.value = payload?.teamScopes?.[0]?.teamId ? String(payload.teamScopes[0].teamId) : ''

  const index = repUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
  if (index >= 0) {
    repUsersList.value[index] = {
      ...repUsersList.value[index],
      id: payload.userId,
      email: payload.email,
      name: payload.name,
      roles: Array.isArray(payload.roles) ? payload.roles : [],
    }
  }
}

function findRepresentative() {
  resetMessages()
  const emailFilter = String(repSearch.value || repSelectedEmail.value || '').trim()
  if (!emailFilter) {
    messageError.value = 'Введите email или выберите представителя.'
    return
  }

  void loadRepresentativeUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(async () => {
    const selected = String(repSelectedEmail.value || '').trim().toLowerCase()
    if (selected) {
      await refreshRepresentativeAccessByEmail(selected)
      return
    }

    if (repUsersList.value.length === 1) {
      const onlyEmail = String(repUsersList.value[0].email || '').toLowerCase()
      await refreshRepresentativeAccessByEmail(onlyEmail)
      return
    }

    if (!repUsersList.value.length) {
      messageError.value = 'Представитель не найден.'
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
      return
    }

    messageError.value = 'Выберите представителя из найденного списка.'
  })
}

async function saveRepresentativeTeam() {
  resetMessages()
  const user = repFoundUser.value
  if (!user) {
    messageError.value = 'Сначала выберите представителя.'
    return
  }

  const nextTeamId = Number(repSelectedTeamId.value)
  if (!Number.isFinite(nextTeamId) || nextTeamId <= 0) {
    messageError.value = 'Выберите команду.'
    return
  }

  const currentScopes = repTeamScopes.value
  if (currentScopes.length === 1 && Number(currentScopes[0].teamId) === nextTeamId) {
    messageError.value = 'Эта команда уже назначена представителю.'
    return
  }

  try {
    for (const scope of currentScopes) {
      await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, {
        method: 'DELETE',
      })
    }

    await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes`, {
      method: 'POST',
      body: JSON.stringify({
        teamId: nextTeamId,
        canEditRoster: true,
        canEditApplication: true,
      }),
    })

    await refreshRepresentativeAccessById(user.id)
    messageOk.value = currentScopes.length ? 'Команда представителя обновлена.' : 'Команда представителю назначена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось назначить команду представителю.'
  }
}

async function unassignRepresentativeTeam() {
  resetMessages()
  const user = repFoundUser.value
  if (!user) {
    messageError.value = 'Сначала выберите представителя.'
    return
  }

  if (!repTeamScopes.value.length) {
    messageError.value = 'У представителя нет активной привязки к команде.'
    return
  }

  try {
    for (const scope of repTeamScopes.value) {
      await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, {
        method: 'DELETE',
      })
    }

    await refreshRepresentativeAccessById(user.id)
    repSelectedTeamId.value = ''
    messageOk.value = 'Команда откреплена от представителя.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось открепить команду.'
  }
}

async function refreshFoundUserAccess() {
  const user = rolesFoundUser.value
  if (!user) return

  const payload = await authorizedApiRequest(`/api/admin/access/users/${user.id}`, {
    method: 'GET',
  })

  const normalizedEmail = String(payload?.email || '').toLowerCase()
  const index = roleUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
  if (index >= 0) {
    roleUsersList.value[index] = {
      ...roleUsersList.value[index],
      id: payload.userId,
      email: payload.email,
      name: payload.name,
      roles: Array.isArray(payload.roles) ? payload.roles : [],
      mustChangePassword: Boolean(payload.mustChangePassword),
    }
  }
}

function unbanUser(email) {
  resetMessages()
  const user = userByEmail.value.get(String(email || '').trim().toLowerCase())
  if (!user) return

  user.banned = false
  user.banReason = ''
  user.updatedAt = new Date().toISOString()
  saveToStorage(USERS_KEY, usersRegistry.value)
  messageOk.value = 'Пользователь разблокирован.'
}

function formatDateOnly(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

onMounted(async () => {
  await loadSeasonRegistry()
  await loadTeamRegistry()
  await loadPlayerRegistry()
  await loadRoleUsers({ pagenum: 0, pagesize: 20 })
  await loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
  await loadSeasons()
  if (seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    await onTourSeasonChange()
  }
})
</script>

<style scoped>
.admin-checkbox-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-inline-check {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.admin-inline-check input[type='checkbox'] {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: #d63b57;
  cursor: pointer;
}

.tour-matches-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.tour-match-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.tour-match-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tour-publish-note {
  margin-top: 16px;
}

.tour-publish-button {
  flex: 0 0 auto;
  min-width: 230px;
}

@media (max-width: 900px) {
  .tour-matches-header {
    align-items: stretch;
    flex-direction: column;
  }

  .tour-match-item {
    align-items: stretch;
    flex-direction: column;
  }

  .tour-publish-button {
    width: 100%;
  }
}

.tour-publish-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
 </style>
