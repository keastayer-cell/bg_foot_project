<template>
  <section class="section-wrap">
    <h2 class="section-title">🛡 Команды</h2>

    <div class="cards-grid" v-if="standings.length">
      <article class="card team-card" v-for="team in standings" :key="team.team">
        <h3>{{ team.rank }}. {{ team.team }}</h3>
        <div class="stats-row">
          <span>Матчи: <b>{{ team.played }}</b></span>
          <span>Победы: <b>{{ team.wins }}</b></span>
          <span>Очки: <b>{{ team.points }}</b></span>
        </div>
        <p class="muted">Голы: {{ team.gf }} : {{ team.ga }}</p>
      </article>
    </div>

    <p class="empty-text" v-else>Нет команд в выбранном сезоне.</p>
  </section>
</template>

<script setup>
import { useStore } from '../store/store'

const { standings } = useStore()
</script>
