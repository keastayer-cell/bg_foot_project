<template>
  <article class="card admin-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Туры и матчи</h3>
      <p class="muted-text">Управление календарём сезона, публикацией туров и матчами внутри тура.</p>
    </div>

    <div class="admin-form admin-surface">
      <label>
        Сезон
        <select v-model="seasonId" @change="onSeasonChange">
          <option value="">— выберите —</option>
          <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <p v-if="selectedSeason" class="muted-text">
        Регулярный этап рассчитан автоматически: {{ selectedSeason.regularToursCount }} туров,
        {{ selectedSeason.roundsCount }} круг(а), {{ teams.length }} команд.
      </p>
      <p v-if="selectedSeason?.playoffEnabled" class="muted-text">
        Плей-офф включен на {{ selectedSeason.playoffTeamCount }} команд.
      </p>
    </div>

    <div v-if="seasonId" class="admin-form admin-surface">
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
  </article>
</template>

<script setup>
import { toRefs } from 'vue'
import AdminTourMatchForm from './AdminTourMatchForm.vue'
import AdminTourMatchesList from './AdminTourMatchesList.vue'

const props = defineProps({
  panel: { type: Object, required: true },
})

const {
  availableAwayTeams,
  canDeleteTourMatch,
  canPublishSelectedTour,
  createMatch,
  deleteMatch,
  formatDateTime,
  matchForm,
  matchLimitMessage,
  matchProtocolStatusLabel,
  matches,
  onSeasonChange,
  onTourChange,
  protocolStatusBadgeClass,
  publish,
  seasonId,
  seasonsList,
  selectedId,
  selectedSeason,
  selectedTour,
  teams,
  tourMatchDeleteTitle,
  tourMatchScoreLabel,
  tours,
} = toRefs(props.panel)
</script>
