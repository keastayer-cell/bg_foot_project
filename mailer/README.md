# Mailer Service

Отдельный сервис `mailer` обрабатывает события из таблицы `mailer.notification_event`, рендерит шаблоны из БД и отправляет письма.

## Режимы отправки

- `MAILER_TRANSPORT_TYPE=log`:
  письма не отправляются наружу; результат фиксируется в delivery log. В application log выводятся только маскированный адрес и размеры сообщения, без тела и reset-token.
- `MAILER_TRANSPORT_TYPE=smtp`:
  сервис реально отправляет письма через SMTP.

Профиль `test` по умолчанию использует `log`. Профиль `prod` по умолчанию использует `smtp` и не запустится без `MAILER_SMTP_USERNAME` и `MAILER_SMTP_PASSWORD`:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/football-stats-mailer-0.0.1-SNAPSHOT.jar
```

Для локальной разработки безопасным значением по умолчанию остаётся `log`.

## Очередь и повторные попытки

- События забираются атомарно через `FOR UPDATE SKIP LOCKED`.
- Один процесс обрабатывает события по одному, до `MAILER_BATCH_SIZE` за проход.
- Зависший `PROCESSING` возвращается в обработку после `MAILER_STALE_LOCK_SECONDS`.
- Запись результата разрешена только владельцу текущего `lock_token`.
- Retry использует exponential backoff от `MAILER_RETRY_DELAY_SECONDS` до `MAILER_MAX_RETRY_DELAY_SECONDS`.
- После `MAILER_RETRY_LIMIT` попыток событие получает статус `DEAD`.
- `deduplication_key` не позволяет повторно поставить одно бизнес-событие в очередь.

## Yandex SMTP

Под текущую конфигурацию сервиса заложен вариант для Яндекс 360 / Яндекс Почты:

- SMTP host: `smtp.yandex.ru`
- SMTP port: `465`
- Шифрование: `SSL`
- Логин: полный email ящика
- Пароль: пароль приложения Яндекс ID

Если нужен вариант на `587`, то переключите:

- `MAILER_SMTP_PORT=587`
- `MAILER_SMTP_SSL_ENABLE=false`
- `MAILER_SMTP_STARTTLS=true`

## Что нужно сделать в Яндексе

1. Включить доступ для почтовых клиентов в настройках ящика.
2. Проверить, что разрешены пароли приложений.
3. Создать отдельный пароль приложения для `mailer`.
4. Использовать полный адрес ящика в `MAILER_SMTP_USERNAME`.

## Локальный запуск

Используйте задачи VS Code:

- `Restart Mailer (8090)`
- `Rebuild + Restart Mailer (8090)`

Перед запуском заполните `.env` в папке `mailer` по примеру `.env.example`.

## Проверка

После старта mailer:

- health: `http://127.0.0.1:8090/actuator/health`
- liveness: `http://127.0.0.1:8090/actuator/health/liveness`
- readiness: `http://127.0.0.1:8090/actuator/health/readiness`
- новые события регистрации должны попадать в `mailer.notification_event`
- фактические попытки отправки отражаются в `mailer.notification_delivery_log`

Readiness проверяет состояние приложения, соединение с БД и чтение очереди. Компонент `mailerQueue` публикует количество `pending`, `processing`, `dead` и время самого старого ожидающего события.

Для разбора окончательно упавших событий:

```sql
select id, event_type, recipient_email, attempt_count, last_error, processed_at
from mailer.notification_event
where status = 'DEAD'
order by processed_at desc;
```

Возвращать событие из `DEAD` в очередь нужно только после устранения причины:

```sql
update mailer.notification_event
set status = 'FAILED',
    next_retry_at = now(),
    processed_at = null,
    locked_at = null,
    lock_token = null
where id = :event_id
  and status = 'DEAD';
```
