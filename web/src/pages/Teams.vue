<template>
  <section class="section-wrap teams-page">
    <h2 class="section-title">Команды</h2>

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

    <div class="card team-filters" v-else>
      <input
        v-model.trim="search"
        type="text"
        placeholder="Поиск по названию команды"
        aria-label="Поиск по названию команды"
      />
      <button class="btn-ghost" type="button" :disabled="!search.trim()" @click="resetSearch">
        Сбросить поиск
      </button>
    </div>

    <div class="teams-meta" v-if="!loading && !errorText && teams.length">
      <span class="muted">Всего команд: {{ teams.length }}</span>
      <span class="muted" v-if="search.trim()">Найдено: {{ filteredTeams.length }}</span>
    </div>

    <div class="card" v-if="!loading && !errorText && filteredTeams.length">
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
.teams-page {
  display: grid;
  gap: 18px;
}

.team-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.team-filters input {
  min-width: min(420px, 100%);
  flex: 1 1 320px;
}

.teams-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.teams-list {
  display: grid;
  gap: 12px;
  align-content: start;
}

.teams-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(16, 24, 52, 0.92), rgba(11, 17, 38, 0.98));
  color: inherit;
  text-decoration: none;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.teams-row:hover {
  transform: translateY(-1px);
  border-color: rgba(97, 232, 162, 0.34);
  box-shadow: 0 16px 28px rgba(3, 8, 24, 0.22);
}

.teams-row-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  width: 100%;
}

.teams-logo-shell {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(124, 163, 255, 0.2);
  background: linear-gradient(180deg, rgba(23, 34, 71, 0.98), rgba(13, 21, 48, 1));
  display: grid;
  place-items: center;
  color: rgba(151, 176, 255, 0.9);
  font-weight: 800;
  letter-spacing: 0.08em;
  flex: 0 0 auto;
}

.teams-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.teams-name {
  color: var(--text);
  font-weight: 700;
  font-size: 1.05rem;
  line-height: 1.2;
}

@media (max-width: 640px) {
  .team-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .team-filters input {
    min-width: 0;
    flex: 0 0 auto;
  }

  .team-filters > * {
    width: 100%;
  }

  .teams-meta {
    flex-direction: column;
  }

  .teams-row {
    padding: 10px 12px;
  }
}
</style>
