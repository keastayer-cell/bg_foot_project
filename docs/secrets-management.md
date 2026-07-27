# Управление секретами

## Решение

Git хранит только имена переменных, безопасные примеры и проверяемую структуру. Настоящие DB/JWT/SMTP/SSH значения никогда не коммитятся.

Источник восстановления секретов для владельца проекта — отдельный password manager. Серверные env-файлы являются runtime-копией, а GitHub Environment хранит только SSH-доступ для deploy.

## Карта

| Среда | Где лежат значения |
|---|---|
| local | корневой `.env`, исключённый из Git |
| frontend local | `web/.env.local`; только публичные `VITE_*`, без секретов |
| test runtime | `/etc/bg-foot/test/common.env`, `app.env`, `mailer.env` |
| prod runtime | `/etc/bg-foot/prod/common.env`, `app.env`, `mailer.env` |
| GitHub test | Environment `test`: SSH secrets и `PUBLIC_BASE_URL` |
| GitHub prod | Environment `prod`: отдельные SSH secrets и `PUBLIC_BASE_URL` |

Сейчас test/prod серверов нет, поэтому runtime-файлы и новые GitHub Environment secrets ещё не созданы.

## Разделение серверных файлов

`common.env`:

- DB host/port/name/schema/user/password;
- backup directory и retention.

Его читают backend, mailer и backup timer. Пароль БД хранится в одном месте.

`app.env`:

- `APP_PROFILE`;
- JWT secret и сроки токенов;
- public URL, cookie и mailer trigger;
- bootstrap super-admin email.

`mailer.env`:

- transport `log`/`smtp`;
- SMTP login/application password;
- sender и параметры очереди.

Все три файла принадлежат `root:root`, mode `0600`. Java-сервисы получают значения через systemd и запускаются непривилегированным пользователем `bg-foot`.

## Local

```bash
cp .env.example .env
cp web/.env.example web/.env.local
```

Backend и mailer нужно запускать из корня репозитория. Оба читают `.env`; альтернативный путь задаётся через `BG_FOOT_ENV_FILE`.

`web/.env.local` попадает в browser bundle. В него нельзя помещать пароль БД, JWT secret, SMTP password или SSH key.

## Подготовка будущего сервера

Из checkout:

```bash
sudo bash scripts/prepare-server-environment.sh --env test
sudoedit /etc/bg-foot/test/common.env
sudoedit /etc/bg-foot/test/app.env
sudoedit /etc/bg-foot/test/mailer.env
sudo bash scripts/validate-server-environment.sh --env test
sudo bash scripts/install-service-units.sh --env test
sudo bash scripts/install-logging-policy.sh
sudo bash scripts/install-backup-policy.sh
```

Для production замените `test` на `prod`. Скрипт подготовки не перезаписывает существующие файлы.

## GitHub Environment

Для test deploy создаётся Environment `test`:

- `VPS_HOST` — адрес сервера;
- `VPS_USER` — SSH user;
- `VPS_SSH_KEY` — отдельный deploy private key;
- `VPS_HOST_FINGERPRINT` — fingerprint host key;
- `PUBLIC_BASE_URL` — environment variable, например `https://test.example.com`.

Для production используются те же имена внутри отдельного Environment `prod`. Благодаря этому workflow не содержит названий конкретных серверов.

Парольный SSH-доступ не используется. Deploy key не должен совпадать с личным SSH-ключом владельца.

Старые repository secrets `TEST_VPS_HOST`, `TEST_VPS_USER`, `TEST_VPS_PASSWORD` новым workflow не используются. После настройки Environment их следует удалить.

## Создание и ротация

- DB password, JWT secret и deploy key генерируются отдельно для каждой среды.
- SMTP использует application password, не пароль от почтового аккаунта.
- После подозрения на утечку секрет сначала меняется у провайдера/в БД, затем в password manager и runtime-файле.
- После смены DB/JWT/SMTP выполняется restart соответствующих systemd services и smoke-check.
- После смены SSH key обновляется только GitHub Environment и `authorized_keys`.

Никогда не отправляйте значения секретов в issue, PR, CI log или чат.

## Автоматические проверки

`scripts/validate-secret-layout.sh` запрещает tracked runtime `.env`, private keys и распространённые token/key signatures.

`scripts/validate-server-environment.sh` проверяет:

- наличие трёх файлов;
- владельца и mode `0600`;
- обязательные переменные;
- соответствие `APP_PROFILE` среде;
- отсутствие placeholder values;
- SMTP credentials при режиме `smtp`.
