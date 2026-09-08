<template>
  <section class="section-wrap teams-page catalog-page">
    <PublicCatalogHeader title="Команды" description="Участники лиги и составы команд." :count="!loading && !errorText ? teams.length : null" count-label="команд" />

    <div class="card" v-if="loading">
      <p class="muted">Загружаем список команд...</p>
    </div>

    <UiState
      v-else-if="errorText"
      tone="error"
      title="Не удалось загрузить команды"
      :message="errorText"
      action-label="Повторить"
      @action="loadTeams"
    />

    <div class="catalog-toolbar" v-else>
      <label class="catalog-search"><span>Поиск команды</span>
      <input
        v-model.trim="search"
        type="text"
        placeholder="Поиск по названию команды"
        aria-label="Поиск по названию команды"
      />
      </label>
      <button class="btn-ghost" type="button" :disabled="!search.trim()" @click="resetSearch">
        Сбросить поиск
      </button>
    </div>

    <div class="catalog-results-meta" v-if="!loading && !errorText && teams.length">
      <span class="muted">Команды лиги</span>
      <span class="muted" v-if="search.trim()">Найдено: {{ filteredTeams.length }}</span>
    </div>

    <div class="catalog-surface" v-if="!loading && !errorText && filteredTeams.length">
      <div class="teams-list">
        <RouterLink
          v-for="team in filteredTeams"
          :key="team.id"
          class="teams-row"
          :to="teamProfileLocation(team)"
        >
          <div class="teams-row-main">
            <div class="teams-logo-shell" :class="{ 'is-empty': !team.logoDataUrl }">
              <img v-if="team.logoDataUrl" :src="team.logoDataUrl" :alt="`Эмблема ${team.name}`" class="teams-logo" />
              <span v-else>{{ teamInitials(team.name) }}</span>
            </div>
            <div class="teams-name">{{ team.name }}</div>
          </div>
          <span class="catalog-row-link">Состав <span aria-hidden="true">↗</span></span>
        </RouterLink>
      </div>
    </div>

    <UiState
      v-if="!loading && !errorText && filteredTeams.length === 0 && search.trim()"
      title="Команды не найдены"
      message="Уточните название или сбросьте поиск."
      action-label="Сбросить поиск"
      @action="resetSearch"
    />
    <UiState
      v-else-if="!loading && !errorText && teams.length === 0"
      title="Команд пока нет"
      message="Активные команды появятся здесь после добавления администратором."
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import UiState from '../components/UiState.vue'
import PublicCatalogHeader from '../components/PublicCatalogHeader.vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'
import { teamProfileLocation } from '../utils/publicUrls'

const { optionalAuthApiRequest } = useAuth()
const catalogApi = createCatalogApi(optionalAuthApiRequest)

const teams = ref([])
const search = ref('')
const loading = ref(false)
const errorText = ref('')

const filteredTeams = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return teams.value
  return teams.value.filter((team) => team.name.toLowerCase().includes(query))
})

async function loadTeams() {
  loading.value = true
  errorText.value = ''

  try {
    const payload = await catalogApi.getActiveTeams()
    const content = Array.isArray(payload) ? payload : []
    teams.value = content
      .map((item) => ({
        id: item.id,
        name: item.name || 'Без названия',
        logoDataUrl: item.logoDataUrl || '',
      }))
      .sort((left, right) => left.name.localeCompare(right.name, 'ru', { sensitivity: 'base' }))
  } catch (error) {
    errorText.value = error.message || 'Не удалось загрузить команды.'
    teams.value = []
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.value = ''
}

function teamInitials(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'FC'
  return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

onMounted(async () => {
  await loadTeams()
})
</script>

<style scoped>
.teams-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.teams-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-width: 0; min-height: 82px; padding: 16px 20px; border-bottom: 1px solid rgba(124, 163, 255, .12); }
.teams-row:nth-child(odd) { border-right: 1px solid rgba(124, 163, 255, .12); }
.teams-row:hover { background: rgba(97, 232, 162, .045); }
.teams-row-main { display: flex; align-items: center; gap: 14px; min-width: 0; }
.teams-logo-shell { width: 42px; height: 42px; flex: 0 0 auto; display: grid; place-items: center; overflow: hidden; border: 1px solid rgba(124, 163, 255, .18); border-radius: 10px; background: rgba(124, 163, 255, .06); color: var(--brand); font-size: .75rem; font-weight: 800; }
.teams-logo { width: 100%; height: 100%; object-fit: contain; }
.teams-name { font-size: .85rem; font-weight: 700; overflow-wrap: anywhere; }
@media (max-width: 760px) {
  .teams-list { grid-template-columns: 1fr; }
  .teams-row { min-height: 72px; padding: 12px 14px; }
  .teams-row:nth-child(odd) { border-right: 0; }
}
</style>
