<template>
  <section class="section-wrap">
    <article class="card form-card">
      <h2 class="section-title">Добавить матч</h2>

      <div class="form-grid">
        <label>
          Команда 1
          <input v-model.trim="teamA" placeholder="Север" />
        </label>

        <label>
          Команда 2
          <input v-model.trim="teamB" placeholder="Юг" />
        </label>

        <label>
          Голы команды 1
          <input type="number" min="0" v-model.number="scoreA" />
        </label>

        <label>
          Голы команды 2
          <input type="number" min="0" v-model.number="scoreB" />
        </label>

        <label>
          Дата
          <input type="date" v-model="date" />
        </label>

        <label>
          Лучший игрок
          <input v-model.trim="bestPlayer" placeholder="Иван Петров" />
        </label>

        <label class="wide">
          Голы команды 1
          <input v-model.trim="scorersA" placeholder="Иван Петров:2, Сергей Иванов:1" />
        </label>

        <label class="wide">
          Голы команды 2
          <input v-model.trim="scorersB" placeholder="Анна Смирнова:1" />
        </label>
      </div>

      <p class="error-text" v-if="errorText">{{ errorText }}</p>

      <div class="actions-row">
        <router-link class="btn-ghost" to="/">Отмена</router-link>
        <button class="btn-primary" @click="save">Сохранить матч</button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useStore } from '../store/store'
import { useRouter } from 'vue-router'

const { addMatch } = useStore()
const router = useRouter()

const teamA = ref('')
const teamB = ref('')
const scoreA = ref(0)
const scoreB = ref(0)
const date = ref(new Date().toISOString().slice(0, 10))
const bestPlayer = ref('')
const scorersA = ref('')
const scorersB = ref('')
const errorText = ref('')

function parseScorers(input) {
  if (!input) return []
  return input
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      const [nameRaw, goalsRaw] = item.split(':')
      const name = (nameRaw || '').trim()
      const goals = Math.max(1, Number.parseInt((goalsRaw || '1').trim(), 10) || 1)
      return { name, goals }
    })
    .filter((item) => item.name.length > 0)
}

function save() {
  errorText.value = ''

  if (!teamA.value || !teamB.value) {
    errorText.value = 'Нужно указать обе команды.'
    return
  }

  if (teamA.value === teamB.value) {
    errorText.value = 'Команды должны быть разными.'
    return
  }

  addMatch({
    teamA: teamA.value,
    teamB: teamB.value,
    scoreA: Math.max(0, Number(scoreA.value) || 0),
    scoreB: Math.max(0, Number(scoreB.value) || 0),
    date: date.value,
    bestPlayer: bestPlayer.value,
    scorersA: parseScorers(scorersA.value),
    scorersB: parseScorers(scorersB.value),
  })

  router.push('/')
}
</script>
