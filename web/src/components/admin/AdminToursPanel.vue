<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Туры и матчи</h3>
      <p class="muted-text">Календарь и матчи Чемпионата и Кубков выбранного сезона.</p>
    </div>

    <div class="admin-form admin-surface">
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
    </div>

    <div v-if="selectedCompetition?.type === 'CHAMPIONSHIP'" class="admin-form admin-surface">
      <label>
        Выберите тур
        <select v-model="selectedId" @change="onTourChange">
          <option value="">— выберите —</option>
          <option v-for="tour in tours" :key="tour.id" :value="String(tour.id)">{{ tour.name }}</option>
        </select>
      </label>
      <p v-if="!tours.length" class="muted-text">
        Для выбранного сезона туры еще не сформированы. Проверьте состав команд и количество кругов.
      </p>
    </div>

    <div v-if="selectedTour" class="admin-grid">
      <AdminTourMatchForm
        :away-teams="availableAwayTeams"
        :form="matchForm"
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
