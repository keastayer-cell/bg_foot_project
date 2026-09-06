<template>
  <article class="card admin-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Турнир</p>
      <h3 class="section-title">Туры и матчи</h3>
      <p class="muted-text">Календарь и матчи Чемпионата и Кубков выбранного сезона.</p>
    </header>

    <section class="admin-step-section tour-context-section">
      <div class="admin-step-heading tour-context-heading">
        <span class="admin-step-number">1</span>
        <div><h4>Контекст календаря</h4><p>Сначала выберите сезон и соревнование, с которым будете работать.</p></div>
      </div>
      <div class="tour-context-fields">
      <label>
        Сезон
        <select v-model="seasonId" @change="onSeasonChange">
          <option value="">— выберите —</option>
          <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label v-if="competitions.length">
        Соревнование
        <select v-model="competitionId" @change="onCompetitionChange">
          <option v-for="competition in competitions" :key="competition.id" :value="String(competition.id)">
            {{ competition.name }} · {{ competition.type === 'CUP' ? 'Кубок' : 'Чемпионат' }}
          </option>
        </select>
      </label>
      </div>
      <div v-if="selectedSeason && selectedCompetition" class="tour-context-summary">
        <span><small>Сезон</small><strong>{{ selectedSeason.name }}</strong></span>
        <span><small>Соревнование</small><strong>{{ selectedCompetition.name }}</strong></span>
        <span><small>Команд</small><strong>{{ teams.length }}</strong></span>
      </div>
      <p v-if="selectedSeason && selectedCompetition?.type === 'CHAMPIONSHIP'" class="muted-text">
        Регулярный этап рассчитан автоматически: {{ selectedSeason.regularToursCount }} туров,
        {{ selectedSeason.roundsCount }} круг(а), {{ teams.length }} команд.
      </p>
      <p v-if="selectedSeason?.playoffEnabled && selectedCompetition?.type === 'CHAMPIONSHIP'" class="muted-text">
        Плей-офф включен на {{ selectedSeason.playoffTeamCount }} команд.
      </p>
      <p v-if="seasonId && !competitions.length" class="muted-text">
        В сезоне пока нет соревнований. Создайте Чемпионат или Кубок в разделе «Соревнования».
      </p>
    </section>

    <section v-if="selectedCompetition?.type === 'CHAMPIONSHIP'" class="admin-step-section">
      <div class="admin-step-heading">
        <span class="admin-step-number">2</span>
        <div><h4>Выберите тур</h4><p>Откройте матчи конкретного тура чемпионата.</p></div>
        <span class="admin-step-count">{{ tours.length }}</span>
      </div>
      <label>
        Тур
        <select v-model="selectedId" @change="onTourChange">
          <option value="">— выберите —</option>
          <option v-for="tour in tours" :key="tour.id" :value="String(tour.id)">{{ tour.name }}</option>
        </select>
      </label>
      <p v-if="!tours.length" class="muted-text">
        Для выбранного сезона туры еще не сформированы. Проверьте состав команд и количество кругов.
      </p>
    </section>

    <div v-if="selectedTour" class="admin-grid tour-management-grid">
      <AdminTourMatchForm
        :availability-message="matchAvailabilityMessage"
        :away-teams="availableAwayTeams"
        :form="matchForm"
        :home-teams="availableHomeTeams"
        :limit-message="matchLimitMessage"
        :teams="teams"
        :tour="selectedTour"
        @create="createMatch"
      />
      <AdminTourMatchesList
        :can-delete="canDeleteTourMatch"
        :can-publish="canPublishSelectedTour"
        :delete-title="tourMatchDeleteTitle"
        :format-date-time="formatDateTime"
        :matches="matches"
        :score-label="tourMatchScoreLabel"
        :status-badge-class="protocolStatusBadgeClass"
        :status-label="matchProtocolStatusLabel"
        :tour="selectedTour"
        @delete="deleteMatch"
        @publish="publish"
      />
    </div>

    <AdminCupMatchesPanel
      v-if="selectedCup"
      v-model:selected-tie-id="selectedCupTieId"
      :can-create="canCreateCupMatches"
      :can-save-winner="canSaveCupTieWinner"
      :cup="selectedCup"
      :draw-busy="cupDrawBusy"
      :draw-order="cupDrawOrder"
      :format-date-time="formatDateTime"
      :kickoff-dates="cupKickoffDates"
      :on-tie-change="onCupTieChange"
      :penalty-form="cupPenaltyForm"
      :rounds="cupRounds"
      :selected-tie="selectedCupTie"
      :needs-winner="needsCupTieWinner"
      :status-badge-class="protocolStatusBadgeClass"
      :status-label="matchProtocolStatusLabel"
      @create="createCupMatches"
      @confirm-draw="confirmCupDraw"
      @draw-manual="drawCupManual"
      @draw-random="drawCupRandom"
      @move-team="moveCupDrawTeam"
      @save-winner="saveCupTieWinner"
    />
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
import AdminCupMatchesPanel from './AdminCupMatchesPanel.vue'
import AdminTourMatchForm from './AdminTourMatchForm.vue'
import AdminTourMatchesList from './AdminTourMatchesList.vue'

const props = defineProps({
  panel: { type: Object, required: true },
})

const {
  availableAwayTeams,
  availableHomeTeams,
  canCreateCupMatches,
  canSaveCupTieWinner,
  canDeleteTourMatch,
  canPublishSelectedTour,
  competitionId,
  competitions,
  confirmCupDraw,
  createMatch,
  createCupMatches,
  cupDrawBusy,
  cupDrawOrder,
  cupKickoffDates,
  cupPenaltyForm,
  cupRounds,
  deleteMatch,
  drawCupManual,
  drawCupRandom,
  formatDateTime,
  matchForm,
  matchAvailabilityMessage,
  matchLimitMessage,
  matchProtocolStatusLabel,
  matches,
  moveCupDrawTeam,
  needsCupTieWinner,
  onSeasonChange,
  onCompetitionChange,
  onCupTieChange,
  onTourChange,
  protocolStatusBadgeClass,
  publish,
  saveCupTieWinner,
  seasonId,
  seasonsList,
  selectedId,
  selectedCompetition,
  selectedCup,
  selectedCupTie,
  selectedCupTieId,
  selectedSeason,
  selectedTour,
  teams,
  tourMatchDeleteTitle,
  tourMatchScoreLabel,
  tours,
} = toRefs(props.panel)
</script>

<style scoped>
.tour-context-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.tour-context-fields label { display: grid; gap: 7px; }
.tour-context-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid rgba(124, 163, 255, .14); border-radius: 10px; overflow: hidden; }
.tour-context-summary > span { display: grid; gap: 3px; padding: 11px 13px; border-right: 1px solid rgba(124, 163, 255, .14); }
.tour-context-summary > span:last-child { border-right: 0; }
.tour-context-summary small { color: var(--muted); font-size: .7rem; }
.tour-context-summary strong { overflow: hidden; font-size: .82rem; text-overflow: ellipsis; white-space: nowrap; }
.tour-management-grid { grid-template-columns: minmax(430px, .9fr) minmax(520px, 1.1fr); align-items: start; }
@media (max-width: 720px) {
  .tour-context-fields, .tour-context-summary { grid-template-columns: 1fr; }
  .tour-context-summary > span { border-right: 0; border-bottom: 1px solid rgba(124, 163, 255, .14); }
  .tour-context-summary > span:last-child { border-bottom: 0; }
}
@media (max-width: 1120px) {
  .tour-management-grid { grid-template-columns: 1fr; }
}
</style>
