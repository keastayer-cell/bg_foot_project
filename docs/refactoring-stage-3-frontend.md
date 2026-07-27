# Refactoring Stage 3 Frontend

Дата завершения: 2026-07-27

## Цель

Разобрать крупные frontend-страницы на UI, состояние, бизнес-правила и API без изменения пользовательского поведения.

## Admin и Tours

- `Admin.vue` разделён на панели сезонов, команд, туров, игроков, судей, ролей, представителей и блокировок.
- Управление командами, турами, игроками, судьями, доступом и правилами сезона вынесено в composables.
- `Tours.vue` разделён на таблицу, шахматку, playoff, список туров и статистику.
- Вычисления таблиц и статусов покрыты unit-тестами.
- Маршруты вынесены в `router/routes.js`, страницы загружаются лениво.

## Match

- `Match.vue`: 1894 -> 410 строк.
- `useMatchPage.js`: загрузка матча и управление протоколом.
- `useMatchLineups.js`: заявки команд, формы добавления, ошибки и сохранение состава.
- `api/matches.js`: получение матча, PDF, заявки и protocol endpoints.
- Scoped CSS вынесен в `styles/pages/match.css`.

## TeamRepDashboard

- `TeamRepDashboard.vue`: 1356 -> 382 строки.
- `useTeamRepDashboard.js`: загрузка dashboard, сезонные заявки и вычисляемые представления.
- `useTeamRepPlayerForm.js`: создание/редактирование игрока и загрузка фото.
- `useIncomingTransfers.js`: пагинация и обработка входящих трансферов.
- `teamRepPresentation.js`: статусы, даты и правила редактирования заявки.
- `api/teamRep.js`: dashboard, заявки, игроки и входящие трансферы.
- Scoped CSS вынесен в `styles/pages/team-rep-dashboard.css`.

## API Layer

HTTP transport, JSON parsing и единый сетевой error boundary находятся в `api/http.js`. Прямого `fetch` вне этого модуля нет.

Доменные API-модули:

- `adminLeague.js`;
- `adminSeasons.js`;
- `auth.js`;
- `catalog.js`;
- `matches.js`;
- `seasonApplications.js`;
- `teamRep.js`;
- `teamRepTransfers.js`.

Страницы и компоненты больше не формируют HTTP-запросы напрямую. Динамический API Explorer также использует общий raw transport.

## Debug Logging

Глобальный fetch logger включается только через:

```bash
VITE_DEBUG_API_LOGS=true
```

По умолчанию logger выключен; чувствительные поля маскируются.

## Проверки

```bash
npm --prefix web test
npm --prefix web run build
npm --prefix web run test:e2e
```

Результат:

- 62 unit/route-теста;
- production build успешен;
- 5 Playwright smoke: главная, login, admin, team representative dashboard, match;
- `Match.vue` и `TeamRepDashboard.vue` проверены реальным Vue router/browser render.

## Статус

Этап 3 завершён. Все пункты исходного чек-листа выполнены.
