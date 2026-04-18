<template>
  <section class="section-wrap api-index-page">
    <div class="toolbar api-index-head">
      <h2 class="section-title">API индекс сервиса</h2>
      <span class="status-chip status-played">{{ apiBaseUrl }}</span>
    </div>

    <article class="card api-index-card" v-for="group in endpointGroups" :key="group.key">
      <h3 class="api-index-group-title">{{ group.title }} ({{ group.endpoints.length }})</h3>

      <div class="api-index-table-wrap">
        <table class="api-index-table">
          <thead>
            <tr>
              <th>Метод</th>
              <th>Путь</th>
              <th>Параметры</th>
              <th>Описание</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="endpoint in group.endpoints" :key="endpoint.key">
              <td>
                <span class="api-method-chip" :data-method="endpoint.method">{{ endpoint.method }}</span>
              </td>
              <td class="api-index-path">{{ endpoint.path }}</td>
              <td>{{ endpointParamList(endpoint) }}</td>
              <td>{{ endpoint.description }}</td>
              <td>
                <router-link class="api-test-link" :to="`/api-explorer/test/${endpoint.key}`">Тест</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>

<script setup>
import { endpointGroups } from '../data/apiExplorerCatalog'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

function endpointParamList(endpoint) {
  const parts = []

  if (endpoint.pathParams?.length) {
    parts.push(...endpoint.pathParams.map((param) => param.name))
  }

  if (endpoint.queryParams?.length) {
    parts.push(...endpoint.queryParams.map((param) => param.name))
  }

  if (endpoint.bodyExample !== undefined) {
    parts.push('body')
  }

  return parts.length ? parts.join(', ') : '—'
}
</script>

<style scoped>
.api-index-page {
  display: grid;
  gap: 16px;
}

.api-index-head {
  align-items: center;
}

.api-index-card {
  display: grid;
  gap: 14px;
}

.api-index-group-title {
  margin: 0;
}

.api-index-table-wrap {
  overflow-x: auto;
}

.api-index-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.api-index-table th,
.api-index-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

.api-index-path {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.api-method-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 62px;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
  background: rgba(124, 163, 255, 0.12);
  border: 1px solid rgba(124, 163, 255, 0.2);
}

.api-test-link {
  color: var(--brand);
  font-weight: 700;
}

@media (max-width: 640px) {
  .api-index-head {
    align-items: stretch;
    flex-direction: column;
  }

  .api-index-table th,
  .api-index-table td {
    padding: 9px 10px;
    font-size: 0.9rem;
  }
}
</style>
