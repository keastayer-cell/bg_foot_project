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


    <AdminTeamsPanel
      v-if="activeTab === 'teams'"
      :panel="teamPanel"
    />


    <AdminToursPanel
      v-if="activeTab === 'tours'"
      :panel="tourPanel"
    />


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
import { createAdminSeasonsApi } from '../api/adminSeasons'
import { useStore } from '../store/store'
import { useAdminAccess } from '../composables/useAdminAccess'
import { useAdminPlayers } from '../composables/useAdminPlayers'
import { useAdminReferees } from '../composables/useAdminReferees'
import {
  PLAYOFF_TEAM_OPTIONS,
  TIE_BREAKER_RULE_OPTIONS,
  useAdminSeasonRules,
} from '../composables/useAdminSeasonRules'
import { useAdminTeams } from '../composables/useAdminTeams'
import { useAdminTours } from '../composables/useAdminTours'
import { useAdminTabs } from '../composables/useAdminTabs'
import AdminTabNavigation from '../components/AdminTabNavigation.vue'
import AdminBanPanel from '../components/admin/AdminBanPanel.vue'
import AdminPlayersPanel from '../components/admin/AdminPlayersPanel.vue'
import AdminRefereesPanel from '../components/admin/AdminRefereesPanel.vue'
import AdminRepresentativesPanel from '../components/admin/AdminRepresentativesPanel.vue'
import AdminRolesPanel from '../components/admin/AdminRolesPanel.vue'
import AdminSeasonsPanel from '../components/admin/AdminSeasonsPanel.vue'
import AdminTeamsPanel from '../components/admin/AdminTeamsPanel.vue'
import AdminToursPanel from '../components/admin/AdminToursPanel.vue'
import AdminLeagueContent from '../components/AdminLeagueContent.vue'

const USERS_KEY = 'football_stats_admin_users_registry'
const router = useRouter()

const { authorizedApiRequest, authorizedApiRequestRaw, hasRole } = useAuth()
const adminSeasonsApi = createAdminSeasonsApi(authorizedApiRequest, authorizedApiRequestRaw)
const { loadSeasons } = useStore()
const { activeTab, visibleTabGroups, selectAdminTab } = useAdminTabs({
  hasRole,
  navigate: (path) => router.push(path),
})

const seasonsList = ref([])
const teamsList = ref([])
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
const {
  addPlayersToRoster: addPlayerToEditingTeam,
  addPlayersToSeason: addSelectedPlayersToSeason,
  cancelEdit: cancelEditTeam,
  create: createTeam,
  deactivate: deactivateTeam,
  editingId: editingTeamId,
  editSelectId: teamEditSelectId,
  form: teamForm,
  isRosterVisible: isTeamRosterVisible,
  isSeasonAtLimit: isTeamSeasonAtLimit,
  loadRegistry: loadTeamRegistry,
  onLogoSelected: onTeamLogoSelected,
  onSeasonChange: onAdminTeamSeasonChange,
  onSelectChange: onTeamSelectChange,
  refreshContext: refreshAdminTeamContext,
  removePlayerFromRoster: removePlayerFromEditingTeam,
  removePlayersFromSeason: removeSelectedPlayersFromSeason,
  roster: teamRoster,
  rosterAddOptions: teamRosterAddOptions,
  rosterBusy: teamRosterBusy,
  rosterToAddIds: teamRosterToAddIds,
  saveEdit: saveEditTeam,
  saving: teamSaving,
  seasonAddOptions: teamSeasonAddOptions,
  seasonAvailablePlayers: teamSeasonAvailablePlayers,
  seasonBusy: teamSeasonBusy,
  seasonMaxRosterSize: teamSeasonMaxRosterSize,
  seasonOptions: teamSeasonOptions,
  seasonPlayers: teamSeasonPlayers,
  seasonRemainingSlots: teamSeasonRemainingSlots,
  seasonRemoveOptions: teamSeasonRemoveOptions,
  seasonSelectedPlayers: teamSeasonSelectedPlayers,
  seasonToAddIds: teamSeasonToAddIds,
  seasonToRemoveIds: teamSeasonToRemoveIds,
  selectedSeasonId: selectedTeamSeasonId,
  subMode: teamSubMode,
  toggleRosterVisibility: toggleTeamRosterVisibility,
  willSelectedPlayersExceedSeasonLimit,
} = useAdminTeams({
  request: authorizedApiRequest,
  teams: teamsList,
  players: playersList,
  loadPlayers: loadPlayerRegistry,
  clearMessages: resetMessages,
  errorMessage: messageError,
  successMessage: messageOk,
  formatDateOnly,
})
const {
  availableAwayTeams,
  canDeleteTourMatch,
  canPublishSelectedTour,
  createMatch: createTourMatch,
  deleteMatch: deleteTourMatch,
  matchForm,
  matchLimitMessage: selectedTourMatchLimitMessage,
  matchProtocolStatusLabel,
  matches: tourMatchesList,
  onSeasonChange: onTourSeasonChange,
  onTourChange: onTourSelectChange,
  protocolStatusBadgeClass,
  publish: publishSelectedTour,
  refresh: refreshToursTabData,
  seasonId: tourSeasonId,
  selectedId: selectedTourId,
  selectedSeason: selectedTourSeason,
  selectedTour,
  teams: tourTeamsList,
  tourMatchDeleteTitle,
  tourMatchScoreLabel,
  tours: toursList,
} = useAdminTours({
  request: authorizedApiRequest,
  seasons: seasonsList,
  clearMessages: resetMessages,
  errorMessage: messageError,
  successMessage: messageOk,
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

const teamPanel = reactive({
  addPlayersToRoster: addPlayerToEditingTeam,
  addPlayersToSeason: addSelectedPlayersToSeason,
  cancelEdit: cancelEditTeam,
  create: createTeam,
  deactivate: deactivateTeam,
  editingId: editingTeamId,
  editSelectId: teamEditSelectId,
  form: teamForm,
  formatDateOnly,
  isRosterVisible: isTeamRosterVisible,
  isSeasonAtLimit: isTeamSeasonAtLimit,
  messageError,
  messageOk,
  onLogoSelected: onTeamLogoSelected,
  onSeasonChange: onAdminTeamSeasonChange,
  onSelectChange: onTeamSelectChange,
  refreshContext: refreshAdminTeamContext,
  removePlayerFromRoster: removePlayerFromEditingTeam,
  removePlayersFromSeason: removeSelectedPlayersFromSeason,
  roster: teamRoster,
  rosterAddOptions: teamRosterAddOptions,
  rosterBusy: teamRosterBusy,
  rosterToAddIds: teamRosterToAddIds,
  saveEdit: saveEditTeam,
  saving: teamSaving,
  seasonAddOptions: teamSeasonAddOptions,
  seasonAvailablePlayers: teamSeasonAvailablePlayers,
  seasonBusy: teamSeasonBusy,
  seasonMaxRosterSize: teamSeasonMaxRosterSize,
  seasonOptions: teamSeasonOptions,
  seasonPlayers: teamSeasonPlayers,
  seasonRemainingSlots: teamSeasonRemainingSlots,
  seasonRemoveOptions: teamSeasonRemoveOptions,
  seasonSelectedPlayers: teamSeasonSelectedPlayers,
  seasonToAddIds: teamSeasonToAddIds,
  seasonToRemoveIds: teamSeasonToRemoveIds,
  selectedSeasonId: selectedTeamSeasonId,
  subMode: teamSubMode,
  teamsList,
  toggleRosterVisibility: toggleTeamRosterVisibility,
  willSelectedPlayersExceedSeasonLimit,
})

const tourPanel = reactive({
  availableAwayTeams,
  canDeleteTourMatch,
  canPublishSelectedTour,
  createMatch: createTourMatch,
  deleteMatch: deleteTourMatch,
  formatDateTime,
  matchForm,
  matchLimitMessage: selectedTourMatchLimitMessage,
  matchProtocolStatusLabel,
  matches: tourMatchesList,
  onSeasonChange: onTourSeasonChange,
  onTourChange: onTourSelectChange,
  protocolStatusBadgeClass,
  publish: publishSelectedTour,
  seasonId: tourSeasonId,
  seasonsList,
  selectedId: selectedTourId,
  selectedSeason: selectedTourSeason,
  selectedTour,
  teams: tourTeamsList,
  tourMatchDeleteTitle,
  tourMatchScoreLabel,
  tours: toursList,
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
    const createdSeason = await adminSeasonsApi.create(buildSeasonPayload(), seasonTeamIds.value)
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
    await adminSeasonsApi.update(editingSeasonId.value, buildSeasonPayload())

    if (seasonTeamsChanged()) {
      await adminSeasonsApi.setTeams(editingSeasonId.value, seasonTeamIds.value)
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
    const updatedSeason = await adminSeasonsApi.completeRegularSeason(editingSeasonId.value)
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
    await adminSeasonsApi.deactivate(seasonId)
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
    const payload = await adminSeasonsApi.getActive()
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
    const response = await adminSeasonsApi.exportProtocols(editingSeasonId.value)
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
    const payload = await adminSeasonsApi.getTeams(seasonId)
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
