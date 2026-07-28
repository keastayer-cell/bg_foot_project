<template>
  <section class="section-wrap league-page">
    <article class="card league-hero">
      <div class="league-hero-copy">
        <h1 class="section-title league-title">О лиге</h1>
      </div>

      <nav class="league-subnav" aria-label="Разделы страницы о лиге">
        <button
          v-for="item in sectionLinks"
          :key="item.id"
          class="league-subnav-link"
          :class="{ 'league-subnav-link-active': activeSection === item.id }"
          type="button"
          @click="activeSection = item.id"
        >{{ item.label }}</button>
      </nav>
    </article>

    <UiState
      v-if="pageError"
      tone="error"
      title="Не удалось загрузить данные лиги"
      :message="pageError"
      action-label="Повторить"
      @action="loadLeagueData"
    />
    <p v-else-if="loading" class="muted-text">Загрузка данных...</p>

    <article v-if="activeSection === 'leadership'" class="card league-section" id="leadership">
      <div class="section-head league-section-head">
        <div>
          <p class="eyebrow">Руководство</p>
          <h2 class="section-title">Ответственные лица</h2>
        </div>
      </div>

      <UiState v-if="!officials.length" title="Руководство пока не опубликовано" />
      <div v-else class="leadership-grid">
        <article v-for="person in officials" :key="person.id" class="leadership-card">
          <div class="leadership-avatar-wrap">
            <img v-if="person.photoDataUrl" :src="person.photoDataUrl" :alt="`Фото: ${person.fullName}`" class="leadership-avatar-photo" />
            <div v-else class="leadership-avatar-fallback">{{ initials(person.fullName) }}</div>
          </div>
          <div class="leadership-copy">
            <p class="leadership-role">{{ person.positionTitle }}</p>
            <h3>{{ person.fullName }}</h3>
            <p>{{ person.bio || 'Описание пока не добавлено.' }}</p>
          </div>
        </article>
      </div>
    </article>

    <article v-if="activeSection === 'venues'" class="card league-section" id="venues">
      <div class="section-head league-section-head">
        <div>
          <p class="eyebrow">Места проведения</p>
          <h2 class="section-title">Площадки турнира</h2>
        </div>
      </div>

      <UiState v-if="!venues.length" title="Площадки пока не опубликованы" />
      <div v-else class="venues-grid">
        <article v-for="venue in venues" :key="venue.id" class="venue-card">
          <div class="venue-visual-wrap">
            <img v-if="venue.photoDataUrl" :src="venue.photoDataUrl" :alt="`Площадка: ${venue.name}`" class="venue-visual-photo" />
            <div v-else class="venue-visual-fallback">{{ venue.shortLabel || shortVenueCode(venue.name) }}</div>
          </div>
          <div class="venue-copy">
            <div class="venue-title-row">
              <h3>{{ venue.name }}</h3>
              <span v-if="venue.shortLabel" class="venue-chip">{{ venue.shortLabel }}</span>
            </div>
            <p class="venue-address">{{ venue.address }}</p>
            <p>{{ venue.description || 'Описание площадки пока не добавлено.' }}</p>
          </div>
        </article>
      </div>
    </article>

    <article v-if="activeSection === 'documents'" class="card league-section" id="season-regulation">
      <div class="section-head league-section-head">
        <div>
          <p class="eyebrow">Положение сезона</p>
          <h2 class="section-title">Официальные документы</h2>
        </div>
      </div>

      <UiState v-if="!seasonDocuments.length" title="Документы пока не опубликованы" />
      <div v-else class="regulation-layout">
        <article v-if="currentSeasonDocument" class="regulation-file-card regulation-file-card-primary">
          <p class="regulation-file-label">Актуальный сезон</p>
          <h3>{{ currentSeasonDocument.seasonName }}</h3>
          <p>Официальный PDF-файл с регламентом и положением сезона.</p>
          <div class="regulation-meta-row">
            <span class="muted-text">Статус: {{ seasonStatusLabel(currentSeasonDocument.seasonStatus) }}</span>
            <span class="muted-text">Обновлен: {{ formatDateTime(currentSeasonDocument.regulationUpdatedAt) }}</span>
          </div>
          <button class="btn-primary" type="button" @click="downloadDocument(currentSeasonDocument)">Скачать PDF</button>
        </article>

        <article class="regulation-file-card">
          <p class="regulation-file-label">Архив сезонов</p>
          <ul class="regulation-archive-list">
            <li v-for="item in archivedSeasonDocuments" :key="item.seasonId">
              <div>
                <strong>{{ item.seasonName }}</strong>
                <p class="muted-text">{{ formatDateTime(item.regulationUpdatedAt) }}</p>
              </div>
              <button class="btn-ghost" type="button" @click="downloadDocument(item)">Скачать</button>
            </li>
          </ul>
        </article>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import UiState from '../components/UiState.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const sectionLinks = [
  { id: 'leadership', label: 'Руководство' },
  { id: 'venues', label: 'Площадки' },
  { id: 'documents', label: 'Регламент' },
]

const loading = ref(false)
const pageError = ref('')
const activeSection = ref(null)
const officials = ref([])
const venues = ref([])
const seasonDocuments = ref([])

const currentSeasonDocument = computed(() => seasonDocuments.value[0] || null)
const archivedSeasonDocuments = computed(() => seasonDocuments.value.slice(1))

async function loadLeagueData() {
  loading.value = true
  pageError.value = ''
  try {
    const payload = await catalogApi.getLeagueOverview()
    officials.value = Array.isArray(payload?.officials) ? payload.officials : []
    venues.value = Array.isArray(payload?.venues) ? payload.venues : []
    seasonDocuments.value = Array.isArray(payload?.seasonDocuments) ? payload.seasonDocuments : []
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить данные о лиге.'
  } finally {
    loading.value = false
  }
}

onMounted(loadLeagueData)

function initials(fullName) {
  return String(fullName || '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0] || '')
    .join('')
    .toUpperCase()
}

function shortVenueCode(name) {
  return String(name || '')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0] || '')
    .join('')
    .toUpperCase()
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

function downloadDocument(item) {
  if (!item?.regulationDownloadUrl) return
  window.open(`${apiBaseUrl}${item.regulationDownloadUrl}`, '_blank', 'noopener')
}
</script>

<style scoped>
.league-page {
  display: grid;
  gap: 14px;
}

.league-hero {
  display: grid;
  gap: 16px;
  padding: 20px 22px;
  background:
    radial-gradient(90% 120% at 0% 0%, rgba(0, 190, 255, 0.14), rgba(0, 190, 255, 0) 26%),
    linear-gradient(180deg, rgba(23, 18, 86, 0.96), rgba(15, 17, 55, 0.98));
}

.league-hero-copy {
  display: grid;
  gap: 10px;
}

.league-title {
  margin: 0;
  font-size: clamp(1.8rem, 2.7vw, 2.6rem);
  line-height: 0.98;
}

.league-subnav {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(136, 170, 214, 0.2);
}

.league-subnav-link {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 14px;
  text-align: center;
  font-size: 0.92rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  border: 0;
  color: inherit;
  cursor: pointer;
  background: linear-gradient(135deg, rgba(97, 47, 212, 0.92), rgba(49, 24, 135, 0.96));
}

.league-subnav-link:nth-child(2) {
  background: linear-gradient(135deg, rgba(54, 32, 157, 0.95), rgba(32, 22, 112, 0.98));
}

.league-subnav-link:nth-child(3) {
  background: linear-gradient(135deg, rgba(35, 38, 135, 0.95), rgba(21, 24, 100, 0.98));
}

.league-subnav-link:nth-child(4) {
  background: linear-gradient(135deg, rgba(221, 17, 111, 0.94), rgba(140, 17, 87, 0.98));
}

.league-subnav-link-active {
  box-shadow: inset 0 -4px 0 rgba(255, 255, 255, 0.32);
  filter: brightness(1.08);
}

.league-section {
  display: grid;
  gap: 14px;
}

.league-section-head {
  align-items: start;
}

.leadership-grid,
.venues-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.leadership-card,
.venue-card,
.regulation-file-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.leadership-copy h3,
.venue-copy h3,
.regulation-file-card h3 {
  margin: 0;
  font-size: 1.08rem;
}

.leadership-copy p,
.venue-copy p,
.regulation-file-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
}

.leadership-card,
.venue-card {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  overflow: hidden;
}

.leadership-avatar-wrap,
.venue-visual-wrap {
  min-height: 100%;
  background: linear-gradient(180deg, rgba(32, 48, 104, 0.95), rgba(16, 23, 56, 0.98));
}

.leadership-avatar-photo,
.venue-visual-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.leadership-avatar-fallback,
.venue-visual-fallback {
  width: 100%;
  height: 100%;
  min-height: 140px;
  display: grid;
  place-items: center;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #ffffff;
}

.leadership-copy,
.venue-copy {
  padding: 16px;
  display: grid;
  gap: 8px;
}

.leadership-role,
.venue-address {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.leadership-role {
  color: rgba(151, 176, 255, 0.84);
}

.venue-address {
  color: rgba(97, 232, 162, 0.78);
}

.venue-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.venue-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.12);
  color: var(--brand);
  font-size: 0.78rem;
  font-weight: 700;
}

.regulation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 14px;
}

.regulation-file-card {
  padding: 18px;
  display: grid;
  gap: 12px;
}

.regulation-file-card-primary {
  background: linear-gradient(180deg, rgba(20, 104, 181, 0.22), rgba(72, 39, 171, 0.2));
}

.regulation-file-label {
  margin: 0;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(151, 176, 255, 0.84);
}

.regulation-meta-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.regulation-archive-list {
  list-style: none;
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
}

.regulation-archive-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.regulation-archive-list li:first-child {
  border-top: 0;
  padding-top: 0;
}

@media (max-width: 1040px) {
  .leadership-grid,
  .venues-grid,
  .regulation-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .league-page {
    gap: 10px;
  }

  .league-hero {
    gap: 12px;
    padding: 16px;
  }

  .league-title {
    font-size: 1.45rem;
    line-height: 1.02;
  }

  .league-subnav {
    grid-template-columns: 1fr;
    border-radius: 16px;
  }

  .league-subnav-link {
    min-height: 50px;
    justify-content: flex-start;
    padding: 12px 16px;
    font-size: 0.82rem;
    letter-spacing: 0.05em;
  }

  .league-subnav-link-active {
    box-shadow: inset 4px 0 0 rgba(255, 255, 255, 0.32);
  }

  .league-section {
    gap: 12px;
  }

  .league-section-head {
    display: grid;
    gap: 6px;
  }

  .league-section-head .section-title {
    font-size: 1.25rem;
  }

  .leadership-card,
  .venue-card {
    grid-template-columns: 1fr;
    border-radius: 16px;
  }

  .leadership-avatar-fallback,
  .venue-visual-fallback,
  .leadership-avatar-photo,
  .venue-visual-photo {
    min-height: 180px;
    max-height: 180px;
  }

  .leadership-copy,
  .venue-copy,
  .regulation-file-card {
    padding: 14px;
  }

  .leadership-copy h3,
  .venue-copy h3,
  .regulation-file-card h3 {
    font-size: 1rem;
  }

  .venue-title-row {
    display: grid;
    gap: 8px;
  }

  .venue-chip {
    justify-self: start;
  }

  .regulation-archive-list li {
    align-items: stretch;
    flex-direction: column;
  }

  .regulation-meta-row {
    display: grid;
    gap: 6px;
  }

  .regulation-file-card .btn-primary,
  .regulation-file-card .btn-ghost,
  .regulation-archive-list .btn-ghost {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 420px) {
  .league-hero {
    padding: 14px;
  }

  .league-title {
    font-size: 1.3rem;
  }

  .leadership-avatar-fallback,
  .venue-visual-fallback,
  .leadership-avatar-photo,
  .venue-visual-photo {
    min-height: 160px;
    max-height: 160px;
  }

  .leadership-role,
  .venue-address,
  .regulation-file-label {
    font-size: 0.72rem;
  }
}
</style>
