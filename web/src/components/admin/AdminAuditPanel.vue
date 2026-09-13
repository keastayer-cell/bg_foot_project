<template>
  <article class="card admin-panel audit-panel">
    <header class="admin-panel-head">
      <p class="admin-panel-kicker">История изменений</p>
      <h3 class="section-title">Журнал действий</h3>
      <p class="muted-text">Кто, когда и что изменил. Записи появляются после успешного сохранения.</p>
    </header>
    <form class="audit-filters" @submit.prevent="pageNumber = 0; load()">
      <label>С даты<input v-model="filters.from" type="datetime-local" /></label>
      <label>По дату<input v-model="filters.to" type="datetime-local" /></label>
      <label>Автор<input v-model="filters.author" placeholder="Имя пользователя" /></label>
      <label>Сущность<select v-model="filters.entityType"><option value="">Все</option><option v-for="(label, key) in entityLabels" :key="key" :value="key">{{ label }}</option></select></label>
      <label>ID сущности<input v-model="filters.entityId" type="number" min="1" /></label>
      <label>Действие<select v-model="filters.action"><option value="">Все</option><option v-for="(label, key) in actionLabels" :key="key" :value="key">{{ label }}</option></select></label>
      <button class="btn-primary" :disabled="loading">Показать</button>
      <button class="btn-ghost" type="button" :disabled="loading" @click="reset">Сбросить</button>
    </form>
    <UiState v-if="loading" tone="loading" title="Загружаем журнал" />
    <UiState v-else-if="error" tone="error" title="Не удалось загрузить журнал" :message="error" action-label="Повторить" @action="load" />
    <UiState v-else-if="!data.items.length" tone="empty" title="Записей нет" message="Здесь появятся новые действия. Попробуйте изменить фильтры." />
    <div v-else class="audit-list">
      <section v-for="item in data.items" :key="item.id" class="audit-entry">
        <div class="audit-entry-head">
          <div><strong>{{ actionLabels[item.action] || item.action }}</strong><p>{{ entityLabels[item.entityType] || item.entityType }} · № {{ item.entityId }}</p></div>
          <div class="audit-author"><strong>{{ item.actorName || (item.actorUserId ? `Пользователь № ${item.actorUserId}` : 'Система') }}</strong><time>{{ formatDate(item.createdAt) }}</time></div>
          <RouterLink v-if="item.path" class="btn-ghost" :to="item.path">Открыть</RouterLink>
          <button v-else-if="internalTab(item.entityType)" class="btn-ghost" @click="$emit('select-tab', internalTab(item.entityType))">Открыть раздел</button>
        </div>
        <details><summary>Что изменилось</summary><div class="audit-diff-wrap"><table><thead><tr><th>Поле</th><th>До</th><th>После</th></tr></thead><tbody><tr v-for="change in differences(item.changes)" :key="change.field"><th>{{ change.field }}</th><td>{{ change.before }}</td><td>{{ change.after }}</td></tr></tbody></table></div></details>
      </section>
    </div>
    <footer v-if="data.totalPages > 1 && !loading" class="audit-pagination"><button class="btn-ghost" :disabled="pageNumber === 0" @click="pageNumber--; load()">Назад</button><span>{{ pageNumber + 1 }} / {{ data.totalPages }} · {{ data.totalElements }} записей</span><button class="btn-ghost" :disabled="pageNumber + 1 >= data.totalPages" @click="pageNumber++; load()">Вперёд</button></footer>
  </article>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import UiState from '../UiState.vue'
import { useAuth } from '../../store/auth'
defineEmits(['select-tab'])
const { authorizedApiRequest } = useAuth()
const filters = reactive({ from: '', to: '', author: '', entityType: '', entityId: '', action: '' })
const pageNumber = ref(0)
const data = ref({ items: [], totalPages: 0, totalElements: 0 })
const loading = ref(false)
const error = ref('')
const entityLabels = { PROTOCOL: 'Протокол', MATCH: 'Матч', TRANSFER: 'Трансфер', APPLICATION: 'Заявка', ROLE: 'Роли пользователя', TEAM_ACCESS: 'Доступ к командам', SEASON: 'Сезон', COMPETITION: 'Соревнование', DISCIPLINE: 'Дисциплина турнира', TEAM: 'Команда', PLAYER: 'Игрок', REFEREE: 'Судья', OFFICIAL: 'Организатор', VENUE: 'Стадион', TOUR: 'Тур' }
const actionLabels = { PROTOCOL_UPDATED: 'Изменён протокол', PROTOCOL_VERIFIED: 'Подтверждён протокол', PROTOCOL_REOPENED: 'Открыт протокол', RESULT_UPDATED: 'Изменён подтверждённый результат', SCHEDULE_UPDATED: 'Изменено расписание', DELETED: 'Удалён матч', TOUR_PUBLISHED: 'Опубликован тур', APPLICATION_APPROVED: 'Одобрена заявка', APPLICATION_RETURNED: 'Заявка возвращена на доработку', APPLICATION_REJECTED: 'Отклонена заявка', TRANSFER_REQUESTED: 'Создан запрос трансфера', TRANSFER_APPROVED: 'Одобрен трансфер', TRANSFER_REJECTED: 'Отклонён трансфер', TRANSFER_REVOKED: 'Отозван трансфер', ROLE_GRANTED: 'Назначена роль', ROLE_REVOKED: 'Отозвана роль', TEAM_ACCESS_GRANTED: 'Выдан доступ к команде', TEAM_ACCESS_REVOKED: 'Отозван доступ к команде', REGULATION_UPDATED: 'Изменён регламент', REGULATION_REMOVED: 'Удалён регламент', DISCIPLINE_ADJUSTED: 'Изменена дисквалификация', DEACTIVATED: 'Сущность деактивирована', SEASON_UPDATED: 'Изменены настройки сезона', COMPETITION_UPDATED: 'Изменено соревнование' }
const fieldLabels = { status: 'Статус', enabled: 'Включено', team_count: 'Команд', third_place_enabled: 'Матч за третье место', round_of_16_legs: 'Матчей в 1/8', quarterfinal_legs: 'Матчей в четвертьфинале', semifinal_legs: 'Матчей в полуфинале', final_legs: 'Матчей в финале', third_place_legs: 'Матчей за третье место', win_points: 'Очки за победу', draw_points: 'Очки за ничью', loss_points: 'Очки за поражение', ranking_rules_json: 'Критерии ранжирования', referee_id: 'Судья (ID)', red_cards_for_suspension: 'Матчей за КК', application_deadline: 'Срок подачи заявок', max_roster_size: 'Размер заявки', transfer_window_start_date: 'Открытие трансферов', transfer_window_end_date: 'Закрытие трансферов', home_score: 'Голы хозяев', away_score: 'Голы гостей', home_technical_defeat: 'Техническое поражение хозяев', away_technical_defeat: 'Техническое поражение гостей', best_player_id: 'Лучший игрок (ID)', chief_referee_id: 'Главный судья (ID)', assistant_referee_one_id: 'Первый помощник (ID)', assistant_referee_two_id: 'Второй помощник (ID)', notes: 'Примечание', started_at: 'Начало матча', finished_at: 'Конец матча', kickoff_at: 'Дата матча', venue_id: 'Стадион (ID)', schedule_status: 'Статус расписания', original_kickoff_at: 'Исходная дата', schedule_change_reason: 'Причина изменения', active: 'Активность', name: 'Название', full_name: 'ФИО', short_name: 'Краткое название', season_id: 'Сезон (ID)', competition_id: 'Соревнование (ID)', tour_id: 'Тур (ID)', player_id: 'Игрок (ID)', team_id: 'Команда (ID)', home_team_id: 'Хозяева (ID)', away_team_id: 'Гости (ID)', from_team_id: 'Исходная команда (ID)', to_team_id: 'Новая команда (ID)', decision_comment: 'Решение', decision_at: 'Время решения', processed_at: 'Время обработки', role_id: 'Роль (ID)', can_edit_roster: 'Правка состава', can_edit_application: 'Правка заявки', valid_from: 'Доступ с', valid_to: 'Доступ до', rounds_count: 'Кругов', playoff_enabled: 'Плей-офф', playoff_team_count: 'Команд в плей-офф', players_on_field: 'Игроков на поле', regulation_media_id: 'Документ регламента (ID)', regulation_updated_at: 'Дата изменения регламента', competition_type: 'Тип соревнования', yellow_cards_for_suspension: 'Порог ЖК', yellow_suspension_matches: 'Матчей за ЖК', red_suspension_matches: 'Матчей за КК', old_remaining_matches: 'Прежний остаток', new_remaining_matches: 'Новый остаток', adjustment_type: 'Тип решения', reason: 'Основание', event_type: 'Событие', minute: 'Минута', extra_minute: 'Добавленная минута', published: 'Публикация' }
function valueText(value) {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'boolean') return value ? 'Да' : 'Нет'
  if (Array.isArray(value)) return value.length ? value.map(valueText).join('\n') : '—'
  if (typeof value === 'object') return Object.entries(value).map(([key, value]) => `${fieldLabels[key] || key}: ${valueText(value)}`).join(' · ')
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) return formatDate(value)
  return String(value)
}
function differences(changes) {
  const before = changes.before || {}; const after = changes.after || {}; const rows = []
  for (const group of new Set([...Object.keys(before), ...Object.keys(after)])) {
    const oldRows = before[group] || []; const newRows = after[group] || []
    if (JSON.stringify(oldRows) === JSON.stringify(newRows)) continue
    const prefix = { playoff: 'Плей-офф · ', rules: 'Правила сезона · ', referees: 'Судьи сезона · ', roles: 'Роли пользователя · ', events: 'События протокола · ' }[group] || ''
    if (oldRows.length <= 1 && newRows.length <= 1) {
      const oldRow = oldRows[0] || {}; const newRow = newRows[0] || {}
      for (const key of new Set([...Object.keys(oldRow), ...Object.keys(newRow)])) {
        if (JSON.stringify(oldRow[key]) !== JSON.stringify(newRow[key])) rows.push({ field: prefix + (fieldLabels[key] || key), before: valueText(oldRow[key]), after: valueText(newRow[key]) })
      }
    } else rows.push({ field: group === 'events' ? 'События протокола' : group === 'roles' ? 'Роли пользователя' : 'Записи', before: valueText(oldRows), after: valueText(newRows) })
  }
  return rows
}
function internalTab(type) { return { TEAM: 'teams', PLAYER: 'players', COMPETITION: 'competitions', SEASON: 'seasons', MATCH: 'tours', ROLE: 'roles', TEAM_ACCESS: 'representatives', REFEREE: 'referees', OFFICIAL: 'league', VENUE: 'league', TOUR: 'tours' }[type] }
function formatDate(value) { return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) }
function reset() { Object.keys(filters).forEach((key) => { filters[key] = '' }); pageNumber.value = 0; load() }
async function load() {
  loading.value = true; error.value = ''
  const params = new URLSearchParams({ pagenum: String(pageNumber.value), pagesize: '25' })
  Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, key === 'from' || key === 'to' ? new Date(value).toISOString() : value) })
  try { data.value = await authorizedApiRequest(`/api/admin/audit?${params}`, { method: 'GET' }) }
  catch (requestError) { error.value = requestError.message || 'Ошибка загрузки.' }
  finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.audit-panel{padding:0}.audit-filters{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;padding:20px;border-bottom:1px solid var(--line)}.audit-filters label{display:grid;gap:7px;color:var(--muted);font-size:.8rem}.audit-filters input,.audit-filters select{min-width:0;width:100%}.audit-list{display:grid;gap:12px;padding:20px}.audit-entry{border:1px solid var(--line);border-radius:12px;background:rgba(10,16,37,.5);overflow:hidden}.audit-entry-head{display:flex;align-items:center;gap:18px;padding:16px}.audit-entry-head>div:first-child{flex:1}.audit-entry-head p{margin:5px 0 0;color:var(--muted);font-size:.8rem}.audit-author{display:grid;gap:5px;font-size:.8rem}.audit-author time{color:var(--muted)}.audit-entry summary{padding:12px 16px;cursor:pointer;color:var(--brand);border-top:1px solid var(--line)}.audit-diff-wrap{overflow:auto}.audit-diff-wrap table{width:100%;border-collapse:collapse;table-layout:fixed;font-size:.8rem}.audit-diff-wrap th,.audit-diff-wrap td{padding:12px;text-align:left;border-top:1px solid var(--line);vertical-align:top;white-space:pre-wrap;overflow-wrap:anywhere}.audit-diff-wrap th{color:var(--muted)}.audit-pagination{display:flex;justify-content:center;align-items:center;gap:14px;padding:16px}@media(max-width:760px){.audit-filters{grid-template-columns:1fr 1fr}.audit-entry-head{flex-wrap:wrap}.audit-entry-head>div:first-child{flex-basis:100%}}@media(max-width:480px){.audit-filters{grid-template-columns:1fr}}
</style>
