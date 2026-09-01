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

          <div v-if="(showProtocolEditor || isVerifiedProtocol) && lineup.players?.length" class="lineup-stats-head" aria-hidden="true">
            <span class="lineup-stats-player-label">Игрок</span>
            <span class="lineup-stat-heading goals-heading" title="Голы">⚽</span>
            <span class="lineup-stat-heading yellow-heading" title="Желтые карточки"><span class="heading-card"></span></span>
            <span class="lineup-stat-heading red-heading" title="Красные карточки"><span class="heading-card"></span></span>
          </div>

          <div v-if="lineup.players?.length" class="lineup-groups">
            <section v-for="group in lineupPlayerGroups(lineup)" :key="`${lineup.teamId}-${group.key}`" class="lineup-group">
              <div class="lineup-group-head">
                <strong>{{ group.title }}</strong>
                <span>{{ group.countLabel }}</span>
              </div>
              <ol class="lineup-list">
            <li class="lineup-item" :class="{ 'lineup-item-with-stats': showProtocolEditor || isVerifiedProtocol }" v-for="player in group.players" :key="player.playerId">
              <div class="lineup-player-main">
                <span class="lineup-order">{{ player.sortOrder }}</span>
                <div class="lineup-player-inline">
                  <span class="player-name-single-line">{{ lineupPlayerDisplayName(lineup, player) }}</span>
                  <span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span>
                  <span v-if="player.suspended" class="player-suspension-badge" :title="player.suspensionReason || 'Игрок дисквалифицирован'">Дискв.</span>
                </div>
              </div>

              <template v-if="showProtocolEditor">
                <label class="lineup-stat-cell goals-stat-cell" title="Голы">
                  <input
                    v-model.number="findOrCreateDraftPlayerStat(lineup, player).goals"
                    :disabled="protocolSaving || isTechnicalDefeatDraft"
                    min="0"
                    type="number"
                    class="lineup-stat-input"
                    aria-label="Голы"
                  />
                </label>
                <label class="lineup-stat-cell yellow-stat-cell" title="Желтые карточки">
                  <input
                    v-model.number="findOrCreateDraftPlayerStat(lineup, player).yellowCards"
                    :disabled="protocolSaving || isTechnicalDefeatDraft"
                    min="0"
                    type="number"
                    class="lineup-stat-input"
                    aria-label="Желтые карточки"
                  />
                </label>
                <label class="lineup-stat-cell red-stat-cell" title="Красные карточки">
                  <input
                    v-model.number="findOrCreateDraftPlayerStat(lineup, player).redCards"
                    :disabled="protocolSaving || isTechnicalDefeatDraft"
                    min="0"
                    type="number"
                    class="lineup-stat-input"
                    aria-label="Красные карточки"
                  />
                </label>
              </template>

              <template v-else-if="isVerifiedProtocol">
                <span class="lineup-stat-cell goals-stat-cell">
                  <span
                    v-if="savedStatsFor(lineup.teamId, player.playerId).goals"
                    class="lineup-stat-marker goal-marker"
                    :title="`Голы: ${savedStatsFor(lineup.teamId, player.playerId).goals}`"
                    :aria-label="`Голы: ${savedStatsFor(lineup.teamId, player.playerId).goals}`"
                  >
                    <span class="soccer-ball">⚽</span>
                    <span v-if="savedStatsFor(lineup.teamId, player.playerId).goals > 1" class="stat-count-badge">{{ savedStatsFor(lineup.teamId, player.playerId).goals }}</span>
                  </span>
                </span>
                <span class="lineup-stat-cell yellow-stat-cell">
                  <span
                    v-if="savedStatsFor(lineup.teamId, player.playerId).yellowCards"
                    class="lineup-stat-marker card-marker yellow-card-marker"
                    :title="`Желтые карточки: ${savedStatsFor(lineup.teamId, player.playerId).yellowCards}`"
                    :aria-label="`Желтые карточки: ${savedStatsFor(lineup.teamId, player.playerId).yellowCards}`"
                  >
                    <span v-if="savedStatsFor(lineup.teamId, player.playerId).yellowCards > 1" class="stat-count-badge">{{ savedStatsFor(lineup.teamId, player.playerId).yellowCards }}</span>
                  </span>
                </span>
                <span class="lineup-stat-cell red-stat-cell">
                  <span
                    v-if="savedStatsFor(lineup.teamId, player.playerId).redCards"
                    class="lineup-stat-marker card-marker red-card-marker"
                    :title="`Красные карточки: ${savedStatsFor(lineup.teamId, player.playerId).redCards}`"
                    :aria-label="`Красные карточки: ${savedStatsFor(lineup.teamId, player.playerId).redCards}`"
                  >
                    <span v-if="savedStatsFor(lineup.teamId, player.playerId).redCards > 1" class="stat-count-badge">{{ savedStatsFor(lineup.teamId, player.playerId).redCards }}</span>
                  </span>
                </span>
              </template>

            </li>
              </ol>
            </section>
          </div>
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
                :disabled="Boolean(lineupSaving[lineup.teamId])"
              >
                {{ lineup.players?.length ? 'Изменить состав' : 'Заполнить состав' }}
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
              <h3 class="section-title">Состав на матч</h3>
              <p class="muted-text">{{ activeLineupForModal.teamName }} · {{ match.seasonName }}</p>
            </div>
            <button class="btn-ghost" type="button" @click="closeAddPlayerModal">Закрыть</button>
          </div>

          <div class="lineup-modal-body">
            <div class="lineup-selection-section">
              <div class="lineup-selection-head">
                <strong>Основной состав</strong>
                <span :class="{ 'is-complete': starterCountIsValid }">{{ selectedStarterPlayerIds.length }} / {{ requiredStarterCount }}</span>
              </div>
              <SearchableSelect
                v-model="selectedStarterPlayerIds"
                :options="starterPlayerOptions"
                multiple
                placeholder="Выберите основной состав"
                search-placeholder="Найдите игрока основного состава"
                empty-text="Игрок по такому ФИО не найден"
                multiple-summary-text="Выбрано основных"
                multiple-action-hint="Количество должно соответствовать формату сезона"
              />
            </div>

            <div class="lineup-selection-section">
              <div class="lineup-selection-head">
                <strong>Запасные</strong>
                <span class="is-complete">{{ selectedSubstitutePlayerIds.length }}</span>
              </div>
              <SearchableSelect
                v-model="selectedSubstitutePlayerIds"
                :options="substitutePlayerOptions"
                multiple
                placeholder="Выберите запасных"
                search-placeholder="Найдите запасного игрока"
                empty-text="Игрок по такому ФИО не найден"
                multiple-summary-text="Выбрано запасных"
                multiple-action-hint="Количество запасных не ограничено"
              />
            </div>

            <div v-if="suspendedAvailablePlayers(activeLineupForModal).length" class="lineup-suspension-list">
              <p class="muted-text">Дисквалифицированы на этот матч:</p>
              <p v-for="player in suspendedAvailablePlayers(activeLineupForModal)" :key="`susp-${player.playerId}`" class="muted-text">
                {{ player.playerName }}: {{ player.suspensionReason || 'Игрок временно недоступен' }}
              </p>
            </div>

            <p class="error-text" v-if="lineupErrors[activeLineupForModal.teamId]">{{ lineupErrors[activeLineupForModal.teamId] }}</p>
            <p class="error-text" v-else-if="!starterCountIsValid">Выберите ровно {{ requiredStarterCount }} игроков основного состава.</p>

            <div class="lineup-actions">
              <button
                class="btn-primary"
                type="button"
                @click="saveLineupSelection"
                :disabled="Boolean(lineupSaving[activeLineupForModal.teamId]) || !starterCountIsValid"
              >
                {{ lineupSaving[activeLineupForModal.teamId] ? 'Сохранение...' : 'Сохранить состав' }}
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
  match,
  loading,
  pageError,
  lineupSaving,
  lineupErrors,
  lineupNotices,
  selectedStarterPlayerIds,
  selectedSubstitutePlayerIds,
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
  starterPlayerOptions,
  substitutePlayerOptions,
  requiredStarterCount,
  starterCountIsValid,
  hasSubmittedLineups,
  canBypassLineupsForProtocol,
  isVerifiedProtocol,
  canDownloadProtocol,
  canReopenVerifiedProtocol,
  showProtocolEditor,
  isTechnicalDefeatDraft,
  protocolScoreHint,
  protocolEditorRoleLabel,
  availableRefereeOptions,
  protocolRefereeCards,
  downloadProtocolPdf,
  canEditLineup,
  openAddPlayerModal,
  closeAddPlayerModal,
  saveLineupSelection,
  resetProtocolDraft,
  saveProtocol,
  reopenVerifiedProtocol,
  toggleTechnicalDefeat,
  savedStatsFor,
  findOrCreateDraftPlayerStat,
  lineupPlayerDisplayName,
  lineupPlayerGroups,
  availableSelectableCount,
  suspendedAvailablePlayers,
  formatDateTime,
  matchStatusLabel,
  matchScoreLabel,
  protocolResultLabel,
  lineupSubmittedLabel,
} = useMatchPage()
</script>

<style scoped src="../styles/pages/match.css"></style>
