# API Explorer Session Context (RU)

Документ нужен для быстрого продолжения работы после обрыва сессии.

## Текущее состояние

- Отдельная страница API индекса: `/api-explorer`
- Отдельная страница теста endpoint: `/api-explorer/test/:endpointKey`
- В `preview` доступно по адресу: `http://127.0.0.1:4173/api-explorer`
- Backend API base URL (frontend): `http://127.0.0.1:8080`

## Ключевые файлы API Explorer

- `src/data/apiExplorerCatalog.js` — единый каталог endpoint-ов, групп, параметров и примеров body.
- `src/pages/ApiExplorer.vue` — индекс endpoint-ов (таблица + кнопка "Тест").
- `src/pages/ApiExplorerTest.vue` — отдельный экран тестирования запроса/ответа.
- `src/router/index.js` — маршруты `/api-explorer` и `/api-explorer/test/:endpointKey`.
- `src/style.css` — стили индекса и тест-страницы API Explorer.

## Обязательное правило сопровождения

При каждом добавлении/изменении backend API обязательно синхронизировать API Explorer.

Минимальный чеклист:

1. Добавить или изменить endpoint в `src/data/apiExplorerCatalog.js`:
   - `key` (уникальный)
   - `method`
   - `path`
   - `description`
   - `auth`
   - `access`
   - `pathParams` (если есть)
   - `bodyExample` (для POST/PUT/PATCH)
2. Проверить, что endpoint отображается на `/api-explorer`.
3. Открыть `/api-explorer/test/:endpointKey` через кнопку "Тест" и выполнить запрос.
4. Проверить статус/тело ответа и обработку ошибок.
5. Пересобрать frontend: `npm run build`.

## Как быстро поднять проверку

Frontend:

```bash
cd /Users/korytov/projects/football-stats-web
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

Backend:

```bash
cd /Users/korytov/projects/football-stats-app
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

Smoke-check:

```bash
curl -sS http://127.0.0.1:8080/api/health
```

## Что уже реализовано в проекте

- JWT авторизация/регистрация с ролями.
- Ролевая модель: `SUPER_ADMIN`, `USER`, `TEAM_REP`.
- Spring Security с ограничениями по HTTP методам и JSON-ответами 401/403.
- Админские endpoint-ы управления ролями и team-scopes.
- API Explorer как отдельный экран с группировкой endpoint-ов.
- Переход "Тест" на отдельную страницу тестирования конкретного endpoint.

## Важное замечание

Если URL `/api-explorer` внезапно редиректит на `/`, сначала проверить:

1. что в `src/router/index.js` у маршрута `/api-explorer` нет `requiresAuth/requiresSuperAdmin`;
2. что выполнен `npm run build` (preview может показывать старый билд).
