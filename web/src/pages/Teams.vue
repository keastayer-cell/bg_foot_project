<template>
  <section class="section-wrap teams-page">
    <h2 class="section-title">Команды</h2>

    <div class="card" v-if="loading">
      <p class="muted">Загружаем список команд...</p>
    </div>

    <div class="card" v-else-if="errorText">
      <p class="error-text">{{ errorText }}</p>
      <div class="actions-row">
        <button class="btn-primary" type="button" @click="loadTeams">Повторить</button>
      </div>
    </div>

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
        <article class="teams-row" v-for="team in filteredTeams" :key="team.id">
          <div class="teams-name">{{ team.name }}</div>
          <div v-if="team.city" class="teams-city muted">{{ team.city }}</div>
        </article>
      </div>
    </div>

    <p class="empty-text" v-if="!loading && !errorText && filteredTeams.length === 0 && search.trim()">
      Ничего не найдено. Уточните название команды или нажмите «Сбросить поиск».
    </p>
    <p class="empty-text" v-else-if="!loading && !errorText && teams.length === 0">
      Пока нет данных по командам.
    </p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '../store/auth'

const { optionalAuthApiRequest } = useAuth()

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
    const payload = await optionalAuthApiRequest('/api/teams?active_flag=1')
    const content = Array.isArray(payload) ? payload : []
    teams.value = content
      .map((item) => ({
        id: item.id,
        name: item.name || 'Без названия',
        city: item.city || '',
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
  gap: 8px;
  align-content: start;
}

.teams-row {
  border-bottom: 1px solid var(--line);
  padding-bottom: 8px;
}

.teams-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.teams-name {
  color: var(--text);
}

.teams-city {
  margin-top: 4px;
}

@media (max-width: 640px) {
  .team-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .team-filters > * {
    width: 100%;
  }

  .teams-meta {
    flex-direction: column;
  }
}
</style>
