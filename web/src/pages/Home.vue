<template>
  <section class="section-wrap">
    <div class="toolbar">
      <label class="season-box">
        <span>Сезон:</span>
        <select v-model="season">
          <option v-for="item in seasons" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <router-link class="btn-primary" to="/create">+ Добавить матч</router-link>
    </div>

    <article class="card">
      <h2 class="section-title">Турнирная таблица</h2>
      <table class="stats-table" v-if="standings.length">
        <thead>
          <tr>
            <th>#</th>
            <th>Команда</th>
            <th>И</th>
            <th>В</th>
            <th>Н</th>
            <th>П</th>
            <th>Голы</th>
            <th>О</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in standings" :key="team.team">
            <td>{{ team.rank }}</td>
            <td>{{ team.team }}</td>
            <td>{{ team.played }}</td>
            <td>{{ team.wins }}</td>
            <td>{{ team.draws }}</td>
            <td>{{ team.losses }}</td>
            <td>{{ team.gf }}:{{ team.ga }}</td>
            <td><b>{{ team.points }}</b></td>
          </tr>
        </tbody>
      </table>
      <p class="empty-text" v-else>В этом сезоне еще нет матчей.</p>
    </article>

    <article class="card">
      <h2 class="section-title">Последние матчи</h2>
      <div class="match-list" v-if="seasonMatches.length">
        <button
          v-for="match in seasonMatches"
          :key="match.id"
          class="match-item"
          @click="$router.push('/match/' + match.id)"
        >
          <div class="match-title">{{ match.teamA }} {{ match.scoreA }} : {{ match.scoreB }} {{ match.teamB }}</div>
          <div class="match-meta">
            <span>{{ formatDate(match.date) }}</span>
            <span v-if="match.bestPlayer">Лучший игрок: {{ match.bestPlayer }}</span>
          </div>
        </button>
      </div>
      <p class="empty-text" v-else>Добавьте первый матч для выбранного сезона.</p>
    </article>

    <article class="card">
      <h2 class="section-title">Лучшие игроки</h2>
      <div class="cards-grid" v-if="topPlayers.length">
        <div class="player-card" v-for="player in topPlayers.slice(0, 4)" :key="player.name">
          <h3>{{ player.name }}</h3>
          <p class="muted">{{ player.team || 'Без команды' }}</p>
          <p>Голы: <b>{{ player.goals }}</b></p>
          <p>Игры: <b>{{ player.matches }}</b></p>
        </div>
      </div>
      <p class="empty-text" v-else>Пока нет данных по игрокам.</p>
    </article>
  </section>
</template>

<script setup>
import { useStore } from '../store/store'

const { season, seasons, seasonMatches, standings, topPlayers } = useStore()

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'long',
  })
}
</script>
