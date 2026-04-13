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
