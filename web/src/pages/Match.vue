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
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '../store/auth'
import SearchableSelect from '../components/SearchableSelect.vue'

const route = useRoute()
const { optionalAuthApiRequest, optionalAuthApiRequestRaw, authorizedApiRequest, user, hasRole } = useAuth()

const match = ref(null)
const loading = ref(false)
const pageError = ref('')
const lineupSaving = ref({})
const lineupErrors = ref({})
const lineupNotices = ref({})
const addPlayerModalTeamId = ref(null)
const selectedAvailablePlayerId = ref('')
const protocolSaving = ref(false)
const protocolError = ref('')
const protocolNotice = ref('')
const protocolDownloadError = ref('')
const downloadingProtocolPdf = ref(false)
const protocolDraft = ref(createEmptyProtocolDraft())

const backLinkTarget = computed(() => {
  if (route.query.from === 'team-profile' && route.query.teamId) {
    return `/teams/${encodeURIComponent(String(route.query.teamId))}`
  }
  return '/'
})

const backLinkLabel = computed(() => {
  if (route.query.from === 'team-profile' && route.query.teamId) {
    return 'Вернуться в профиль команды'
  }
  return 'Вернуться на главную'
})

const backLinkArrowLabel = computed(() => {
  if (route.query.from === 'team-profile' && route.query.teamId) {
    return '← В профиль команды'
  }
  return '← На главную'
})

const lineupCards = computed(() => {
  if (!match.value) return []
  return [match.value.homeLineup, match.value.awayLineup].filter(Boolean)
})

const activeLineupForModal = computed(() => {
  return lineupCards.value.find((lineup) => String(lineup.teamId) === String(addPlayerModalTeamId.value)) || null
})

const activeLineupPlayerOptions = computed(() => {
  return (activeLineupForModal.value?.availablePlayers || []).map((player) => ({
    value: String(player.playerId),
    label: formatPlayerOptionLabel(player),
    keywords: `${player.playerName || ''}`,
    disabled: Boolean(player.suspended),
  }))
})

const savedStatsMap = computed(() => buildSavedStatsMap(match.value?.protocol?.events || []))

const hasSubmittedLineups = computed(() => {
  return lineupCards.value.length === 2 && lineupCards.value.every((lineup) => Boolean(lineup.submittedAt))
})

const canBypassLineupsForProtocol = computed(() => hasRole('SUPER_ADMIN'))

const isVerifiedProtocol = computed(() => {
  return match.value?.protocol?.status === 'VERIFIED'
})

const canDownloadProtocol = computed(() => isVerifiedProtocol.value)

const canReopenVerifiedProtocol = computed(() => {
  return canEditProtocol() && isVerifiedProtocol.value
})

const showProtocolEditor = computed(() => {
  return canEditProtocol() && !isVerifiedProtocol.value
})

const isTechnicalDefeatDraft = computed(() => {
  return Boolean(protocolDraft.value.homeTechnicalDefeat || protocolDraft.value.awayTechnicalDefeat)
})

const protocolScoreHint = computed(() => {
  if (!hasSubmittedLineups.value && canBypassLineupsForProtocol.value) {
    return 'Тестовый режим SUPER_ADMIN: счет можно сохранить без голов по игрокам и без поданных заявок.'
  }
  if (isTechnicalDefeatDraft.value) {
    return 'При тех. поражении счет выставляется автоматически: 0:3.'
  }
  return 'Если тех.поражение не включено, сумма голов по игрокам должна совпадать со счетом матча.'
})

const adminProtocolTeams = computed(() => {
  return lineupCards.value.map((lineup) => ({
    teamId: lineup.teamId,
    teamName: lineup.teamName,
    players: (lineup.players || []).map((player) => findOrCreateDraftPlayerStat(lineup, player)),
  }))
})

watch(
  () => route.params.id,
  async (matchId) => {
    if (!matchId) {
      match.value = null
      return
    }
    await loadMatch(matchId)
  },
  { immediate: true }
)

async function loadMatch(matchId) {
  loading.value = true
  pageError.value = ''
  protocolDownloadError.value = ''

  try {
    const payload = await optionalAuthApiRequest(`/api/matches/${encodeURIComponent(matchId)}`, {
      method: 'GET',
    })
    match.value = payload
    lineupErrors.value = {}
    syncProtocolDraft(payload)
  } catch (error) {
    match.value = null
    resetProtocolDraft()
    pageError.value = error.message || 'Не удалось загрузить матч.'
  } finally {
    loading.value = false
  }
}

function canEditProtocol() {
  return hasRole('SUPER_ADMIN') || hasRole('REFEREE')
}

const protocolEditorRoleLabel = computed(() => {
  if (hasRole('SUPER_ADMIN')) return 'SUPER_ADMIN'
  if (hasRole('REFEREE')) return 'REFEREE'
  return ''
})

const availableRefereeOptions = computed(() => {
  return (match.value?.availableReferees || []).map((referee) => ({
    value: String(referee.id),
    label: refereeOptionLabel(referee),
  }))
})

const protocolRefereeCards = computed(() => {
  const protocol = match.value?.protocol || {}
  return [
    buildProtocolRefereeCard('chief', 'Главный арбитр', protocol.chiefReferee),
    buildProtocolRefereeCard('assistant-1', 'Помощник 1', protocol.assistantRefereeOne),
    buildProtocolRefereeCard('assistant-2', 'Помощник 2', protocol.assistantRefereeTwo),
  ]
})

async function downloadProtocolPdf() {
  if (!match.value || !canDownloadProtocol.value) return

  downloadingProtocolPdf.value = true
  protocolDownloadError.value = ''

  try {
    const response = await optionalAuthApiRequestRaw(`/api/matches/${encodeURIComponent(match.value.id)}/protocol/pdf`, {
      method: 'GET',
    })
    const pdfBlob = await response.blob()
    const disposition = response.headers.get('content-disposition') || ''
    const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const fileName = fileNameMatch ? decodeURIComponent(fileNameMatch[1]) : 'match-protocol.pdf'
    const objectUrl = URL.createObjectURL(pdfBlob)

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
  } catch (error) {
    protocolDownloadError.value = error.message || 'Не удалось скачать PDF протокола.'
  } finally {
    downloadingProtocolPdf.value = false
  }
}

function canEditLineup(teamId) {
  if (!user.value) return false
  if (isVerifiedProtocol.value) return false
  if (hasRole('SUPER_ADMIN')) return true
  if (!hasRole('TEAM_REP')) return false
  return String(user.value.teamId || '') === String(teamId) && Boolean(user.value.teamScope?.canEditRoster)
}

function lineupByTeamId(teamId) {
  return lineupCards.value.find((lineup) => String(lineup.teamId) === String(teamId)) || null
}

async function refreshMatch() {
  if (!match.value?.id) return

  const payload = await optionalAuthApiRequest(`/api/matches/${encodeURIComponent(match.value.id)}`, {
    method: 'GET',
  })
  match.value = payload
  syncProtocolDraft(payload)
}

async function openAddPlayerModal(teamId) {
  if (!match.value) return

  pageError.value = ''
  await refreshMatch()
  const lineup = lineupByTeamId(teamId)
  if (!availableSelectableCount(lineup)) {
    lineupNotices.value = {
      ...lineupNotices.value,
      [teamId]: lineup?.availablePlayers?.length
        ? 'Все оставшиеся игроки этой команды сейчас дисквалифицированы на матч.'
        : 'Для этой команды сейчас нет доступных игроков для добавления.',
    }
    return
  }

  addPlayerModalTeamId.value = teamId
  selectedAvailablePlayerId.value = ''
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }
}

function closeAddPlayerModal() {
  addPlayerModalTeamId.value = null
  selectedAvailablePlayerId.value = ''
}

async function addLineupPlayer() {
  if (!activeLineupForModal.value || !selectedAvailablePlayerId.value) return

  const currentIds = Array.isArray(activeLineupForModal.value.players)
    ? activeLineupForModal.value.players.map((player) => player.playerId)
    : []

  await saveLineup(
    activeLineupForModal.value.teamId,
    [...currentIds, Number(selectedAvailablePlayerId.value)],
    'Игрок добавлен в заявку.'
  )

  if (!lineupErrors.value[activeLineupForModal.value.teamId]) {
    closeAddPlayerModal()
  }
}

async function removeLineupPlayer(teamId, playerId) {
  const lineup = lineupByTeamId(teamId)
  if (!lineup) return

  const nextIds = Array.isArray(lineup.players)
    ? lineup.players.map((player) => player.playerId).filter((id) => id !== playerId)
    : []

  await saveLineup(teamId, nextIds, 'Игрок убран из заявки.')
}

async function clearLineup(teamId) {
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }

  await saveLineup(teamId, [], 'Заявка очищена.')
}

async function saveLineup(teamId, playerIds, successMessage) {
  if (!match.value) return

  lineupSaving.value = {
    ...lineupSaving.value,
    [teamId]: true,
  }
  lineupErrors.value = {
    ...lineupErrors.value,
    [teamId]: '',
  }
  lineupNotices.value = {
    ...lineupNotices.value,
    [teamId]: '',
  }

  try {
    const payload = await authorizedApiRequest(
      `/api/matches/${encodeURIComponent(match.value.id)}/lineups/${encodeURIComponent(teamId)}`,
      {
        method: 'PUT',
        body: JSON.stringify({ playerIds }),
      }
    )
    match.value = payload
    syncProtocolDraft(payload)
    lineupNotices.value = {
      ...lineupNotices.value,
      [teamId]: successMessage || 'Заявка сохранена.',
    }
  } catch (error) {
    lineupErrors.value = {
      ...lineupErrors.value,
      [teamId]: error.message || 'Не удалось сохранить заявку.',
    }
  } finally {
    lineupSaving.value = {
      ...lineupSaving.value,
      [teamId]: false,
    }
  }
}

function createEmptyProtocolDraft() {
  return {
    homeScore: 0,
    awayScore: 0,
    homeTechnicalDefeat: false,
    awayTechnicalDefeat: false,
    chiefRefereeId: '',
    assistantRefereeOneId: '',
    assistantRefereeTwoId: '',
    playerStats: [],
  }
}

function syncProtocolDraft(payload) {
  const protocol = payload?.protocol || {}
  const nextPlayerStats = []
  const savedMap = buildSavedStatsMap(protocol.events || [])

  for (const lineup of [payload?.homeLineup, payload?.awayLineup].filter(Boolean)) {
    for (const player of lineup.players || []) {
      const key = statKey(lineup.teamId, player.playerId)
      const saved = savedMap.get(key) || emptyStats()
      nextPlayerStats.push({
        teamId: lineup.teamId,
        teamName: lineup.teamName,
        playerId: player.playerId,
        playerName: player.playerName,
        sortOrder: player.sortOrder,
        goals: saved.goals,
        yellowCards: saved.yellowCards,
        redCards: saved.redCards,
      })
    }
  }

  protocolDraft.value = {
    homeScore: Number.isInteger(protocol.homeScore) ? protocol.homeScore : 0,
    awayScore: Number.isInteger(protocol.awayScore) ? protocol.awayScore : 0,
    homeTechnicalDefeat: Boolean(protocol.homeTechnicalDefeat),
    awayTechnicalDefeat: Boolean(protocol.awayTechnicalDefeat),
    chiefRefereeId: protocol.chiefReferee?.id ? String(protocol.chiefReferee.id) : '',
    assistantRefereeOneId: protocol.assistantRefereeOne?.id ? String(protocol.assistantRefereeOne.id) : '',
    assistantRefereeTwoId: protocol.assistantRefereeTwo?.id ? String(protocol.assistantRefereeTwo.id) : '',
    playerStats: nextPlayerStats,
  }
  protocolError.value = ''
  protocolNotice.value = ''
}

function resetProtocolDraft() {
  if (match.value) {
    syncProtocolDraft(match.value)
    return
  }
  protocolDraft.value = createEmptyProtocolDraft()
  protocolError.value = ''
  protocolNotice.value = ''
}

async function saveProtocol(asVerified) {
  if (!match.value) return
  if (!hasSubmittedLineups.value && !canBypassLineupsForProtocol.value) {
    protocolError.value = 'Сначала нужно подать обе заявки на матч.'
    return
  }

  protocolSaving.value = true
  protocolError.value = ''
  protocolNotice.value = ''

  try {
    const payload = buildProtocolPayload(asVerified)
    const response = await authorizedApiRequest(`/api/matches/${encodeURIComponent(match.value.id)}/protocol`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    match.value = response
    syncProtocolDraft(response)
    protocolNotice.value = asVerified ? 'Протокол подтвержден.' : 'Протокол сохранен.'
  } catch (error) {
    protocolError.value = error.message || 'Не удалось сохранить протокол матча.'
  } finally {
    protocolSaving.value = false
  }
}

async function reopenVerifiedProtocol() {
  if (!match.value?.id) return

  protocolSaving.value = true
  protocolError.value = ''
  protocolNotice.value = ''

  try {
    const response = await authorizedApiRequest(`/api/matches/${encodeURIComponent(match.value.id)}/protocol/reopen`, {
      method: 'POST',
    })
    match.value = response
    syncProtocolDraft(response)
    protocolNotice.value = 'Протокол выведен из подтвержденного статуса. Теперь его можно редактировать.'
  } catch (error) {
    protocolError.value = error.message || 'Не удалось перевести протокол обратно в редактирование.'
  } finally {
    protocolSaving.value = false
  }
}

function buildProtocolPayload(asVerified) {
  const homeTechnicalDefeat = Boolean(protocolDraft.value.homeTechnicalDefeat)
  const awayTechnicalDefeat = Boolean(protocolDraft.value.awayTechnicalDefeat)
  const normalizedStats = protocolDraft.value.playerStats.map((playerStat) => ({
    teamId: playerStat.teamId,
    playerId: playerStat.playerId,
    goals: normalizeNonNegative(playerStat.goals),
    yellowCards: normalizeNonNegative(playerStat.yellowCards),
    redCards: normalizeNonNegative(playerStat.redCards),
  }))
  const hasRecordedPlayerStats = normalizedStats.some((playerStat) => hasAnyProtocolStats(playerStat))

  let homeScore = normalizeNonNegative(protocolDraft.value.homeScore)
  let awayScore = normalizeNonNegative(protocolDraft.value.awayScore)

  if (homeTechnicalDefeat) {
    homeScore = 0
    awayScore = 3
  }
  if (awayTechnicalDefeat) {
    homeScore = 3
    awayScore = 0
  }

  if (!homeTechnicalDefeat && !awayTechnicalDefeat) {
    const canSaveScoreOnly = canBypassLineupsForProtocol.value && !hasRecordedPlayerStats
    const homeGoals = sumGoals(match.value.homeTeam.id)
    const awayGoals = sumGoals(match.value.awayTeam.id)
    if (!canSaveScoreOnly && (homeGoals !== homeScore || awayGoals !== awayScore)) {
      throw new Error('Сумма голов по игрокам должна совпадать со счетом матча.')
    }
  }

  return {
    status: asVerified ? 'VERIFIED' : 'FINISHED',
    homeScore,
    awayScore,
    homeTechnicalDefeat,
    awayTechnicalDefeat,
    bestPlayerId: null,
    chiefRefereeId: protocolDraft.value.chiefRefereeId ? Number(protocolDraft.value.chiefRefereeId) : null,
    assistantRefereeOneId: protocolDraft.value.assistantRefereeOneId ? Number(protocolDraft.value.assistantRefereeOneId) : null,
    assistantRefereeTwoId: protocolDraft.value.assistantRefereeTwoId ? Number(protocolDraft.value.assistantRefereeTwoId) : null,
    notes: null,
    startedAt: null,
    finishedAt: null,
    playerStats: normalizedStats,
  }
}

function toggleTechnicalDefeat(side, checked) {
  const nextDraft = {
    ...protocolDraft.value,
    homeTechnicalDefeat: side === 'home' ? checked : checked ? false : protocolDraft.value.homeTechnicalDefeat,
    awayTechnicalDefeat: side === 'away' ? checked : checked ? false : protocolDraft.value.awayTechnicalDefeat,
    playerStats: protocolDraft.value.playerStats.map((playerStat) => ({
      ...playerStat,
      goals: 0,
      yellowCards: 0,
      redCards: 0,
    })),
  }

  if (checked) {
    if (side === 'home') {
      nextDraft.homeScore = 0
      nextDraft.awayScore = 3
    } else {
      nextDraft.homeScore = 3
      nextDraft.awayScore = 0
    }
  } else if (!nextDraft.homeTechnicalDefeat && !nextDraft.awayTechnicalDefeat) {
    nextDraft.homeScore = 0
    nextDraft.awayScore = 0
  }

  protocolDraft.value = nextDraft
}

function hasAnyProtocolStats(playerStat) {
  return Number(playerStat?.goals || 0) > 0
    || Number(playerStat?.yellowCards || 0) > 0
    || Number(playerStat?.redCards || 0) > 0
}

function findOrCreateDraftPlayerStat(lineup, player) {
  const existing = protocolDraft.value.playerStats.find(
    (item) => item.teamId === lineup.teamId && item.playerId === player.playerId
  )
  if (existing) return existing

  const created = {
    teamId: lineup.teamId,
    teamName: lineup.teamName,
    playerId: player.playerId,
    playerName: player.playerName,
    sortOrder: player.sortOrder,
    goals: 0,
    yellowCards: 0,
    redCards: 0,
  }
  protocolDraft.value.playerStats.push(created)
  return created
}

function buildSavedStatsMap(events) {
  const map = new Map()

  for (const event of events || []) {
    if (!event?.teamId || !event?.playerId) continue
    const key = statKey(event.teamId, event.playerId)
    const current = map.get(key) || emptyStats()
    if (event.eventType === 'GOAL' || event.eventType === 'PENALTY_GOAL') current.goals += 1
    if (event.eventType === 'YELLOW_CARD') current.yellowCards += 1
    if (event.eventType === 'RED_CARD' || event.eventType === 'SECOND_YELLOW_RED') current.redCards += 1
    map.set(key, current)
  }

  return map
}

function savedStatsFor(teamId, playerId) {
  return savedStatsMap.value.get(statKey(teamId, playerId)) || emptyStats()
}

function hasVisibleSavedStats(teamId, playerId) {
  const stats = savedStatsFor(teamId, playerId)
  return stats.goals > 0 || stats.yellowCards > 0 || stats.redCards > 0
}

function sumGoals(teamId) {
  return protocolDraft.value.playerStats
    .filter((item) => item.teamId === teamId)
    .reduce((total, item) => total + normalizeNonNegative(item.goals), 0)
}

function normalizeNonNegative(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0) return 0
  return Math.floor(parsed)
}

function statKey(teamId, playerId) {
  return `${teamId}:${playerId}`
}

function emptyStats() {
  return { goals: 0, yellowCards: 0, redCards: 0 }
}

function repeatCount(count) {
  return Array.from({ length: Math.max(0, count) }, (_, index) => index + 1)
}

function formatPlayerOptionLabel(player) {
  if (!player) return ''
  return `${player.playerName || ''}`
}

function availableSelectableCount(lineup) {
  if (!lineup?.availablePlayers?.length) return 0
  return lineup.availablePlayers.filter((player) => !player.suspended).length
}

function suspendedAvailablePlayers(lineup) {
  if (!lineup?.availablePlayers?.length) return []
  return lineup.availablePlayers.filter((player) => player.suspended)
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function matchStatusLabel(status) {
  if (status === 'LIVE') return 'Матч идет'
  if (status === 'FINISHED') return 'Матч завершен'
  if (status === 'VERIFIED') return 'Протокол подтвержден'
  if (status === 'LINEUPS_SUBMITTED') return 'Заявки поданы'
  return 'Матч запланирован'
}

function matchScoreLabel(protocol) {
  const homeScore = Number.isInteger(protocol?.homeScore) ? protocol.homeScore : 0
  const awayScore = Number.isInteger(protocol?.awayScore) ? protocol.awayScore : 0
  return `${homeScore} : ${awayScore}`
}

function protocolResultLabel(protocol) {
  if (!match.value || !protocol) return ''
  if (protocol.homeTechnicalDefeat || protocol.awayTechnicalDefeat) return 'Тех. пор.'
  return ''
}

function lineupSubmittedLabel(lineup) {
  if (!lineup?.submittedAt) return 'Не подана'
  return `Подана ${formatDateTime(lineup.submittedAt)}`
}

function refereeOptionLabel(referee) {
  if (!referee) return ''
  const city = referee.city ? `, ${referee.city}` : ''
  return `${referee.fullName || ''}${city}`
}

function buildProtocolRefereeCard(key, label, referee) {
  return {
    key,
    label,
    name: referee?.fullName || 'Не назначен',
    meta: referee?.city || '',
  }
}

</script>

<style scoped>
.match-screen {
  display: grid;
  gap: 22px;
}

.match-topbar,
.lineup-editor-head,
.lineup-actions,
.protocol-editor-actions,
.technical-defeat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.match-topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.match-hero,
.lineup-grid,
.admin-protocol-grid,
.protocol-layout-top {
  display: grid;
  gap: 16px;
}

.match-hero {
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 18px;
}

.lineup-grid,
.admin-protocol-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.lineup-grid {
  align-items: start;
}

.protocol-layout-top {
  grid-template-columns: minmax(220px, 1fr) auto minmax(220px, 1fr);
  align-items: stretch;
}

.match-status-badge,
.lineup-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-weight: 700;
}

.match-status-badge {
  padding: 8px 12px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  border: 1px solid rgba(97, 232, 162, 0.2);
  font-size: 0.82rem;
}

.match-team-card,
.match-score-card,
.match-section,
.protocol-side-card,
.protocol-score-center,
.protocol-team-card {
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
}

.match-team-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  text-align: center;
}

.match-team-card h2,
.protocol-player-name,
.protocol-side-card h4 {
  margin: 0;
}

.match-team-logo {
  width: 68px;
  height: 68px;
  padding: 6px;
  border-radius: 50%;
  object-fit: contain;
  background: rgba(245, 237, 232, 0.92);
}

.match-score-card {
  min-width: 220px;
  text-align: center;
}

.match-date,
.match-result-note {
  margin: 0 0 8px;
  color: var(--muted);
}

.match-score {
  font-size: 2.3rem;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 10px;
}

.match-section,
.lineup-editor,
.protocol-team-card,
.protocol-player-list,
.protocol-toolbar-footer,
.protocol-side-card,
.protocol-score-center,
.referee-summary-card,
.referee-summary-item,
.protocol-referee-field {
  display: grid;
  gap: 14px;
}

.referee-summary-card,
.protocol-editor-card {
  gap: 16px;
}

.lineup-card {
  align-content: start;
}

.protocol-team-card {
  align-content: start;
}

.referee-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.referee-summary-item {
  gap: 6px;
  align-content: start;
  min-height: 104px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.028));
}

.referee-role-label,
.protocol-referee-label {
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.referee-summary-name {
  font-size: 1.05rem;
  line-height: 1.2;
}

.referee-summary-meta {
  min-height: 1.2em;
}

.protocol-referee-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

.protocol-referee-field {
  gap: 8px;
  min-width: 0;
}

.protocol-referee-empty {
  margin: -4px 0 0;
  padding: 0 2px;
}

.protocol-document-card,
.protocol-document-sheet,
.protocol-document-block,
.protocol-document-team-block,
.protocol-document-summary-card,
.protocol-document-note-box {
  display: grid;
  gap: 14px;
}

.protocol-document-card {
  gap: 18px;
}

.protocol-document-sheet {
  gap: 18px;
  padding: 24px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(250, 251, 255, 0.98), rgba(245, 247, 252, 0.96));
  color: #182033;
}

.protocol-document-sheet h2,
.protocol-document-sheet h4,
.protocol-document-sheet h5,
.protocol-document-sheet p {
  margin: 0;
}

.protocol-document-header,
.protocol-document-scorebar,
.protocol-document-footer-row,
.protocol-document-referee-row,
.protocol-document-team-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.protocol-document-head-copy {
  display: grid;
  gap: 6px;
}

.protocol-document-kicker {
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #5b6477;
}

.protocol-document-head-copy h2 {
  font-size: 1.8rem;
  line-height: 1.1;
}

.protocol-document-head-meta {
  display: grid;
  gap: 6px;
  justify-items: end;
  font-size: 0.92rem;
  color: #4d5566;
}

.protocol-document-scorebar {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(30, 42, 70, 0.14);
  background: rgba(255, 255, 255, 0.84);
}

.protocol-document-team {
  flex: 1 1 0;
}

.protocol-document-team-away {
  text-align: right;
}

.protocol-document-scorebox {
  display: grid;
  gap: 6px;
  min-width: 140px;
  justify-items: center;
}

.protocol-document-score-label,
.protocol-document-referee-role,
.protocol-document-team-head span {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #616a7e;
}

.protocol-document-scorebox strong {
  font-size: 2rem;
  line-height: 1;
}

.protocol-document-block {
  gap: 12px;
}

.protocol-document-block-head {
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(24, 32, 51, 0.12);
}

.protocol-document-referee-list,
.protocol-document-footer {
  display: grid;
  gap: 10px;
}

.protocol-document-referee-role {
  min-width: 170px;
}

.protocol-document-referee-name {
  flex: 1 1 auto;
}

.protocol-document-signature-line {
  display: inline-block;
  min-width: 180px;
  border-bottom: 1px solid rgba(24, 32, 51, 0.55);
  height: 24px;
}

.protocol-document-team-block {
  gap: 10px;
}

.protocol-document-team-head {
  padding-top: 4px;
}

.protocol-document-team-head h5 {
  font-size: 1.05rem;
}

.protocol-document-table-wrap {
  overflow-x: auto;
}

.protocol-document-table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.88);
}

.protocol-document-table th,
.protocol-document-table td {
  padding: 8px 10px;
  border: 1px solid rgba(24, 32, 51, 0.16);
  font-size: 0.92rem;
  text-align: center;
}

.protocol-document-table th {
  background: rgba(232, 236, 245, 0.9);
  font-weight: 700;
}

.protocol-document-player-cell {
  text-align: left !important;
}

.protocol-document-empty-cell {
  color: #5f687b;
}

.protocol-document-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.protocol-document-summary-card {
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(24, 32, 51, 0.12);
  background: rgba(255, 255, 255, 0.8);
}

.protocol-document-summary-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
}

.protocol-document-summary-empty {
  color: #5f687b;
}

.protocol-document-note-box {
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(24, 32, 51, 0.12);
  background: rgba(255, 255, 255, 0.78);
}

.protocol-document-note-lines {
  display: grid;
  gap: 18px;
}

.protocol-document-note-lines span {
  display: block;
  border-bottom: 1px solid rgba(24, 32, 51, 0.22);
}

.protocol-document-footer-row span:first-child {
  min-width: 170px;
}

.protocol-side-card {
  min-height: 116px;
  align-content: space-between;
}

.protocol-side-card-left {
  justify-items: start;
}

.protocol-side-card-right {
  justify-items: end;
  text-align: right;
}

.protocol-score-center {
  min-width: 190px;
  justify-items: center;
  align-content: center;
  text-align: center;
}

.protocol-score-label {
  color: var(--muted);
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.protocol-score-inputs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.score-square-input {
  width: 68px;
  height: 68px;
  padding: 0;
  text-align: center;
  font-size: 1.65rem;
  font-weight: 800;
}

.protocol-score-separator {
  font-size: 1.7rem;
  font-weight: 800;
}

.match-section-head {
  margin-bottom: 0;
}

.lineup-list,
.protocol-player-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.protocol-player-list {
  width: min(100%, 460px);
  justify-self: center;
  align-content: start;
}

.lineup-list {
  gap: 4px;
  align-content: start;
}

.lineup-item,
.protocol-player-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 3px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.lineup-player-main,
.protocol-player-name {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.lineup-player-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
}

.lineup-item {
  padding: 2px 8px;
}

.lineup-order {
  min-width: 24px;
  height: 24px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  font-size: 0.74rem;
}

.player-name-single-line {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goalkeeper-icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  font-size: 0.9em;
  line-height: 1;
}

.player-suspension-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 114, 114, 0.14);
  border: 1px solid rgba(255, 114, 114, 0.28);
  color: #ffb1b1;
  font-size: 0.7rem;
  line-height: 1;
  flex-shrink: 0;
}

.player-stat-icons,
.player-stat-inputs {
  display: grid;
  align-items: start;
  gap: 4px;
}

.player-stat-icons {
  min-height: 16px;
  flex-shrink: 0;
}

.player-stat-inputs {
  grid-template-columns: repeat(3, 52px);
  justify-content: end;
}

.stat-icon-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.goal-ball {
  font-size: 0.92rem;
  line-height: 1;
}

.card-icon {
  display: inline-block;
  width: 14px;
  height: 20px;
  border-radius: 3px;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.yellow-card {
  background: linear-gradient(180deg, #ffde59 0%, #deb017 100%);
}

.red-card {
  background: linear-gradient(180deg, #ff7272 0%, #db4545 100%);
}

.technical-defeat-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--muted);
}

.tiny-field {
  display: grid;
  justify-items: center;
  gap: 3px;
  width: 52px;
  font-size: 0.82rem;
  font-weight: 700;
  color: #eef2ff;
}

.micro-input {
  width: 38px;
  height: 38px;
  padding: 0;
  border-radius: 12px;
  text-align: center;
  text-indent: 0;
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 38px;
  appearance: textfield;
  -moz-appearance: textfield;
}

.micro-input::-webkit-outer-spin-button,
.micro-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.protocol-editor-actions-bottom {
  justify-content: flex-end;
  padding-top: 8px;
}

.lineup-modal {
  width: min(520px, calc(100vw - 24px));
}

.lineup-modal-body {
  display: grid;
  gap: 14px;
}

.lineup-suspension-list {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 114, 114, 0.08);
  border: 1px solid rgba(255, 114, 114, 0.14);
}

.btn-compact {
  min-width: 0;
  min-height: 38px;
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 0.95rem;
  line-height: 1;
}

@media (max-width: 960px) {
  .match-hero,
  .lineup-grid,
  .admin-protocol-grid,
  .protocol-layout-top,
  .referee-summary-grid,
  .protocol-referee-grid,
  .protocol-document-summary-grid {
    grid-template-columns: 1fr;
  }

  .protocol-side-card-right {
    justify-items: start;
    text-align: left;
  }

  .protocol-document-header,
  .protocol-document-scorebar,
  .protocol-document-footer-row,
  .protocol-document-referee-row,
  .protocol-document-team-head {
    align-items: stretch;
    flex-direction: column;
  }

  .protocol-document-head-meta {
    justify-items: start;
  }

  .protocol-document-team,
  .protocol-document-team-away {
    text-align: left;
  }

  .protocol-player-list {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .match-topbar,
  .technical-defeat-row,
  .protocol-editor-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .match-topbar-actions {
    width: 100%;
    display: grid;
    gap: 10px;
  }

  .match-topbar-actions > * {
    width: 100%;
    justify-content: center;
  }

  .match-status-badge {
    align-self: flex-start;
  }

  .match-screen {
    gap: 14px;
  }

  .match-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .match-team-card,
  .match-score-card,
  .match-section,
  .protocol-side-card,
  .protocol-score-center,
  .protocol-team-card {
    padding: 14px;
  }

  .match-score-card {
    grid-column: 1 / -1;
    order: -1;
    padding: 16px 14px;
  }

  .match-team-card {
    min-height: 0;
    gap: 8px;
    padding: 12px 10px;
  }

  .referee-summary-item,
  .protocol-referee-grid,
  .protocol-document-sheet,
  .protocol-document-scorebar,
  .protocol-document-summary-card,
  .protocol-document-note-box {
    padding: 12px 14px;
  }

  .protocol-document-head-copy h2 {
    font-size: 1.45rem;
  }

  .protocol-document-scorebox strong {
    font-size: 1.6rem;
  }

  .protocol-document-signature-line {
    min-width: 0;
    width: 100%;
  }

  .match-score {
    font-size: 1.9rem;
  }

  .match-team-logo {
    width: 48px;
    height: 48px;
    padding: 4px;
  }

  .match-team-card h2 {
    font-size: 1rem;
    line-height: 1.15;
  }

  .match-date,
  .match-result-note,
  .match-score-card .muted-text {
    font-size: 0.96rem;
  }

  .match-section-head {
    align-items: flex-start;
    gap: 8px;
  }

  .section-head.match-section-head .muted-text {
    font-size: 0.86rem;
  }

  .protocol-score-center,
  .match-score-card {
    min-width: 0;
  }

  .score-square-input {
    width: 58px;
    height: 58px;
    font-size: 1.4rem;
  }

  .protocol-score-separator {
    font-size: 1.35rem;
  }

  .lineup-item,
  .protocol-player-row {
    grid-template-columns: 1fr;
  }

  .lineup-player-main,
  .protocol-player-name {
    grid-template-columns: 24px minmax(0, 1fr);
    gap: 6px;
  }

  .lineup-player-inline {
    gap: 6px;
  }

  .lineup-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .lineup-actions > * {
    width: 100%;
  }

  .player-name-single-line {
    white-space: normal;
  }
}

@media (max-width: 420px) {
  .match-hero {
    grid-template-columns: 1fr;
  }

  .match-score-card {
    grid-column: auto;
  }

  .match-team-card {
    grid-template-columns: 40px minmax(0, 1fr);
    justify-items: start;
    align-items: center;
    text-align: left;
  }

  .match-team-logo {
    width: 40px;
    height: 40px;
    margin: 0;
  }
}
</style>
