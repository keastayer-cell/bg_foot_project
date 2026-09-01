# BG Foot

Монорепозиторий футбольной лиги:

- `app` — Spring Boot backend и Flyway migrations;
- `web` — Vue/Vite frontend;
- `mailer` — обработчик очереди уведомлений;
- `docs` — документация и roadmap;
- `scripts` — локальные Git/deploy helpers.

Source of truth: репозиторий `keastayer-cell/bg_foot_project`, рабочая ветка `dev`. Актуальные runtime migrations находятся только в `app/src/main/resources/db/migration`.

История крупных изменений и текущая сборка ведутся в [RELEASE.md](RELEASE.md).

## Требования

- рекомендуемый Docker-запуск: Docker 24+, Compose plugin 2.20+ и GNU Make;
- запуск сервисов на хосте: Java 21, Maven 3.9+, Node.js 20+ и npm;
- запуск без контейнерной БД: PostgreSQL 15+.

Проверить окружение:

```bash
java -version
mvn -version
node --version
npm --version
psql --version
docker compose version
make --version
```

## Локальная база

При Docker-запуске пользователь и база создаются автоматически. Для PostgreSQL,
запущенного на хосте, создайте пользователя и БД с правами на схемы `public`,
`work` и `mailer`:

```sql
CREATE ROLE football_app LOGIN PASSWORD 'local-password';
CREATE DATABASE football_db OWNER football_app;
```

Flyway создаёт и обновляет структуру при запуске backend. Для локальной разработки не применяйте SQL-файлы вручную.

## Настройка

Создайте единый локальный env-файл в корне репозитория. Для запуска frontend на
хосте также создайте отдельный публичный Vite-файл:

```bash
cp .env.example .env
cp web/.env.example web/.env.local
```

Корневой `.env` читают Compose, backend и mailer. Обязательно замените
`DB_PASSWORD` и `JWT_SECRET`. Секрет JWT должен содержать не менее 32 байт. Для
безопасного локального запуска mailer используйте:

```dotenv
MAILER_TRANSPORT_TYPE=log
```

Режим `smtp` включайте только с рабочими SMTP credentials.

## Локальный запуск

Для работающего сайта нужны PostgreSQL, backend и frontend. Запущенный только Vite
откроет интерфейс на `5173`, но страницы с данными и авторизация без backend на
`8080` работать не будут.

Для наполнения текущей локальной базы реалистичными данными и проверки всех ролей
используйте раздел «Админ-панель → Тестовая лига». Подробное описание:
[docs/Тестовая-лига.md](docs/Тестовая-лига.md).

Все команды ниже выполняются из корня репозитория.

### Запуск через Docker

Это рекомендуемый способ: PostgreSQL 16, backend, mailer и frontend запускаются
одной командой в воспроизводимом окружении.

```bash
make up
make smoke
```

После успешной проверки откройте `http://127.0.0.1:5173`. Состояние и логи:

```bash
make ps
make logs
make logs s=backend
```

PostgreSQL опубликован на хосте через порт `5433`, чтобы не конфликтовать с
локальным PostgreSQL на `5432`. Обычная остановка сохраняет данные:

```bash
make down
```

Все команды и их краткое описание показывает `make help`. Полный сброс удаляет
данные лиги и сохранённые frontend-зависимости, поэтому требует подтверждения:

```bash
make reset-db CONFIRM=yes
```

Mailer в стандартной локальной конфигурации использует транспорт `log` и не
отправляет реальные письма.

### Запуск без Docker

1. Убедитесь, что PostgreSQL запущен:

```bash
pg_isready -h 127.0.0.1 -p 5432
```

Для PostgreSQL из Homebrew:

```bash
brew services start postgresql@17
```

Ожидаемый результат `pg_isready`: `accepting connections`.

2. Запустите backend в первом терминале:

```bash
mvn -f app/pom.xml spring-boot:run
```

Дождитесь сообщения `Tomcat started on port 8080`, затем проверьте:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

Ожидаемый ответ:

```json
{"status":"UP","service":"football-stats-app"}
```

3. Запустите frontend во втором терминале:

```bash
npm --prefix web install
npm --prefix web run dev -- --host 127.0.0.1
```

После сообщения `Local: http://127.0.0.1:5173/` откройте этот адрес в браузере.

4. Mailer нужен только для обработки уведомлений. Перед локальным запуском
убедитесь, что в `.env` задан безопасный режим `MAILER_TRANSPORT_TYPE=log`, затем
запустите его в третьем терминале:

```bash
mvn -f mailer/pom.xml spring-boot:run
```

Не запускайте mailer с `MAILER_TRANSPORT_TYPE=smtp`, если не планируете реальную
отправку накопившихся писем.

Адреса по умолчанию:

- frontend: `http://127.0.0.1:5173`;
- backend: `http://127.0.0.1:8080`;
- backend health: `http://127.0.0.1:8080/api/health`;
- mailer health: `http://127.0.0.1:8090/actuator/health`;
- mailer readiness: `http://127.0.0.1:8090/actuator/health/readiness`.

Чтобы запустить backend на хосте против PostgreSQL из Compose:

```bash
make db
make run-backend
```

### Если PostgreSQL не запускается

Проверьте состояние сервиса и последние строки журнала:

```bash
brew services list
tail -n 100 /opt/homebrew/var/log/postgresql@17.log
```

Ошибка `lock file "postmaster.pid" already exists` может означать как уже
работающий PostgreSQL, так и устаревший lock-файл после аварийного завершения.
Сначала проверьте PID из первой строки файла:

```bash
PG_LOCK_PID="$(head -n 1 /opt/homebrew/var/postgresql@17/postmaster.pid)"
ps -p "$PG_LOCK_PID" -o pid=,comm=,args=
```

Не удаляйте `postmaster.pid`, если этот PID принадлежит процессу PostgreSQL. Если
PID отсутствует или уже принадлежит другому процессу, сохраните lock-файл в
резервную копию и перезапустите сервис:

```bash
mv /opt/homebrew/var/postgresql@17/postmaster.pid \
  /tmp/postmaster.pid.stale
brew services restart postgresql@17
pg_isready -h 127.0.0.1 -p 5432
```

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
Перед первым e2e-запуском установите Chromium для desktop/Android и WebKit для iOS:

```bash
cd web
npx playwright install chromium webkit
```

Полная матрица включает desktop Chrome, компактный и современный Android Chrome, компактный и современный iPhone WebKit. Её можно запускать целиком или по платформе:

```bash
# Все desktop и mobile проекты
npm --prefix web run test:e2e

# Только четыре мобильных проекта
npm --prefix web run test:e2e:mobile

# Только Android Chromium
npm --prefix web run test:e2e:android

# Только iPhone WebKit
npm --prefix web run test:e2e:ios
```

Для визуального запуска пользовательских сценариев используйте Playwright UI:

```bash
npm --prefix web run test:e2e:ui
```

В окне Playwright можно запускать весь набор или отдельный сценарий, видеть браузер, шаги, запросы и trace при падении. Дополнительные режимы:

```bash
# Все сценарии последовательно в видимом браузере
npm --prefix web run test:e2e:headed

# Пошаговая отладка с Playwright Inspector
npm --prefix web run test:e2e:debug
```

Текущий smoke-набор изолирован от локальной БД: он проверяет frontend-сценарии на предсказуемых API-ответах. Полноценные live E2E через локальные backend и PostgreSQL должны храниться отдельным набором с тестовыми пользователями и очисткой созданных данных.

CI автоматически устанавливает Chromium/WebKit и запускает всю матрицу. Mobile layout-тесты дополнительно проверяют настоящий device viewport, отсутствие горизонтального переполнения, компактность фильтров и минимальную высоту основных touch-targets.

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

Deploy использует GitHub Environment `test`. В нём настраиваются:

- secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_HOST_FINGERPRINT`;
- variable: `PUBLIC_BASE_URL`.

DB/JWT/SMTP credentials в GitHub не передаются. Они находятся только в `/etc/bg-foot/test` на сервере. Полная модель описана в [управлении секретами](docs/Рефакторинг%2028.07.2026/secrets-management.md).

## Production

Автоматического production workflow в репозитории пока нет. Общие backup/restore, ротация логов, smoke-check и процедура rollback описаны в [эксплуатационной документации](docs/operations.md). Production restore требует отдельного подтверждения и выполняется только во время согласованного downtime.

## Документация

- [Полный roadmap](docs/Рефакторинг%2028.07.2026/refactoring-roadmap.md)
- [Baseline этапа 1](docs/Рефакторинг%2028.07.2026/refactoring-stage-1-baseline.md)
- [Отчёт этапа 2](docs/Рефакторинг%2028.07.2026/refactoring-stage-2-tests.md)
- [Отчёт этапа 3](docs/Рефакторинг%2028.07.2026/refactoring-stage-3-frontend.md)
- [Отчёт этапа 4](docs/Рефакторинг%2028.07.2026/refactoring-stage-4-backend.md)
- [Отчёт этапа 5](docs/Рефакторинг%2028.07.2026/refactoring-stage-5-mailer.md)
- [Отчёт этапа 6](docs/Рефакторинг%2028.07.2026/refactoring-stage-6-operations.md)
- [Эксплуатация](docs/operations.md)
- [Журнал замечаний](docs/Замечания.md)
- [Управление секретами](docs/Рефакторинг%2028.07.2026/secrets-management.md)

Документация внутри репозитория является актуальной. Локальные каталоги вне репозитория не считаются source of truth.
