<template>
  <section class="section-wrap">
    <article class="card" v-if="loading">
      <p class="muted-text">Загрузка матча...</p>
    </article>

    <article class="card" v-else-if="pageError">
      <h2 class="section-title">Матч недоступен</h2>
      <p class="error-text">{{ pageError }}</p>
      <router-link class="btn-ghost" :to="backLinkTarget">{{ backLinkLabel }}</router-link>
    </article>

    <article class="card match-screen" v-else-if="match">
      <div class="match-topbar">
        <div class="match-topbar-actions">
          <router-link class="btn-ghost" :to="backLinkTarget">{{ backLinkArrowLabel }}</router-link>
          <button v-if="canDownloadProtocol" class="btn-ghost" type="button" @click="downloadProtocolPdf" :disabled="downloadingProtocolPdf">
            {{ downloadingProtocolPdf ? 'Подготовка PDF...' : 'Скачать протокол PDF' }}
          </button>
        </div>
        <span class="match-status-badge">{{ matchStatusLabel(match.protocol?.status) }}</span>
      </div>

      <p v-if="protocolDownloadError" class="error-text">{{ protocolDownloadError }}</p>

      <div class="match-hero">
        <div class="match-team-card">
          <img v-if="match.homeTeam.logoDataUrl" :src="match.homeTeam.logoDataUrl" :alt="match.homeTeam.name" class="match-team-logo" />
          <h2>{{ match.homeTeam.name }}</h2>
        </div>

        <div class="match-score-card">
          <p class="match-date">{{ formatDateTime(match.kickoffAt) }}</p>
          <div class="match-score">{{ matchScoreLabel(match.protocol) }}</div>
          <p v-if="protocolResultLabel(match.protocol)" class="match-result-note">{{ protocolResultLabel(match.protocol) }}</p>
          <p class="muted-text">{{ match.seasonName }} · {{ match.tourName }}</p>
        </div>

        <div class="match-team-card">
          <img v-if="match.awayTeam.logoDataUrl" :src="match.awayTeam.logoDataUrl" :alt="match.awayTeam.name" class="match-team-logo" />
          <h2>{{ match.awayTeam.name }}</h2>
        </div>
      </div>

      <section class="lineup-grid">
        <article class="match-section lineup-card" v-for="lineup in lineupCards" :key="lineup.teamId">
          <div class="section-head match-section-head">
            <div>
              <h3 class="section-title">Состав: {{ lineup.teamName }}</h3>
            </div>
            <span class="muted-text">{{ lineupSubmittedLabel(lineup) }}</span>
          </div>

          <ol class="lineup-list" v-if="lineup.players?.length">
            <li class="lineup-item" v-for="player in lineup.players" :key="player.playerId">
              <div class="lineup-player-main">
                <span class="lineup-order">{{ player.sortOrder }}</span>
                <div class="lineup-player-inline">
                  <span class="player-name-single-line">{{ player.playerName }}</span>
                  <span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span>
                  <span v-if="player.suspended" class="player-suspension-badge" :title="player.suspensionReason || 'Игрок дисквалифицирован'">Дискв.</span>
                  <div class="player-stat-icons" v-if="hasVisibleSavedStats(lineup.teamId, player.playerId)">
                    <div class="stat-icon-group" v-if="savedStatsFor(lineup.teamId, player.playerId).goals > 0" aria-label="Голы">
                      <span class="goal-ball" v-for="index in repeatCount(savedStatsFor(lineup.teamId, player.playerId).goals)" :key="`goal-${player.playerId}-${index}`">⚽</span>
                    </div>
                    <div class="stat-icon-group" v-if="savedStatsFor(lineup.teamId, player.playerId).yellowCards > 0" aria-label="Желтые карточки">
                      <span class="card-icon yellow-card" v-for="index in repeatCount(savedStatsFor(lineup.teamId, player.playerId).yellowCards)" :key="`yellow-${player.playerId}-${index}`"></span>
                    </div>
                    <div class="stat-icon-group" v-if="savedStatsFor(lineup.teamId, player.playerId).redCards > 0" aria-label="Красные карточки">
                      <span class="card-icon red-card" v-for="index in repeatCount(savedStatsFor(lineup.teamId, player.playerId).redCards)" :key="`red-${player.playerId}-${index}`"></span>
                    </div>
                  </div>
                </div>
              </div>

              <button
                v-if="canEditLineup(lineup.teamId)"
                class="btn-danger btn-compact"
                type="button"
                @click="removeLineupPlayer(lineup.teamId, player.playerId)"
                :disabled="Boolean(lineupSaving[lineup.teamId])"
              >
                Убрать
              </button>
            </li>
          </ol>
          <p class="empty-text" v-else>Заявка этой команды пока не подана.</p>

          <div v-if="canEditLineup(lineup.teamId)" class="lineup-editor">
            <div class="lineup-editor-head">
              <strong>Редактирование заявки</strong>
              <span class="muted-text">В заявке: {{ lineup.players?.length || 0 }}</span>
            </div>

            <p class="muted-text" v-if="lineup.availablePlayers?.length">Доступно к добавлению: {{ lineup.availablePlayers.length }}</p>
            <p class="muted-text" v-if="availableSelectableCount(lineup) !== lineup.availablePlayers?.length">Из них доступны для выбора: {{ availableSelectableCount(lineup) }}. Игроки с дисквалификацией заблокированы.</p>
            <p class="empty-text" v-else>Для этой команды сейчас нет доступных игроков для добавления в состав матча.</p>
            <p class="muted-text" v-if="suspendedAvailablePlayers(lineup).length">Недоступны: {{ suspendedAvailablePlayers(lineup).map((player) => player.playerName).join(', ') }}</p>

            <p class="error-text" v-if="lineupErrors[lineup.teamId]">{{ lineupErrors[lineup.teamId] }}</p>
            <p class="muted-text" v-else-if="lineupNotices[lineup.teamId]">{{ lineupNotices[lineup.teamId] }}</p>

            <div class="lineup-actions">
              <button
                class="btn-primary"
                type="button"
                @click="openAddPlayerModal(lineup.teamId)"
                :disabled="Boolean(lineupSaving[lineup.teamId]) || !availableSelectableCount(lineup)"
              >
                Добавить игрока
              </button>
              <button class="btn-ghost" type="button" @click="clearLineup(lineup.teamId)" :disabled="Boolean(lineupSaving[lineup.teamId]) || !lineup.players?.length">
                Очистить
              </button>
            </div>
          </div>
        </article>
      </section>

      <article class="match-section referee-summary-card">
        <div class="section-head match-section-head">
          <div>
            <h3 class="section-title">Судейская бригада</h3>
            <p class="muted-text">Состав арбитров, закрепленный в протоколе матча.</p>
          </div>
        </div>

        <div class="referee-summary-grid">
          <article v-for="item in protocolRefereeCards" :key="item.key" class="referee-summary-item">
            <span class="muted-text referee-role-label">{{ item.label }}</span>
            <strong class="referee-summary-name">{{ item.name }}</strong>
            <span class="muted-text referee-summary-meta">{{ item.meta || 'Город не указан' }}</span>
          </article>
        </div>
      </article>

      <article class="match-section protocol-editor-card" v-if="showProtocolEditor">
        <div class="section-head match-section-head">
          <div>
            <h3 class="section-title">Протокол матча</h3>
            <p class="muted-text">Администратор или рефери заполняет протокол по уже поданным составам. События формируются автоматически.</p>
          </div>
          <span class="muted-text">{{ protocolSaving ? 'Сохранение...' : protocolEditorRoleLabel }}</span>
        </div>

        <p class="error-text" v-if="protocolError">{{ protocolError }}</p>
        <p class="muted-text" v-else-if="protocolNotice">{{ protocolNotice }}</p>

        <p class="empty-text" v-if="!hasSubmittedLineups && !canBypassLineupsForProtocol">Администратор может сам сформировать составы обеих команд выше, а затем заполнить протокол.</p>
        <p class="muted-text" v-else-if="!hasSubmittedLineups">Тестовый режим SUPER_ADMIN: можно сохранить протокол, изменив только счет матча, без составов и статистики игроков.</p>

        <template v-if="hasSubmittedLineups || canBypassLineupsForProtocol">
          <div class="protocol-referee-grid">
            <label class="protocol-referee-field">
              <span class="protocol-referee-label">Главный арбитр</span>
              <select v-model="protocolDraft.chiefRefereeId" :disabled="protocolSaving">
                <option value="">— не выбран —</option>
                <option v-for="referee in availableRefereeOptions" :key="`chief-${referee.value}`" :value="referee.value">{{ referee.label }}</option>
              </select>
            </label>
            <label class="protocol-referee-field">
              <span class="protocol-referee-label">Помощник 1</span>
              <select v-model="protocolDraft.assistantRefereeOneId" :disabled="protocolSaving">
                <option value="">— не выбран —</option>
                <option v-for="referee in availableRefereeOptions" :key="`assistant-one-${referee.value}`" :value="referee.value">{{ referee.label }}</option>
              </select>
            </label>
            <label class="protocol-referee-field">
              <span class="protocol-referee-label">Помощник 2</span>
              <select v-model="protocolDraft.assistantRefereeTwoId" :disabled="protocolSaving">
                <option value="">— не выбран —</option>
                <option v-for="referee in availableRefereeOptions" :key="`assistant-two-${referee.value}`" :value="referee.value">{{ referee.label }}</option>
              </select>
            </label>
          </div>
          <p class="muted-text protocol-referee-empty" v-if="!availableRefereeOptions.length">Для сезона этого матча пока не привязано ни одного судьи.</p>

          <div class="protocol-layout-top">
            <article class="protocol-side-card protocol-side-card-left">
              <h4 class="section-title">{{ match.homeTeam.name }}</h4>
              <label class="technical-defeat-toggle">
                <input
                  :checked="protocolDraft.homeTechnicalDefeat"
                  :disabled="protocolSaving"
                  type="checkbox"
                  @change="toggleTechnicalDefeat('home', $event.target.checked)"
                />
                <span>Тех. пор.</span>
              </label>
            </article>

            <article class="protocol-score-center">
              <div class="protocol-score-label">Счет</div>
              <div class="protocol-score-inputs">
                <input v-model.number="protocolDraft.homeScore" :disabled="protocolSaving || isTechnicalDefeatDraft" min="0" type="number" class="score-square-input" />
                <span class="protocol-score-separator">:</span>
                <input v-model.number="protocolDraft.awayScore" :disabled="protocolSaving || isTechnicalDefeatDraft" min="0" type="number" class="score-square-input" />
              </div>
            </article>

            <article class="protocol-side-card protocol-side-card-right">
              <h4 class="section-title">{{ match.awayTeam.name }}</h4>
              <label class="technical-defeat-toggle">
                <input
                  :checked="protocolDraft.awayTechnicalDefeat"
                  :disabled="protocolSaving"
                  type="checkbox"
                  @change="toggleTechnicalDefeat('away', $event.target.checked)"
                />
                <span>Тех. пор.</span>
              </label>
            </article>
          </div>

          <p class="muted-text">{{ protocolScoreHint }}</p>

          <div class="admin-protocol-grid">
            <article class="protocol-team-card" v-for="team in adminProtocolTeams" :key="team.teamId">
              <div class="section-head match-section-head">
                <div>
                  <h4 class="section-title">{{ team.teamName }}</h4>
                  <p class="muted-text">Заполняй только маленькие поля напротив игроков из заявки.</p>
                </div>
              </div>

              <div class="protocol-player-list" v-if="team.players.length">
                <article class="protocol-player-row" v-for="player in team.players" :key="player.playerId">
                  <div class="protocol-player-name">
                    <span class="lineup-order">{{ player.sortOrder }}</span>
                    <span class="player-name-single-line">{{ player.playerName }}</span>
                    <span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span>
                  </div>

                  <div class="player-stat-inputs">
                    <label class="tiny-field">
                      <span>Г</span>
                      <input v-model.number="player.goals" :disabled="protocolSaving || isTechnicalDefeatDraft" min="0" type="number" class="micro-input" />
                    </label>

                    <label class="tiny-field">
                      <span>ЖК</span>
                      <input v-model.number="player.yellowCards" :disabled="protocolSaving || isTechnicalDefeatDraft" min="0" type="number" class="micro-input" />
                    </label>

                    <label class="tiny-field">
                      <span>КК</span>
                      <input v-model.number="player.redCards" :disabled="protocolSaving || isTechnicalDefeatDraft" min="0" type="number" class="micro-input" />
                    </label>
                  </div>
                </article>
              </div>

              <p class="empty-text" v-else>Для этой команды пока нет игроков в заявке матча.</p>
            </article>
          </div>

          <div class="protocol-editor-actions protocol-editor-actions-bottom">
            <button class="btn-ghost" type="button" @click="resetProtocolDraft" :disabled="protocolSaving">Сбросить</button>
            <button class="btn-primary" type="button" @click="saveProtocol(false)" :disabled="protocolSaving">{{ protocolSaving ? 'Сохранение...' : 'Сохранить' }}</button>
            <button class="btn-primary" type="button" @click="saveProtocol(true)" :disabled="protocolSaving">Подтвердить протокол</button>
          </div>
        </template>
      </article>

      <article class="match-section protocol-editor-card" v-else-if="canReopenVerifiedProtocol">
        <div class="section-head match-section-head">
          <div>
            <h3 class="section-title">Протокол матча</h3>
            <p class="muted-text">Протокол подтвержден и открыт только для просмотра. Чтобы снова менять счет, составы и статистику, сначала переведите его обратно в редактирование.</p>
          </div>
          <span class="muted-text">SUPER_ADMIN</span>
        </div>

        <p class="error-text" v-if="protocolError">{{ protocolError }}</p>
        <p class="muted-text" v-else-if="protocolNotice">{{ protocolNotice }}</p>

        <div class="protocol-editor-actions protocol-editor-actions-bottom">
          <button class="btn-primary" type="button" @click="reopenVerifiedProtocol" :disabled="protocolSaving">
            {{ protocolSaving ? 'Сохранение...' : 'Изменить протокол' }}
          </button>
        </div>
      </article>

      <div v-if="activeLineupForModal" class="modal-backdrop" @click.self="closeAddPlayerModal">
        <article class="card auth-modal team-rep-modal lineup-modal">
          <div class="toolbar auth-modal-head">
            <div>
              <h3 class="section-title">Добавить игрока в состав</h3>
              <p class="muted-text">{{ activeLineupForModal.teamName }} · {{ match.seasonName }}</p>
            </div>
            <button class="btn-ghost" type="button" @click="closeAddPlayerModal">Закрыть</button>
          </div>

          <div class="lineup-modal-body">
            <SearchableSelect
              v-model="selectedAvailablePlayerId"
              :options="activeLineupPlayerOptions"
              placeholder="Выберите игрока"
              search-placeholder="Начните вводить ФИО игрока"
              empty-text="Игрок по такому ФИО не найден"
            />

            <div v-if="suspendedAvailablePlayers(activeLineupForModal).length" class="lineup-suspension-list">
              <p class="muted-text">Дисквалифицированы на этот матч:</p>
              <p v-for="player in suspendedAvailablePlayers(activeLineupForModal)" :key="`susp-${player.playerId}`" class="muted-text">
                {{ player.playerName }}: {{ player.suspensionReason || 'Игрок временно недоступен' }}
              </p>
            </div>

            <p class="error-text" v-if="lineupErrors[activeLineupForModal.teamId]">{{ lineupErrors[activeLineupForModal.teamId] }}</p>

            <div class="lineup-actions">
              <button
                class="btn-primary"
                type="button"
                @click="addLineupPlayer"
                :disabled="Boolean(lineupSaving[activeLineupForModal.teamId]) || !selectedAvailablePlayerId"
              >
                {{ lineupSaving[activeLineupForModal.teamId] ? 'Сохранение...' : 'Добавить' }}
              </button>
            </div>
          </div>
        </article>
      </div>
    </article>
  </section>
</template>

<script setup>
import SearchableSelect from '../components/SearchableSelect.vue'
import { useMatchPage } from '../composables/useMatchPage.js'

const {
  route,
  match,
  loading,
  pageError,
  lineupSaving,
  lineupErrors,
  lineupNotices,
  addPlayerModalTeamId,
  selectedAvailablePlayerId,
  protocolSaving,
  protocolError,
  protocolNotice,
  protocolDownloadError,
  downloadingProtocolPdf,
  protocolDraft,
  backLinkTarget,
  backLinkLabel,
  backLinkArrowLabel,
  lineupCards,
  activeLineupForModal,
  activeLineupPlayerOptions,
  savedStatsMap,
  hasSubmittedLineups,
  canBypassLineupsForProtocol,
  isVerifiedProtocol,
  canDownloadProtocol,
  canReopenVerifiedProtocol,
  showProtocolEditor,
  isTechnicalDefeatDraft,
  protocolScoreHint,
  adminProtocolTeams,
  loadMatch,
  canEditProtocol,
  protocolEditorRoleLabel,
  availableRefereeOptions,
  protocolRefereeCards,
  downloadProtocolPdf,
  canEditLineup,
  lineupByTeamId,
  refreshMatch,
  openAddPlayerModal,
  closeAddPlayerModal,
  addLineupPlayer,
  removeLineupPlayer,
  clearLineup,
  saveLineup,
  createEmptyProtocolDraft,
  syncProtocolDraft,
  resetProtocolDraft,
  saveProtocol,
  reopenVerifiedProtocol,
  buildProtocolPayload,
  toggleTechnicalDefeat,
  hasAnyProtocolStats,
  findOrCreateDraftPlayerStat,
  buildSavedStatsMap,
  savedStatsFor,
  hasVisibleSavedStats,
  sumGoals,
  normalizeNonNegative,
  statKey,
  emptyStats,
  repeatCount,
  formatPlayerOptionLabel,
  availableSelectableCount,
  suspendedAvailablePlayers,
  formatDateTime,
  matchStatusLabel,
  matchScoreLabel,
  protocolResultLabel,
  lineupSubmittedLabel,
  refereeOptionLabel,
  buildProtocolRefereeCard,
} = useMatchPage()
</script>

<style scoped src="../styles/pages/match.css"></style>
