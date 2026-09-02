# Эксплуатация BG Foot

## CI

`.github/workflows/ci.yml` запускается для pull request в `dev`, `test`, `prod` и для push в `dev`.

Актуальная модель веток: `dev -> test -> prod`.

- `dev` — разработка и обязательный CI;
- `test` — автоматически разворачиваемая версия тестового окружения;
- `prod` — история принятых production-релизов.

Ветка `main` не используется, поскольку в этой модели она дублировала бы назначение `prod`.

Проверяются:

- backend tests/package на Java 21;
- mailer tests/package на Java 21;
- frontend ESLint, Vitest и production build на Node.js 20;
- последовательность Flyway-файлов и append-only история миграций;
- реальное применение всех Flyway migrations на чистом PostgreSQL 16;
- синтаксис и изолированные тесты эксплуатационных shell-скриптов.

Опубликованные `V*.sql` не редактируются. Исправление структуры всегда оформляется новой миграцией с очередным номером.

## Test deploy

Push в `test` запускает `.github/workflows/deploy-test.yml`.

До изменения runtime workflow:

1. повторно выполняет tests/lint/build;
2. проверяет структуру Flyway migrations;
3. собирает release с backend, mailer и frontend;
4. на сервере создаёт PostgreSQL backup;
5. сохраняет предыдущие jars и frontend;
6. устанавливает политику journald/logrotate;
7. перезапускает backend, который применяет Flyway migrations;
8. перезапускает mailer и проверяет backend/frontend/mailer.

Test-сервер должен содержать:

- `/etc/bg-foot/test/common.env`, `app.env`, `mailer.env`;
- systemd units `football-stats-app@test` и `football-stats-mailer@test`;
- Nginx, обслуживающий `/var/www/football-stats-web`;
- PostgreSQL client tools (`pg_dump`, `pg_restore`);
- каталоги `/opt/football-stats-app`, `/opt/football-stats-mailer`, `/var/backups/bg-foot`.

При провале smoke-check deploy автоматически возвращает предыдущие jars и frontend. Flyway migrations остаются применёнными: они должны быть backward-compatible с предыдущим backend.

В deploy log печатаются:

- каталог предыдущих runtime-артефактов;
- путь к backup БД;
- release SHA.

Database restore не запускается автоматически. Это отдельная аварийная операция.
Runtime-копии старше 30 дней и database backup старше заданного retention удаляются автоматически.

## Smoke-check

Публичная проверка:

```bash
bash scripts/smoke-check.sh \
  --base-url https://test.example.ru \
  --retries 15
```

Локальная проверка на сервере с mailer:

```bash
bash scripts/smoke-check.sh \
  --base-url http://127.0.0.1 \
  --mailer-url http://127.0.0.1:8090
```

Скрипт проверяет `/api/health`, frontend `/` и, если передан mailer URL, `/actuator/health/readiness`.

## Backup

Ручной backup одинаков для test и production; среда указывается явно:

```bash
DB_HOST=127.0.0.1 \
DB_PORT=5432 \
DB_NAME=football_db \
DB_USER=football_app \
DB_PASSWORD='set-in-secret-storage' \
bash scripts/db-backup.sh --env test
```

Backup создаётся в custom format, получает SHA-256 checksum и права согласно `umask 077`. По умолчанию файлы хранятся 14 дней в `/var/backups/bg-foot/<env>`.

Для ежедневного запуска подготовьте `/etc/bg-foot/test/common.env` и/или `/etc/bg-foot/prod/common.env`:

```dotenv
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=football_db
DB_SCHEMA=work
DB_USER=football_app
DB_PASSWORD=replace-with-secret
BACKUP_DIR=/var/backups/bg-foot
BACKUP_RETENTION_DAYS=14
```

Файл должен принадлежать root и иметь mode `0600`. Он является тем же `common.env`, который читают backend и mailer. Затем из checkout:

```bash
sudo bash scripts/install-backup-policy.sh
systemctl list-timers 'bg-foot-db-backup@*'
```

Таймер запускается ежедневно с random delay. Наличие файла и checksum нужно мониторить отдельно; backup считается проверенным только после пробного restore в изолированную БД.

## Restore

Restore заменяет существующие объекты и требует downtime. Сначала остановите backend и mailer, затем укажите среду дважды:

```bash
sudo systemctl stop football-stats-app@test football-stats-mailer@test

DB_HOST=127.0.0.1 \
DB_PORT=5432 \
DB_NAME=football_db \
DB_USER=football_app \
DB_PASSWORD='set-in-secret-storage' \
bash scripts/db-restore.sh \
  --env test \
  --backup /var/backups/bg-foot/test/football_db_YYYYMMDDTHHMMSSZ.dump \
  --confirm-env test \
  --yes
```

Production дополнительно требует `ALLOW_PRODUCTION_RESTORE=yes`. После restore запустите сервисы и выполните smoke-check.

## Логи

Установка политики:

```bash
sudo bash scripts/install-logging-policy.sh
```

Она задаёт:

- journald: максимум `500M` persistent и `100M` runtime, retention 14 дней, compression;
- rate limit отдельно для backend и mailer;
- fallback Nginx rotation ежедневно, не более 14 архивов, немедленную ротацию при `50M`;
- вывод Java-сервисов только в journald.

Если сервер уже содержит системную Nginx logrotate policy для `/var/log/nginx/access.log`, установщик сохраняет её, чтобы не создавать дублирующее правило, и проверяет её синтаксис. Репозиторная policy используется как fallback.

Профили backend `test` и `prod` держат Hibernate SQL/bind logging на уровне `WARN`. Необработанные backend-ошибки фиксируются по request ID и типу исключения без exception message/stack. Mailer log transport не пишет тело письма, полный email или reset-token. Секреты хранятся только в env-файлах с mode `0600`; shell-скрипты не используют `set -x` и не выводят значения DB/SMTP/JWT credentials.

Диагностика с ограниченным объёмом:

```bash
journalctl -u football-stats-app@test --since '-30 min' -n 300 --no-pager
journalctl -u football-stats-mailer@test --since '-30 min' -n 300 --no-pager
journalctl --disk-usage
```
