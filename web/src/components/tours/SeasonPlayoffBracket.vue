<template>
  <div ref="wrapElement" class="playoff-bracket-wrap">
    <div ref="shellElement" class="playoff-stage-shell" :style="shellStyle">
      <svg
        v-if="connectorSize.width && connectorSize.height"
        class="playoff-connectors"
        :viewBox="`0 0 ${connectorSize.width} ${connectorSize.height}`"
        aria-hidden="true"
      >
        <path v-for="connector in connectors" :key="connector.key" :d="connector.path" />
      </svg>

      <div class="playoff-stage-side playoff-stage-side-left">
        <section
          v-for="column in leftColumns"
          :key="`left-${column.key}`"
          class="playoff-side-column playoff-side-column-left"
          :class="roundClass(column)"
        >
          <header class="playoff-round-head">
            <h3>{{ column.label }}</h3>
          </header>
          <div class="playoff-side-cards">
            <div
              v-for="card in column.cards"
              :key="card.key"
              class="playoff-card-anchor"
              :data-tie-id="card.tieId || undefined"
              :data-round-key="card.roundKey || column.key"
              :data-source-tie-ids="sourceTieIds(card)"
            >
              <PlayoffMatchCard :card="card" :season-id="seasonId" />
            </div>
          </div>
        </section>
      </div>

      <div class="playoff-stage-center">
        <section class="playoff-center-stack">
          <div
            v-for="card in centerCards"
            :key="card.key"
            class="playoff-center-card-wrap"
            :class="{
              'is-final': card.roundKey === 'FINAL',
              'is-third-place': card.roundKey === 'THIRD_PLACE',
            }"
          >
            <header class="playoff-round-head playoff-round-head-center">
              <h3>{{ card.roundLabel }}</h3>
            </header>
            <div
              class="playoff-card-anchor"
              :data-tie-id="card.tieId || undefined"
              :data-round-key="card.roundKey"
              :data-source-tie-ids="sourceTieIds(card)"
            >
              <PlayoffMatchCard :card="card" :season-id="seasonId" center />
            </div>
          </div>
        </section>
      </div>

      <div class="playoff-stage-side playoff-stage-side-right">
        <section
          v-for="column in rightColumns"
          :key="`right-${column.key}`"
          class="playoff-side-column playoff-side-column-right"
          :class="roundClass(column)"
        >
          <header class="playoff-round-head">
            <h3>{{ column.label }}</h3>
          </header>
          <div class="playoff-side-cards">
            <div
              v-for="card in column.cards"
              :key="card.key"
              class="playoff-card-anchor"
              :data-tie-id="card.tieId || undefined"
              :data-round-key="card.roundKey || column.key"
              :data-source-tie-ids="sourceTieIds(card)"
            >
              <PlayoffMatchCard :card="card" :season-id="seasonId" />
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PlayoffMatchCard from './PlayoffMatchCard.vue'

const props = defineProps({
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
  seasonId: {
    type: [String, Number],
    default: '',
  },
})

const shellElement = ref(null)
const wrapElement = ref(null)
const connectors = ref([])
const connectorSize = ref({ width: 0, height: 0 })
let resizeObserver = null

const shellStyle = computed(() => {
  const sideColumns = [...props.leftColumns, ...props.rightColumns]
  const maximumCards = Math.max(
    1,
    ...sideColumns.map((column) => Number(column?.cards?.length || 0)),
  )

  return {
    '--playoff-stage-height': `${Math.max(520, 180 + maximumCards * 150)}px`,
  }
})

function roundClass(round) {
  return {
    'is-round-of-16': round.key === 'ROUND_OF_16',
    'is-quarterfinal': round.key === 'QUARTERFINAL',
    'is-semifinal': round.key === 'SEMIFINAL',
  }
}

function sourceTieIds(card) {
  const ids = [card?.homeSourceTieId, card?.awaySourceTieId].filter(Boolean)
  return ids.length ? ids.join(',') : undefined
}

function connectorPath(sourceRect, targetRect, shellRect) {
  const sourceCenterX = sourceRect.left + sourceRect.width / 2
  const targetCenterX = targetRect.left + targetRect.width / 2
  const flowsRight = sourceCenterX < targetCenterX
  const sourceX = (flowsRight ? sourceRect.right : sourceRect.left) - shellRect.left
  const targetX = (flowsRight ? targetRect.left : targetRect.right) - shellRect.left
  const sourceY = sourceRect.top + sourceRect.height / 2 - shellRect.top
  const targetY = targetRect.top + targetRect.height / 2 - shellRect.top
  const middleX = sourceX + (targetX - sourceX) / 2

  return `M ${sourceX} ${sourceY} H ${middleX} V ${targetY} H ${targetX}`
}

async function updateConnectors() {
  await nextTick()
  const shell = shellElement.value
  if (!shell) return

  const shellRect = shell.getBoundingClientRect()
  const tieElements = new Map()
  shell.querySelectorAll('[data-tie-id]').forEach((element) => {
    tieElements.set(String(element.dataset.tieId), element)
  })

  const nextConnectors = []
  shell.querySelectorAll('[data-source-tie-ids]').forEach((target) => {
    if (target.dataset.roundKey === 'THIRD_PLACE') return

    String(target.dataset.sourceTieIds || '').split(',').filter(Boolean).forEach((sourceId) => {
      const source = tieElements.get(sourceId)
      if (!source) return
      nextConnectors.push({
        key: `${sourceId}-${target.dataset.tieId}`,
        path: connectorPath(source.getBoundingClientRect(), target.getBoundingClientRect(), shellRect),
      })
    })
  })

  connectorSize.value = {
    width: Math.max(shellRect.width, 1),
    height: Math.max(shellRect.height, 1),
  }
  connectors.value = nextConnectors
}

watch(
  () => [props.leftColumns, props.rightColumns, props.centerCards],
  updateConnectors,
  { deep: true },
)

onMounted(() => {
  resizeObserver = new ResizeObserver(updateConnectors)
  if (shellElement.value) resizeObserver.observe(shellElement.value)
  if (wrapElement.value) resizeObserver.observe(wrapElement.value)
  window.addEventListener('resize', updateConnectors)
  void updateConnectors()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', updateConnectors)
})
</script>
