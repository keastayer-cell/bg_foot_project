<template>
  <article class="card admin-panel admin-league-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">Турнир</p>
      <h3 class="section-title">Лига</h3>
      <p class="muted-text">Руководство лиги, места проведения и документы сезонов и соревнований.</p>
    </header>

    <UiState v-if="messageError" tone="error" title="Операция не выполнена" :message="messageError" />
    <UiState v-if="messageOk" tone="success" title="Готово" :message="messageOk" />

    <nav class="admin-league-modules" aria-label="Разделы управления лигой">
      <button
        v-for="module in leagueModules"
        :key="module.value"
        class="admin-league-module"
        :class="{ 'is-active': activeLeagueSection === module.value }"
        type="button"
        @click="activeLeagueSection = module.value"
      >
        <span class="admin-league-module-number">{{ module.number }}</span>
        <span class="admin-league-module-copy">
          <strong>{{ module.label }}</strong>
          <small>{{ module.description }}</small>
        </span>
        <span v-if="module.count !== undefined" class="admin-league-module-count">{{ module.count }}</span>
      </button>
    </nav>

    <div class="admin-grid admin-league-grid">
      <section v-if="activeLeagueSection === 'officials'" class="admin-step-section admin-league-section">
        <div class="section-head admin-league-head">
          <span class="admin-step-number">1</span>
          <div>
            <p class="eyebrow">Руководство</p>
            <h4 class="section-title">Карточки руководства</h4>
            <p class="muted-text">Создавайте и упорядочивайте публичный состав руководителей лиги.</p>
          </div>
          <span class="admin-step-count">{{ officialsList.length }}</span>
        </div>

        <AdminEntityModeSwitch :model-value="officialMode" :options="officialModeOptions" @update:model-value="switchOfficialMode" />

        <form v-if="officialMode === 'create'" class="admin-form admin-league-entity-form" @submit.prevent="createOfficial">
          <label>
            ФИО
            <input v-model.trim="officialForm.fullName" type="text" required />
          </label>
          <label>
            Должность
            <input v-model.trim="officialForm.positionTitle" type="text" required />
          </label>
          <label>
            Порядок вывода
            <input v-model="officialForm.sortOrder" type="number" min="0" />
          </label>
          <label class="admin-league-wide-field">
            Краткое описание
            <textarea v-model.trim="officialForm.bio" rows="4" placeholder="Чем отвечает в лиге"></textarea>
          </label>
          <label class="admin-league-wide-field admin-league-file-field">
            Фото
            <input type="file" accept="image/*" @change="onOfficialPhotoSelected" />
          </label>
          <img v-if="officialForm.photoDataUrl" :src="officialForm.photoDataUrl" alt="Превью фото руководителя" class="team-rep-player-photo-preview" />
          <div class="admin-primary-actions admin-league-form-actions">
            <button class="btn-primary" type="submit">Создать карточку</button>
          </div>
        </form>

        <div v-else class="admin-entity-flow admin-league-edit-flow">
          <label class="admin-league-record-picker">
            Выберите карточку
            <select v-model="officialEditId" @change="onOfficialSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in officialsList" :key="item.id" :value="String(item.id)">{{ item.fullName }} · {{ item.positionTitle }}</option>
            </select>
          </label>
          <form v-if="editingOfficialId" class="admin-form admin-league-entity-form" @submit.prevent="saveOfficial">
            <label>
              ФИО
              <input v-model.trim="officialForm.fullName" type="text" />
            </label>
            <label>
              Должность
              <input v-model.trim="officialForm.positionTitle" type="text" />
            </label>
            <label>
              Порядок вывода
              <input v-model="officialForm.sortOrder" type="number" min="0" />
            </label>
            <label class="admin-league-wide-field">
              Краткое описание
              <textarea v-model.trim="officialForm.bio" rows="4"></textarea>
            </label>
            <label class="admin-league-wide-field admin-league-file-field">
              Фото
              <input type="file" accept="image/*" @change="onOfficialPhotoSelected" />
            </label>
            <img v-if="officialForm.photoDataUrl" :src="officialForm.photoDataUrl" alt="Превью фото руководителя" class="team-rep-player-photo-preview" />
            <div class="admin-primary-actions admin-league-form-actions">
              <button class="btn-ghost" type="button" @click="switchOfficialMode('create')">Отмена</button>
              <button class="btn-primary" type="submit">Сохранить изменения</button>
            </div>
            <div class="admin-danger-zone admin-league-danger-zone">
              <div><strong>Удаление карточки</strong><p>Руководитель исчезнет с публичной страницы лиги.</p></div>
              <button class="btn-danger btn-sm" type="button" @click="deactivateOfficial(editingOfficialId)">Удалить карточку</button>
            </div>
          </form>
          <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите руководителя, чтобы открыть его карточку.</span></div>
        </div>
      </section>

      <section v-if="activeLeagueSection === 'venues'" class="admin-step-section admin-league-section">
        <div class="section-head admin-league-head">
          <span class="admin-step-number">2</span>
          <div>
            <p class="eyebrow">Площадки</p>
            <h4 class="section-title">Места проведения</h4>
            <p class="muted-text">Ведите единый реестр стадионов и площадок турнира.</p>
          </div>
          <span class="admin-step-count">{{ venuesList.length }}</span>
        </div>

        <AdminEntityModeSwitch :model-value="venueMode" :options="venueModeOptions" @update:model-value="switchVenueMode" />

        <form v-if="venueMode === 'create'" class="admin-form admin-league-entity-form" @submit.prevent="createVenue">
          <label>
            Название площадки
            <input v-model.trim="venueForm.name" type="text" required />
          </label>
          <label>
            Короткий код
            <input v-model.trim="venueForm.shortLabel" type="text" placeholder="Например: A1" />
          </label>
          <label>
            Адрес
            <input v-model.trim="venueForm.address" type="text" required />
          </label>
          <label>
            Порядок вывода
            <input v-model="venueForm.sortOrder" type="number" min="0" />
          </label>
          <label class="admin-league-wide-field">
            Описание
            <textarea v-model.trim="venueForm.description" rows="4"></textarea>
          </label>
          <label class="admin-league-wide-field admin-league-file-field">
            Фото площадки
            <input type="file" accept="image/*" @change="onVenuePhotoSelected" />
          </label>
          <img v-if="venueForm.photoDataUrl" :src="venueForm.photoDataUrl" alt="Превью площадки" class="team-rep-player-photo-preview" />
          <div class="admin-primary-actions admin-league-form-actions">
            <button class="btn-primary" type="submit">Создать площадку</button>
          </div>
        </form>

        <div v-else class="admin-entity-flow admin-league-edit-flow">
          <label class="admin-league-record-picker">
            Выберите площадку
            <select v-model="venueEditId" @change="onVenueSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in venuesList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
            </select>
          </label>
          <form v-if="editingVenueId" class="admin-form admin-league-entity-form" @submit.prevent="saveVenue">
            <label>
              Название площадки
              <input v-model.trim="venueForm.name" type="text" />
            </label>
            <label>
              Короткий код
              <input v-model.trim="venueForm.shortLabel" type="text" />
            </label>
            <label>
              Адрес
              <input v-model.trim="venueForm.address" type="text" />
            </label>
            <label>
              Порядок вывода
              <input v-model="venueForm.sortOrder" type="number" min="0" />
            </label>
            <label class="admin-league-wide-field">
              Описание
              <textarea v-model.trim="venueForm.description" rows="4"></textarea>
            </label>
            <label class="admin-league-wide-field admin-league-file-field">
              Фото площадки
              <input type="file" accept="image/*" @change="onVenuePhotoSelected" />
            </label>
            <img v-if="venueForm.photoDataUrl" :src="venueForm.photoDataUrl" alt="Превью площадки" class="team-rep-player-photo-preview" />
            <div class="admin-primary-actions admin-league-form-actions">
              <button class="btn-ghost" type="button" @click="switchVenueMode('create')">Отмена</button>
              <button class="btn-primary" type="submit">Сохранить изменения</button>
            </div>
            <div class="admin-danger-zone admin-league-danger-zone">
              <div><strong>Удаление площадки</strong><p>Площадка исчезнет с публичной страницы лиги.</p></div>
              <button class="btn-danger btn-sm" type="button" @click="deactivateVenue(editingVenueId)">Удалить площадку</button>
            </div>
          </form>
          <div v-else class="admin-record-placeholder"><span>↳</span><span>Выберите площадку, чтобы открыть её карточку.</span></div>
        </div>
      </section>
    </div>

    <AdminRegulationsPanel v-if="activeLeagueSection === 'documents'" @changed="emit('refresh-seasons')" />
    <AdminHonorsPanel v-if="activeLeagueSection === 'honors'" />
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import UiState from './UiState.vue'
import AdminRegulationsPanel from './admin/AdminRegulationsPanel.vue'
import AdminHonorsPanel from './admin/AdminHonorsPanel.vue'
import AdminEntityModeSwitch from './admin/AdminEntityModeSwitch.vue'
import { useAuth } from '../store/auth'
import { createAdminLeagueApi } from '../api/adminLeague'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const officialModeOptions = [
  { value: 'create', label: 'Создать', description: 'Новая карточка' },
  { value: 'edit', label: 'Редактировать', description: 'Реестр руководства' },
]
const venueModeOptions = [
  { value: 'create', label: 'Создать', description: 'Новая площадка' },
  { value: 'edit', label: 'Редактировать', description: 'Реестр площадок' },
]

const emit = defineEmits(['refresh-seasons'])

const { authorizedApiRequest } = useAuth()
const { confirmAction } = useConfirmDialog()
const leagueApi = createAdminLeagueApi(authorizedApiRequest)

const messageError = ref('')
const messageOk = ref('')

const officialsList = ref([])
const venuesList = ref([])
const activeLeagueSection = ref('officials')

const officialMode = ref('create')
const officialEditId = ref('')
const editingOfficialId = ref(null)
const officialForm = reactive({
  fullName: '',
  positionTitle: '',
  bio: '',
  photoDataUrl: '',
  sortOrder: '100',
})

const venueMode = ref('create')
const venueEditId = ref('')
const editingVenueId = ref(null)
const venueForm = reactive({
  name: '',
  shortLabel: '',
  address: '',
  description: '',
  photoDataUrl: '',
  sortOrder: '100',
})

const leagueModules = computed(() => [
  {
    value: 'officials',
    number: '01',
    label: 'Руководство',
    description: 'Люди и должности',
    count: officialsList.value.length,
  },
  {
    value: 'venues',
    number: '02',
    label: 'Площадки',
    description: 'Адреса и места игр',
    count: venuesList.value.length,
  },
  {
    value: 'documents',
    number: '03',
    label: 'Документы',
    description: 'Сезоны, чемпионаты и кубки',
  },
  {
    value: 'honors',
    number: '04',
    label: 'Зал славы',
    description: 'Призёры, награды и сборные',
  },
])

onMounted(async () => {
  await Promise.all([loadOfficials(), loadVenues()])
})

function resetMessages() {
  messageError.value = ''
  messageOk.value = ''
}

function buildOfficialPayload() {
  return {
    fullName: officialForm.fullName,
    positionTitle: officialForm.positionTitle,
    bio: officialForm.bio || null,
    photoDataUrl: officialForm.photoDataUrl || null,
    sortOrder: Number(officialForm.sortOrder || 100),
  }
}

function buildVenuePayload() {
  return {
    name: venueForm.name,
    shortLabel: venueForm.shortLabel || null,
    address: venueForm.address,
    description: venueForm.description || null,
    photoDataUrl: venueForm.photoDataUrl || null,
    sortOrder: Number(venueForm.sortOrder || 100),
  }
}

async function loadOfficials() {
  const payload = await leagueApi.officials.list()
  officialsList.value = Array.isArray(payload) ? payload : []
}

async function loadVenues() {
  const payload = await leagueApi.venues.list()
  venuesList.value = Array.isArray(payload) ? payload : []
}

async function createOfficial() {
  resetMessages()
  try {
    await leagueApi.officials.create(buildOfficialPayload())
    await loadOfficials()
    resetOfficialForm()
    messageOk.value = 'Карточка руководства создана.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать карточку руководства.'
  }
}

async function saveOfficial() {
  resetMessages()
  try {
    await leagueApi.officials.update(editingOfficialId.value, buildOfficialPayload())
    await loadOfficials()
    switchOfficialMode('create')
    messageOk.value = 'Карточка руководства обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить карточку руководства.'
  }
}

async function deactivateOfficial(officialId) {
  const official = officialsList.value.find((item) => String(item.id) === String(officialId))
  const accepted = await confirmAction({
    title: 'Удалить карточку руководства?',
    message: `${official?.fullName || 'Карточка'} больше не будет отображаться на странице лиги.`,
    confirmLabel: 'Удалить карточку',
  })
  if (!accepted) return

  resetMessages()
  try {
    await leagueApi.officials.deactivate(officialId)
    await loadOfficials()
    switchOfficialMode('create')
    messageOk.value = 'Карточка руководства удалена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить карточку руководства.'
  }
}

async function createVenue() {
  resetMessages()
  try {
    await leagueApi.venues.create(buildVenuePayload())
    await loadVenues()
    resetVenueForm()
    messageOk.value = 'Площадка создана.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать площадку.'
  }
}

async function saveVenue() {
  resetMessages()
  try {
    await leagueApi.venues.update(editingVenueId.value, buildVenuePayload())
    await loadVenues()
    switchVenueMode('create')
    messageOk.value = 'Площадка обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить площадку.'
  }
}

async function deactivateVenue(venueId) {
  const venue = venuesList.value.find((item) => String(item.id) === String(venueId))
  const accepted = await confirmAction({
    title: 'Удалить площадку?',
    message: `${venue?.name || 'Площадка'} больше не будет отображаться на странице лиги.`,
    confirmLabel: 'Удалить площадку',
  })
  if (!accepted) return

  resetMessages()
  try {
    await leagueApi.venues.deactivate(venueId)
    await loadVenues()
    switchVenueMode('create')
    messageOk.value = 'Площадка удалена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить площадку.'
  }
}

function switchOfficialMode(mode) {
  officialMode.value = mode
  officialEditId.value = ''
  editingOfficialId.value = null
  resetOfficialForm()
}

function switchVenueMode(mode) {
  venueMode.value = mode
  venueEditId.value = ''
  editingVenueId.value = null
  resetVenueForm()
}

function onOfficialSelectChange() {
  const item = officialsList.value.find((entry) => String(entry.id) === officialEditId.value)
  if (!item) return
  editingOfficialId.value = item.id
  officialForm.fullName = item.fullName || ''
  officialForm.positionTitle = item.positionTitle || ''
  officialForm.bio = item.bio || ''
  officialForm.photoDataUrl = item.photoDataUrl || ''
  officialForm.sortOrder = String(item.sortOrder || 100)
}

function onVenueSelectChange() {
  const item = venuesList.value.find((entry) => String(entry.id) === venueEditId.value)
  if (!item) return
  editingVenueId.value = item.id
  venueForm.name = item.name || ''
  venueForm.shortLabel = item.shortLabel || ''
  venueForm.address = item.address || ''
  venueForm.description = item.description || ''
  venueForm.photoDataUrl = item.photoDataUrl || ''
  venueForm.sortOrder = String(item.sortOrder || 100)
}

function onOfficialPhotoSelected(event) {
  readFileAsDataUrl(event, (dataUrl) => {
    officialForm.photoDataUrl = dataUrl
  })
}

function onVenuePhotoSelected(event) {
  readFileAsDataUrl(event, (dataUrl) => {
    venueForm.photoDataUrl = dataUrl
  })
}

function readFileAsDataUrl(event, assign) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => assign(String(reader.result || ''))
  reader.readAsDataURL(file)
}

function resetOfficialForm() {
  officialForm.fullName = ''
  officialForm.positionTitle = ''
  officialForm.bio = ''
  officialForm.photoDataUrl = ''
  officialForm.sortOrder = '100'
}

function resetVenueForm() {
  venueForm.name = ''
  venueForm.shortLabel = ''
  venueForm.address = ''
  venueForm.description = ''
  venueForm.photoDataUrl = ''
  venueForm.sortOrder = '100'
}

</script>

<style scoped>
.admin-league-panel {
  display: grid;
  gap: 18px;
}

.admin-league-modules {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(124, 163, 255, .17);
  border-radius: 14px;
  background: rgba(6, 11, 28, .52);
}

.admin-league-module {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--muted);
  text-align: left;
  cursor: pointer;
  transition: border-color .18s ease, background .18s ease, color .18s ease;
}

.admin-league-module:hover {
  border-color: rgba(124, 163, 255, .18);
  background: rgba(124, 163, 255, .055);
  color: var(--text);
}

.admin-league-module.is-active {
  border-color: rgba(97, 232, 162, .4);
  background: linear-gradient(135deg, rgba(97, 232, 162, .12), rgba(97, 232, 162, .045));
  color: var(--text);
  box-shadow: inset 3px 0 0 var(--brand);
}

.admin-league-module-number {
  color: var(--brand);
  font-size: .68rem;
  font-weight: 800;
}

.admin-league-module-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.admin-league-module-copy strong,
.admin-league-module-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-league-module-copy strong { font-size: .82rem; }
.admin-league-module-copy small { color: var(--muted); font-size: .68rem; }

.admin-league-module-count {
  display: grid;
  place-items: center;
  min-width: 27px;
  height: 25px;
  padding: 0 7px;
  border-radius: 7px;
  background: rgba(124, 163, 255, .1);
  color: #cbd8ff;
  font-size: .68rem;
  font-weight: 800;
}

.admin-league-module.is-active .admin-league-module-count {
  background: rgba(97, 232, 162, .13);
  color: var(--brand);
}

.admin-league-grid {
  grid-template-columns: 1fr;
  align-items: start;
}

.admin-league-section {
  display: grid;
  gap: 18px;
  padding: 20px;
}

.admin-league-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 11px;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(124, 163, 255, .14);
}

.admin-league-head .eyebrow,
.admin-league-head .section-title,
.admin-league-head .muted-text {
  margin: 0;
}

.admin-league-head .section-title { margin-top: 2px; font-size: .96rem; }
.admin-league-head .muted-text { margin-top: 5px; font-size: .76rem; }

.admin-league-entity-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 17px;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 12px;
  background: rgba(5, 10, 26, .28);
}

.admin-league-entity-form > label {
  display: grid;
  align-content: start;
  gap: 7px;
}

.admin-league-wide-field,
.admin-league-entity-form > img,
.admin-league-form-actions,
.admin-league-danger-zone {
  grid-column: 1 / -1;
}

.admin-league-file-field {
  padding: 13px;
  border: 1px dashed rgba(124, 163, 255, .22);
  border-radius: 10px;
  background: rgba(124, 163, 255, .035);
}

.admin-league-file-field input[type='file'] {
  width: 100%;
}

.admin-league-edit-flow {
  gap: 14px;
}

.admin-league-record-picker {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 12px;
  background: rgba(5, 10, 26, .28);
}

.admin-league-form-actions {
  margin-top: 2px;
}

.admin-league-danger-zone {
  margin-top: 2px;
}

.admin-league-docs {
  padding: 20px;
}

.admin-league-doc-form {
  grid-template-columns: minmax(260px, .8fr) minmax(0, 1.2fr);
  gap: 14px;
  padding: 17px;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 12px;
  background: rgba(5, 10, 26, .28);
}

.admin-league-doc-form > label {
  display: grid;
  align-content: start;
  gap: 8px;
}

.admin-league-doc-form > .admin-league-file-field,
.admin-league-doc-form > .muted-text,
.admin-league-doc-actions,
.admin-league-doc-form > .admin-league-danger-zone {
  grid-column: 1 / -1;
}

.admin-league-doc-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-self: end;
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, .14);
  border-radius: 10px;
  background: rgba(124, 163, 255, .035);
}

.admin-league-doc-meta p {
  margin: 0;
  padding: 12px;
  border-right: 1px solid rgba(124, 163, 255, .12);
  font-size: .7rem;
}

.admin-league-doc-meta p:last-child { border-right: 0; }

.admin-league-doc-actions {
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .admin-league-modules { grid-template-columns: 1fr; }
  .admin-league-entity-form,
  .admin-league-doc-form { grid-template-columns: 1fr; }
  .admin-league-wide-field,
  .admin-league-entity-form > img,
  .admin-league-form-actions,
  .admin-league-danger-zone,
  .admin-league-doc-form > .admin-league-file-field,
  .admin-league-doc-form > .muted-text,
  .admin-league-doc-actions,
  .admin-league-doc-form > .admin-league-danger-zone { grid-column: 1; }
  .admin-league-doc-meta { grid-template-columns: 1fr; }
  .admin-league-doc-meta p { border-right: 0; border-bottom: 1px solid rgba(124, 163, 255, .12); }
  .admin-league-doc-meta p:last-child { border-bottom: 0; }
}

@media (max-width: 560px) {
  .admin-league-section { padding: 15px; }
  .admin-league-head { grid-template-columns: auto minmax(0, 1fr); }
  .admin-league-head .admin-step-count { display: none; }
  .admin-league-entity-form,
  .admin-league-doc-form { padding: 13px; }
  .admin-league-form-actions,
  .admin-league-doc-actions { align-items: stretch; flex-direction: column-reverse; }
  .admin-league-form-actions > *,
  .admin-league-doc-actions > * { width: 100%; }
}
</style>
