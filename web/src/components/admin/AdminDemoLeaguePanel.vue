<template>
  <article class="card admin-panel demo-league-panel">
    <div class="admin-panel-head">
      <div>
        <span class="eyebrow">Только локальная среда</span>
        <h3 class="section-title">Тестовая лига</h3>
      </div>
      <button class="btn-ghost btn-sm" type="button" :disabled="loading" @click="$emit('refresh')">
        Обновить
      </button>
    </div>

    <UiState
      v-if="error"
      tone="error"
      title="Генератор недоступен"
      :message="error"
    />

    <div v-if="status" class="demo-league-content">
      <section class="admin-surface demo-league-overview">
        <div>
          <span class="demo-status-label">{{ status.exists ? 'Набор создан' : 'Набора пока нет' }}</span>
          <h4>{{ status.seasonName || status.name }}</h4>
          <p class="muted-text">
            Данные создаются в текущей локальной базе. Сброс удаляет только объекты из реестра этого набора.
          </p>
        </div>
        <span v-if="status.stage" class="status-badge status-verified">
          Этап {{ status.stageIndex }} из 5 · {{ stageLabel(status.stage) }}
        </span>
      </section>

      <ol class="demo-stage-list">
        <li
          v-for="(stage, index) in stages"
          :key="stage.code"
          :class="[
            'demo-stage-card',
            {
              'is-complete': status.stageIndex >= index + 1,
              'is-current': status.stageIndex === index + 1,
            },
          ]"
        >
          <span class="demo-stage-number">{{ index + 1 }}</span>
          <div>
            <strong>{{ stage.title }}</strong>
            <span>{{ stage.description }}</span>
          </div>
        </li>
      </ol>

      <section v-if="status.exists" class="demo-count-grid">
        <article v-for="item in countCards" :key="item.label" class="admin-surface demo-count-card">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </article>
      </section>

      <section class="admin-surface demo-actions">
        <div>
          <h4>Следующее действие</h4>
          <p class="muted-text">{{ nextActionDescription }}</p>
        </div>
        <div class="actions-row">
          <button
            v-if="can('BASE')"
            class="btn-primary"
            type="button"
            :disabled="loading"
            @click="$emit('action', 'BASE')"
          >
            Создать базовую лигу
          </button>
          <button
            v-if="can('SCHEDULE')"
            class="btn-primary"
            type="button"
            :disabled="loading"
            @click="$emit('action', 'SCHEDULE')"
          >
            Сформировать расписание
          </button>
          <button
            v-if="can('RESULTS')"
            class="btn-primary"
            type="button"
            :disabled="loading"
            @click="$emit('action', 'RESULTS')"
          >
            Добавить сыгранные матчи
          </button>
          <button
            v-if="can('TRANSFERS')"
            class="btn-primary"
            type="button"
            :disabled="loading"
            @click="$emit('action', 'TRANSFERS')"
          >
            Подготовить трансфер
          </button>
          <button
            v-if="can('PLAYOFF')"
            class="btn-primary"
            type="button"
            :disabled="loading"
            @click="$emit('action', 'PLAYOFF')"
          >
            Сформировать плей-офф
          </button>
          <span v-if="loading" class="muted-text">Выполняется операция…</span>
        </div>
      </section>

      <section v-if="status.accounts?.length" class="admin-surface demo-accounts">
        <div class="demo-section-head">
          <div>
            <h4>Тестовые пользователи</h4>
            <p class="muted-text">Выйдите из текущего аккаунта и войдите под нужной ролью.</p>
          </div>
          <button class="btn-ghost btn-sm" type="button" @click="copyAccounts">Копировать доступы</button>
        </div>
        <div class="demo-account-list">
          <article v-for="account in status.accounts" :key="account.email" class="demo-account-row">
            <div>
              <strong>{{ account.role }}</strong>
              <span>{{ account.context }}</span>
            </div>
            <code>{{ account.email }}</code>
            <code>{{ account.password }}</code>
          </article>
        </div>
      </section>

      <section v-if="status.exists" class="admin-surface demo-danger-zone">
        <div>
          <h4>Сброс набора</h4>
          <p class="muted-text">Удалит сезон, расписание, демо-команды, игроков, судей и тестовые аккаунты.</p>
        </div>
        <button class="btn-danger" type="button" :disabled="loading" @click="$emit('reset')">
          Удалить тестовую лигу
        </button>
      </section>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import UiState from '../UiState.vue'

const props = defineProps({
  status: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

defineEmits(['action', 'refresh', 'reset'])

const stages = [
  { code: 'BASE', title: 'Базовая лига', description: '10 команд, 150 игроков, 8 судей и аккаунты ролей.' },
  { code: 'SCHEDULE', title: 'Расписание', description: '18 туров и 90 матчей в два круга.' },
  { code: 'RESULTS', title: 'Живой сезон', description: '20 протоколов, составы, голы и турнирная таблица.' },
  { code: 'TRANSFERS', title: 'Трансферное окно', description: 'Открытое окно и ожидающая согласования заявка.' },
  { code: 'PLAYOFF', title: 'Плей-офф', description: 'Сетка на 8 команд, сыгранные раунды, финал и матч за 3-е место.' },
]

const countCards = computed(() => {
  const counts = props.status?.counts || {}
  return [
    { label: 'Команд', value: counts.teams || 0 },
    { label: 'Игроков', value: counts.players || 0 },
    { label: 'Судей', value: counts.referees || 0 },
    { label: 'Туров', value: counts.tours || 0 },
    { label: 'Матчей', value: counts.matches || 0 },
    { label: 'Сыграно', value: counts.completedMatches || 0 },
    { label: 'Пар плей-офф', value: counts.playoffTies || 0 },
    { label: 'Матчей плей-офф', value: counts.playoffMatches || 0 },
  ]
})

const nextActionDescription = computed(() => {
  if (!props.status?.exists) return 'Создать участников, сезон, составы и учетные записи для проверки ролей.'
  if (can('SCHEDULE')) return 'Система разложит все пары команд по 18 турам и опубликует их.'
  if (can('RESULTS')) return 'Первые 20 матчей получат составы, события и проверенные протоколы.'
  if (can('TRANSFERS')) return 'Будет создана заявка на переход игрока для проверки согласования двумя представителями.'
  if (can('PLAYOFF')) return 'Регулярная часть завершится, после чего появится сетка на 8 команд с матчем за третье место.'
  return 'Набор полностью подготовлен. Можно проходить пользовательские сценарии.'
})

function can(action) {
  return props.status?.allowedActions?.includes(action)
}

function stageLabel(stage) {
  return stages.find((item) => item.code === stage)?.title || stage
}

async function copyAccounts() {
  const text = props.status.accounts
    .map((account) => `${account.role}: ${account.email} / ${account.password}`)
    .join('\n')
  await navigator.clipboard.writeText(text)
}
</script>

<style scoped>
.demo-league-panel,
.demo-league-content {
  display: grid;
  gap: 16px;
}

.demo-league-panel .admin-panel-head,
.demo-league-overview,
.demo-section-head,
.demo-danger-zone {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.demo-league-overview h4,
.demo-actions h4,
.demo-accounts h4,
.demo-danger-zone h4 {
  margin: 4px 0 6px;
}

.demo-status-label {
  color: var(--brand);
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.demo-stage-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.demo-stage-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.025);
  opacity: 0.62;
}

.demo-stage-card.is-current,
.demo-stage-card.is-complete {
  border-color: rgba(97, 232, 162, 0.34);
  opacity: 1;
}

.demo-stage-card.is-current {
  background: rgba(97, 232, 162, 0.08);
}

.demo-stage-number {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(124, 163, 255, 0.16);
  font-weight: 800;
}

.demo-stage-card div,
.demo-account-row div {
  display: grid;
  gap: 4px;
}

.demo-stage-card span:not(.demo-stage-number),
.demo-account-row span {
  color: var(--muted);
  font-size: 0.82rem;
  line-height: 1.35;
}

.demo-count-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.demo-count-card {
  display: grid;
  gap: 3px;
  padding: 14px;
}

.demo-count-card strong {
  font-size: 1.55rem;
}

.demo-count-card span {
  color: var(--muted);
  font-size: 0.8rem;
}

.demo-actions {
  display: grid;
  gap: 14px;
}

.demo-account-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.demo-account-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(210px, auto) minmax(110px, auto);
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
}

.demo-account-row code {
  color: var(--text);
  font-size: 0.82rem;
}

@media (max-width: 1100px) {
  .demo-stage-list,
  .demo-count-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .demo-league-panel .admin-panel-head,
  .demo-league-overview,
  .demo-section-head,
  .demo-danger-zone {
    align-items: stretch;
    flex-direction: column;
  }

  .demo-stage-list,
  .demo-count-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .demo-account-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
