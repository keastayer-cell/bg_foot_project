<template>
  <section class="admin-step-section regulations-admin">
    <div class="admin-step-heading"><span class="admin-step-number">3</span><div><h4>Регламенты и положения</h4><p>Отдельный PDF для сезона, чемпионата или кубка.</p></div></div>
    <UiState v-if="error" tone="error" title="Операция не выполнена" :message="error" />
    <UiState v-if="success" tone="success" title="Готово" :message="success" />
    <UiState v-if="loading" tone="loading" title="Загружаем документы" />
    <template v-else>
      <div class="regulations-fields">
        <label>Сезон<select v-model="seasonId" :disabled="busy"><option v-for="season in seasons" :key="season.id" :value="String(season.id)">{{ season.name }}</option></select></label>
        <label>К чему относится документ<select v-model="targetKey" :disabled="busy"><option v-for="item in seasonTargets" :key="key(item)" :value="key(item)">{{ targetLabel(item) }}</option></select></label>
      </div>
      <template v-if="selected">
        <div class="regulations-selected"><strong>{{ selected.seasonName }} · {{ targetLabel(selected) }}</strong><span>{{ selected.available ? `PDF опубликован · ${formatDate(selected.updatedAt)}` : 'PDF ещё не загружен' }}</span></div>
        <label class="regulations-upload">PDF-файл<input :key="fileVersion" type="file" accept="application/pdf,.pdf" :disabled="busy" @change="selectFile" /></label>
        <p v-if="fileName" class="regulations-file-name">Выбран файл: {{ fileName }}</p>
        <p class="muted-text">Новый файл заменит только документ «{{ targetLabel(selected) }}» в сезоне «{{ selected.seasonName }}».</p>
        <div class="admin-primary-actions">
          <button class="btn-primary" type="button" :disabled="busy || !dataUrl" @click="save">{{ busy ? 'Сохраняем…' : selected.available ? 'Заменить PDF' : 'Опубликовать PDF' }}</button>
          <a v-if="selected.downloadUrl" class="btn-ghost" :href="apiBaseUrl + selected.downloadUrl" target="_blank" rel="noopener">Скачать PDF</a>
          <button v-if="selected.available" class="btn-danger" type="button" :disabled="busy" @click="remove">Удалить PDF</button>
        </div>
      </template>
      <UiState v-else title="Сначала создайте сезон или соревнование" />
      <div v-if="seasonTargets.length" class="regulations-register">
        <h4>Документы выбранного сезона</h4>
        <button v-for="item in seasonTargets" :key="key(item)" type="button" :disabled="busy" :class="{ 'is-selected': key(item) === targetKey }" @click="targetKey = key(item)"><strong>{{ targetLabel(item) }}</strong><span>{{ item.available ? 'PDF опубликован' : 'Нет документа' }}</span></button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import UiState from '../UiState.vue'
import { useAuth } from '../../store/auth'
import { createAdminLeagueApi } from '../../api/adminLeague'
import { useConfirmDialog } from '../../composables/useConfirmDialog'
const emit = defineEmits(['changed'])
const { authorizedApiRequest: request } = useAuth()
const leagueApi = createAdminLeagueApi(request)
const { confirmAction } = useConfirmDialog()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const targets = ref([])
const seasonId = ref('')
const targetKey = ref('')
const dataUrl = ref('')
const fileName = ref('')
const fileVersion = ref(0)
let readVersion = 0
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const success = ref('')
const key = (item) => `${item.targetType}:${item.targetId}`
const targetLabel = (item) => item.targetType === 'SEASON' ? 'Общий регламент сезона' : `${item.competitionType === 'CUP' ? 'Кубок' : 'Чемпионат'} · ${item.competitionName}`
const seasons = computed(() => [...new Map(targets.value.map((item) => [item.seasonId, { id: item.seasonId, name: item.seasonName }])).values()])
const seasonTargets = computed(() => targets.value.filter((item) => String(item.seasonId) === seasonId.value))
const selected = computed(() => seasonTargets.value.find((item) => key(item) === targetKey.value))
watch(seasonId, () => { targetKey.value = seasonTargets.value[0] ? key(seasonTargets.value[0]) : '' })
watch(targetKey, () => { dataUrl.value = ''; fileName.value = ''; fileVersion.value++; readVersion++; error.value = ''; success.value = '' })
const formatDate = (value) => value ? new Date(value).toLocaleDateString('ru-RU') : '—'
async function load() {
  loading.value = true
  try {
    const result = await leagueApi.regulations.list()
    targets.value = Array.isArray(result) ? result : []
    if (!seasonId.value && seasons.value.length) seasonId.value = String(seasons.value[0].id)
  } catch (err) { error.value = err.message || 'Не удалось загрузить документы.' }
  finally { loading.value = false }
}
async function selectFile(event) {
  dataUrl.value = ''
  error.value = ''
  const file = event.target.files?.[0]
  if (!file) return
  if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { error.value = 'Выберите PDF-файл.'; return }
  fileName.value = file.name
  const targetAtStart = targetKey.value
  const version = ++readVersion
  try {
    const result = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result).replace(/^data:[^;]*;/, 'data:application/pdf;'))
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
    if (targetAtStart === targetKey.value && version === readVersion) dataUrl.value = result
  } catch { error.value = 'Не удалось прочитать файл.' }
}
async function save() {
  if (!selected.value || !dataUrl.value || busy.value) return
  const target = selected.value
  busy.value = true; error.value = ''; success.value = ''
  try {
    await leagueApi.regulations.save(target.targetType, target.targetId, dataUrl.value)
    dataUrl.value = ''; fileName.value = ''; fileVersion.value++
    await load()
    success.value = `Опубликован: ${target.seasonName} · ${targetLabel(target)}.`
    emit('changed')
  } catch (err) { error.value = err.message || 'Не удалось сохранить PDF.' }
  finally { busy.value = false }
}
async function remove() {
  const target = selected.value
  if (!target || busy.value) return
  const accepted = await confirmAction({ title: 'Удалить PDF?', message: `${target.seasonName} · ${targetLabel(target)}. Документ станет недоступен для скачивания.`, confirmLabel: 'Удалить PDF' })
  if (!accepted) return
  busy.value = true; error.value = ''; success.value = ''
  try {
    await leagueApi.regulations.remove(target.targetType, target.targetId)
    await load()
    success.value = `Документ удалён: ${targetLabel(target)}.`
    emit('changed')
  } catch (err) { error.value = err.message || 'Не удалось удалить PDF.' }
  finally { busy.value = false }
}
onMounted(load)
</script>

<style scoped>
.regulations-admin { display: grid; gap: 16px; }
.regulations-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.regulations-fields label, .regulations-upload { display: grid; gap: 7px; font-size: .75rem; min-width: 0; }
.regulations-fields select { width: 100%; min-width: 0; }
.regulations-selected { display: grid; gap: 6px; padding: 14px; border: 1px solid rgba(97, 232, 162, .2); border-radius: 9px; font-size: .75rem; }
.regulations-selected span { color: var(--muted); font-size: .65rem; }
.regulations-upload input { width: 100%; min-width: 0; }
.regulations-file-name { margin: 0; color: var(--brand); font-size: .68rem; }
.regulations-register { display: grid; gap: 5px; }
.regulations-register h4 { margin: 10px 0; font-size: .8rem; }
.regulations-register button { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px; border: 1px solid rgba(124, 163, 255, .15); border-radius: 7px; background: transparent; color: var(--text); text-align: left; cursor: pointer; font-size: .7rem; }
.regulations-register button.is-selected { border-color: rgba(97, 232, 162, .4); }
.regulations-register span { color: var(--muted); font-size: .65rem; }
@media(max-width: 640px) { .regulations-fields { grid-template-columns: 1fr; } .regulations-register button { flex-wrap: wrap; } }
</style>
