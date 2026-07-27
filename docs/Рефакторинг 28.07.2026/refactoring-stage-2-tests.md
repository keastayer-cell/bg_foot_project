# Refactoring Stage 2 Tests

Дата проверки: 2026-07-27

## Цель этапа

Защитить критичные auth, access-control и бизнес-сценарии до дальнейшего рефакторинга.

## Backend

- `ApplicationSmokeTest`: Spring context стартует, `/api/health` возвращает `UP`.
- `AuthServiceTest`: login, password reset, одноразовый reset token, `tokenVersion`.
- `RefreshTokenServiceTest`: issue, rotation, logout/revoke, устаревшая версия сессии.
- `ApiAccessRuleServiceTest`: `SUPER_ADMIN`, `REFEREE`, `TEAM_REP`, `GUEST`, HTTP methods и URL patterns.
- `SeasonApplicationServiceTest`: утверждение заявки и синхронизация дозаявки.
- `TeamRepTransferServiceTest`: подтверждение трансфера и перенос игрока.
- `SeasonStandingsServiceTest`: очки и позиции по подтверждённому протоколу.
- Сохранены ранее добавленные `JwtServiceTest` и `AuthRateLimitFilterTest`.

Для smoke-теста добавлен профиль `application-test.yml` с изолированной H2 в PostgreSQL compatibility mode. Production datasource и Flyway этим профилем не затрагиваются.

## Frontend

- 55 Vitest unit/route-тестов.
- Playwright smoke:
  - главная страница;
  - login через реальную форму;
  - `/admin` для `SUPER_ADMIN`;
  - `/team-rep-dashboard` для `TEAM_REP`.

E2E запускает реальный Vite/Vue frontend, а backend API перехватывается на уровне браузера. Это делает smoke воспроизводимым без локальной PostgreSQL.

## Mailer

- 2 теста `TemplateRendererTest`.
- Чистая Maven package-сборка.

## Команды проверки

```bash
mvn -f app/pom.xml clean test
mvn -f mailer/pom.xml clean test package
npm --prefix web test
npm --prefix web run build
npm --prefix web run test:e2e
```

## Результат

- Backend: 18 тестов, 0 failures, 0 errors.
- Frontend: 55 unit/route + 4 e2e, все проходят.
- Mailer: 2 теста, 0 failures, 0 errors.
- Backend, frontend и mailer собираются успешно.

## Статус

Этап завершён. Все пять пунктов исходного чек-листа подтверждены автоматическими проверками.
