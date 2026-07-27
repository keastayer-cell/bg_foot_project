<template>
  <section class="section-wrap team-rep-page">
    <article class="card team-rep-profile-card">
      <div class="toolbar team-rep-card-head">
        <h2 class="section-title">Кабинет сезонных заявок команды</h2>
        <div class="actions-row">
          <button v-if="canOpenTransfers" class="btn-ghost" type="button" @click="router.push('/team-rep-transfers')">Трансферы</button>
          <button v-if="canManagePlayers" class="btn-primary" type="button" @click="openCreatePlayerModal()">Создать игрока</button>
        </div>
      </div>

      <div class="team-rep-profile-grid">
        <div v-if="isSuperAdminEditor">
          <span class="team-rep-label">Режим</span>
          <div class="team-rep-value">SUPER_ADMIN</div>
        </div>
        <div>
          <span class="team-rep-label">Имя</span>
          <div class="team-rep-value">{{ profile.name }}</div>
        </div>
        <div>
          <span class="team-rep-label">Почта</span>
          <div class="team-rep-value">{{ profile.email }}</div>
        </div>
        <div>
          <span class="team-rep-label">Команда</span>
          <div v-if="!isSuperAdminEditor" class="team-rep-value team-rep-team">{{ profile.teamName }}</div>
          <label v-else class="team-rep-admin-team-picker">
            <select v-model="selectedAdminTeamId">
              <option value="">— выберите команду —</option>
              <option v-for="team in adminTeams" :key="team.id" :value="String(team.id)">
                {{ team.name }}
              </option>
            </select>
          </label>
        </div>
      </div>

      <p class="error-text" v-if="pageError">{{ pageError }}</p>
      <p class="success-text" v-if="pageSuccess">{{ pageSuccess }}</p>

      <div v-if="canOpenTransfers && incomingTransfersSummary.totalPendingCount > 0" class="team-rep-transfer-alert">
        <button class="btn-primary team-rep-transfer-alert-btn" type="button" @click="openIncomingTransfersModal">
          У вас новая заявка на трансфер · {{ incomingTransfersSummary.totalPendingCount }}
        </button>
      </div>
    </article>

    <article class="card team-rep-history-card">
      <div class="toolbar team-rep-card-head">
        <h3 class="section-title">Сезоны команды</h3>
        <button class="btn-ghost" type="button" @click="loadDashboard" :disabled="dashboardLoading">Обновить</button>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка данных...</p>
      <p v-else-if="isSuperAdminEditor && !selectedAdminTeamId" class="muted-text">Выберите команду, чтобы открыть сезонные заявки.</p>
      <p v-else-if="!teamSeasons.length" class="muted-text">Для выбранной команды пока нет доступных сезонов.</p>

      <div v-else class="team-rep-form">
        <label>
          Выберите сезон для просмотра заявки
          <select v-model="selectedSeasonId">
            <option value="">— выберите —</option>
            <option v-for="season in teamSeasons" :key="season.id" :value="String(season.id)">
              {{ season.name }}
            </option>
          </select>
        </label>

        <template v-if="selectedSeasonSummary">
          <div class="toolbar team-rep-card-head team-rep-season-actions">
            <div class="team-rep-badge-row">
              <span class="team-rep-season-chip">
                В заявке:
                {{ `${selectedSeasonSummary.selectedPlayersCount} из ${selectedSeasonSummary.maxRosterSize || '∞'}` }}
              </span>
              <span class="team-rep-season-chip">{{ selectedSeasonSummary.applicationDeadline ? `Дедлайн: ${formatDateOnly(selectedSeasonSummary.applicationDeadline)}` : 'Дедлайн не задан' }}</span>
              <span class="team-rep-season-chip">Статус: {{ formatSeasonStatus(selectedSeasonSummary.status) }}</span>
              <span class="team-rep-season-chip" :class="applicationStatusChipClass(selectedSeasonSummary.applicationStatus)">Заявка: {{ formatApplicationStatus(selectedSeasonSummary.applicationStatus) }}</span>
              <span class="team-rep-season-chip" :class="selectedSeasonSummary.applicationOpen ? 'team-rep-season-chip-open' : 'team-rep-season-chip-closed'">
                {{ selectedSeasonSummary.applicationOpen ? 'Добавление открыто' : 'Добавление закрыто' }}
              </span>
            </div>
            <div class="actions-row team-rep-season-actions-row">
              <button
                class="btn-ghost"
                type="button"
                @click="toggleSelectedSeasonPlayersFilter"
              >
                {{ showSelectedSeasonPlayersOnly ? 'Показать весь состав' : 'Показать игроков в заявке' }}
              </button>
              <button class="btn-ghost" type="button" @click="submitSeasonApplication" :disabled="seasonLoading || !selectedSeasonSummary.applicationSubmittable">
                {{ selectedSeasonSummary.applicationStatus === 'RETURNED' ? 'Отправить повторно' : 'Отправить на проверку' }}
              </button>
              <button class="btn-primary" type="button" @click="openAddPlayerModal(selectedSeasonSummary.id)" :disabled="!canEditSelectedSeasonApplication">Добавить игрока</button>
            </div>
          </div>
          <div v-if="selectedSeasonSummary.applicationDecisionComment" class="team-rep-review-note" :class="applicationReviewNoteClass(selectedSeasonSummary.applicationStatus)">
            <strong>{{ selectedSeasonSummary.applicationStatus === 'APPROVED' ? 'Решение по заявке' : 'Комментарий проверяющего' }}</strong>
            <p>{{ selectedSeasonSummary.applicationDecisionComment }}</p>
          </div>
          <p v-if="!selectedSeasonSummary.applicationOpen" class="muted-text">
            {{ selectedSeasonSummary.status !== 'ACTIVE'
              ? 'Изменения заявки закрыты, потому что сезон не находится в активном статусе.'
              : 'Дедлайн изменений сезонной заявки истек.' }}
          </p>
          <p v-else-if="selectedSeasonSummary.applicationStatus === 'SUBMITTED'" class="muted-text">
            Заявка отправлена на проверку. Дождитесь решения рефери или администратора.
          </p>
        </template>

        <p v-if="seasonError" class="error-text">{{ seasonError }}</p>
        <p v-if="seasonSuccess" class="success-text">{{ seasonSuccess }}</p>
      </div>
    </article>

    <article class="card team-rep-players-card">
      <div class="toolbar team-rep-card-head">
        <div>
          <h3 class="section-title">Текущий состав команды</h3>
          <p v-if="showSelectedSeasonPlayersOnly && selectedSeasonSummary" class="muted-text team-rep-filter-hint">
            Показаны только игроки, относящиеся к сезону «{{ selectedSeasonSummary.name }}».
          </p>
        </div>
        <button class="btn-ghost" type="button" @click="toggleTeamRosterVisibility">
          {{ isTeamRosterVisible ? 'Скрыть состав' : 'Показать состав' }}
        </button>
      </div>

      <p v-if="dashboardLoading" class="muted-text">Загрузка состава...</p>
      <p v-else-if="!isTeamRosterVisible" class="muted-text">Состав скрыт. Нажмите «Показать состав», чтобы открыть список игроков.</p>
      <p v-else-if="!displayedTeamPlayers.length" class="muted-text">
        {{ showSelectedSeasonPlayersOnly ? 'Для выбранного сезона в текущем составе нет игроков.' : 'В текущем составе команды пока нет игроков.' }}
      </p>

      <div v-else class="team-rep-player-list">
        <article class="team-rep-player-item" v-for="player in displayedTeamPlayers" :key="player.id">
          <div class="team-rep-player-main">
            <strong>{{ player.fullName }}<span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
          </div>
          <img
            v-if="player.photoDataUrl"
            :src="player.photoDataUrl"
            alt="Фото игрока"
            class="team-rep-player-photo"
          />
          <div class="actions-row team-rep-player-row-actions">
            <button v-if="canManagePlayers" class="btn-ghost" type="button" @click="openEditPlayerModal(player)">Редактировать</button>
            <button
              v-if="selectedSeasonId && playerHasSelectedSeason(player)"
              class="btn-danger btn-compact"
              type="button"
              @click="removeFromSelectedSeason(player.id)"
              :disabled="!canEditSelectedSeasonApplication"
            >
              Убрать из сезона
            </button>
            <button v-if="canManagePlayers" class="btn-danger btn-compact" type="button" @click="removeFromTeam(player.id)">Удалить из команды</button>
          </div>
        </article>
      </div>
    </article>

    <div v-if="incomingTransfersModalOpen" class="modal-backdrop" @click.self="closeIncomingTransfersModal">
      <article class="card auth-modal team-rep-modal incoming-transfer-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Входящие заявки на трансфер</h3>
            <p class="muted-text">Здесь можно подтвердить или отклонить новые заявки.</p>
          </div>
          <button class="btn-ghost" type="button" @click="closeIncomingTransfersModal">Закрыть</button>
        </div>

        <p v-if="incomingTransfersLoading" class="muted-text">Загрузка заявок...</p>
        <p v-else-if="!incomingTransfersSummary.requests.length" class="muted-text">Новых входящих заявок нет.</p>

        <div v-else class="incoming-transfer-list">
          <article class="incoming-transfer-item" v-for="request in incomingTransfersSummary.requests" :key="request.id">
            <div class="incoming-transfer-main">
              <strong>{{ request.playerName }}<span v-if="request.playerGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
              <p class="muted-text">{{ request.toTeamName }} хочет забрать игрока из вашей команды</p>
              <p class="muted-text">Заявка создана: {{ formatDateTime(request.requestedAt) }}</p>
              <p v-if="request.requestComment" class="muted-text">Комментарий: {{ request.requestComment }}</p>
            </div>
            <textarea v-model.trim="incomingDecisionComments[request.id]" rows="2" placeholder="Комментарий к решению"></textarea>
            <div class="actions-row team-rep-player-row-actions">
              <button class="btn-primary btn-compact" type="button" @click="processIncomingTransfer(request.id, 'approve')" :disabled="incomingDecisionLoadingId === request.id">Подтвердить</button>
              <button class="btn-danger btn-compact" type="button" @click="processIncomingTransfer(request.id, 'reject')" :disabled="incomingDecisionLoadingId === request.id">Отклонить</button>
            </div>
          </article>
        </div>

        <div class="pagination-bar" v-if="incomingTransfersSummary.totalPages > 1">
          <button class="btn-ghost" type="button" @click="changeIncomingTransfersPage(incomingTransfersSummary.pageNumber - 1)" :disabled="incomingTransfersLoading || incomingTransfersSummary.pageNumber <= 0">Назад</button>
          <span class="muted-text">Страница {{ incomingTransfersSummary.pageNumber + 1 }} из {{ incomingTransfersSummary.totalPages }} · всего {{ incomingTransfersSummary.totalElements }}</span>
          <button class="btn-ghost" type="button" @click="changeIncomingTransfersPage(incomingTransfersSummary.pageNumber + 1)" :disabled="incomingTransfersLoading || incomingTransfersSummary.pageNumber + 1 >= incomingTransfersSummary.totalPages">Вперёд</button>
        </div>
      </article>
    </div>

    <div v-if="addPlayerModalOpen" class="modal-backdrop" @click.self="closeSeasonModals">
      <article class="card auth-modal team-rep-modal team-rep-season-modal">
        <div class="toolbar auth-modal-head">
          <div>
            <h3 class="section-title">Добавить игрока в заявку</h3>
            <p v-if="seasonView" class="muted-text">{{ seasonView.seasonName }} · {{ seasonView.teamName }}</p>
          </div>
          <div class="actions-row team-rep-season-modal-head-actions">
            <button class="btn-ghost" type="button" @click="closeSeasonModals">Закрыть</button>
          </div>
        </div>

        <p v-if="seasonLoading && !seasonView" class="muted-text">Загрузка списка игроков...</p>

        <div v-else-if="seasonView" class="team-rep-inline-picker compact">
          <SearchableSelect
            :key="`team-rep-season-picker-${seasonView.seasonId}-${seasonSelectablePlayerOptions.length}`"
            v-model="selectedAvailablePlayerIds"
            :options="seasonSelectablePlayerOptions"
            multiple
            multiple-summary-text="Выбрано игроков"
            multiple-action-hint="После выбора нажмите «Добавить выбранных»"
            placeholder="Выберите игроков"
            search-placeholder="Начните вводить ФИО игрока"
            empty-text="Игрок по такому ФИО не найден"
          />
          <div class="team-rep-season-picker-meta">
            <span class="team-rep-selected-count">
              Выбрано: {{ selectedAvailablePlayerIds.length }}
            </span>
            <button
              class="btn-primary"
              type="button"
              @click="addAvailablePlayersToSeason"
              :disabled="seasonLoading || !seasonView || !seasonView.applicationOpen || !selectedAvailablePlayerIds.length"
            >
              Добавить выбранных
            </button>
          </div>
        </div>

        <p v-if="seasonView && !seasonView.applicationOpen" class="muted-text">
          Добавление новых игроков закрыто с {{ formatDateOnly(seasonView.applicationDeadline) }}.
        </p>

        <p v-if="!seasonView" class="muted-text">Не удалось загрузить доступных игроков.</p>

        <p class="error-text" v-if="seasonError">{{ seasonError }}</p>

      </article>
    </div>

    <div v-if="playerModalOpen" class="modal-backdrop" @click.self="closePlayerModal">
      <article class="card auth-modal team-rep-modal">
        <div class="toolbar auth-modal-head">
          <h3 class="section-title">{{ editingPlayerId ? 'Редактировать игрока' : 'Создать игрока' }}</h3>
          <button class="btn-ghost" type="button" @click="closePlayerModal">Закрыть</button>
        </div>

        <form class="team-rep-form" @submit.prevent="savePlayer">
          <label>
            ФИО
            <input v-model.trim="playerForm.fullName" type="text" required minlength="5" />
          </label>

          <label>
            Дата рождения
            <input v-model="playerForm.birthDate" type="date" />
          </label>

          <label>
            Прописка
            <input v-model.trim="playerForm.residence" type="text" placeholder="Например: Богородск" />
          </label>

          <label class="team-rep-checkbox-row">
            <input v-model="playerForm.isGoalkeeper" type="checkbox" />
            <span>Вратарь</span>
          </label>

          <label>
            Фото
            <input type="file" accept="image/*" @change="onPhotoSelected" />
          </label>

          <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото" class="team-rep-player-photo-preview" />

          <p class="error-text" v-if="playerModalError">{{ playerModalError }}</p>

          <div class="actions-row">
            <button class="btn-primary" type="submit" :disabled="playerSaving">Сохранить</button>
          </div>
        </form>
      </article>
    </div>
  </section>
</template>

<script setup>
import SearchableSelect from '../components/SearchableSelect.vue'
import { useTeamRepDashboard } from '../composables/useTeamRepDashboard.js'

const {
  route,
  router,
  dashboardLoading,
  seasonLoading,
  playerSaving,
  pageError,
  pageSuccess,
  seasonError,
  seasonSuccess,
  incomingTransfersLoading,
  incomingTransfersModalOpen,
  incomingDecisionLoadingId,
  incomingTransfersSummary,
  incomingDecisionComments,
  adminTeams,
  teamSeasons,
  teamPlayers,
  seasonView,
  selectedAdminTeamId,
  selectedSeasonId,
  selectedAvailablePlayerIds,
  addPlayerModalOpen,
  showSelectedSeasonPlayersOnly,
  isTeamRosterVisible,
  playerModalOpen,
  editingPlayerId,
  playerModalError,
  playerForm,
  suspendSeasonSelectionWatch,
  isSuperAdminEditor,
  canOpenDashboard,
  canOpenTransfers,
  canManagePlayers,
  activeTeamName,
  profile,
  selectedSeasonSummary,
  canEditSelectedSeasonApplication,
  displayedTeamPlayers,
  seasonSelectablePlayers,
  seasonSelectablePlayerOptions,
  loadAdminTeams,
  teamScopedPath,
  syncDashboardQuery,
  clearDashboardState,
  loadDashboard,
  loadIncomingTransfersNotifications,
  openIncomingTransfersModal,
  closeIncomingTransfersModal,
  changeIncomingTransfersPage,
  processIncomingTransfer,
  loadSeasonView,
  openAddPlayerModal,
  closeSeasonModals,
  toggleSelectedSeasonPlayersFilter,
  toggleTeamRosterVisibility,
  removeFromSeason,
  addAvailablePlayersToSeason,
  submitSeasonApplication,
  mutateSeasonPlayer,
  playerHasSelectedSeason,
  removeFromSelectedSeason,
  removeFromTeam,
  openCreatePlayerModal,
  openEditPlayerModal,
  closePlayerModal,
  savePlayer,
  onPhotoSelected,
  formatDateOnly,
  formatDateTime,
  formatPlayerOptionLabel,
  canEditApplicationSummary,
  formatSeasonStatus,
  formatApplicationStatus,
  applicationStatusChipClass,
  applicationReviewNoteClass,
} = useTeamRepDashboard()
</script>

<style scoped src="../styles/pages/team-rep-dashboard.css"></style>
