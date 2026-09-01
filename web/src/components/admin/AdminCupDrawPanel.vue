<template>
  <section class="cup-draw-panel">
    <header class="cup-draw-header">
      <div>
        <span class="muted-text">Подготовка Кубка</span>
        <h4>Жеребьёвка и сетка</h4>
      </div>
      <span class="cup-draw-status" :class="`is-${String(cup.drawStatus).toLowerCase()}`">
        {{ drawStatusLabel }}
      </span>
    </header>

    <div class="cup-draw-steps" aria-label="Этапы жеребьёвки">
      <div class="is-complete"><span>1</span><strong>Участники</strong></div>
      <div :class="{ 'is-active': cup.drawStatus === 'NOT_DRAWN', 'is-complete': cup.drawStatus === 'DRAFT' }"><span>2</span><strong>Жеребьёвка</strong></div>
      <div :class="{ 'is-active': cup.drawStatus === 'DRAFT' }"><span>3</span><strong>Утверждение</strong></div>
    </div>

    <div class="cup-draw-layout">
      <section class="cup-draw-section">
        <div class="cup-draw-section-head">
          <div>
            <h5>Расстановка команд</h5>
            <span class="muted-text">{{ drawOrder.length }} участников</span>
          </div>
          <button class="btn-ghost" type="button" :disabled="busy || drawOrder.length < 2" @click="$emit('draw-random')">
            Перемешать
          </button>
        </div>

        <div class="cup-seeding-list">
          <div v-for="(teamId, index) in drawOrder" :key="teamId" class="cup-seeding-row">
            <span class="cup-seeding-number">{{ index + 1 }}</span>
            <strong>{{ teamName(teamId) }}</strong>
            <div class="cup-seeding-actions">
              <button class="icon-button" type="button" title="Поднять команду" :disabled="busy || index === 0" @click="$emit('move-team', index, -1)">↑</button>
              <button class="icon-button" type="button" title="Опустить команду" :disabled="busy || index === drawOrder.length - 1" @click="$emit('move-team', index, 1)">↓</button>
            </div>
          </div>
        </div>

        <button class="btn-ghost cup-apply-order" type="button" :disabled="busy || drawOrder.length < 2" @click="$emit('draw-manual')">
          Сформировать по этой расстановке
        </button>
      </section>

      <section class="cup-draw-section cup-bracket-preview">
        <div class="cup-draw-section-head">
          <div>
            <h5>Предварительная сетка</h5>
            <span class="muted-text">{{ cup.regularTieLegs === 1 ? 'Один матч' : 'Два матча' }} в паре</span>
          </div>
          <button class="btn-primary" type="button" :disabled="busy || cup.drawStatus !== 'DRAFT'" @click="$emit('confirm')">
            Утвердить сетку
          </button>
        </div>

        <div v-if="rounds.length" class="cup-preview-rounds">
          <section v-for="round in rounds" :key="round.code" class="cup-preview-round">
            <h6>{{ round.label }}</h6>
            <div class="cup-preview-ties">
              <article v-for="tie in round.ties" :key="tie.id" class="cup-preview-tie">
                <span class="cup-preview-pair">{{ pairTitle(tie, round) }}</span>
                <div><strong>{{ tieTeamName(tie, 'home') }}</strong></div>
                <div><strong>{{ tieTeamName(tie, 'away') }}</strong></div>
                <small>{{ tie.status === 'BYE' ? 'Свободный проход' : matchesLabel(tie.legCount) }}</small>
              </article>
            </div>
          </section>
        </div>

        <div v-else class="cup-preview-rounds">
          <section class="cup-preview-round">
            <h6>Первый раунд</h6>
            <div class="cup-preview-ties">
              <article v-for="pair in previewPairs" :key="pair.slot" class="cup-preview-tie">
                <span class="cup-preview-pair">Пара {{ pair.slot }}</span>
                <div><strong>{{ teamName(pair.homeId) }}</strong></div>
                <div><strong>{{ pair.awayId ? teamName(pair.awayId) : 'Свободный проход' }}</strong></div>
                <small>{{ pair.awayId ? matchesLabel(cup.regularTieLegs) : 'Без матча' }}</small>
              </article>
            </div>
          </section>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  busy: { type: Boolean, default: false },
  cup: { type: Object, required: true },
  drawOrder: { type: Array, required: true },
  rounds: { type: Array, required: true },
})

defineEmits(['confirm', 'draw-manual', 'draw-random', 'move-team'])

const drawStatusLabel = computed(() => ({
  NOT_DRAWN: 'Не проводилась',
  DRAFT: 'Черновик готов',
})[props.cup.drawStatus] || props.cup.drawStatus)

const teamMap = computed(() => new Map((props.cup.teams || []).map((team) => [String(team.id), team])))
const tieMap = computed(() => new Map((props.cup.ties || []).map((tie) => [String(tie.id), tie])))
const previewPairs = computed(() => {
  if (props.drawOrder.length < 2) return []
  let bracketSize = 1
  while (bracketSize < props.drawOrder.length) bracketSize *= 2
  const byes = bracketSize - props.drawOrder.length
  const pairs = []
  let cursor = 0
  for (let slot = 1; slot <= bracketSize / 2; slot += 1) {
    const homeId = props.drawOrder[cursor++]
    const awayId = slot <= byes ? null : props.drawOrder[cursor++]
    pairs.push({ slot, homeId, awayId })
  }
  return pairs
})

function teamName(teamId) {
  const team = teamMap.value.get(String(teamId || ''))
  return team?.shortName || team?.name || 'Команда не определена'
}

function sourceName(tieId, result) {
  const source = tieMap.value.get(String(tieId || ''))
  const prefix = result === 'LOSER' ? 'Проигравший' : 'Победитель'
  return source ? `${prefix} пары ${source.slotOrder}` : `${prefix} предыдущей пары`
}

function tieTeamName(tie, side) {
  const team = side === 'home' ? tie.homeTeam : tie.awayTeam
  if (team) return team.shortName || team.name
  return sourceName(
    side === 'home' ? tie.homeSourceTieId : tie.awaySourceTieId,
    side === 'home' ? tie.homeSourceResult : tie.awaySourceResult
  )
}

function pairTitle(tie, round) {
  return round.ties.length === 1 ? round.label : `Пара ${tie.slotOrder}`
}

function matchesLabel(count) {
  return `${count} ${Number(count) === 1 ? 'матч' : 'матча'}`
}
</script>

<style scoped>
.cup-draw-panel{display:grid;gap:18px}.cup-draw-header,.cup-draw-section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.cup-draw-header h4,.cup-draw-section h5,.cup-preview-round h6{margin:3px 0 0}.cup-draw-status{padding:7px 10px;border:1px solid var(--line);color:var(--muted)}.cup-draw-status.is-draft{border-color:rgba(97,232,162,.45);color:var(--brand)}.cup-draw-steps{display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--line)}.cup-draw-steps div{display:flex;align-items:center;gap:9px;min-height:54px;padding:10px 14px;border-right:1px solid var(--line);color:var(--muted)}.cup-draw-steps div:last-child{border-right:0}.cup-draw-steps span{display:grid;place-items:center;width:26px;height:26px;border:1px solid var(--line);border-radius:50%}.cup-draw-steps .is-active,.cup-draw-steps .is-complete{color:var(--text)}.cup-draw-steps .is-active{background:rgba(97,232,162,.08)}.cup-draw-steps .is-active span,.cup-draw-steps .is-complete span{border-color:var(--brand);color:var(--brand)}.cup-draw-layout{display:grid;grid-template-columns:minmax(280px,.72fr) minmax(0,1.28fr);gap:18px}.cup-draw-section{display:grid;align-content:start;gap:14px;padding:16px;border:1px solid var(--line);background:var(--bg-soft)}.cup-seeding-list{display:grid}.cup-seeding-row{display:grid;grid-template-columns:32px minmax(0,1fr) 84px;align-items:center;gap:10px;min-height:52px;border-bottom:1px solid var(--line)}.cup-seeding-number{display:grid;place-items:center;width:28px;height:28px;border:1px solid var(--line);border-radius:50%;color:var(--brand);font-weight:800}.cup-seeding-actions{display:flex;gap:6px;justify-content:flex-end}.cup-apply-order{width:100%}.cup-preview-rounds{display:grid;grid-auto-flow:column;grid-auto-columns:minmax(180px,1fr);gap:14px;overflow-x:auto;padding-bottom:4px}.cup-preview-round{display:grid;grid-template-rows:32px 1fr;gap:10px;min-width:0}.cup-preview-round h6{text-align:center}.cup-preview-ties{display:flex;flex-direction:column;justify-content:space-around;gap:12px}.cup-preview-tie{display:grid;gap:5px;padding:10px;border:1px solid var(--line);background:rgba(255,255,255,.035)}.cup-preview-pair{color:var(--brand);font-size:.75rem;font-weight:800}.cup-preview-tie div{min-width:0;padding:7px 9px;background:rgba(255,255,255,.06)}.cup-preview-tie strong{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.cup-preview-tie small{color:var(--muted)}@media(max-width:1000px){.cup-draw-layout{grid-template-columns:1fr}.cup-preview-rounds{grid-auto-columns:minmax(210px,1fr)}}@media(max-width:600px){.cup-draw-header,.cup-draw-section-head{display:grid}.cup-draw-steps{grid-template-columns:1fr}.cup-draw-steps div{border-right:0;border-bottom:1px solid var(--line)}.cup-draw-steps div:last-child{border-bottom:0}.cup-seeding-row{grid-template-columns:30px minmax(0,1fr) 78px}.cup-preview-rounds{grid-auto-flow:row;grid-auto-columns:auto;grid-template-columns:1fr;overflow-x:visible}.cup-preview-round{grid-template-rows:auto 1fr}.cup-preview-round h6{text-align:left}}
</style>
