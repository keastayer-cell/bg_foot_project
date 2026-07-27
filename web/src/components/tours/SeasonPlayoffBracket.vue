<template>
  <div class="playoff-bracket-wrap">
    <div class="playoff-stage-shell">
      <div class="playoff-stage-side playoff-stage-side-left">
        <section
          v-for="column in leftColumns"
          :key="`left-${column.key}`"
          class="playoff-side-column playoff-side-column-left"
          :class="roundClass(column)"
          :style="sideColumnStyle(column)"
        >
          <header class="playoff-round-head">
            <h3>{{ column.label }}</h3>
          </header>
          <div class="playoff-side-cards">
            <PlayoffMatchCard v-for="card in column.cards" :key="card.key" :card="card" />
          </div>
        </section>
      </div>

      <div class="playoff-stage-center">
        <section class="playoff-center-stack">
          <div v-for="card in centerCards" :key="card.key" class="playoff-center-card-wrap">
            <header class="playoff-round-head playoff-round-head-center">
              <h3>{{ card.roundLabel }}</h3>
            </header>
            <PlayoffMatchCard :card="card" center />
          </div>
        </section>
      </div>

      <div class="playoff-stage-side playoff-stage-side-right">
        <section
          v-for="column in rightColumns"
          :key="`right-${column.key}`"
          class="playoff-side-column playoff-side-column-right"
          :class="roundClass(column)"
          :style="sideColumnStyle(column)"
        >
          <header class="playoff-round-head">
            <h3>{{ column.label }}</h3>
          </header>
          <div class="playoff-side-cards">
            <PlayoffMatchCard v-for="card in column.cards" :key="card.key" :card="card" />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import PlayoffMatchCard from './PlayoffMatchCard.vue'

defineProps({
  leftColumns: {
    type: Array,
    required: true,
  },
  rightColumns: {
    type: Array,
    required: true,
  },
  centerCards: {
    type: Array,
    required: true,
  },
})

function roundClass(round) {
  return {
    'is-round-of-16': round.key === 'ROUND_OF_16',
    'is-quarterfinal': round.key === 'QUARTERFINAL',
    'is-semifinal': round.key === 'SEMIFINAL',
    'is-final': round.key === 'FINAL',
    'is-third-place': round.key === 'THIRD_PLACE',
  }
}

function sideColumnStyle(round) {
  const tieCount = Math.max(Number(round?.expectedTieCount || round?.cards?.length || 1), 1)
  const depth = Math.max(Math.log2(tieCount), 0)

  return {
    '--playoff-side-gap': `${18 + depth * 30}px`,
    '--playoff-side-padding-top': `${Math.max(depth * 26, 0)}px`,
  }
}
</script>
