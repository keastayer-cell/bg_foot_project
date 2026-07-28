<template>
  <section class="section-wrap api-index-page">
    <header class="api-index-head">
      <div>
        <p class="api-index-kicker">Инструменты администратора</p>
        <h2 class="section-title">API Explorer</h2>
        <p class="muted api-index-summary">
          {{ visibleEndpointCount }} из {{ endpointCount }} endpoint’ов
        </p>
      </div>
      <code class="api-base-url">{{ apiBaseUrl }}</code>
    </header>

    <div class="api-index-controls" aria-label="Фильтры API">
      <label class="api-search-field">
        <span>Поиск</span>
        <input
          v-model.trim="searchQuery"
          type="search"
          placeholder="Название, путь или описание"
        />
      </label>

      <label>
        <span>Метод</span>
        <select v-model="methodFilter">
          <option value="">Все методы</option>
          <option v-for="method in availableMethods" :key="method" :value="method">
            {{ method }}
          </option>
        </select>
      </label>

      <label>
        <span>Доступ</span>
        <select v-model="accessFilter">
          <option value="">Все endpoint’ы</option>
          <option value="public">Публичные</option>
          <option value="protected">С авторизацией</option>
        </select>
      </label>

      <button
        class="btn-ghost api-reset-filters"
        type="button"
        :disabled="!hasActiveFilters"
        @click="resetFilters"
      >
        Сбросить
      </button>
    </div>

    <nav class="api-group-nav" aria-label="Разделы API">
      <button
        v-for="group in filteredGroups"
        :key="group.key"
        class="api-group-nav-item"
        type="button"
        @click="scrollToGroup(group.key)"
      >
        {{ group.title }}
        <span>{{ group.endpoints.length }}</span>
      </button>
    </nav>

    <div v-if="filteredGroups.length" class="api-index-groups">
      <section
        v-for="group in filteredGroups"
        :id="`api-group-${group.key}`"
        :key="group.key"
        class="api-index-group"
      >
        <div class="api-index-group-head">
          <h3>{{ group.title }}</h3>
          <span>{{ group.endpoints.length }}</span>
        </div>

        <div class="api-endpoint-list">
          <article
            v-for="endpoint in group.endpoints"
            :key="endpoint.key"
            class="api-endpoint-row"
          >
            <div class="api-endpoint-route">
              <span class="api-method-chip" :data-method="endpoint.method">
                {{ endpoint.method }}
              </span>
              <code>{{ endpoint.path }}</code>
            </div>

            <div class="api-endpoint-content">
              <h4>{{ endpoint.title }}</h4>
              <p>{{ endpoint.description }}</p>
              <div class="api-endpoint-meta">
                <span :class="endpoint.auth ? 'api-auth-required' : 'api-auth-public'">
                  {{ endpoint.access }}
                </span>
                <span v-if="endpointParamList(endpoint)">
                  Параметры: {{ endpointParamList(endpoint) }}
                </span>
              </div>
            </div>

            <router-link
              class="btn-ghost api-test-link"
              :to="`/api-explorer/test/${endpoint.key}`"
            >
              Тест
            </router-link>
          </article>
        </div>
      </section>
    </div>

    <div v-else class="api-empty-state">
      <h3>Ничего не найдено</h3>
      <p class="muted">Измените фильтры или поисковый запрос.</p>
      <button class="btn-ghost" type="button" @click="resetFilters">Сбросить фильтры</button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { endpointGroups } from '../data/apiExplorerCatalog'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const searchQuery = ref('')
const methodFilter = ref('')
const accessFilter = ref('')

const endpointCount = endpointGroups.reduce((total, group) => total + group.endpoints.length, 0)
const availableMethods = [...new Set(
  endpointGroups.flatMap((group) => group.endpoints.map((endpoint) => endpoint.method)),
)].sort()

const hasActiveFilters = computed(() => Boolean(
  searchQuery.value || methodFilter.value || accessFilter.value,
))

const filteredGroups = computed(() => {
  const query = searchQuery.value.toLocaleLowerCase('ru-RU')

  return endpointGroups
    .map((group) => ({
      ...group,
      endpoints: group.endpoints.filter((endpoint) => {
        if (methodFilter.value && endpoint.method !== methodFilter.value) return false
        if (accessFilter.value === 'public' && endpoint.auth) return false
        if (accessFilter.value === 'protected' && !endpoint.auth) return false

        if (!query) return true
        return [
          endpoint.title,
          endpoint.path,
          endpoint.description,
          endpoint.access,
          group.title,
        ].some((value) => String(value || '').toLocaleLowerCase('ru-RU').includes(query))
      }),
    }))
    .filter((group) => group.endpoints.length)
})

const visibleEndpointCount = computed(() => (
  filteredGroups.value.reduce((total, group) => total + group.endpoints.length, 0)
))

function endpointParamList(endpoint) {
  const parts = [
    ...(endpoint.pathParams || []).map((param) => param.name),
    ...(endpoint.queryParams || []).map((param) => param.name),
  ]
  if (endpoint.bodyExample !== undefined) parts.push('body')
  return parts.join(', ')
}

function resetFilters() {
  searchQuery.value = ''
  methodFilter.value = ''
  accessFilter.value = ''
}

function scrollToGroup(groupKey) {
  document.getElementById(`api-group-${groupKey}`)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}
</script>

<style scoped>
.api-index-page {
  display: grid;
  gap: 18px;
}

.api-index-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.api-index-kicker {
  margin: 0 0 4px;
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.api-index-summary {
  margin: 6px 0 0;
}

.api-base-url {
  max-width: 100%;
  padding: 9px 11px;
  overflow-wrap: anywhere;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
}

.api-index-controls {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(130px, 180px) minmax(160px, 220px) auto;
  gap: 12px;
  align-items: end;
}

.api-index-controls label {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.api-index-controls label > span {
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.api-index-controls input,
.api-index-controls select {
  width: 100%;
}

.api-reset-filters {
  min-height: 42px;
}

.api-group-nav {
  display: flex;
  gap: 8px;
  padding-bottom: 4px;
  overflow-x: auto;
}

.api-group-nav-item {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 7px 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
}

.api-group-nav-item span {
  color: var(--muted);
  font-size: 0.78rem;
}

.api-index-groups {
  display: grid;
  gap: 20px;
}

.api-index-group {
  scroll-margin-top: 16px;
  border-top: 1px solid var(--line);
}

.api-index-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 2px 10px;
}

.api-index-group-head h3 {
  margin: 0;
  font-size: 1.08rem;
}

.api-index-group-head span {
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.api-endpoint-list {
  display: grid;
  border: 1px solid var(--line);
  border-radius: 7px;
  overflow: hidden;
}

.api-endpoint-row {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(320px, 1.4fr) auto;
  gap: 18px;
  align-items: start;
  padding: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.api-endpoint-row + .api-endpoint-row {
  border-top: 1px solid var(--line);
}

.api-endpoint-route {
  display: grid;
  grid-template-columns: 66px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.api-endpoint-route code {
  padding-top: 5px;
  overflow-wrap: anywhere;
  color: var(--text);
  font-size: 0.88rem;
}

.api-method-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 66px;
  min-height: 28px;
  padding: 0 7px;
  border: 1px solid rgba(124, 163, 255, 0.28);
  border-radius: 5px;
  background: rgba(124, 163, 255, 0.12);
  font-size: 0.75rem;
  font-weight: 800;
}

.api-method-chip[data-method='POST'] {
  border-color: rgba(97, 232, 162, 0.34);
  background: rgba(97, 232, 162, 0.1);
}

.api-method-chip[data-method='PUT'],
.api-method-chip[data-method='PATCH'] {
  border-color: rgba(245, 190, 80, 0.38);
  background: rgba(245, 190, 80, 0.1);
}

.api-method-chip[data-method='DELETE'] {
  border-color: rgba(226, 88, 112, 0.42);
  background: rgba(226, 88, 112, 0.11);
}

.api-endpoint-content {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.api-endpoint-content h4,
.api-endpoint-content p {
  margin: 0;
}

.api-endpoint-content h4 {
  font-size: 0.98rem;
}

.api-endpoint-content p {
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.api-endpoint-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 14px;
  font-size: 0.78rem;
}

.api-auth-required {
  color: #f2c56c;
}

.api-auth-public {
  color: #72d9a5;
}

.api-test-link {
  min-width: 72px;
  text-align: center;
  text-decoration: none;
}

.api-empty-state {
  display: grid;
  justify-items: start;
  gap: 8px;
  padding: 32px 0;
}

.api-empty-state h3,
.api-empty-state p {
  margin: 0;
}

@media (max-width: 980px) {
  .api-index-controls {
    grid-template-columns: minmax(220px, 1fr) repeat(2, minmax(140px, 0.5fr));
  }

  .api-reset-filters {
    justify-self: start;
  }

  .api-endpoint-row {
    grid-template-columns: minmax(230px, 0.8fr) minmax(0, 1.2fr);
  }

  .api-test-link {
    grid-column: 2;
    justify-self: start;
  }
}

@media (max-width: 700px) {
  .api-index-head,
  .api-index-controls {
    grid-template-columns: 1fr;
  }

  .api-index-head {
    display: grid;
    align-items: stretch;
  }

  .api-base-url {
    justify-self: stretch;
  }

  .api-reset-filters {
    width: 100%;
  }

  .api-endpoint-list {
    border-right: 0;
    border-left: 0;
    border-radius: 0;
  }

  .api-endpoint-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 14px 2px;
  }

  .api-test-link {
    grid-column: 1;
    width: 100%;
  }
}
</style>
