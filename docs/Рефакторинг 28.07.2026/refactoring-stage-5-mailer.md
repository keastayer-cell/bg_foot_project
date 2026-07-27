# Этап 5. Mailer и уведомления

Дата выполнения: 2026-07-27.

## Очередь

- Исправлен stale lock: зависшие события `PROCESSING` снова доступны после таймаута.
- Очередь по-прежнему использует атомарный `FOR UPDATE SKIP LOCKED`.
- События claim'ятся по одному, поэтому долгое письмо не состаривает блокировку остальных элементов batch.
- Claim получает уникальный `lock_token`; `SENT`/`FAILED` может записать только текущий владелец.
- Переход события и event/delivery logs выполняются в одной транзакции.
- Delivery attempt защищён уникальностью `(event_id, attempt_number)`.
- Retry стал экспоненциальным и ограничивается `MAILER_MAX_RETRY_DELAY_SECONDS`.
- После лимита событие переходит в `DEAD`; процедура ручного возврата описана в `mailer/README.md`.

## Дедупликация

Миграция `V54__harden_mailer_queue.sql` добавляет `deduplication_key` и уникальный частичный индекс. Backend формирует устойчивые ключи для регистрации, выдачи ролей, password reset и каждой версии решения по сезонной заявке. Повторный enqueue возвращает ID уже существующего события и не создаёт второе письмо.

Reset-token не попадает в deduplication key: используется SHA-256 от ссылки.

## Health и режимы

- `/actuator/health/liveness` проверяет состояние процесса.
- `/actuator/health/readiness` включает readiness state, datasource и `mailerQueue`.
- `mailerQueue` показывает pending/processing/dead и возраст очереди, а при недоступной БД возвращает `DOWN`.
- Профиль `test` по умолчанию использует `log`.
- Профиль `prod` по умолчанию использует `smtp` и требует SMTP username/password.
- `log` transport больше не пишет полное письмо, email или reset-link в application log.

## Шаблоны и тесты

- Сохранены тесты подстановки макросов и ошибки незаполненного макроса.
- Добавлен тест HTML escaping пользовательских значений.
- Добавлены тесты успешной доставки, retry, exponential backoff, перехода в `DEAD`, stale reclaim, readiness и проверки SMTP-конфигурации.
- Отдельные backend-тесты фиксируют передачу deduplication key и отсутствие reset-token в ключе.
