<template>
  <article class="card admin-panel admin-league-panel">
    <div class="admin-panel-head">
      <h3 class="section-title">Лига</h3>
      <p class="muted-text">Руководство лиги, места проведения и PDF положения по каждому сезону.</p>
    </div>

    <div class="card" v-if="messageError || messageOk">
      <p class="error-text" v-if="messageError">{{ messageError }}</p>
      <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
    </div>

    <div class="admin-grid admin-league-grid">
      <section class="card admin-league-section">
        <div class="section-head admin-league-head">
          <div>
            <p class="eyebrow">Руководство</p>
            <h4 class="section-title">Карточки руководства</h4>
          </div>
        </div>

        <div class="admin-subnav">
          <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': officialMode === 'create' }" type="button" @click="switchOfficialMode('create')">Создать</button>
          <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': officialMode === 'edit' }" type="button" @click="switchOfficialMode('edit')">Редактировать</button>
        </div>

        <form v-if="officialMode === 'create'" class="admin-form" @submit.prevent="createOfficial">
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
          <label>
            Краткое описание
            <textarea v-model.trim="officialForm.bio" rows="4" placeholder="Чем отвечает в лиге"></textarea>
          </label>
          <label>
            Фото
            <input type="file" accept="image/*" @change="onOfficialPhotoSelected" />
          </label>
          <img v-if="officialForm.photoDataUrl" :src="officialForm.photoDataUrl" alt="Превью фото руководителя" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать карточку</button>
          </div>
        </form>

        <div v-else class="admin-form">
          <label>
            Выберите карточку
            <select v-model="officialEditId" @change="onOfficialSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in officialsList" :key="item.id" :value="String(item.id)">{{ item.fullName }} · {{ item.positionTitle }}</option>
            </select>
          </label>
          <template v-if="editingOfficialId">
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
            <label>
              Краткое описание
              <textarea v-model.trim="officialForm.bio" rows="4"></textarea>
            </label>
            <label>
              Фото
              <input type="file" accept="image/*" @change="onOfficialPhotoSelected" />
            </label>
            <img v-if="officialForm.photoDataUrl" :src="officialForm.photoDataUrl" alt="Превью фото руководителя" class="team-rep-player-photo-preview" />
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveOfficial">Сохранить</button>
              <button class="btn-danger" type="button" @click="deactivateOfficial(editingOfficialId)">Удалить</button>
              <button class="btn-ghost" type="button" @click="switchOfficialMode('create')">Отмена</button>
            </div>
          </template>
        </div>
      </section>

      <section class="card admin-league-section">
        <div class="section-head admin-league-head">
          <div>
            <p class="eyebrow">Площадки</p>
            <h4 class="section-title">Места проведения</h4>
          </div>
        </div>

        <div class="admin-subnav">
          <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': venueMode === 'create' }" type="button" @click="switchVenueMode('create')">Создать</button>
          <button class="btn-ghost admin-subnav-btn" :class="{ 'admin-subnav-active': venueMode === 'edit' }" type="button" @click="switchVenueMode('edit')">Редактировать</button>
        </div>

        <form v-if="venueMode === 'create'" class="admin-form" @submit.prevent="createVenue">
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
          <label>
            Описание
            <textarea v-model.trim="venueForm.description" rows="4"></textarea>
          </label>
          <label>
            Фото площадки
            <input type="file" accept="image/*" @change="onVenuePhotoSelected" />
          </label>
          <img v-if="venueForm.photoDataUrl" :src="venueForm.photoDataUrl" alt="Превью площадки" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать площадку</button>
          </div>
        </form>

        <div v-else class="admin-form">
          <label>
            Выберите площадку
            <select v-model="venueEditId" @change="onVenueSelectChange">
              <option value="">— выберите —</option>
              <option v-for="item in venuesList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
            </select>
          </label>
          <template v-if="editingVenueId">
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
            <label>
              Описание
              <textarea v-model.trim="venueForm.description" rows="4"></textarea>
            </label>
            <label>
              Фото площадки
              <input type="file" accept="image/*" @change="onVenuePhotoSelected" />
            </label>
            <img v-if="venueForm.photoDataUrl" :src="venueForm.photoDataUrl" alt="Превью площадки" class="team-rep-player-photo-preview" />
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveVenue">Сохранить</button>
              <button class="btn-danger" type="button" @click="deactivateVenue(editingVenueId)">Удалить</button>
              <button class="btn-ghost" type="button" @click="switchVenueMode('create')">Отмена</button>
            </div>
          </template>
        </div>
      </section>
    </div>

    <section class="card admin-league-section admin-league-docs">
      <div class="section-head admin-league-head">
        <div>
          <p class="eyebrow">Документы</p>
          <h4 class="section-title">Положение сезона в PDF</h4>
        </div>
        <p class="muted-text">Загрузите подписанный PDF в конкретный сезон, после чего его смогут скачать все пользователи.</p>
      </div>

      <div class="admin-form admin-league-doc-form">
        <label>
          Сезон
          <select v-model="regulationSeasonId">
            <option value="">— выберите —</option>
            <option v-for="season in seasonsList" :key="season.id" :value="String(season.id)">{{ season.name }}</option>
          </select>
        </label>

        <div v-if="selectedSeason" class="admin-league-doc-meta">
          <p><strong>Статус:</strong> {{ seasonStatusLabel(selectedSeason.status) }}</p>
          <p><strong>PDF загружен:</strong> {{ selectedSeason.regulationDocumentAvailable ? 'да' : 'нет' }}</p>
          <p><strong>Обновлен:</strong> {{ selectedSeason.regulationUpdatedAt ? formatDateTime(selectedSeason.regulationUpdatedAt) : '—' }}</p>
        </div>

        <label>
          PDF-файл положения
          <input type="file" accept="application/pdf" @change="onRegulationFileSelected" />
        </label>
        <p v-if="regulationFileName" class="muted-text">Выбран файл: {{ regulationFileName }}</p>

        <div class="actions-row">
          <button class="btn-primary" type="button" :disabled="!regulationSeasonId || !regulationDataUrl" @click="saveSeasonRegulation">Сохранить PDF</button>
          <button class="btn-danger" type="button" :disabled="!selectedSeason?.regulationDocumentAvailable" @click="removeSeasonRegulation">Удалить PDF</button>
          <button class="btn-ghost" type="button" :disabled="!selectedSeason?.regulationDownloadUrl" @click="downloadSeasonRegulation">Скачать текущий PDF</button>
        </div>
      </div>
    </section>
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useAuth } from '../store/auth'

const props = defineProps({
  seasonsList: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['refresh-seasons'])

const { authorizedApiRequest } = useAuth()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const messageError = ref('')
const messageOk = ref('')

const officialsList = ref([])
const venuesList = ref([])

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

const regulationSeasonId = ref('')
const regulationDataUrl = ref('')
const regulationFileName = ref('')

const selectedSeason = computed(() => {
  return props.seasonsList.find((season) => String(season.id) === String(regulationSeasonId.value)) || null
})

watch(
  () => props.seasonsList,
  (list) => {
    if (!regulationSeasonId.value && Array.isArray(list) && list.length) {
      regulationSeasonId.value = String(list[0].id)
    }
  },
  { immediate: true }
)

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
  const payload = await authorizedApiRequest('/api/admin/league/officials?active_flag=1', { method: 'GET' })
  officialsList.value = Array.isArray(payload) ? payload : []
}

async function loadVenues() {
  const payload = await authorizedApiRequest('/api/admin/league/venues?active_flag=1', { method: 'GET' })
  venuesList.value = Array.isArray(payload) ? payload : []
}

async function createOfficial() {
  resetMessages()
  try {
    await authorizedApiRequest('/api/admin/league/officials', {
      method: 'POST',
      body: JSON.stringify(buildOfficialPayload()),
    })
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
    await authorizedApiRequest(`/api/admin/league/officials/${editingOfficialId.value}`, {
      method: 'PUT',
      body: JSON.stringify(buildOfficialPayload()),
    })
    await loadOfficials()
    switchOfficialMode('create')
    messageOk.value = 'Карточка руководства обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить карточку руководства.'
  }
}

async function deactivateOfficial(officialId) {
  resetMessages()
  try {
    await authorizedApiRequest(`/api/admin/league/officials/${officialId}`, { method: 'DELETE' })
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
    await authorizedApiRequest('/api/admin/league/venues', {
      method: 'POST',
      body: JSON.stringify(buildVenuePayload()),
    })
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
    await authorizedApiRequest(`/api/admin/league/venues/${editingVenueId.value}`, {
      method: 'PUT',
      body: JSON.stringify(buildVenuePayload()),
    })
    await loadVenues()
    switchVenueMode('create')
    messageOk.value = 'Площадка обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить площадку.'
  }
}

async function deactivateVenue(venueId) {
  resetMessages()
  try {
    await authorizedApiRequest(`/api/admin/league/venues/${venueId}`, { method: 'DELETE' })
    await loadVenues()
    switchVenueMode('create')
    messageOk.value = 'Площадка удалена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить площадку.'
  }
}

async function saveSeasonRegulation() {
  resetMessages()
  if (!regulationSeasonId.value || !regulationDataUrl.value) {
    messageError.value = 'Сначала выберите сезон и PDF-файл.'
    return
  }

  try {
    await authorizedApiRequest(`/api/admin/league/seasons/${regulationSeasonId.value}/regulation`, {
      method: 'PUT',
      body: JSON.stringify({ documentDataUrl: regulationDataUrl.value }),
    })
    regulationDataUrl.value = ''
    regulationFileName.value = ''
    await emit('refresh-seasons')
    messageOk.value = 'PDF положения сезона сохранен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось сохранить PDF положения сезона.'
  }
}

async function removeSeasonRegulation() {
  resetMessages()
  if (!regulationSeasonId.value) {
    messageError.value = 'Сначала выберите сезон.'
    return
  }

  try {
    await authorizedApiRequest(`/api/admin/league/seasons/${regulationSeasonId.value}/regulation`, {
      method: 'DELETE',
    })
    await emit('refresh-seasons')
    messageOk.value = 'PDF положения сезона удален.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить PDF положения сезона.'
  }
}

function downloadSeasonRegulation() {
  if (!selectedSeason.value?.regulationDownloadUrl) return
  window.open(`${apiBaseUrl}${selectedSeason.value.regulationDownloadUrl}`, '_blank', 'noopener')
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

function onRegulationFileSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  regulationFileName.value = file.name
  const reader = new FileReader()
  reader.onload = () => {
    regulationDataUrl.value = String(reader.result || '')
  }
  reader.readAsDataURL(file)
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

function seasonStatusLabel(status) {
  if (status === 'ACTIVE') return 'Активный'
  if (status === 'CLOSED') return 'Закрыт'
  return 'Черновик'
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
</script>

<style scoped>
.admin-league-panel {
  display: grid;
  gap: 18px;
}

.admin-league-grid {
  align-items: start;
}

.admin-league-section {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.admin-league-head {
  align-items: start;
}

.admin-league-docs {
  padding: 18px;
}

.admin-league-doc-form {
  gap: 14px;
}

.admin-league-doc-meta {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
}

.admin-league-doc-meta p {
  margin: 0;
}
</style>