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
          <p class="eyebrow">Сезоны и соревнования</p>
          <h2 class="section-title">Официальные документы</h2>
        </div>
      </div>

      <CompetitionContextPicker
        v-if="documentSeasons.length"
        class="regulation-filter"
        v-model:season-id="documentSeasonId"
        v-model:competition-id="documentCompetitionId"
        :seasons="documentSeasons"
        :competitions="documentCompetitions"
      />
      <UiState v-if="!regulations.length" title="Документы пока не опубликованы" />
      <div v-if="regulations.length" class="regulation-groups">
        <section v-for="season in visibleDocumentSeasons" :key="season.id" class="regulation-group">
          <header><h3>{{ season.name }}</h3><span>{{ seasonStatusLabel(season.status) }}</span></header>
          <article v-for="item in season.documents" :key="`${item.targetType}:${item.targetId}`" class="regulation-document">
            <span class="regulation-pdf-mark">PDF</span>
            <div><small>{{ item.targetType === 'SEASON' ? 'Весь сезон' : item.competitionType === 'CUP' ? 'Кубок' : 'Чемпионат' }}</small><h4>{{ item.competitionName || 'Общий регламент сезона' }}</h4><p>Обновлён {{ formatDateTime(item.updatedAt) }}</p></div>
            <a class="btn-ghost" :href="apiBaseUrl + item.downloadUrl" target="_blank" rel="noopener" :aria-label="`Скачать PDF: ${item.competitionName || 'Общий регламент сезона'} · ${season.name}`">Скачать PDF</a>
          </article>
        </section>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiState from '../components/UiState.vue'
import CompetitionContextPicker from '../components/CompetitionContextPicker.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'
import { useCompetitionContext } from '../store/competitionContext'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)
const route = useRoute()
const router = useRouter()
const {
  seasonId: documentSeasonId,
  competitionId: documentCompetitionId,
  selectAvailable,
  syncUrl,
} = useCompetitionContext(route, router)
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const sectionLinks = [
  { id: 'leadership', label: 'Руководство' },
  { id: 'venues', label: 'Площадки' },
  { id: 'documents', label: 'Регламенты' },
]

const loading = ref(false)
const pageError = ref('')
const activeSection = ref('leadership')
const officials = ref([])
const venues = ref([])
const regulations = ref([])
const documentSeasons = computed(() => {
  const groups = new Map()
  for (const item of regulations.value) {
    if (!groups.has(item.seasonId)) groups.set(item.seasonId, { id: item.seasonId, name: item.seasonName, status: item.seasonStatus, documents: [] })
    groups.get(item.seasonId).documents.push(item)
  }
  return [...groups.values()]
})
const documentCompetitions = computed(() => {
  const values = regulations.value
    .filter((item) => String(item.seasonId) === documentSeasonId.value && item.targetType === 'COMPETITION')
    .map((item) => ({
      id: item.targetId,
      seasonId: item.seasonId,
      name: item.competitionName,
      type: item.competitionType,
    }))
  return [...new Map(values.map((item) => [item.id, item])).values()]
})
const visibleDocumentSeasons = computed(() => documentSeasons.value
  .filter((season) => String(season.id) === documentSeasonId.value)
  .map((season) => ({
    ...season,
    documents: season.documents.filter((item) => (
      item.targetType === 'SEASON' || String(item.targetId) === documentCompetitionId.value
    )),
  })))

async function loadLeagueData() {
  loading.value = true
  pageError.value = ''
  try {
    const [payload, documents] = await Promise.all([
      catalogApi.getLeagueOverview(),
      catalogApi.getLeagueRegulations(),
    ])
    officials.value = Array.isArray(payload?.officials) ? payload.officials : []
    venues.value = Array.isArray(payload?.venues) ? payload.venues : []
    regulations.value = Array.isArray(documents) ? documents : []
    const allCompetitions = regulations.value
      .filter((item) => item.targetType === 'COMPETITION')
      .map((item) => ({
        id: item.targetId,
        seasonId: item.seasonId,
        name: item.competitionName,
        type: item.competitionType,
      }))
    selectAvailable(documentSeasons.value, allCompetitions)
  } catch (error) {
    pageError.value = error.message || 'Не удалось загрузить данные о лиге.'
  } finally {
    loading.value = false
  }
}

onMounted(loadLeagueData)
watch(documentSeasonId, () => {
  if (!documentCompetitions.value.some((item) => String(item.id) === documentCompetitionId.value)) {
    documentCompetitionId.value = documentCompetitions.value[0] ? String(documentCompetitions.value[0].id) : ''
  }
  syncUrl()
})
watch(documentCompetitionId, syncUrl)

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

</script>

<style scoped>
.regulation-filter { display: grid; gap: 6px; max-width: 360px; margin-bottom: 18px; font-size: .7rem; color: var(--muted); }
.regulation-filter select { min-width: 0; width: 100%; }
.regulation-groups { display: grid; gap: 20px; }
.regulation-group { border: 1px solid rgba(124, 163, 255, .16); border-radius: 10px; overflow: hidden; }
.regulation-group > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; background: rgba(124, 163, 255, .05); }
.regulation-group h3 { margin: 0; font-size: .85rem; }
.regulation-group > header > span { color: var(--muted); font-size: .64rem; }
.regulation-document { display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 14px; padding: 16px; border-top: 1px solid rgba(124, 163, 255, .12); }
.regulation-pdf-mark { display: grid; place-items: center; height: 44px; border: 1px solid rgba(97, 232, 162, .2); border-radius: 7px; color: var(--brand); font-size: .6rem; }
.regulation-document h4 { margin: 5px 0; font-size: .8rem; overflow-wrap: anywhere; }
.regulation-document small, .regulation-document p { color: var(--muted); font-size: .63rem; margin: 0; }
.regulation-document .btn-ghost { font-size: .7rem; min-height: 40px; }
@media(max-width: 640px) { .regulation-document { grid-template-columns: 32px minmax(0, 1fr); gap: 10px; padding: 12px; } .regulation-document .btn-ghost { grid-column: 2; justify-self: start; min-height: 44px; } }

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
