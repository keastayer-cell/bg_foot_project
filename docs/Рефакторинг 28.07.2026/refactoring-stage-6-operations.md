# Этап 6. CI/CD и эксплуатация

Дата выполнения: 2026-07-28.

## CI

- Добавлен PR/push workflow для backend, mailer и frontend.
- Frontend получил ESLint и production dependency audit; удалены обнаруженные lint'ом неиспользуемые остатки после декомпозиции.
- Flyway проверяется статически, на append-only историю и реальным запуском backend на чистом PostgreSQL 16.
- Эксплуатационные shell-скрипты проходят syntax check и изолированные тесты.

## Test deploy

- Deploy больше не собирает backend с `-DskipTests`.
- В release входят backend, mailer и frontend.
- Перед изменением runtime создаётся DB backup и копия предыдущих артефактов.
- Flyway migrations применяются backend при старте и подтверждаются health-check.
- При ошибке автоматически восстанавливаются предыдущие jars и frontend.
- Database rollback оставлен явной ручной операцией, потому что автоматический откат Flyway небезопасен.
- Локальная и публичная post-deploy проверка вынесена в `scripts/smoke-check.sh`.

## Логи

- Добавлены journald limits, retention, compression и per-service rate limit.
- Добавлен logrotate для Nginx.
- Профили backend `test`/`prod` отключают SQL/bind debug.
- Скрипты не печатают credentials; backend error log не пишет exception message/stack, mailer application log не содержит тела письма и reset-token.

## Backup/restore

- `db-backup.sh`: test/prod, custom dump, SHA-256, retention, закрытые права.
- `db-restore.sh`: checksum, явное подтверждение среды, дополнительная защита production.
- Добавлены systemd service/timer и установщик ежедневных backup.
- Процедуры backup, пробного restore, downtime и post-restore smoke описаны в `docs/operations.md`.

## Остаточный риск

`npm audit --omit=dev` не находит production-уязвимостей. Полный audit сообщает advisory в dev toolchain Vite 5/ESLint 9; автоматическое исправление требует breaking upgrade Vite 8/ESLint 10. Этот major-upgrade не смешивался с эксплуатационным этапом и должен выполняться отдельно с полной frontend-регрессией.

## Унификация секретов

- Local backend/mailer используют один корневой `.env`.
- Серверы используют `/etc/bg-foot/<env>/common.env`, `app.env`, `mailer.env`.
- Backend, mailer и backup читают один `common.env`, без копирования DB password.
- Добавлены instance-based systemd units для `test`/`prod`.
- GitHub Environment хранит только SSH key/fingerprint; DB/JWT/SMTP остаются на сервере.
- CI запрещает tracked runtime env/private keys и проверяет secret contract.
