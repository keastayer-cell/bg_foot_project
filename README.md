# bg_foot_project

Единый репозиторий проекта статистики футбола.

## Веточная схема

- `dev` — основная ветка разработки
- `test` — ветка для выкладки на test-стенд
- `prod` — задел под production-ветку

Рабочая схема:

- локальная разработка ведется относительно `dev`
- когда нужно выкатить изменения на test VPS, изменения попадают в `test`
- push в `test` запускает GitHub Actions и автодеплой на test-сервер

## Структура

- `app` — Spring Boot backend
- `web` — Vue 3 frontend
- `db` — Flyway SQL migrations, вынесенные отдельно для удобства просмотра

## Локальный запуск

### Backend

```bash
mvn -q -DskipTests -f app/pom.xml package
mvn -f app/pom.xml spring-boot:run
```

### Frontend

```bash
cd web
npm install
npm run dev
```

## Test Deploy

Автодеплой test-стенда завязан на push в ветку `test`.

Workflow:

1. Собирает backend JAR из `app/`
2. Собирает frontend из `web/` с `VITE_API_BASE_URL=http://<test-host>`
3. Загружает артефакты на test VPS
4. Обновляет `/opt/football-stats-app/app.jar`
5. Обновляет `/var/www/football-stats-web`
6. Перезапускает `football-stats-app`
7. Проверяет `nginx -t` и делает reload nginx

Необходимые GitHub Actions secrets:

- `TEST_VPS_HOST`
- `TEST_VPS_USER`
- `TEST_VPS_PASSWORD`

## Примечания

- Секреты и локальные `.env` файлы в репозиторий не включены.
- Сборочные артефакты (`target`, `dist`, `node_modules`) исключены из git.