# BG Foot

Монорепозиторий футбольной лиги:

- `app` — Spring Boot backend и Flyway migrations;
- `web` — Vue/Vite frontend;
- `mailer` — обработчик очереди уведомлений;
- `docs` — документация и roadmap;
- `scripts` — локальные Git/deploy helpers.

Source of truth: репозиторий `keastayer-cell/bg_foot_project`, рабочая ветка `dev`. Актуальные runtime migrations находятся только в `app/src/main/resources/db/migration`.

## Требования

- Java 21;
- Maven 3.9+;
- Node.js 20+ и npm;
- PostgreSQL 15+.

Проверить окружение:

```bash
java -version
mvn -version
node --version
npm --version
psql --version
```

## Локальная база

Создайте пользователя и БД с правами на схемы `public`, `work` и `mailer`:

```sql
CREATE ROLE football_app LOGIN PASSWORD 'local-password';
CREATE DATABASE football_db OWNER football_app;
```

Flyway создаёт и обновляет структуру при запуске backend. Для локальной разработки не применяйте SQL-файлы вручную.

## Настройка

Создайте локальные env-файлы, которые исключены из Git:

```bash
cp app/.env.example app/.env
cp web/.env.example web/.env
cp mailer/.env.example mailer/.env
```

Обязательно замените `DB_PASSWORD`, `MAILER_DB_PASSWORD` и `JWT_SECRET`. Секрет JWT должен содержать не менее 32 байт. Для безопасного локального запуска mailer используйте:

```dotenv
MAILER_TRANSPORT_TYPE=log
```

Режим `smtp` включайте только с рабочими SMTP credentials.

## Запуск

Backend:

```bash
mvn -f app/pom.xml spring-boot:run
```

Frontend во втором терминале:

```bash
npm --prefix web install
npm --prefix web run dev
```

Mailer в третьем терминале:

```bash
mvn -f mailer/pom.xml spring-boot:run
```

Адреса по умолчанию:

- frontend: `http://127.0.0.1:5173`;
- backend: `http://127.0.0.1:8080`;
- backend health: `http://127.0.0.1:8080/api/health`;
- mailer health: `http://127.0.0.1:8090/actuator/health`;
- mailer readiness: `http://127.0.0.1:8090/actuator/health/readiness`.

## Проверки

Полный локальный набор:

```bash
mvn -f app/pom.xml test
mvn -f mailer/pom.xml test
npm --prefix web run lint
npm --prefix web test
npm --prefix web run build
npm --prefix web run test:e2e
```

Frontend e2e сам запускает Vite dev server и использует перехват API-запросов, поэтому локальный backend ему не нужен.
Перед первым e2e-запуском установите браузер:

```bash
cd web
npx playwright install chromium
```

## Ветки

- `dev` — текущая разработка;
- `test` — test deployment;
- `main` — стабильная история проекта;
- `prod` — production deployment history.

Все изменения сначала фиксируются в `dev`. Перед продвижением рабочее дерево должно быть чистым.

## Test deploy

Локальный promote:

```bash
bash ./scripts/promote-dev-to-test.sh
```

Скрипт:

1. проверяет ветку `dev` и чистое рабочее дерево;
2. обновляет `dev`;
3. проверяет миграции, backend, mailer, frontend lint/tests/build;
4. отправляет `dev`;
5. объединяет `dev -> test`;
6. отправляет `test` и возвращается в `dev`.

Push в `test` запускает `.github/workflows/deploy-test.yml`. Workflow проверяет все модули, создаёт backup БД, доставляет backend, mailer и frontend, применяет Flyway migrations при старте backend и запускает отдельный локальный/public smoke-check. При провале health предыдущие jars и frontend возвращаются автоматически; миграции БД автоматически не откатываются.

Параметры сервера находятся только в GitHub Actions secrets:

- `TEST_VPS_HOST`;
- `TEST_VPS_USER`;
- `TEST_VPS_PASSWORD`.

## Production

Автоматического production workflow в репозитории пока нет. Общие backup/restore, ротация логов, smoke-check и процедура rollback описаны в [эксплуатационной документации](docs/operations.md). Production restore требует отдельного подтверждения и выполняется только во время согласованного downtime.

## Документация

- [Полный roadmap](docs/refactoring-roadmap.md)
- [Baseline этапа 1](docs/refactoring-stage-1-baseline.md)
- [Отчёт этапа 2](docs/refactoring-stage-2-tests.md)
- [Отчёт этапа 3](docs/refactoring-stage-3-frontend.md)
- [Отчёт этапа 4](docs/refactoring-stage-4-backend.md)
- [Отчёт этапа 5](docs/refactoring-stage-5-mailer.md)
- [Отчёт этапа 6](docs/refactoring-stage-6-operations.md)
- [Эксплуатация](docs/operations.md)

Документация внутри репозитория является актуальной. Локальные каталоги вне репозитория не считаются source of truth.
