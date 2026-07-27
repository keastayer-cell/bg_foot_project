<template>
  <section class="section-wrap admin-hub">
    <article class="card admin-hub-header">
      <h2 class="section-title">Админ-панель</h2>
      <p class="muted-text">Управление турниром, участниками и правами доступа из одного экрана.</p>
    </article>

    <AdminTabNavigation
      :groups="visibleTabGroups"
      :active-tab="activeTab"
      @select="selectAdminTab"
    />

    <AdminSeasonsPanel
      v-if="activeTab === 'seasons'"
      :panel="seasonPanel"
    />


    <article class="card admin-panel" v-if="activeTab === 'teams'">
      <div class="admin-panel-head">
        <h3 class="section-title">Команды и составы</h3>
        <p class="muted-text">Карточка команды, текущий состав и сезонная заявка игроков.</p>
      </div>
      <div class="admin-inline-message" v-if="messageError || messageOk">
        <p class="error-text" v-if="messageError">{{ messageError }}</p>
        <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
      </div>
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

      <form v-if="teamSubMode === 'create'" class="admin-form admin-surface" @submit.prevent="createTeam">
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
        <img v-if="teamForm.logoDataUrl" :src="teamForm.logoDataUrl" alt="Превью лого команды" class="admin-team-logo-preview" />
        <div class="actions-row">
          <button class="btn-primary" type="submit">Создать команду</button>
        </div>
      </form>

      <div v-else class="admin-form admin-surface">
        <label>
          Выберите команду
          <select v-model="teamEditSelectId" @change="onTeamSelectChange">
            <option value="">— выберите —</option>
            <option v-for="t in teamsList" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
          </select>
        </label>
        <template v-if="editingTeamId">
          <div class="admin-sticky-actions-spacer"></div>
          <div class="admin-team-editor-shell">
            <section class="admin-surface admin-team-identity-card">
              <div class="admin-team-identity-head">
                <div>
                  <h4 class="admin-list-title">Карточка команды</h4>
                  <p class="muted-text">Базовые поля и быстрые показатели в одном компактном блоке.</p>
                </div>
                <div class="admin-team-summary-grid">
                  <article class="admin-team-summary-card">
                    <span class="admin-team-summary-label">В составе</span>
                    <strong>{{ teamRoster.length }}</strong>
                  </article>
                  <article class="admin-team-summary-card">
                    <span class="admin-team-summary-label">Сезонов</span>
                    <strong>{{ teamSeasonOptions.length }}</strong>
                  </article>
                  <article class="admin-team-summary-card" :class="{ 'is-accent': selectedTeamSeasonId }">
                    <span class="admin-team-summary-label">В заявке</span>
                    <strong>{{ selectedTeamSeasonId ? teamSeasonSelectedPlayers.length : '—' }}</strong>
                  </article>
                </div>
              </div>

              <div class="admin-team-identity-grid">
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
                <label class="admin-team-logo-field">
                  Лого команды
                  <input type="file" accept="image/*" @change="onTeamLogoSelected" />
                </label>
                <div v-if="teamForm.logoDataUrl" class="admin-team-logo-preview-wrap">
                  <img :src="teamForm.logoDataUrl" alt="Превью лого команды" class="admin-team-logo-preview" />
                </div>
              </div>
            </section>

            <div class="admin-team-management-grid">
            <section class="admin-list admin-team-management-card">
              <div class="toolbar admin-team-management-head">
                <div>
                  <h4 class="admin-list-title">Состав команды</h4>
                  <p class="muted-text">Массовое добавление в команду и быстрый контроль текущего состава.</p>
                </div>
                <div class="actions-row admin-team-head-actions">
                  <button class="btn-ghost btn-sm" type="button" @click="toggleTeamRosterVisibility">
                    {{ isTeamRosterVisible ? 'Скрыть состав' : 'Показать весь состав' }}
                  </button>
                  <button class="btn-ghost btn-sm" type="button" @click="refreshAdminTeamContext">Обновить</button>
                </div>
              </div>

              <div class="actions-row admin-team-picker-row admin-team-management-toolbar">
                <SearchableSelect
                  :key="`team-roster-multi-${editingTeamId}-${teamRosterAddOptions.length}`"
                  v-model="teamRosterToAddIds"
                  :options="teamRosterAddOptions"
                  multiple
                  multiple-summary-text="Выбрано игроков"
                  placeholder="Выберите игроков в состав"
                  search-placeholder="Начните вводить ФИО игрока"
                  empty-text="Игрок по такому ФИО не найден"
                />
                <div class="admin-team-picker-side">
                  <span class="admin-team-picker-count">Выбрано: {{ teamRosterToAddIds.length }}</span>
                  <button class="btn-primary btn-sm" type="button" @click="addPlayerToEditingTeam" :disabled="teamRosterBusy || !teamRosterToAddIds.length">
                    Добавить выбранных
                  </button>
                </div>
              </div>

              <p v-if="!isTeamRosterVisible" class="muted-text">Состав скрыт. Нажмите «Показать весь состав», если нужен полный список игроков.</p>
              <p v-else-if="!teamRoster.length" class="muted-text">В текущем составе команды пока нет игроков.</p>
              <div v-else class="admin-list-items">
                <article v-for="player in teamRoster" :key="`team-roster-${player.id}`" class="admin-list-item admin-player-manage-item">
                  <div class="admin-player-manage-copy">
                    <strong>{{ player.fullName }}<span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
                    <span class="muted-text">В команде с {{ formatDateOnly(player.inTeamSince) }}</span>
                  </div>
                  <button class="btn-danger btn-sm" type="button" @click="removePlayerFromEditingTeam(player.id)" :disabled="teamRosterBusy">
                    Удалить из состава
                  </button>
                </article>
              </div>
            </section>

            <section class="admin-list admin-team-management-card">
              <div class="toolbar admin-team-management-head">
                <div>
                  <h4 class="admin-list-title">Заявка на сезон</h4>
                  <p class="muted-text">Выбери сезон, затем массово добавь или сними игроков без длинного списка.</p>
                </div>
              </div>

              <div class="admin-team-management-toolbar admin-team-management-toolbar-spacer" aria-hidden="true"></div>

              <label class="admin-team-season-select-field">
                Выберите сезон
                <select v-model="selectedTeamSeasonId" @change="onAdminTeamSeasonChange">
                  <option value="">— выберите —</option>
                  <option v-for="season in teamSeasonOptions" :key="`team-season-${season.id}`" :value="String(season.id)">
                    {{ season.name }}
                  </option>
                </select>
              </label>

              <p v-if="!teamSeasonOptions.length" class="muted-text">Эта команда пока не включена ни в один активный сезон.</p>
              <p v-else-if="!selectedTeamSeasonId" class="muted-text">Выберите сезон, чтобы управлять заявкой команды.</p>
              <p v-else-if="!teamSeasonPlayers.length" class="muted-text">В составе команды пока нет игроков для управления заявкой сезона.</p>

              <div v-else class="admin-team-season-tools">
                <div class="admin-team-season-summary">
                  <span class="admin-season-player-badge is-selected">В заявке: {{ teamSeasonSelectedPlayers.length }}</span>
                  <span class="admin-season-player-badge is-not-selected">Доступно: {{ teamSeasonAvailablePlayers.length }}</span>
                  <span v-if="teamSeasonMaxRosterSize" class="admin-season-player-badge">Лимит: {{ teamSeasonMaxRosterSize }}</span>
                  <span v-if="teamSeasonMaxRosterSize" class="admin-season-player-badge" :class="teamSeasonRemainingSlots > 0 ? 'is-not-selected' : 'is-selected'">
                    Осталось мест: {{ teamSeasonRemainingSlots }}
                  </span>
                </div>

                <div class="admin-team-season-action-block">
                  <label class="admin-team-season-control">
                    Добавить игроков в сезон
                    <SearchableSelect
                      :key="`team-season-add-${selectedTeamSeasonId}-${teamSeasonAddOptions.length}`"
                      v-model="teamSeasonToAddIds"
                      :options="teamSeasonAddOptions"
                      multiple
                      multiple-summary-text="Выбрано игроков"
                      placeholder="Выберите игроков для заявки"
                      search-placeholder="Начните вводить ФИО игрока"
                      empty-text="Нет доступных игроков для добавления"
                      :disabled="teamSeasonBusy || !teamSeasonAddOptions.length || isTeamSeasonAtLimit"
                    />
                  </label>
                  <div class="admin-team-picker-side admin-team-picker-side-inline">
                    <span class="admin-team-picker-count">Выбрано: {{ teamSeasonToAddIds.length }}</span>
                    <button class="btn-primary btn-sm" type="button" @click="addSelectedPlayersToSeason" :disabled="teamSeasonBusy || !teamSeasonToAddIds.length || willSelectedPlayersExceedSeasonLimit">
                      Добавить выбранных
                    </button>
                  </div>
                  <p v-if="teamSeasonMaxRosterSize && isTeamSeasonAtLimit" class="muted-text">
                    Лимит заявки уже достигнут. Сначала уберите кого-то из сезона.
                  </p>
                  <p v-else-if="willSelectedPlayersExceedSeasonLimit" class="error-text">
                    Нельзя добавить {{ teamSeasonToAddIds.length }} игрок(ов): будет превышен лимит {{ teamSeasonMaxRosterSize }}.
                  </p>
                </div>

                <div class="admin-team-season-action-block" v-if="teamSeasonRemoveOptions.length">
                  <label class="admin-team-season-control">
                    Убрать игроков из сезона
                    <SearchableSelect
                      :key="`team-season-remove-${selectedTeamSeasonId}-${teamSeasonRemoveOptions.length}`"
                      v-model="teamSeasonToRemoveIds"
                      :options="teamSeasonRemoveOptions"
                      multiple
                      multiple-summary-text="Выбрано игроков"
                      placeholder="Выберите игроков для удаления"
                      search-placeholder="Начните вводить ФИО игрока"
                      empty-text="В заявке пока нет игроков"
                      :disabled="teamSeasonBusy"
                    />
                  </label>
                  <div class="admin-team-picker-side admin-team-picker-side-inline">
                    <span class="admin-team-picker-count">Выбрано: {{ teamSeasonToRemoveIds.length }}</span>
                    <button class="btn-danger btn-sm" type="button" @click="removeSelectedPlayersFromSeason" :disabled="teamSeasonBusy || !teamSeasonToRemoveIds.length">
                      Убрать выбранных
                    </button>
                  </div>
                </div>
              </div>
            </section>
            </div>
          </div>

          <div class="actions-row admin-sticky-actions">
            <button class="btn-primary" type="button" @click="saveEditTeam" :disabled="teamSaving">
              {{ teamSaving ? 'Сохраняем...' : 'Сохранить изменения' }}
            </button>
            <button class="btn-danger" type="button" @click="deactivateTeam(editingTeamId)">Удалить команду</button>
            <button class="btn-ghost" type="button" @click="cancelEditTeam(); teamEditSelectId = ''">Отмена</button>
          </div>
        </template>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'tours'">
      <div class="admin-panel-head">
        <h3 class="section-title">Туры и матчи</h3>
        <p class="muted-text">Управление календарём сезона, публикацией туров и матчами внутри тура.</p>
      </div>

      <div class="admin-form admin-surface">
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

      <div class="admin-form admin-surface" v-if="tourSeasonId">
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
        <form class="admin-form admin-surface" @submit.prevent="createTourMatch">
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
              <option v-for="team in availableAwayTeams" :key="`away-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <label>
            Время матча
            <input v-model="matchForm.kickoffAt" type="datetime-local" class="admin-temporal-input admin-temporal-input-wide" step="60" />
          </label>
          <p v-if="selectedTourMatchLimitMessage" class="error-text">{{ selectedTourMatchLimitMessage }}</p>
          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="Boolean(selectedTourMatchLimitMessage)">Добавить матч</button>
          </div>
          <p class="muted-text">Публично на сайт попадут только опубликованные туры.</p>
        </form>

        <div class="admin-list admin-surface">
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
                <span class="muted-text" v-if="tourMatchScoreLabel(match)">{{ tourMatchScoreLabel(match) }}</span>
                <span class="tour-match-status-badge" :class="protocolStatusBadgeClass(match.protocolStatus)">
                  {{ matchProtocolStatusLabel(match.protocolStatus) }}
                </span>
              </div>
              <button
                class="btn-danger btn-sm"
                type="button"
                @click="deleteTourMatch(match.id)"
                :disabled="!canDeleteTourMatch(match)"
                :title="tourMatchDeleteTitle(match)"
              >
                Удалить
              </button>
            </article>
          </div>
          <p class="muted-text tour-publish-note">Публично на сайт попадут только опубликованные туры.</p>
        </div>
      </div>
    </article>

    <AdminPlayersPanel
      v-if="activeTab === 'players'"
      v-model:sub-mode="playerSubMode"
      v-model:edit-select-id="playerEditSelectId"
      :form="playerForm"
      :edit-options="playerEditOptions"
      :editing-id="editingPlayerId"
      @cancel="cancelEditPlayer"
      @create="createPlayer"
      @deactivate="deactivatePlayer"
      @photo-selected="onPlayerPhotoSelected"
      @save="saveEditPlayer"
    />

    <AdminRefereesPanel
      v-if="activeTab === 'referees'"
      v-model:sub-mode="refereeSubMode"
      v-model:edit-select-id="refereeEditSelectId"
      :form="refereeForm"
      :referees="refereesList"
      :editing-id="editingRefereeId"
      @cancel="cancelEditReferee"
      @create="createReferee"
      @deactivate="deactivateReferee"
      @photo-selected="onRefereePhotoSelected"
      @save="saveEditReferee"
      @selection-change="onRefereeSelectChange"
    />

    <AdminLeagueContent
      v-if="activeTab === 'league'"
      :seasons-list="seasonsList"
      @refresh-seasons="handleLeagueSeasonRefresh"
    />

    <AdminRolesPanel
      v-if="activeTab === 'roles'"
      v-model:search="rolesSearch"
      v-model:selected-email="rolesSelectedEmail"
      v-model:replace-target="replaceRoleTarget"
      v-model:replace-code="replaceRoleNewCode"
      v-model:assign-code="assignRoleCode"
      :users="filteredUsersForSelect"
      :has-users="Boolean(roleUsersList.length)"
      :found-user="rolesFoundUser"
      :password-reset-result="passwordResetResult"
      :password-reset-link="absolutePasswordResetLink"
      :format-date-time="formatDateTime"
      @assign-role="assignRoleToFound"
      @confirm-replace="confirmReplaceRole"
      @copy-reset-link="copyPasswordResetLink"
      @find="findUserForRoles"
      @remove-role="removeRoleFromFound"
      @reset-password="resetPasswordForFoundUser"
      @start-replace="startReplaceRole"
    />

    <AdminRepresentativesPanel
      v-if="activeTab === 'representatives'"
      v-model:search="repSearch"
      v-model:selected-email="repSelectedEmail"
      v-model:selected-team-id="repSelectedTeamId"
      :users="filteredRepresentativeUsersForSelect"
      :has-users="Boolean(repUsersList.length)"
      :found-user="repFoundUser"
      :current-team-scope="repCurrentTeamScope"
      :has-multiple-team-scopes="repHasMultipleTeamScopes"
      :teams="teamsList"
      :primary-action-label="repPrimaryActionLabel"
      @find="findRepresentative"
      @save-team="saveRepresentativeTeam"
      @unassign-team="unassignRepresentativeTeam"
    />

    <AdminBanPanel
      v-if="activeTab === 'ban'"
      :form="banForm"
      :users="usersRegistry"
      @ban="banUser"
      @unban="unbanUser"
    />

    <article class="card" v-if="messageError || messageOk">
      <p class="error-text" v-if="messageError">{{ messageError }}</p>
      <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'
import { useStore } from '../store/store'
import { useAdminAccess } from '../composables/useAdminAccess'
import { useAdminPlayers } from '../composables/useAdminPlayers'
import { useAdminReferees } from '../composables/useAdminReferees'
import {
  PLAYOFF_TEAM_OPTIONS,
  TIE_BREAKER_RULE_OPTIONS,
  useAdminSeasonRules,
} from '../composables/useAdminSeasonRules'
import { useAdminTabs } from '../composables/useAdminTabs'
import AdminTabNavigation from '../components/AdminTabNavigation.vue'
import AdminBanPanel from '../components/admin/AdminBanPanel.vue'
import AdminPlayersPanel from '../components/admin/AdminPlayersPanel.vue'
import AdminRefereesPanel from '../components/admin/AdminRefereesPanel.vue'
import AdminRepresentativesPanel from '../components/admin/AdminRepresentativesPanel.vue'
import AdminRolesPanel from '../components/admin/AdminRolesPanel.vue'
import AdminSeasonsPanel from '../components/admin/AdminSeasonsPanel.vue'
import SearchableSelect from '../components/SearchableSelect.vue'
import AdminLeagueContent from '../components/AdminLeagueContent.vue'

const USERS_KEY = 'football_stats_admin_users_registry'
const router = useRouter()

const { authorizedApiRequest, authorizedApiRequestRaw, hasRole } = useAuth()
const { loadSeasons } = useStore()
const { activeTab, visibleTabGroups, selectAdminTab } = useAdminTabs({
  hasRole,
  navigate: (path) => router.push(path),
})

const seasonsList = ref([])
const teamsList = ref([])
const tourTeamsList = ref([])
const toursList = ref([])
const tourMatchesList = ref([])
const seasonTourMatchesList = ref([])
const usersRegistry = ref(loadFromStorage(USERS_KEY))

const messageError = ref('')
const messageOk = ref('')
const {
  cancelEditPlayer,
  createPlayer,
  deactivatePlayer,
  editingPlayerId,
  loadPlayerRegistry,
  onPlayerPhotoSelected,
  playerEditOptions,
  playerEditSelectId,
  playerForm,
  playersList,
  playerSubMode,
  saveEditPlayer,
} = useAdminPlayers({
  request: authorizedApiRequest,
  clearMessages: resetMessages,
  errorMessage: messageError,
  successMessage: messageOk,
})
const {
  cancelEditReferee,
  createReferee,
  deactivateReferee,
  editingRefereeId,
  loadRefereeRegistry,
  onRefereePhotoSelected,
  onRefereeSelectChange,
  refereeEditSelectId,
  refereeForm,
  refereesList,
  refereeSubMode,
  saveEditReferee,
} = useAdminReferees({
  request: authorizedApiRequest,
  clearMessages: resetMessages,
  errorMessage: messageError,
  successMessage: messageOk,
})
const {
  absolutePasswordResetLink,
  assignRoleCode,
  assignRoleToFound,
  confirmReplaceRole,
  copyPasswordResetLink,
  filteredRepresentativeUsersForSelect,
  filteredUsersForSelect,
  findRepresentative,
  findUserForRoles,
  loadRepresentativeUsers,
  loadRoleUsers,
  passwordResetResult,
  removeRoleFromFound,
  repCurrentTeamScope,
  repFoundUser,
  repHasMultipleTeamScopes,
  repPrimaryActionLabel,
  repSearch,
  repSelectedEmail,
  repSelectedTeamId,
  repUsersList,
  replaceRoleNewCode,
  replaceRoleTarget,
  resetPasswordForFoundUser,
  roleUsersList,
  rolesFoundUser,
  rolesSearch,
  rolesSelectedEmail,
  saveRepresentativeTeam,
  startReplaceRole,
  unassignRepresentativeTeam,
} = useAdminAccess({
  activeTab,
  request: authorizedApiRequest,
  clearMessages: resetMessages,
  errorMessage: messageError,
  successMessage: messageOk,
})

const playoffTeamOptions = PLAYOFF_TEAM_OPTIONS
const tieBreakerRuleOptions = TIE_BREAKER_RULE_OPTIONS

const seasonForm = reactive({
  name: '',
  roundsCount: '1',
  playoffEnabled: false,
  playoffTeamCount: '',
  thirdPlaceEnabled: false,
  status: 'DRAFT',
  maxRosterSize: '',
  applicationDeadline: '',
  transferWindowStartDate: '',
  transferWindowEndDate: '',
  rankingRules: ['GOAL_DIFFERENCE', 'GOALS_FOR'],
  yellowCardsForSuspension: '0',
  redCardsForSuspension: '0',
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

const editingSeasonId = ref(null)
const seasonSubMode = ref('create')
const seasonEditSelectId = ref('')
const seasonProtocolMenuOpen = ref(false)
const downloadingSeasonProtocols = ref(false)
const seasonProtocolProgressText = ref('')
const completingRegularSeason = ref(false)
const seasonTeamIds = ref([])
const originalSeasonTeamIds = ref([])
const seasonTeamToAddId = ref('')
const seasonRefereeIds = ref([])
const originalSeasonRefereeIds = ref([])
const seasonRefereeToAddId = ref('')
const editingTeamId = ref(null)
const teamSubMode = ref('create')
const teamEditSelectId = ref('')
const teamRoster = ref([])
const teamSeasonOptions = ref([])
const teamSeasonPlayers = ref([])
const teamRosterToAddIds = ref([])
const selectedTeamSeasonId = ref('')
const teamSeasonToAddIds = ref([])
const teamSeasonToRemoveIds = ref([])
const teamRosterBusy = ref(false)
const teamSeasonBusy = ref(false)
const isTeamRosterVisible = ref(false)
const teamSaving = ref(false)
const tourSeasonId = ref('')
const selectedTourId = ref('')
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

const seasonSelectedReferees = computed(() => {
  const selectedIds = new Set(seasonRefereeIds.value.map((id) => Number(id)))
  const refereesById = new Map(refereesList.value.map((referee) => [Number(referee.id), referee]))

  return seasonRefereeIds.value
    .map((id) => refereesById.get(Number(id)))
    .filter(Boolean)
    .filter((referee, index, array) => array.findIndex((item) => Number(item.id) === Number(referee.id)) === index)
    .filter((referee) => selectedIds.has(Number(referee.id)))
})

const seasonAvailableReferees = computed(() => {
  const selectedIds = new Set(seasonRefereeIds.value.map((id) => Number(id)))
  return refereesList.value.filter((referee) => !selectedIds.has(Number(referee.id)))
})

const teamPlayersAvailableForRoster = computed(() => {
  const rosterIds = new Set(teamRoster.value.map((player) => Number(player.id)))
  const currentEditingTeamId = Number(editingTeamId.value)

  return playersList.value.filter((player) => {
    const playerId = Number(player.id)
    const playerActiveSeasonTeamId = Number(player.activeSeasonTeamId)

    if (rosterIds.has(playerId)) {
      return false
    }

    if (!Number.isFinite(playerActiveSeasonTeamId) || playerActiveSeasonTeamId <= 0) {
      return true
    }

    return playerActiveSeasonTeamId === currentEditingTeamId
  })
})

const teamRosterAddOptions = computed(() => {
  return teamPlayersAvailableForRoster.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const teamSeasonSelectedPlayers = computed(() => {
  return teamSeasonPlayers.value.filter((player) => Boolean(player?.selectedForSeason))
})

const teamSeasonAvailablePlayers = computed(() => {
  return teamSeasonPlayers.value.filter((player) => !player?.selectedForSeason)
})

const selectedAdminTeamSeason = computed(() => {
  return teamSeasonOptions.value.find((season) => String(season.id) === String(selectedTeamSeasonId.value)) || null
})

const teamSeasonMaxRosterSize = computed(() => {
  const rawValue = selectedAdminTeamSeason.value?.maxRosterSize
  const normalized = Number(rawValue)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : null
})

const teamSeasonRemainingSlots = computed(() => {
  if (!teamSeasonMaxRosterSize.value) {
    return null
  }
  return Math.max(teamSeasonMaxRosterSize.value - teamSeasonSelectedPlayers.value.length, 0)
})

const isTeamSeasonAtLimit = computed(() => {
  return teamSeasonRemainingSlots.value === 0
})

const willSelectedPlayersExceedSeasonLimit = computed(() => {
  if (teamSeasonRemainingSlots.value == null) {
    return false
  }
  return teamSeasonToAddIds.value.length > teamSeasonRemainingSlots.value
})

const teamSeasonAddOptions = computed(() => {
  return teamSeasonAvailablePlayers.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const teamSeasonRemoveOptions = computed(() => {
  return teamSeasonSelectedPlayers.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const {
  addRankingRule: addSeasonRankingRule,
  availableTieBreakerRuleOptions,
  buildPayload: buildSeasonPayload,
  isCreateDisabled: isSeasonCreateDisabled,
  normalizeRankingRulesForForm: normalizeSeasonRankingRulesForForm,
  rankingRulesSummary: seasonRankingRulesSummary,
  regularToursCount: seasonRegularToursCount,
  removeRankingRule: removeSeasonRankingRule,
  validateForm: validateSeasonForm,
} = useAdminSeasonRules({
  form: seasonForm,
  refereeIds: seasonRefereeIds,
  selectedTeamCount: computed(() => seasonSelectedTeams.value.length),
})

const selectedTourSeason = computed(() => {
  return seasonsList.value.find((season) => String(season.id) === String(tourSeasonId.value)) || null
})

const selectedSeasonEditItem = computed(() => {
  return seasonsList.value.find((season) => String(season.id) === String(editingSeasonId.value)) || null
})

const seasonCompletionActionLabel = computed(() => {
  if (!selectedSeasonEditItem.value) {
    return 'Завершить сезон'
  }
  return selectedSeasonEditItem.value.playoffEnabled
    ? 'Завершить регулярную часть и сформировать плей-офф'
    : 'Завершить сезон'
})

const seasonPanel = reactive({
  addSeasonRankingRule,
  addSeasonRefereeToForm,
  addSeasonTeamToForm,
  availableTieBreakerRuleOptions,
  cancelEditSeason,
  completeRegularSeason,
  completingRegularSeason,
  createSeason,
  deactivateSeason,
  downloadSeasonProtocolsArchive,
  downloadingSeasonProtocols,
  editingSeasonId,
  formatDateOnly,
  isSeasonCreateDisabled,
  messageError,
  messageOk,
  onSeasonSelectChange,
  playoffTeamOptions,
  removeSeasonRankingRule,
  removeSeasonRefereeFromForm,
  removeSeasonTeamFromForm,
  saveEditSeason,
  seasonAvailableReferees,
  seasonAvailableTeams,
  seasonCompletionActionLabel,
  seasonEditSelectId,
  seasonForm,
  seasonProtocolMenuOpen,
  seasonProtocolProgressText,
  seasonRankingRulesSummary,
  seasonRefereeToAddId,
  seasonRegularToursCount,
  seasonsList,
  seasonSelectedReferees,
  seasonSelectedTeams,
  seasonSubMode,
  seasonTeamToAddId,
  tieBreakerRuleOptions,
  toggleSeasonProtocolMenu,
})

const selectedTour = computed(() => {
  return toursList.value.find((tour) => String(tour.id) === String(selectedTourId.value)) || null
})

const canPublishSelectedTour = computed(() => {
  return Boolean(selectedTour.value) && !selectedTour.value.published && tourMatchesList.value.length > 0
})

function countSeasonHeadToHeadMeetings(firstTeamId, secondTeamId) {
  const normalizedFirstTeamId = Number(firstTeamId || 0)
  const normalizedSecondTeamId = Number(secondTeamId || 0)

  if (normalizedFirstTeamId <= 0 || normalizedSecondTeamId <= 0) {
    return 0
  }

  return seasonTourMatchesList.value.filter((match) => {
    const homeTeamId = Number(match.homeTeamId || 0)
    const awayTeamId = Number(match.awayTeamId || 0)
    return (
      (homeTeamId === normalizedFirstTeamId && awayTeamId === normalizedSecondTeamId)
      || (homeTeamId === normalizedSecondTeamId && awayTeamId === normalizedFirstTeamId)
    )
  }).length
}

const availableAwayTeams = computed(() => {
  const season = selectedTourSeason.value
  const homeTeamId = Number(matchForm.homeTeamId || 0)

  if (!season || homeTeamId <= 0) {
    return tourTeamsList.value
  }

  const allowedMeetings = Math.max(Number(season.roundsCount || 1), 1)

  return tourTeamsList.value.filter((team) => {
    const awayTeamId = Number(team.id || 0)
    if (awayTeamId <= 0 || awayTeamId === homeTeamId) {
      return false
    }
    return countSeasonHeadToHeadMeetings(homeTeamId, awayTeamId) < allowedMeetings
  })
})

const selectedTourMatchLimitMessage = computed(() => {
  const season = selectedTourSeason.value
  const homeTeamId = Number(matchForm.homeTeamId || 0)
  const awayTeamId = Number(matchForm.awayTeamId || 0)

  if (!season || homeTeamId <= 0 || awayTeamId <= 0 || homeTeamId === awayTeamId) {
    return ''
  }

  const allowedMeetings = Math.max(Number(season.roundsCount || 1), 1)
  const existingMeetings = countSeasonHeadToHeadMeetings(homeTeamId, awayTeamId)

  if (existingMeetings < allowedMeetings) {
    return ''
  }

  const homeTeam = tourTeamsList.value.find((team) => Number(team.id) === homeTeamId)
  const awayTeam = tourTeamsList.value.find((team) => Number(team.id) === awayTeamId)
  const homeTeamName = String(homeTeam?.name || 'Команда 1')
  const awayTeamName = String(awayTeam?.name || 'Команда 2')

  return `Пара ${homeTeamName} - ${awayTeamName} уже исчерпала лимит очных встреч для сезона: ${allowedMeetings} круг(а).`
})

watch(availableAwayTeams, (teams) => {
  if (!matchForm.awayTeamId) {
    return
  }

  const isStillAvailable = teams.some((team) => String(team.id) === String(matchForm.awayTeamId))
  if (!isStillAvailable) {
    matchForm.awayTeamId = ''
  }
})

watch(activeTab, (tabId) => {
  if (tabId === 'representatives') {
    const emailFilter = String(repSearch.value || '').trim()
    void loadRepresentativeUsers({
      email: emailFilter,
      pagenum: 0,
      pagesize: emailFilter ? 50 : 20,
    })
  }
  if (tabId === 'tours' && !tourSeasonId.value && seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    void onTourSeasonChange()
    return
  }
  if (tabId === 'tours' && tourSeasonId.value) {
    void refreshToursTabData()
  }
})

watch(visibleTabGroups, (groups) => {
  const allowedTabIds = new Set(groups.flatMap((group) => group.items.map((item) => item.id)))
  if (!allowedTabIds.has(activeTab.value)) {
    activeTab.value = groups[0]?.items[0]?.id || 'seasons'
  }
}, { immediate: true })

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

  const seasonValidationError = validateSeasonForm()
  if (seasonValidationError) {
    messageError.value = seasonValidationError
    return
  }

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
    showSeasonOperationError(error.message || 'Не удалось создать сезон.')
  }
}

async function startEditSeason(item) {
  editingSeasonId.value = item.id
  seasonForm.name = item.name
  seasonForm.roundsCount = String(item.roundsCount || 1)
  seasonForm.playoffEnabled = Boolean(item.playoffEnabled)
  seasonForm.playoffTeamCount = item.playoffTeamCount ? String(item.playoffTeamCount) : ''
  seasonForm.thirdPlaceEnabled = Boolean(item.thirdPlaceEnabled)
  seasonForm.status = item.status || 'ACTIVE'
  seasonForm.maxRosterSize = item.maxRosterSize ? String(item.maxRosterSize) : ''
  seasonForm.applicationDeadline = item.applicationDeadline || ''
  seasonForm.transferWindowStartDate = item.transferWindowStartDate || ''
  seasonForm.transferWindowEndDate = item.transferWindowEndDate || ''
  seasonForm.rankingRules = normalizeSeasonRankingRulesForForm(item.rankingRules)
  seasonForm.yellowCardsForSuspension = String(item.yellowCardsForSuspension || 0)
  seasonForm.redCardsForSuspension = String(item.redCardsForSuspension || 0)
  seasonRefereeIds.value = Array.isArray(item.referees) ? item.referees.map((referee) => Number(referee.id)).filter(Boolean) : []
  originalSeasonRefereeIds.value = [...seasonRefereeIds.value]
  seasonTeamIds.value = await loadSeasonTeams(item.id)
  originalSeasonTeamIds.value = [...seasonTeamIds.value]
  resetMessages()
}

function cancelEditSeason() {
  editingSeasonId.value = null
  seasonProtocolMenuOpen.value = false
  seasonProtocolProgressText.value = ''
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

function addSeasonRefereeToForm() {
  resetMessages()

  const refereeId = Number(seasonRefereeToAddId.value)
  if (!Number.isFinite(refereeId) || refereeId <= 0) {
    messageError.value = 'Сначала выберите судью из списка.'
    return
  }

  if (seasonRefereeIds.value.some((id) => Number(id) === refereeId)) {
    messageError.value = 'Этот судья уже привязан к сезону.'
    return
  }

  seasonRefereeIds.value = [...seasonRefereeIds.value, refereeId]
  seasonRefereeToAddId.value = ''
}

function removeSeasonTeamFromForm(teamId) {
  seasonTeamIds.value = seasonTeamIds.value.filter((id) => Number(id) !== Number(teamId))
  if (!seasonAvailableTeams.value.length) {
    seasonTeamToAddId.value = ''
  }
}

function removeSeasonRefereeFromForm(refereeId) {
  seasonRefereeIds.value = seasonRefereeIds.value.filter((id) => Number(id) !== Number(refereeId))
  if (!seasonAvailableReferees.value.length) {
    seasonRefereeToAddId.value = ''
  }
}

async function saveEditSeason() {
  resetMessages()

  const seasonValidationError = validateSeasonForm()
  if (seasonValidationError) {
    messageError.value = seasonValidationError
    return
  }

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

    if (seasonTeamsChanged()) {
      await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}/teams`, {
        method: 'PUT',
        body: JSON.stringify({ teamIds: seasonTeamIds.value }),
      })
    }

    await loadSeasonRegistry()
    await loadSeasons()
    if (String(tourSeasonId.value || '') === String(editingSeasonId.value)) {
      await onTourSeasonChange()
    }
    cancelEditSeason()
    messageOk.value = 'Сезон обновлен.'
  } catch (error) {
    showSeasonOperationError(error.message || 'Не удалось обновить сезон.')
  }
}

async function completeRegularSeason() {
  if (!editingSeasonId.value || completingRegularSeason.value) {
    return
  }

  resetMessages()
  completingRegularSeason.value = true

  try {
    const updatedSeason = await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}/complete-regular-season`, {
      method: 'POST',
    })
    await loadSeasonRegistry()
    await loadSeasons()
    const actualSeason = seasonsList.value.find((item) => String(item.id) === String(updatedSeason?.id || editingSeasonId.value))
    if (actualSeason) {
      await startEditSeason(actualSeason)
    }
    if (String(tourSeasonId.value || '') === String(editingSeasonId.value)) {
      await onTourSeasonChange()
    }
    messageOk.value = updatedSeason?.playoffEnabled
      ? 'Регулярный этап завершен, сетка плей-офф сформирована.'
      : 'Сезон завершен.'
  } catch (error) {
    showSeasonOperationError(error.message || 'Не удалось завершить регулярный этап сезона.')
  } finally {
    completingRegularSeason.value = false
  }
}

async function onSeasonSelectChange() {
  seasonProtocolMenuOpen.value = false
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

async function handleLeagueSeasonRefresh() {
  await loadSeasonRegistry()
  await loadSeasons()
}

function toggleSeasonProtocolMenu() {
  if (!editingSeasonId.value || downloadingSeasonProtocols.value) return
  seasonProtocolMenuOpen.value = !seasonProtocolMenuOpen.value
}

async function downloadSeasonProtocolsArchive() {
  if (!editingSeasonId.value || downloadingSeasonProtocols.value) return

  resetMessages()
  seasonProtocolMenuOpen.value = false
  downloadingSeasonProtocols.value = true
  seasonProtocolProgressText.value = 'Подготовка архива на сервере...'

  try {
    const response = await authorizedApiRequestRaw(`/api/seasons/${editingSeasonId.value}/protocols/export`, {
      method: 'GET',
    })
    const archiveBlob = await response.blob()
    const disposition = response.headers.get('content-disposition') || ''
    const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const archiveName = fileNameMatch
      ? decodeURIComponent(fileNameMatch[1])
      : buildSeasonProtocolsArchiveName(selectedSeasonEditItem.value?.name || seasonForm.name || 'season')

    downloadBlobFile(archiveBlob, archiveName)
    messageOk.value = 'Архив протоколов сезона скачан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось скачать архив протоколов сезона.'
  } finally {
    downloadingSeasonProtocols.value = false
    seasonProtocolProgressText.value = ''
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
  seasonForm.thirdPlaceEnabled = false
  seasonForm.status = 'DRAFT'
  seasonForm.maxRosterSize = ''
  seasonForm.applicationDeadline = ''
  seasonForm.transferWindowStartDate = ''
  seasonForm.transferWindowEndDate = ''
  seasonForm.rankingRules = ['GOAL_DIFFERENCE', 'GOALS_FOR']
  seasonForm.yellowCardsForSuspension = '0'
  seasonForm.redCardsForSuspension = '0'
  seasonTeamIds.value = []
  originalSeasonTeamIds.value = []
  seasonTeamToAddId.value = ''
  seasonRefereeIds.value = []
  originalSeasonRefereeIds.value = []
  seasonRefereeToAddId.value = ''
}

function seasonTeamsChanged() {
  return !haveSameTeamIds(originalSeasonTeamIds.value, seasonTeamIds.value)
}

function haveSameTeamIds(left, right) {
  const normalizedLeft = [...new Set((left || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))].sort((a, b) => a - b)
  const normalizedRight = [...new Set((right || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))].sort((a, b) => a - b)

  if (normalizedLeft.length !== normalizedRight.length) {
    return false
  }

  return normalizedLeft.every((teamId, index) => teamId === normalizedRight[index])
}

function showSeasonOperationError(message) {
  messageError.value = message
  if (typeof window !== 'undefined' && typeof window.alert === 'function') {
    window.alert(message)
  }
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
  isTeamRosterVisible.value = false
  resetMessages()
}

function cancelEditTeam() {
  editingTeamId.value = null
  resetTeamForm()
  resetAdminTeamContext()
  resetMessages()
}

async function saveEditTeam() {
  resetMessages()

  if (!editingTeamId.value) {
    messageError.value = 'Сначала выберите команду для редактирования.'
    return
  }

  if (!teamForm.name || !teamForm.shortName || !teamForm.city) {
    messageError.value = 'Заполните все поля команды.'
    return
  }

  teamSaving.value = true

  try {
    const updatedTeam = await authorizedApiRequest(`/api/teams/${editingTeamId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: teamForm.name,
        shortName: teamForm.shortName,
        city: teamForm.city,
        logoDataUrl: teamForm.logoDataUrl,
      }),
    })

    await loadTeamRegistry()

    const refreshedTeam = teamsList.value.find((team) => String(team.id) === String(updatedTeam?.id || editingTeamId.value)) || updatedTeam
    if (refreshedTeam?.id) {
      teamEditSelectId.value = String(refreshedTeam.id)
      startEditTeam(refreshedTeam)
      await refreshAdminTeamContext(refreshedTeam.id)
    }

    messageOk.value = 'Команда обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить команду.'
  } finally {
    teamSaving.value = false
  }
}

async function onTeamSelectChange() {
  if (!teamEditSelectId.value) {
    cancelEditTeam()
    return
  }
  const item = teamsList.value.find((t) => String(t.id) === teamEditSelectId.value)
  if (item) {
    startEditTeam(item)
    await refreshAdminTeamContext(item.id)
  }
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

function resetAdminTeamContext() {
  teamRoster.value = []
  teamSeasonOptions.value = []
  teamSeasonPlayers.value = []
  teamRosterToAddIds.value = []
  selectedTeamSeasonId.value = ''
  teamSeasonToAddIds.value = []
  teamSeasonToRemoveIds.value = []
  teamRosterBusy.value = false
  teamSeasonBusy.value = false
  isTeamRosterVisible.value = false
}

function toggleTeamRosterVisibility() {
  isTeamRosterVisible.value = !isTeamRosterVisible.value
}

async function refreshAdminTeamContext(teamId = editingTeamId.value) {
  const normalizedTeamId = Number(teamId)
  if (!Number.isFinite(normalizedTeamId) || normalizedTeamId <= 0) {
    resetAdminTeamContext()
    return
  }

  await loadEditingTeamRoster(normalizedTeamId)
  await loadEditingTeamSeasonOptions(normalizedTeamId)

  if (selectedTeamSeasonId.value && !teamSeasonOptions.value.some((season) => String(season.id) === String(selectedTeamSeasonId.value))) {
    selectedTeamSeasonId.value = ''
    teamSeasonPlayers.value = []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
  }

  if (selectedTeamSeasonId.value) {
    await loadEditingTeamSeasonPlayers(normalizedTeamId, selectedTeamSeasonId.value)
  }
}

async function loadEditingTeamRoster(teamId) {
  teamRosterBusy.value = true
  try {
    const payload = await authorizedApiRequest(`/api/teams/${encodeURIComponent(teamId)}/players`, {
      method: 'GET',
    })
    teamRoster.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamRoster.value = []
    messageError.value = error.message || 'Не удалось загрузить состав команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function loadEditingTeamSeasonOptions(teamId) {
  teamSeasonBusy.value = true
  try {
    const payload = await authorizedApiRequest(`/api/teams/${encodeURIComponent(teamId)}/seasons`, {
      method: 'GET',
    })
    teamSeasonOptions.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamSeasonOptions.value = []
    messageError.value = error.message || 'Не удалось определить сезоны команды.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function onAdminTeamSeasonChange() {
  resetMessages()
  teamSeasonToAddIds.value = []
  teamSeasonToRemoveIds.value = []

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    teamSeasonPlayers.value = []
    return
  }

  await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
}

async function loadEditingTeamSeasonPlayers(teamId, seasonId) {
  teamSeasonBusy.value = true
  try {
    const payload = await authorizedApiRequest(
      `/api/seasons/${encodeURIComponent(seasonId)}/teams/${encodeURIComponent(teamId)}/players`,
      { method: 'GET' }
    )
    teamSeasonPlayers.value = Array.isArray(payload) ? payload : []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
  } catch (error) {
    teamSeasonPlayers.value = []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
    messageError.value = error.message || 'Не удалось загрузить заявку команды на сезон.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function addPlayerToEditingTeam() {
  resetMessages()

  if (!editingTeamId.value) {
    messageError.value = 'Сначала выберите команду.'
    return
  }

  const playerIds = normalizePositiveIdList(teamRosterToAddIds.value)

  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для добавления в состав.'
    return
  }

  teamRosterBusy.value = true
  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'POST' }
      )
    }
    teamRosterToAddIds.value = []
    await loadPlayerRegistry()
    await refreshAdminTeamContext()
    messageOk.value = playerIds.length === 1 ? 'Игрок добавлен в состав команды.' : `В состав команды добавлено игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить игрока в состав команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function removePlayerFromEditingTeam(playerId) {
  resetMessages()

  if (!editingTeamId.value) {
    messageError.value = 'Сначала выберите команду.'
    return
  }

  if (!window.confirm('Убрать игрока из состава команды?')) {
    return
  }

  teamRosterBusy.value = true
  try {
    await authorizedApiRequest(
      `/api/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
      { method: 'DELETE' }
    )
    await loadPlayerRegistry()
    await refreshAdminTeamContext()
    messageOk.value = 'Игрок убран из состава команды.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось убрать игрока из состава команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function addSelectedPlayersToSeason() {
  resetMessages()

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    messageError.value = 'Сначала выберите команду и сезон.'
    return
  }

  const playerIds = normalizePositiveIdList(teamSeasonToAddIds.value)
  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для добавления в заявку сезона.'
    return
  }
  if (teamSeasonRemainingSlots.value != null && playerIds.length > teamSeasonRemainingSlots.value) {
    messageError.value = `Нельзя превысить лимит заявки сезона: ${teamSeasonMaxRosterSize.value}.`
    return
  }

  teamSeasonBusy.value = true

  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/seasons/${encodeURIComponent(selectedTeamSeasonId.value)}/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'POST' }
      )
    }
    await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
    messageOk.value = playerIds.length === 1 ? 'Игрок добавлен в заявку сезона.' : `В заявку сезона добавлено игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить игроков в заявку сезона.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function removeSelectedPlayersFromSeason() {
  resetMessages()

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    messageError.value = 'Сначала выберите команду и сезон.'
    return
  }

  const playerIds = normalizePositiveIdList(teamSeasonToRemoveIds.value)
  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для удаления из заявки сезона.'
    return
  }

  teamSeasonBusy.value = true

  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/seasons/${encodeURIComponent(selectedTeamSeasonId.value)}/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'DELETE' }
      )
    }
    await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
    messageOk.value = playerIds.length === 1 ? 'Игрок убран из заявки сезона.' : `Из заявки сезона убрано игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось изменить заявку сезона.'
  } finally {
    teamSeasonBusy.value = false
  }
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
    seasonTourMatchesList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/tours?season_id=${encodeURIComponent(tourSeasonId.value)}&active_flag=1`, {
      method: 'GET',
    })
    toursList.value = Array.isArray(payload) ? payload : []
    await loadSeasonTourMatchesSnapshot()
  } catch (error) {
    toursList.value = []
    seasonTourMatchesList.value = []
    messageError.value = error.message || 'Не удалось загрузить туры.'
  }
}

async function loadSeasonTourMatchesSnapshot() {
  if (!toursList.value.length) {
    seasonTourMatchesList.value = []
    return
  }

  const payloads = await Promise.all(
    toursList.value.map((tour) => authorizedApiRequest(`/api/tours/${tour.id}/matches?active_flag=1`, {
      method: 'GET',
    }))
  )

  seasonTourMatchesList.value = payloads.flatMap((payload) => Array.isArray(payload) ? payload : [])
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

async function refreshToursTabData() {
  if (!tourSeasonId.value) {
    return
  }

  await Promise.all([loadTours(), loadTeamsForTourSeason()])
  if (selectedTourId.value) {
    await onTourSelectChange()
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
  if (selectedTourMatchLimitMessage.value) {
    messageError.value = selectedTourMatchLimitMessage.value
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
    await loadSeasonTourMatchesSnapshot()
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
  const match = tourMatchesList.value.find((item) => Number(item.id) === Number(matchId))
  if (match && !canDeleteTourMatch(match)) {
    messageError.value = tourMatchDeleteTitle(match)
    return
  }
  if (!window.confirm('Удалить матч из тура без возможности восстановления?')) {
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches/${matchId}`, {
      method: 'DELETE',
    })
    await loadSeasonTourMatchesSnapshot()
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

function canDeleteTourMatch(match) {
  return String(match?.protocolStatus || 'SCHEDULED') === 'SCHEDULED'
}

function tourMatchScoreLabel(match) {
  const homeScore = Number.isInteger(match?.homeScore) ? match.homeScore : null
  const awayScore = Number.isInteger(match?.awayScore) ? match.awayScore : null
  if (homeScore === null || awayScore === null) {
    return ''
  }
  return `Счет: ${homeScore}:${awayScore}`
}

function tourMatchDeleteTitle(match) {
  if (canDeleteTourMatch(match)) {
    return 'Удалить матч из тура'
  }
  return 'Нельзя удалить матч, если по нему уже поданы составы или подтвержден протокол.'
}

function matchProtocolStatusLabel(status) {
  switch (String(status || 'SCHEDULED')) {
    case 'LINEUPS_SUBMITTED':
      return 'Составы поданы'
    case 'LIVE':
      return 'Идет матч'
    case 'FINISHED':
      return 'Матч сыгран'
    case 'VERIFIED':
      return 'Протокол подтвержден'
    default:
      return 'Не сыгран'
  }
}

function buildSeasonProtocolsArchiveName(seasonName) {
  const normalizedSeasonName = String(seasonName || 'season').replace(/[\\/:*?"<>|]/g, '_').trim() || 'season'
  return `Протоколы_${normalizedSeasonName}.zip`
}

function downloadBlobFile(blob, fileName) {
  const objectUrl = URL.createObjectURL(blob)

  try {
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

function protocolStatusBadgeClass(status) {
  return {
    'is-scheduled': String(status || 'SCHEDULED') === 'SCHEDULED',
    'is-lineups': String(status || '') === 'LINEUPS_SUBMITTED',
    'is-live': String(status || '') === 'LIVE',
    'is-finished': String(status || '') === 'FINISHED',
    'is-verified': String(status || '') === 'VERIFIED',
  }
}

function formatSeasonApplicationDeadline(value) {
  return value ? formatDateOnly(value) : 'Без ограничения'
}

function formatPlayerOptionLabel(player) {
  if (!player) return ''
  return `${player.fullName || ''}`
}

function formatAdminRosterPlayerOption(player) {
  if (!player) return ''
  return formatPlayerOptionLabel(player)
}

function formatAdminPlayerOptionCaption(player) {
  if (!player) return ''

  const parts = []
  if (player.birthDate) {
    parts.push(`ДР: ${formatDateOnly(player.birthDate)}`)
  }
  if (player.residence) {
    parts.push(player.residence)
  }
  if (player.isGoalkeeper) {
    parts.push('Вратарь')
  }

  return parts.join(' · ')
}

function normalizePositiveIdList(values) {
  return [...new Set((Array.isArray(values) ? values : [])
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0))]
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
  await loadRefereeRegistry()
  await loadRoleUsers({ pagenum: 0, pagesize: 20 })
  await loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
  await loadSeasons()
  if (seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    await onTourSeasonChange()
  }
})
</script>

<style>
.admin-temporal-input {
  min-height: 44px;
  border-radius: 12px;
  border-color: rgba(124, 163, 255, 0.34);
  background:
    linear-gradient(180deg, rgba(31, 43, 86, 0.96), rgba(16, 24, 53, 0.98)),
    rgba(19, 26, 52, 0.98);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 22px rgba(3, 8, 24, 0.24);
  color: var(--text);
  letter-spacing: 0.02em;
}

.admin-temporal-input:hover {
  border-color: rgba(97, 232, 162, 0.52);
}

.admin-temporal-input:focus-visible {
  outline: none;
  border-color: rgba(97, 232, 162, 0.78);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 3px rgba(97, 232, 162, 0.16),
    0 10px 26px rgba(3, 8, 24, 0.28);
}

.admin-tab-groups {
  align-items: start;
}

.admin-tab-group {
  align-content: start;
}

.admin-tabs-grid {
  align-content: start;
}

.admin-temporal-input-wide {
  font-weight: 600;
}

.admin-season-form {
  display: grid;
  gap: 18px;
}

.admin-season-edit-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-season-edit-picker {
  flex: 1 1 320px;
}

.admin-season-export-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.admin-season-export-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 280px;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(10, 16, 37, 0.98);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
  z-index: 4;
}

.admin-season-export-action {
  width: 100%;
  justify-content: flex-start;
}

.admin-season-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
  align-items: stretch;
}

.admin-season-field {
  min-width: 0;
}

.admin-season-field-wide {
  grid-column: 1 / -1;
}

.admin-season-toggle-field {
  display: grid;
  gap: 6px;
}

.admin-season-toggle-control {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 0 16px;
  border: 1px solid rgba(124, 163, 255, 0.18);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(24, 35, 72, 0.88), rgba(14, 22, 48, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.admin-season-toggle-control input {
  margin: 0;
}

.admin-season-toggle-control span {
  line-height: 1.25;
}

.admin-season-section {
  display: grid;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  background: linear-gradient(180deg, rgba(18, 27, 57, 0.82), rgba(11, 18, 41, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.admin-season-section-compact {
  gap: 12px;
  padding: 14px 16px;
}

.admin-season-section-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.admin-season-section-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.admin-season-section-head-compact {
  align-items: center;
}

.admin-season-team-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: stretch;
  width: min(100%, 720px);
}

.admin-season-team-row select {
  min-width: 0;
}

.admin-season-team-row .btn-ghost {
  white-space: nowrap;
}

.admin-season-selected-note {
  margin: -2px 0 0;
  font-size: 0.84rem;
  letter-spacing: 0.02em;
}

.admin-season-empty-state {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px dashed rgba(124, 163, 255, 0.18);
  background: rgba(255, 255, 255, 0.025);
}

.admin-season-team-list {
  gap: 10px;
}

.admin-season-picked-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

.admin-season-picked-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.admin-season-picked-copy p {
  margin: 0;
}

.admin-referee-list-item {
  align-items: center;
}

.admin-season-meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.admin-season-meta-card {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

.admin-season-meta-card p {
  margin: 0;
}

.admin-season-meta-card strong {
  font-size: 1rem;
  color: #f2f5ff;
}

.admin-season-meta-card-accent {
  border-color: rgba(97, 232, 162, 0.22);
  background: linear-gradient(180deg, rgba(97, 232, 162, 0.08), rgba(255, 255, 255, 0.03));
}

.admin-season-meta-label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9eb4ff;
}

.admin-season-rules-panel {
  margin-top: 2px;
}

.admin-ranking-rule-list {
  display: grid;
  gap: 10px;
}

.admin-ranking-rule-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(124, 163, 255, 0.18);
}

.admin-ranking-rule-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.14);
  border: 1px solid rgba(97, 232, 162, 0.28);
  color: #aef3ca;
  font-weight: 700;
}

.admin-ranking-rule-field {
  min-width: 0;
}

.admin-ranking-rule-label {
  display: block;
  margin-bottom: 6px;
  font-size: 0.84rem;
  color: var(--muted);
}

.admin-season-rules-footer {
  display: grid;
  gap: 6px;
}

.admin-season-rules-summary {
  color: #dfe8ff;
}

.admin-season-actions {
  justify-content: flex-start;
  padding-top: 4px;
}

.admin-temporal-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  filter: invert(88%) sepia(17%) saturate(1186%) hue-rotate(88deg) brightness(103%) contrast(88%);
  opacity: 0.9;
}

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

.admin-team-logo-preview {
  width: 84px;
  height: 84px;
  padding: 6px;
  object-fit: contain;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.admin-team-editor-shell {
  display: grid;
  gap: 16px;
}

.admin-team-identity-card {
  display: grid;
  gap: 14px;
}

.admin-team-identity-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.admin-team-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
}

.admin-team-summary-card {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  background: rgba(15, 22, 50, 0.72);
}

.admin-team-summary-card.is-accent {
  border-color: rgba(97, 232, 162, 0.32);
  background: rgba(17, 43, 39, 0.52);
}

.admin-team-summary-label {
  font-size: 0.76rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--muted);
}

.admin-team-identity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.admin-team-logo-field {
  grid-column: span 2;
}

.admin-team-logo-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.admin-team-management-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.admin-team-management-card {
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(11, 17, 39, 0.82);
  border-radius: 20px;
}

.admin-team-management-head {
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.admin-team-head-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-team-management-toolbar {
  min-height: 44px;
}

.admin-team-management-toolbar-spacer {
  display: block;
}

.admin-team-season-select-field {
  display: grid;
  gap: 8px;
}

.admin-team-picker-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
}

.admin-team-picker-side {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.admin-team-picker-side-inline {
  min-width: 0;
}

.admin-team-picker-count {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  background: rgba(124, 163, 255, 0.1);
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.admin-team-picker-side .btn-primary,
.admin-team-picker-side .btn-danger {
  min-width: 220px;
}

.admin-player-manage-item {
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.admin-player-manage-copy {
  display: grid;
  gap: 4px;
}

.admin-team-season-tools {
  display: grid;
  gap: 12px;
}

.admin-team-season-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-team-season-action-block {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(17, 24, 52, 0.52);
}

.admin-team-season-control {
  display: grid;
  gap: 8px;
}

.admin-team-management-card :deep(.searchable-select.is-multiple.is-open .searchable-select-dropdown) {
  max-height: none;
}

.admin-season-player-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.admin-season-player-badge.is-selected {
  background: rgba(97, 232, 162, 0.18);
  color: #8ff0bb;
}

.admin-season-player-badge.is-not-selected {
  background: rgba(124, 163, 255, 0.16);
  color: #b5c7ff;
}

.admin-sticky-actions-spacer {
  height: 0;
}

.admin-sticky-actions {
  position: sticky;
  bottom: 0;
  z-index: 12;
  margin-top: 8px;
  padding: 14px 16px calc(14px + env(safe-area-inset-bottom, 0px));
  border-radius: 16px;
  border: 1px solid rgba(124, 163, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(16, 24, 53, 0.96), rgba(10, 16, 38, 0.98)),
    rgba(10, 16, 38, 0.98);
  box-shadow: 0 -12px 30px rgba(3, 8, 24, 0.28);
  backdrop-filter: blur(10px);
}

@media (max-width: 960px) {
  .admin-team-identity-head,
  .admin-team-picker-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .admin-team-management-toolbar-spacer {
    display: none;
  }

  .admin-team-identity-grid,
  .admin-team-summary-grid,
  .admin-season-grid,
  .admin-season-meta-grid {
    grid-template-columns: 1fr;
  }

  .admin-season-section-head,
  .admin-season-section-head-compact,
  .admin-ranking-rule-card {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .admin-season-toggle-control {
    min-height: 52px;
  }

  .admin-season-team-row,
  .admin-season-picked-item,
  .admin-referee-list-item {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .admin-season-picked-item,
  .admin-referee-list-item {
    align-items: stretch;
  }

  .admin-team-management-grid {
    grid-template-columns: 1fr;
  }

  .admin-team-logo-field {
    grid-column: auto;
  }

  .admin-team-picker-side {
    min-width: 0;
    align-items: stretch;
  }

  .admin-sticky-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-team-logo-preview {
    width: 72px;
    height: 72px;
  }
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

.tour-match-status-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.05);
}

.tour-match-status-badge.is-scheduled {
  color: #bfd0ff;
  border-color: rgba(124, 163, 255, 0.34);
  background: rgba(86, 122, 214, 0.16);
}

.tour-match-status-badge.is-lineups {
  color: #ffe2a3;
  border-color: rgba(255, 196, 84, 0.34);
  background: rgba(255, 196, 84, 0.14);
}

.tour-match-status-badge.is-live {
  color: #ffcfbf;
  border-color: rgba(255, 124, 84, 0.34);
  background: rgba(255, 124, 84, 0.14);
}

.tour-match-status-badge.is-finished {
  color: #d9dff8;
  border-color: rgba(188, 196, 230, 0.3);
  background: rgba(188, 196, 230, 0.12);
}

.tour-match-status-badge.is-verified {
  color: #bff8d8;
  border-color: rgba(97, 232, 162, 0.38);
  background: rgba(97, 232, 162, 0.14);
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

@media (max-width: 640px) {
  .admin-tab-groups,
  .admin-tabs-grid,
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .admin-subnav {
    width: 100%;
  }

  .admin-subnav-btn,
  .admin-season-edit-picker,
  .admin-season-edit-picker select,
  .admin-season-team-row > *,
  .admin-role-manage-row > *,
  .admin-add-role-row > *,
  .tour-publish-row > *,
  .tour-matches-header > * {
    width: 100%;
  }

  .admin-season-edit-toolbar,
  .admin-role-manage-row,
  .admin-add-role-row,
  .tour-publish-row {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-team-identity-head,
  .admin-team-management-head,
  .admin-team-head-actions,
  .admin-team-picker-side,
  .admin-inline-check,
  .tour-match-item {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-team-head-actions > *,
  .admin-team-picker-side > *,
  .admin-team-picker-side .btn-primary,
  .admin-team-picker-side .btn-danger,
  .admin-sticky-actions > *,
  .tour-match-item > * {
    width: 100%;
    min-width: 0;
  }

  .admin-team-summary-grid {
    grid-template-columns: 1fr;
  }

  .admin-team-management-card,
  .admin-season-section,
  .admin-season-section-compact {
    padding: 14px;
  }

  .admin-season-picked-item {
    gap: 10px;
  }

  .admin-surface,
  .admin-tab-group,
  .admin-found-user {
    padding: 14px;
  }
}
 </style>
