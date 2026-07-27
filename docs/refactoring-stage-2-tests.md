# Refactoring Stage 2 Tests

Дата проверки: 2026-07-27

## Цель этапа

Добавить первую тестовую сетку для критичных частей проекта, чтобы дальнейший рефакторинг начинался не вслепую.

## Что покрыто

### Backend `app`

- `JwtServiceTest`
  - генерация и парсинг пользовательского JWT;
  - guest token с ролью `GUEST`;
  - отказ запуска сервиса с коротким `JWT_SECRET`.

- `AuthRateLimitFilterTest`
  - лимит `/api/auth/login`: первые 10 запросов проходят, следующий получает `429`;
  - нерелевантные POST endpoint'ы не ограничиваются auth rate limiter'ом.

### Mailer

- `TemplateRendererTest`
  - подстановка переменных из payload и event metadata;
  - ошибка при незаполненных макросах.

## Команды проверки

```bash
mvn -f app/pom.xml test
mvn -f mailer/pom.xml test
npm --prefix web run build
```

## Результат проверки

- Backend `app`: 5 тестов, 0 failures, 0 errors.
- Mailer: 2 теста, 0 failures, 0 errors.
- Frontend production build проходит успешно.

## Что осталось на следующие шаги

- Добавить Spring MVC/security тесты для ролей и `ApiAccessRuleFilter`.
- Добавить тесты бизнес-логики заявок, трансферов, таблицы и протоколов матчей.
- Добавить frontend тестовый раннер или e2e smoke для ключевых пользовательских путей.
