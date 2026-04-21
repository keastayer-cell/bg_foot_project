# Mailer Service

Отдельный сервис `mailer` обрабатывает события из таблицы `mailer.notification_event`, рендерит шаблоны из БД и отправляет письма.

## Режимы отправки

- `MAILER_TRANSPORT_TYPE=log`:
  письма не отправляются наружу, а только пишутся в лог и в delivery log в БД.
- `MAILER_TRANSPORT_TYPE=smtp`:
  сервис реально отправляет письма через SMTP.

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
- новые события регистрации должны попадать в `mailer.notification_event`
- фактические попытки отправки отражаются в `mailer.notification_delivery_log`
