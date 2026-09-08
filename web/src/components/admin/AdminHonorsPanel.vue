<template>
  <section class="admin-step-section honors-admin">
    <div class="admin-step-heading">
      <span class="admin-step-number">4</span>
      <div><h4>Зал славы и награды</h4><p>Призёры, индивидуальные награды и символическая сборная каждого соревнования.</p></div>
    </div>

    <UiState v-if="error" tone="error" title="Операция не выполнена" :message="error" />
    <UiState v-if="success" tone="success" title="Готово" :message="success" />

    <div class="honors-pickers">
      <label>Сезон<select v-model="seasonId" :disabled="busy"><option value="">— выберите —</option><option v-for="season in seasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option></select></label>
      <label>Соревнование<select v-model="competitionId" :disabled="busy || !seasonId"><option value="">— выберите —</option><option v-for="item in competitions" :key="item.id" :value="String(item.id)">{{ item.type === 'CUP' ? 'Кубок' : 'Чемпионат' }} · {{ item.name }}</option></select></label>
    </div>

    <UiState v-if="loading" tone="loading" title="Загружаем награды" />
    <template v-else-if="editor">
      <div class="honors-publication">
        <div><strong>{{ editor.competitionName }}</strong><span>{{ editor.seasonName }} · {{ editor.playersOnField }} игроков на поле</span></div>
        <label><input v-model="published" type="checkbox" /> Опубликовать в Зале славы</label>
      </div>

      <section class="honors-block">
        <header><div><h4>Награды и призёры</h4><p>Название награды можно изменить или добавить собственную.</p></div><button class="btn-ghost btn-sm" type="button" @click="addAward">Добавить награду</button></header>
        <div class="honors-award-list">
          <div v-for="(award, index) in manualAwards" :key="award.localId" class="honors-award-row">
            <input v-model.trim="award.title" type="text" aria-label="Название награды" placeholder="Название награды" />
            <select v-model="award.winnerType" aria-label="Тип победителя" @change="award.winnerId = ''"><option value="TEAM">Команда</option><option value="PLAYER">Игрок</option></select>
            <select v-if="award.winnerType === 'TEAM'" v-model="award.winnerId" aria-label="Победитель">
              <option value="">— выберите победителя —</option>
              <option v-for="team in editor.teams" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
            </select>
            <SearchableSelect
              v-else
              v-model="award.winnerId"
              :options="playerOptions()"
              :max-visible-options="10"
              aria-label="Победитель"
              placeholder="— выбрать игрока —"
              search-placeholder="Поиск по ФИО или команде"
              empty-text="Игроки не найдены"
            />
            <button class="honors-remove" type="button" aria-label="Удалить награду" @click="manualAwards.splice(index, 1)">×</button>
          </div>
        </div>
        <div class="honors-auto-award">
          <span>Автоматически</span><div><strong>Лучший бомбардир</strong><small>{{ topScorersLabel }}</small></div>
        </div>
      </section>

      <section class="honors-block">
        <header><div><h4>Символическая сборная</h4><p>Вратарь добавляется автоматически, остальные линии задаются числами через дефис.</p></div></header>
        <div class="honors-formation-bar">
          <label>Схема<input v-model.trim="formation" type="text" :placeholder="editor.playersOnField <= 5 ? '1-2-1' : '4-4-2'" /></label>
          <button class="btn-ghost" type="button" @click="buildFormation">Построить схему</button>
          <span>Всего мест: {{ slots.length }}</span>
        </div>
        <div v-if="slots.length" class="honors-lineup-editor">
          <div v-for="(slot, index) in slots" :key="slot.localId" class="honors-slot-row">
            <span>{{ index + 1 }}</span>
            <input v-model.trim="slot.positionLabel" type="text" aria-label="Позиция" />
            <SearchableSelect
              v-model="slot.playerId"
              :options="playerOptions(slot.playerId, true)"
              :max-visible-options="10"
              aria-label="Игрок сборной"
              placeholder="— выбрать игрока —"
              search-placeholder="Поиск по ФИО или команде"
              empty-text="Игроки не найдены"
            />
          </div>
        </div>
      </section>

      <div class="admin-primary-actions honors-actions">
        <span>Лучший бомбардир будет пересчитан по подтверждённым протоколам при сохранении.</span>
        <button class="btn-primary" type="button" :disabled="busy" @click="save">{{ busy ? 'Сохраняем…' : 'Сохранить награды' }}</button>
      </div>
    </template>
    <UiState v-else title="Выберите соревнование" message="Для Чемпионата и каждого Кубка сохраняется собственный набор наград." />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import UiState from '../UiState.vue'
import SearchableSelect from '../SearchableSelect.vue'
import { useAuth } from '../../store/auth'
import { createCompetitionsApi } from '../../api/competitions'

const { authorizedApiRequest: request } = useAuth()
const api = createCompetitionsApi(request)
const seasons = ref([])
const competitions = ref([])
const seasonId = ref('')
const competitionId = ref('')
const editor = ref(null)
const manualAwards = ref([])
const slots = ref([])
const formation = ref('')
const published = ref(false)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const success = ref('')
let localId = 0

const defaultAwards = [
  ['CHAMPION', 'Чемпион', 'TEAM'], ['SILVER', 'Серебряный призёр', 'TEAM'], ['BRONZE', 'Бронзовый призёр', 'TEAM'],
  ['BEST_PLAYER', 'Лучший игрок', 'PLAYER'], ['BEST_GOALKEEPER', 'Лучший вратарь', 'PLAYER'],
  ['BEST_DEFENDER', 'Лучший защитник', 'PLAYER'], ['BEST_MIDFIELDER', 'Лучший полузащитник', 'PLAYER'], ['BEST_FORWARD', 'Лучший нападающий', 'PLAYER'],
]

const topScorersLabel = computed(() => {
  const rows = editor.value?.awards?.filter((award) => award.code === 'TOP_SCORER') || []
  return rows.length ? rows.map((award) => `${award.winnerName} · ${award.statValue} гол.`).join(', ') : 'Рассчитается после сохранения'
})

watch(seasonId, async () => {
  competitionId.value = ''; editor.value = null; competitions.value = []
  if (!seasonId.value) return
  try { competitions.value = await api.list(seasonId.value) } catch (err) { error.value = err.message || 'Не удалось загрузить соревнования.' }
})

watch(competitionId, async () => {
  editor.value = null
  if (!competitionId.value) return
  await loadEditor()
})

async function loadEditor() {
  loading.value = true; error.value = ''; success.value = ''
  try {
    editor.value = await api.honors(seasonId.value, competitionId.value)
    published.value = Boolean(editor.value.published)
    formation.value = editor.value.formation || ''
    const existing = (editor.value.awards || []).filter((award) => award.code !== 'TOP_SCORER')
    manualAwards.value = (existing.length ? existing.map((award) => [award.code, award.title, award.winnerType, award.playerId || award.teamId]) : defaultAwards)
      .map(([code, title, winnerType, winnerId]) => ({ localId: ++localId, code, title, winnerType, winnerId: winnerId ? String(winnerId) : '' }))
    slots.value = (editor.value.slots || []).map((slot) => ({ ...slot, localId: ++localId, playerId: String(slot.playerId) }))
    if (!slots.value.length) buildFormation()
  } catch (err) { error.value = err.message || 'Не удалось загрузить награды.' }
  finally { loading.value = false }
}

function addAward() { manualAwards.value.push({ localId: ++localId, code: null, title: '', winnerType: 'PLAYER', winnerId: '' }) }

function buildFormation() {
  const lines = formation.value.split('-').map(Number).filter((value) => Number.isInteger(value) && value > 0 && value <= 8)
  if (!lines.length) { error.value = 'Укажите схему числами через дефис, например 4-4-2 или 1-2-1.'; return }
  const expected = Number(editor.value?.playersOnField || 0)
  if (lines.reduce((sum, value) => sum + value, 1) !== expected) { error.value = `Схема должна содержать ${expected} игроков вместе с вратарём.`; return }
  error.value = ''
  const previous = slots.value.map((slot) => slot.playerId)
  const result = [{ localId: ++localId, playerId: previous.shift() || '', positionLabel: 'ВР', xPercent: 50, yPercent: 88, sortOrder: 10 }]
  lines.forEach((count, lineIndex) => {
    const y = Math.round(70 - (lineIndex * 50 / Math.max(1, lines.length - 1)))
    for (let index = 0; index < count; index += 1) {
      result.push({ localId: ++localId, playerId: previous.shift() || '', positionLabel: lineIndex === lines.length - 1 ? 'НАП' : lineIndex === 0 ? 'ЗАЩ' : 'ПЗ', xPercent: Math.round((index + 1) * 100 / (count + 1)), yPercent: y, sortOrder: result.length * 10 + 10 })
    }
  })
  slots.value = result
}

function availablePlayers(currentId) {
  const used = new Set(slots.value.map((slot) => slot.playerId).filter((id) => id && id !== currentId))
  return (editor.value?.players || []).filter((player) => !used.has(String(player.playerId)))
}

function playerOptions(currentId = '', excludeUsed = false) {
  const candidates = excludeUsed ? availablePlayers(currentId) : (editor.value?.players || [])
  return candidates.map((player) => ({
    value: String(player.playerId),
    label: player.playerName,
    caption: player.teamName,
    keywords: `${player.playerName} ${player.teamName || ''}`,
  }))
}

async function save() {
  const invalidAward = manualAwards.value.find((award) => award.title && !award.winnerId)
  if (invalidAward) { error.value = `Выберите победителя для награды «${invalidAward.title}».`; return }
  busy.value = true; error.value = ''; success.value = ''
  try {
    editor.value = await api.saveHonors(seasonId.value, competitionId.value, {
      published: published.value,
      formation: formation.value,
      awards: manualAwards.value.filter((award) => award.title && award.winnerId).map((award, index) => ({ code: award.code, title: award.title, winnerType: award.winnerType, winnerId: Number(award.winnerId), sortOrder: (index + 1) * 10 })),
      slots: slots.value.filter((slot) => slot.playerId).map((slot, index) => ({ playerId: Number(slot.playerId), positionLabel: slot.positionLabel, xPercent: slot.xPercent, yPercent: slot.yPercent, sortOrder: (index + 1) * 10 })),
    })
    success.value = published.value ? 'Награды сохранены и опубликованы в Зале славы.' : 'Награды сохранены как черновик.'
    await loadEditor()
    success.value = published.value ? 'Награды сохранены и опубликованы в Зале славы.' : 'Награды сохранены как черновик.'
  } catch (err) { error.value = err.message || 'Не удалось сохранить награды.' }
  finally { busy.value = false }
}

onMounted(async () => {
  try {
    seasons.value = await request('/api/seasons?active_flag=0', { method: 'GET' })
    if (seasons.value.length) seasonId.value = String(seasons.value[0].id)
  } catch (err) { error.value = err.message || 'Не удалось загрузить сезоны.' }
})
</script>

<style scoped>
.honors-admin { display: grid; gap: 16px; }
.honors-pickers { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.honors-pickers label, .honors-formation-bar label { display: grid; gap: 7px; min-width: 0; color: var(--muted); font-size: .7rem; }
.honors-pickers select { width: 100%; min-width: 0; }
.honors-publication { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 16px; border: 1px solid rgba(97,232,162,.22); border-radius: 10px; background: rgba(97,232,162,.05); }
.honors-publication > div { display: grid; gap: 4px; }
.honors-publication span, .honors-publication label { color: var(--muted); font-size: .68rem; }
.honors-publication label { display: flex; align-items: center; gap: 8px; color: var(--text); }
.honors-block { overflow: hidden; border: 1px solid rgba(124,163,255,.15); border-radius: 10px; }
.honors-block > header { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 14px 16px; background: rgba(124,163,255,.04); }
.honors-block h4, .honors-block p { margin: 0; }
.honors-block h4 { font-size: .82rem; }
.honors-block p { margin-top: 4px; color: var(--muted); font-size: .66rem; }
.honors-award-list { display: grid; }
.honors-award-row { display: grid; grid-template-columns: minmax(170px, 1fr) 130px minmax(220px, 1.3fr) 34px; gap: 8px; padding: 10px 16px; border-top: 1px solid rgba(124,163,255,.1); }
.honors-award-row :is(input, select) { min-width: 0; width: 100%; font-size: .7rem; }
.honors-remove { border: 0; background: transparent; color: #ff8da2; font-size: 1.1rem; cursor: pointer; }
.honors-auto-award { display: grid; grid-template-columns: 110px 1fr; gap: 12px; padding: 13px 16px; border-top: 1px solid rgba(124,163,255,.12); background: rgba(97,232,162,.035); }
.honors-auto-award > span { color: var(--brand); font-size: .62rem; font-weight: 800; text-transform: uppercase; }
.honors-auto-award div { display: grid; gap: 3px; }
.honors-auto-award small { color: var(--muted); font-size: .65rem; }
.honors-formation-bar { display: grid; grid-template-columns: minmax(180px, 260px) auto 1fr; align-items: end; gap: 12px; padding: 14px 16px; border-top: 1px solid rgba(124,163,255,.1); }
.honors-formation-bar > span { align-self: center; color: var(--muted); font-size: .66rem; }
.honors-lineup-editor { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 12px; padding: 0 16px 14px; }
.honors-slot-row { display: grid; grid-template-columns: 24px 70px minmax(0, 1fr); align-items: center; gap: 7px; padding: 7px 0; border-top: 1px solid rgba(124,163,255,.08); }
.honors-slot-row > span { color: var(--muted); font-size: .65rem; }
.honors-slot-row :is(input, select) { width: 100%; min-width: 0; font-size: .68rem; }
.honors-award-row :deep(.searchable-select), .honors-slot-row :deep(.searchable-select) { min-width: 0; }
.honors-award-row :deep(.searchable-select-input), .honors-slot-row :deep(.searchable-select-input) { min-width: 0; min-height: 38px; font-size: .68rem; }
.honors-award-row :deep(.searchable-select-toggle), .honors-slot-row :deep(.searchable-select-toggle) { min-width: 38px; }
.honors-actions { align-items: center; justify-content: space-between; }
.honors-actions > span { color: var(--muted); font-size: .66rem; }
@media(max-width: 760px) { .honors-pickers, .honors-lineup-editor { grid-template-columns: 1fr; } .honors-publication, .honors-block > header { align-items: stretch; flex-direction: column; } .honors-award-row { grid-template-columns: 1fr 110px 34px; } .honors-award-row > input { grid-column: 1 / -1; } .honors-formation-bar { grid-template-columns: 1fr; align-items: stretch; } }
</style>
