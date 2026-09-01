<template>
  <article class="card admin-panel competition-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Соревнования сезона</h3>
      <p class="muted-text">Сезон — контейнер. Чемпионат и Кубки создаются внутри него отдельными соревнованиями.</p>
    </div>

    <label class="competition-season-picker">
      Сезон
      <select v-model="seasonId">
        <option value="">— выберите сезон —</option>
        <option v-for="season in seasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option>
      </select>
    </label>

    <UiState v-if="errorText" tone="error" title="Операция не выполнена" :message="errorText" />
    <p v-if="successText" class="success-text">{{ successText }}</p>

    <template v-if="seasonId">
      <div class="competition-layout">
        <aside class="competition-list">
          <div class="competition-create-group">
            <span class="competition-sidebar-label">Новое соревнование</span>
            <button v-if="!hasChampionship" class="btn-primary competition-create" type="button" @click="startCreateChampionship">Добавить чемпионат</button>
            <button class="btn-ghost competition-create" type="button" @click="startCreateCup">Добавить Кубок</button>
          </div>
          <div class="competition-list-heading"><span class="competition-sidebar-label">Соревнования</span><span>{{ competitions.length }}</span></div>
          <button
            v-for="item in competitions"
            :key="item.id"
            class="competition-list-item"
            :class="{ active: String(item.id) === selectedId }"
            type="button"
            @click="selectCompetition(item)"
          >
            <small>{{ item.type === 'CUP' ? 'Кубок' : 'Чемпионат' }}</small>
            <span>{{ item.name }}</span>
          </button>
        </aside>

        <section v-if="mode === 'create-championship' || selectedChampionship" class="competition-editor">
          <div class="competition-editor-head">
            <div>
              <span class="competition-kind">Чемпионат</span>
              <h4>{{ mode === 'create-championship' ? 'Новый чемпионат' : selectedChampionship.name }}</h4>
              <p class="muted-text">Регламент чемпионата не влияет на Кубки этого сезона.</p>
            </div>
          </div>
          <form class="competition-regulation-form" @submit.prevent="saveChampionship">
            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">01</span><h5>Основное</h5></div>
              <div class="competition-fields competition-fields-2">
                <label class="competition-field competition-field-wide">Название чемпионата<input v-model.trim="championshipForm.name" required maxlength="160" /></label>
                <label class="competition-field">Количество кругов
                  <select v-model.number="championshipForm.roundsCount"><option v-for="count in 4" :key="count" :value="count">{{ count }}</option></select>
                </label>
                <label class="competition-field">Игроков на поле<input v-model.number="championshipForm.playersOnField" type="number" min="1" max="22" required /></label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">02</span><h5>Заявка и трансферы</h5></div>
              <div class="competition-fields competition-fields-3">
                <label class="competition-field">Лимит заявки<input v-model.number="championshipForm.maxRosterSize" type="number" min="1" placeholder="Без ограничения" /></label>
                <label class="competition-field">Дедлайн заявки<input v-model="championshipForm.applicationDeadline" type="date" /></label>
                <span class="competition-field-spacer" aria-hidden="true"></span>
                <label class="competition-field">Старт трансферного окна<input v-model="championshipForm.transferWindowStartDate" type="date" /></label>
                <label class="competition-field">Конец трансферного окна<input v-model="championshipForm.transferWindowEndDate" type="date" /></label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">03</span><h5>Плей-офф</h5></div>
              <div class="competition-settings-row">
                <label class="competition-switch">
                  <input v-model="championshipForm.playoffEnabled" type="checkbox" />
                  <span><strong>Плей-офф после чемпионата</strong><small>Формируется после завершения регулярного этапа</small></span>
                </label>
                <label v-if="championshipForm.playoffEnabled" class="competition-field competition-inline-field">Команд в плей-офф
                  <select v-model.number="championshipForm.playoffTeamCount"><option :value="4">4</option><option :value="8">8</option><option :value="16">16</option></select>
                </label>
                <label v-if="championshipForm.playoffEnabled" class="competition-switch competition-switch-compact">
                  <input v-model="championshipForm.thirdPlaceEnabled" type="checkbox" />
                  <span><strong>Матч за третье место</strong></span>
                </label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">04</span><h5>Дисциплина</h5></div>
              <div class="competition-fields competition-fields-3">
                <label class="competition-field">ЖК до дисквалификации<input v-model.number="championshipForm.yellowCardsForSuspension" type="number" min="0" /></label>
                <label class="competition-field">Пропусков за ЖК<input v-model.number="championshipForm.yellowSuspensionMatches" type="number" min="1" /></label>
                <label class="competition-field">Пропусков за КК<input v-model.number="championshipForm.redCardsForSuspension" type="number" min="1" /></label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">05</span><h5>Турнирная таблица</h5></div>
              <AdminSeasonRankingRules
                :available-options="availableChampionshipRuleOptions"
                :form="championshipForm"
                :rule-options="TIE_BREAKER_RULE_OPTIONS"
                :summary="championshipRankingRulesSummary"
                @add="addChampionshipRankingRule"
                @move="moveChampionshipRankingRule"
                @remove="removeChampionshipRankingRule"
              />
            </section>

            <section class="competition-form-section competition-participants-section">
              <div class="competition-section-head"><span class="competition-section-index">06</span><h5>Участники</h5><span class="competition-count">{{ seasonTeams.length }} команд</span></div>
              <div class="competition-team-grid competition-team-grid-readonly">
                <div v-for="team in seasonTeams" :key="team.id" class="competition-team-row"><span class="competition-team-check">✓</span><span>{{ team.name }}</span></div>
              </div>
              <p class="competition-section-note">Чемпионат использует все команды сезона.</p>
            </section>

            <div class="actions-row competition-form-actions">
              <button class="btn-primary" type="submit" :disabled="busy">{{ mode === 'create-championship' ? 'Создать чемпионат' : 'Сохранить чемпионат' }}</button>
              <button v-if="selectedChampionship" class="btn-ghost" type="button" :disabled="busy" @click="completeChampionship">Завершить регулярный этап</button>
            </div>
          </form>
        </section>

        <section v-else-if="mode === 'create-cup' || selectedCup" class="competition-editor">
          <div class="competition-editor-head">
            <div>
              <span class="competition-kind">Кубок</span>
              <h4>{{ mode === 'create-cup' ? 'Новый Кубок' : selectedCup.name }}</h4>
              <p v-if="selectedCup" class="competition-status-line"><span>Сетка</span><strong>{{ drawStatusLabel(selectedCup.drawStatus) }}</strong></p>
            </div>
            <button v-if="mode === 'edit-cup'" class="btn-danger" type="button" @click="removeCup">Удалить</button>
          </div>

          <form class="competition-regulation-form" @submit.prevent="save">
            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">01</span><h5>Основное</h5></div>
              <div class="competition-fields competition-fields-2">
                <label class="competition-field competition-field-wide">Название Кубка<input v-model.trim="form.name" required maxlength="160" /></label>
                <label class="competition-field">Заявка
                  <select v-model="form.rosterMode">
                    <option value="SEASON_SHARED">Общая заявка сезона</option>
                    <option value="OWN">Собственная заявка Кубка</option>
                  </select>
                </label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">02</span><h5>Состав и протокол</h5></div>
              <div class="competition-fields competition-fields-3">
                <label class="competition-field">Игроков на поле<input v-model.number="form.playersOnField" type="number" min="1" max="22" required /></label>
                <label class="competition-field">Лимит заявки<input v-model.number="form.maxRosterSize" type="number" min="1" placeholder="Без ограничения" /></label>
                <label class="competition-field">Игроков в протоколе<input v-model.number="form.matchRosterSize" type="number" :min="form.playersOnField || 1" placeholder="Без ограничения" /></label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">03</span><h5>Формат матчей</h5></div>
              <div class="competition-fields competition-fields-3">
                <label class="competition-field">Матчей в паре
                  <select v-model.number="form.regularTieLegs"><option :value="1">1 матч</option><option :value="2">2 матча</option></select>
                </label>
                <label class="competition-field">Матчей в финале
                  <select v-model.number="form.finalLegs"><option :value="1">1 матч</option><option :value="2">2 матча</option></select>
                </label>
                <label v-if="form.thirdPlaceEnabled" class="competition-field">Матчей за третье место
                  <select v-model.number="form.thirdPlaceLegs"><option :value="1">1 матч</option><option :value="2">2 матча</option></select>
                </label>
              </div>
              <div class="competition-switch-grid">
                <label class="competition-switch competition-switch-compact"><input v-model="form.thirdPlaceEnabled" type="checkbox" /><span><strong>Матч за третье место</strong></span></label>
                <label class="competition-switch competition-switch-compact"><input v-model="form.extraTimeEnabled" type="checkbox" /><span><strong>Дополнительное время</strong></span></label>
                <label class="competition-switch competition-switch-compact"><input v-model="form.penaltiesEnabled" type="checkbox" /><span><strong>Серия пенальти</strong></span></label>
              </div>
              <div v-if="form.extraTimeEnabled" class="competition-fields competition-fields-3 competition-dependent-fields">
                <label class="competition-field">Минут дополнительного времени<input v-model.number="form.extraTimeMinutes" type="number" min="1" max="60" /></label>
              </div>
            </section>

            <section class="competition-form-section">
              <div class="competition-section-head"><span class="competition-section-index">04</span><h5>Дисциплина</h5></div>
              <div class="competition-fields competition-fields-3">
                <label class="competition-field">ЖК до дисквалификации<input v-model.number="form.yellowCardsForSuspension" type="number" min="0" /></label>
                <label class="competition-field">Пропусков за ЖК<input v-model.number="form.yellowSuspensionMatches" type="number" min="1" /></label>
                <label class="competition-field">Пропусков за КК<input v-model.number="form.redSuspensionMatches" type="number" min="1" /></label>
              </div>
            </section>

            <section class="competition-form-section competition-participants-section">
              <div class="competition-section-head">
                <span class="competition-section-index">05</span><h5>Участники Кубка</h5>
                <span class="competition-count">{{ form.teamIds.length }} из {{ seasonTeams.length }}</span>
              </div>
              <div class="competition-participants-content">
                <div class="competition-section-actions">
                  <button class="btn-ghost btn-sm" type="button" @click="selectAllCupTeams">Выбрать все</button>
                  <button class="btn-ghost btn-sm" type="button" :disabled="!form.teamIds.length" @click="clearCupTeams">Очистить</button>
                </div>
                <div class="competition-team-grid">
                  <label v-for="team in seasonTeams" :key="team.id" class="competition-team-row" :class="{ selected: form.teamIds.includes(team.id) }">
                    <input v-model="form.teamIds" type="checkbox" :value="team.id" />
                    <span>{{ team.name }}</span>
                  </label>
                </div>
              </div>
            </section>

            <div class="actions-row competition-form-actions">
              <button class="btn-primary" type="submit" :disabled="busy">{{ mode === 'create-cup' ? 'Создать Кубок' : 'Сохранить настройки' }}</button>
              <button v-if="mode === 'edit-cup'" class="btn-ghost" type="button" @click="fillFrom(selectedCup)">Отменить изменения</button>
            </div>
          </form>

          <section v-if="selectedCup && selectedCup.rosterMode === 'OWN'" class="competition-roster">
            <div class="competition-roster-head">
              <div>
                <h4>Заявка Кубка</h4>
                <p class="muted-text">Выберите игроков из актуальной заявки команды на сезон.</p>
              </div>
              <label>Команда
                <select v-model="rosterTeamId">
                  <option value="">— выберите команду —</option>
                  <option v-for="team in selectedCup.teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
                </select>
              </label>
            </div>
            <div v-if="rosterTeamId" class="competition-roster-grid">
              <fieldset class="competition-roster-list">
                <legend>Доступны для дозаявки</legend>
                <label v-for="player in availableRosterCandidates" :key="player.playerId">
                  <input v-model="rosterSelection" type="checkbox" :value="player.playerId" />
                  <span>{{ player.playerName }}</span>
                </label>
                <p v-if="!availableRosterCandidates.length" class="muted-text">Все доступные игроки уже заявлены.</p>
                <button class="btn-primary" type="button" :disabled="busy || !rosterSelection.length" @click="addSelectedRosterPlayers">Дозаявить выбранных</button>
              </fieldset>
              <div class="competition-roster-list">
                <strong>Заявлены: {{ teamRoster.length }}<template v-if="selectedCup.maxRosterSize"> из {{ selectedCup.maxRosterSize }}</template></strong>
                <div v-for="player in teamRoster" :key="player.playerId" class="competition-roster-player">
                  <span>{{ player.playerName }}</span>
                  <button class="icon-button" type="button" title="Удалить из заявки Кубка" @click="removeRosterPlayer(player)">×</button>
                </div>
                <p v-if="!teamRoster.length" class="muted-text">В заявке пока нет игроков.</p>
              </div>
            </div>
          </section>

        </section>

        <UiState v-else tone="empty" title="Выберите соревнование" message="Создайте чемпионат, Кубок или откройте существующее соревнование." />
      </div>
    </template>
  </article>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { createCompetitionsApi } from '../../api/competitions'
import { TIE_BREAKER_RULE_OPTIONS } from '../../composables/useAdminSeasonRules'
import AdminSeasonRankingRules from './AdminSeasonRankingRules.vue'
import UiState from '../UiState.vue'

const props = defineProps({ request: { type: Function, required: true }, seasons: { type: Array, default: () => [] } })
const api = createCompetitionsApi(props.request)
const seasonId = ref('')
const competitions = ref([])
const seasonTeamOptions = ref([])
const selectedId = ref('')
const mode = ref('idle')
const busy = ref(false)
const errorText = ref('')
const successText = ref('')
const rosterTeamId = ref('')
const roster = ref([])
const rosterCandidates = ref([])
const rosterSelection = ref([])
const emptyForm = () => ({ name: '', rosterMode: 'SEASON_SHARED', maxRosterSize: null, matchRosterSize: null, playersOnField: 11,
  regularTieLegs: 1, finalLegs: 1, thirdPlaceEnabled: false, thirdPlaceLegs: 1, extraTimeEnabled: true,
  extraTimeMinutes: 30, penaltiesEnabled: true, yellowCardsForSuspension: 0, yellowSuspensionMatches: 1,
  redSuspensionMatches: 1, teamIds: [] })
const form = reactive(emptyForm())
const championshipForm = reactive({ name: 'Чемпионат', roundsCount: 1, playersOnField: 11, maxRosterSize: null,
  applicationDeadline: '', transferWindowStartDate: '', transferWindowEndDate: '', playoffEnabled: false,
  playoffTeamCount: 4, thirdPlaceEnabled: false, yellowCardsForSuspension: 0, redCardsForSuspension: 1,
  yellowSuspensionMatches: 1, rankingRules: ['GOAL_DIFFERENCE', 'GOALS_FOR'] })

const selectedCup = computed(() => competitions.value.find((item) => item.type === 'CUP' && String(item.id) === selectedId.value) || null)
const selectedChampionship = computed(() => competitions.value.find((item) => item.type === 'CHAMPIONSHIP' && String(item.id) === selectedId.value) || null)
const hasChampionship = computed(() => competitions.value.some((item) => item.type === 'CHAMPIONSHIP'))
const selectedSeason = computed(() => props.seasons.find((item) => String(item.id) === seasonId.value) || null)
const seasonTeams = computed(() => seasonTeamOptions.value)
const teamRoster = computed(() => roster.value.filter((player) => String(player.teamId) === rosterTeamId.value))
const availableRosterCandidates = computed(() => {
  const selected = new Set(teamRoster.value.map((player) => String(player.playerId)))
  return rosterCandidates.value.filter((player) => !selected.has(String(player.playerId)))
})
watch(seasonId, load)
watch(rosterTeamId, loadRosterCandidates)

async function load() {
  competitions.value = []
  seasonTeamOptions.value = []
  selectedId.value = ''
  mode.value = 'idle'
  if (!seasonId.value) return
  await run(async () => {
    [competitions.value, seasonTeamOptions.value] = await Promise.all([
      api.list(seasonId.value),
      props.request(`/api/seasons/${encodeURIComponent(seasonId.value)}/teams`, { method: 'GET' }),
    ])
  })
}
function startCreateCup() { selectedId.value = ''; mode.value = 'create-cup'; rosterTeamId.value = ''; Object.assign(form, emptyForm()) }
function startCreateChampionship() { selectedId.value = ''; mode.value = 'create-championship'; fillChampionshipForm() }
function selectCompetition(item) {
  selectedId.value = String(item.id)
  mode.value = item.type === 'CUP' ? 'edit-cup' : 'edit-championship'
  rosterTeamId.value = ''; roster.value = []; rosterCandidates.value = []
  if (item.type === 'CUP') { fillFrom(item); loadRoster() } else fillChampionshipForm(item)
}
function fillChampionshipForm(item = null) {
  const season = selectedSeason.value || {}
  const rankingRules = Array.isArray(season.rankingRules)
    ? season.rankingRules.filter((rule) => rule !== 'POINTS' && rule !== 'ALPHABETICAL')
    : ['GOAL_DIFFERENCE', 'GOALS_FOR']
  Object.assign(championshipForm, { name: item?.name || 'Чемпионат', roundsCount: Number(season.roundsCount || 1),
    playersOnField: Number(season.playersOnField || 11), maxRosterSize: season.maxRosterSize || null,
    applicationDeadline: season.applicationDeadline || '', transferWindowStartDate: season.transferWindowStartDate || '',
    transferWindowEndDate: season.transferWindowEndDate || '', playoffEnabled: Boolean(season.playoffEnabled),
    playoffTeamCount: Number(season.playoffTeamCount || 4), thirdPlaceEnabled: Boolean(season.thirdPlaceEnabled),
    yellowCardsForSuspension: Number(season.yellowCardsForSuspension || 0), redCardsForSuspension: Number(season.redCardsForSuspension || 1),
    yellowSuspensionMatches: Number(season.yellowSuspensionMatches || 1),
    rankingRules: rankingRules.length ? [...rankingRules] : ['GOAL_DIFFERENCE', 'GOALS_FOR'] })
}
function normalizedChampionshipRankingRules() {
  return championshipForm.rankingRules.map((rule) => String(rule || '').trim()).filter(Boolean)
}
function availableChampionshipRuleOptions(index) {
  const used = new Set(championshipForm.rankingRules.filter((_, ruleIndex) => ruleIndex !== index).filter(Boolean))
  return TIE_BREAKER_RULE_OPTIONS.filter((option) => !used.has(option.value) || option.value === championshipForm.rankingRules[index])
}
function addChampionshipRankingRule() {
  const used = new Set(normalizedChampionshipRankingRules())
  const next = TIE_BREAKER_RULE_OPTIONS.find((option) => !used.has(option.value))
  if (next) championshipForm.rankingRules.push(next.value)
}
function removeChampionshipRankingRule(index) { championshipForm.rankingRules.splice(index, 1) }
function moveChampionshipRankingRule(index, offset) {
  const target = index + offset
  if (target < 0 || target >= championshipForm.rankingRules.length) return
  const [rule] = championshipForm.rankingRules.splice(index, 1)
  championshipForm.rankingRules.splice(target, 0, rule)
}
function championshipRankingRulesSummary() {
  const labels = normalizedChampionshipRankingRules().map((rule) => TIE_BREAKER_RULE_OPTIONS.find((option) => option.value === rule)?.label || rule)
  return labels.length ? `очки → ${labels.join(' → ')} → алфавит` : 'очки → алфавит'
}
function fillFrom(item) {
  if (!item) return
  Object.assign(form, { ...emptyForm(), ...item, teamIds: (item.teams || []).map((team) => team.id) })
}
function selectAllCupTeams() { form.teamIds = seasonTeams.value.map((team) => team.id) }
function clearCupTeams() { form.teamIds = [] }
function payload() { return { ...form, maxRosterSize: form.maxRosterSize || null, matchRosterSize: form.matchRosterSize || null, teamIds: form.teamIds.map(Number) } }
async function save() {
  await run(async () => {
    const saved = mode.value === 'create-cup' ? await api.createCup(seasonId.value, payload()) : await api.update(seasonId.value, selectedId.value, payload())
    await refreshAndSelect(saved.id)
    successText.value = mode.value === 'create-cup' ? 'Кубок создан.' : 'Настройки Кубка сохранены.'
  })
}
async function saveChampionship() {
  const rankingRules = normalizedChampionshipRankingRules()
  if (rankingRules.length !== championshipForm.rankingRules.length) {
    errorText.value = 'Выберите критерий для каждого приоритета таблицы.'
    return
  }
  if (rankingRules.length !== new Set(rankingRules).size) {
    errorText.value = 'Критерии сортировки таблицы не должны повторяться.'
    return
  }
  await run(async () => {
    const creating = mode.value === 'create-championship'
    const competition = creating
      ? await api.createChampionship(seasonId.value, championshipForm.name)
      : await api.renameChampionship(seasonId.value, selectedChampionship.value.id, championshipForm.name)
    const season = selectedSeason.value || {}
    const updatedSeason = await props.request(`/api/seasons/${encodeURIComponent(seasonId.value)}`, { method: 'PUT', body: JSON.stringify({
      name: season.name, status: season.status || 'DRAFT', roundsCount: Number(championshipForm.roundsCount),
      playoffEnabled: Boolean(championshipForm.playoffEnabled), playoffTeamCount: championshipForm.playoffEnabled ? Number(championshipForm.playoffTeamCount) : null,
      thirdPlaceEnabled: championshipForm.playoffEnabled ? Boolean(championshipForm.thirdPlaceEnabled) : false,
      applicationDeadline: championshipForm.applicationDeadline || null, maxRosterSize: championshipForm.maxRosterSize || null,
      playersOnField: Number(championshipForm.playersOnField), transferWindowStartDate: championshipForm.transferWindowStartDate || null,
      transferWindowEndDate: championshipForm.transferWindowEndDate || null, rankingRules: ['POINTS', ...rankingRules, 'ALPHABETICAL'],
      refereeIds: (season.referees || []).map((referee) => referee.id), yellowCardsForSuspension: Number(championshipForm.yellowCardsForSuspension || 0),
      yellowSuspensionMatches: Number(championshipForm.yellowSuspensionMatches || 1),
      redCardsForSuspension: Number(championshipForm.redCardsForSuspension || 1),
    }) })
    Object.assign(season, updatedSeason)
    await refreshAndSelect(competition.id)
    successText.value = creating ? 'Чемпионат создан внутри сезона.' : 'Настройки чемпионата сохранены.'
  })
}
async function completeChampionship() {
  if (!window.confirm('Завершить регулярный этап чемпионата и сформировать плей-офф, если он включён?')) return
  await run(async () => {
    const updatedSeason = await props.request(`/api/seasons/${encodeURIComponent(seasonId.value)}/complete-regular-season`, { method: 'POST' })
    Object.assign(selectedSeason.value, updatedSeason)
    successText.value = updatedSeason.playoffEnabled ? 'Регулярный этап завершён, плей-офф сформирован.' : 'Чемпионат завершён.'
  })
}
async function removeCup() { if (!selectedCup.value || !window.confirm(`Удалить Кубок «${selectedCup.value.name}»?`)) return; await run(async () => { await api.deactivate(seasonId.value, selectedCup.value.id); await load(); successText.value = 'Кубок удалён.' }) }
async function loadRoster() { if (!selectedCup.value || selectedCup.value.rosterMode !== 'OWN') return; roster.value = await api.roster(seasonId.value, selectedCup.value.id) }
async function loadRosterCandidates() {
  rosterSelection.value = []
  rosterCandidates.value = []
  if (!rosterTeamId.value || !selectedCup.value || selectedCup.value.rosterMode !== 'OWN') return
  await run(async () => { rosterCandidates.value = await api.rosterCandidates(seasonId.value, selectedCup.value.id, rosterTeamId.value) })
}
async function addSelectedRosterPlayers() {
  await run(async () => {
    roster.value = await api.addRosterPlayers(seasonId.value, selectedCup.value.id, Number(rosterTeamId.value), rosterSelection.value.map(Number))
    rosterSelection.value = []
    successText.value = 'Игроки добавлены в заявку Кубка.'
  })
}
async function removeRosterPlayer(player) {
  await run(async () => {
    roster.value = await api.removeRosterPlayer(seasonId.value, selectedCup.value.id, Number(rosterTeamId.value), player.playerId)
    successText.value = 'Игрок удалён из заявки Кубка.'
  })
}
async function refreshAndSelect(id) { competitions.value = await api.list(seasonId.value); const item = competitions.value.find((entry) => String(entry.id) === String(id)); if (item) selectCompetition(item) }
async function run(action) { busy.value = true; errorText.value = ''; successText.value = ''; try { await action() } catch (error) { errorText.value = error?.message || 'Не удалось выполнить операцию.' } finally { busy.value = false } }
function drawStatusLabel(status) { return ({ NOT_DRAWN: 'не сформирована', DRAFT: 'черновик', CONFIRMED: 'утверждена' })[status] || status }
</script>

<style scoped>
.competition-season-picker {
  display: grid;
  gap: 7px;
  width: min(100%, 520px);
  margin: 20px 0 26px;
}

.competition-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.competition-list {
  position: sticky;
  top: 18px;
  display: grid;
  align-content: start;
  gap: 8px;
}

.competition-create-group {
  display: grid;
  gap: 8px;
  padding-bottom: 18px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.competition-create {
  width: 100%;
}

.competition-sidebar-label {
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.competition-list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 3px 2px 7px;
  color: var(--muted);
}

.competition-list-item {
  display: grid;
  gap: 4px;
  min-height: 66px;
  padding: 11px 13px;
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.025);
  color: inherit;
}

.competition-list-item:hover {
  border-color: rgba(97, 232, 162, 0.5);
}

.competition-list-item.active {
  border-color: var(--brand);
  background: rgba(97, 232, 162, 0.08);
  box-shadow: inset 3px 0 0 var(--brand);
}

.competition-list-item small {
  color: var(--muted);
  font-size: 0.75rem;
  text-transform: uppercase;
}

.competition-list-item span {
  font-weight: 700;
}

.competition-editor {
  min-width: 0;
}

.competition-editor-head,
.competition-roster-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  min-height: 72px;
  padding: 2px 0 20px;
}

.competition-editor h4,
.competition-roster h4 {
  margin: 3px 0 0;
  font-size: 1.35rem;
}

.competition-kind {
  color: #8fa4d9;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
}

.competition-status-line {
  display: flex;
  gap: 8px;
  margin: 8px 0 0;
  color: var(--muted);
}

.competition-status-line strong {
  color: #94edbd;
  font-weight: 700;
}

.competition-regulation-form {
  border-top: 1px solid var(--line);
}

.competition-form-section {
  display: grid;
  grid-template-columns: minmax(180px, 220px) minmax(0, 1fr);
  gap: 28px;
  padding: 24px 0;
  border-bottom: 1px solid var(--line);
}

.competition-form-section > :not(.competition-section-head) {
  grid-column: 2;
}

.competition-section-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.competition-section-head h5 {
  margin: 1px 0 0;
  font-size: 1rem;
}

.competition-section-index {
  color: #61e8a2;
  font-size: 0.72rem;
  font-weight: 800;
  line-height: 1.7;
}

.competition-count {
  margin-left: auto;
  color: var(--muted);
  font-size: 0.82rem;
  white-space: nowrap;
}

.competition-section-actions {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  margin-bottom: 10px;
}

.competition-participants-section .competition-section-head {
  flex-wrap: wrap;
}

.competition-participants-content {
  min-width: 0;
}

.competition-fields {
  display: grid;
  gap: 14px;
  width: 100%;
  max-width: 1120px;
}

.competition-fields-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.competition-fields-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.competition-field {
  display: grid;
  align-content: start;
  gap: 7px;
  min-width: 0;
}

.competition-field-wide {
  grid-column: 1 / -1;
}

.competition-inline-field {
  width: min(100%, 220px);
}

.competition-settings-row {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 14px;
  max-width: 1120px;
}

.competition-switch-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: 1120px;
  margin-top: 16px;
}

.competition-switch {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  min-width: min(100%, 300px);
  padding: 12px 14px;
  border: 1px solid rgba(124, 163, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.025);
}

.competition-switch:has(input:checked) {
  border-color: rgba(97, 232, 162, 0.45);
  background: rgba(97, 232, 162, 0.07);
}

.competition-switch input {
  margin-top: 3px;
}

.competition-switch span {
  display: grid;
  gap: 3px;
}

.competition-switch small {
  color: var(--muted);
  font-weight: 400;
}

.competition-switch-compact {
  align-items: center;
  min-width: 0;
}

.competition-dependent-fields {
  margin-top: 14px;
}

.competition-team-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.competition-team-row {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 44px;
  padding: 9px 12px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.02);
}

label.competition-team-row {
  cursor: pointer;
}

.competition-team-row.selected {
  border-color: rgba(97, 232, 162, 0.42);
  background: rgba(97, 232, 162, 0.07);
}

.competition-team-grid-readonly {
  opacity: 0.86;
}

.competition-team-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #61e8a2;
  font-weight: 800;
}

.competition-section-note {
  grid-column: 2;
  margin: 2px 0 0;
  color: var(--muted);
  font-size: 0.86rem;
}

.competition-form-actions {
  justify-content: flex-end;
  padding: 20px 0 2px;
}

.competition-roster {
  margin-top: 30px;
  padding-top: 24px;
  border-top: 1px solid var(--line);
}

.competition-roster-head label {
  display: grid;
  gap: 6px;
  min-width: 240px;
}

.competition-roster-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.competition-roster-list {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.competition-roster-list label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.competition-roster-player {
  display: grid;
  grid-template-columns: 1fr 38px;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  border-bottom: 1px solid var(--line);
}

@media (max-width: 1100px) {
  .competition-form-section {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .competition-form-section > :not(.competition-section-head) {
    grid-column: 1;
  }

  .competition-section-note {
    grid-column: 1;
  }

  .competition-fields-3,
  .competition-team-grid,
  .competition-switch-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .competition-section-head {
    align-items: center;
  }
}

@media (max-width: 900px) {
  .competition-layout {
    grid-template-columns: 1fr;
  }

  .competition-list {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .competition-create-group,
  .competition-list-heading {
    grid-column: 1 / -1;
  }

  .competition-create-group {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .competition-create-group .competition-sidebar-label {
    grid-column: 1 / -1;
  }
}

@media (max-width: 600px) {
  .competition-season-picker {
    width: 100%;
  }

  .competition-list,
  .competition-create-group,
  .competition-fields-2,
  .competition-fields-3,
  .competition-team-grid,
  .competition-switch-grid,
  .competition-roster-grid {
    grid-template-columns: 1fr;
  }

  .competition-editor-head,
  .competition-roster-head {
    display: grid;
  }

  .competition-editor-head .btn-danger {
    width: 100%;
  }

  .competition-field-spacer {
    display: none;
  }

  .competition-settings-row,
  .competition-switch,
  .competition-inline-field {
    width: 100%;
  }

  .competition-section-actions {
    width: 100%;
  }

  .competition-section-actions > * {
    flex: 1;
  }

  .competition-form-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
