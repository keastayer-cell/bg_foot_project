# Refactoring Stage 3 Frontend

Дата проверки: 2026-07-27

## Цель этапа

Начать безопасную декомпозицию frontend-кода без изменения пользовательского поведения.

## Что сделано

### API debug logging

- Логика debug-логирования API-запросов вынесена из `web/src/main.js` в `web/src/utils/apiDebugLogger.js`.
- Глобальный monkey-patch `window.fetch` больше не включается по умолчанию.
- Для локальной диагностики логирование включается явно:

```bash
VITE_DEBUG_API_LOGS=true
```

### Router auth guard

- Проверка доступа к маршрутам вынесена из `web/src/router/index.js` в `web/src/router/authGuard.js`.
- `router/index.js` теперь отвечает в основном за декларацию маршрутов, а auth-логика стала отдельным модулем.

## Почему это первый шаг

Крупные страницы `Admin.vue`, `Tours.vue`, `Match.vue`, `TeamRepDashboard.vue` требуют декомпозиции, но резать их без e2e-тестов рискованно. Этот этап убирает глобальный production-риск и отделяет routing/auth boundary, не трогая сложную бизнес-разметку страниц.

## Команды проверки

```bash
npm --prefix web run build
mvn -f app/pom.xml test
mvn -f mailer/pom.xml test
```

## Результат проверки

- Frontend production build проходит успешно.
- Backend `app`: 5 тестов, 0 failures, 0 errors.
- Mailer: 2 теста, 0 failures, 0 errors.

## Что осталось на следующие шаги

- Добавить frontend тестовый раннер или e2e smoke.
- После этого начать декомпозицию `Admin.vue` по вкладкам и `Tours.vue` по composables.
