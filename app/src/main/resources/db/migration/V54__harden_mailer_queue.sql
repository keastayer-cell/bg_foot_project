ALTER TABLE mailer.notification_event
    ADD COLUMN IF NOT EXISTS deduplication_key varchar(255),
    ADD COLUMN IF NOT EXISTS lock_token varchar(36);

CREATE UNIQUE INDEX IF NOT EXISTS ux_mailer_notification_event_deduplication_key
    ON mailer.notification_event (deduplication_key)
    WHERE deduplication_key IS NOT NULL;

DELETE FROM mailer.notification_delivery_log duplicate
USING mailer.notification_delivery_log retained
WHERE duplicate.event_id = retained.event_id
  AND duplicate.attempt_number = retained.attempt_number
  AND duplicate.id > retained.id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_mailer_notification_delivery_attempt
    ON mailer.notification_delivery_log (event_id, attempt_number);

COMMENT ON COLUMN mailer.notification_event.deduplication_key IS
    'Устойчивый ключ бизнес-события, предотвращающий повторную постановку в очередь';
COMMENT ON COLUMN mailer.notification_event.lock_token IS
    'Токен обработчика, владеющего текущей блокировкой события';

DROP FUNCTION IF EXISTS mailer.enqueue_event(varchar, bigint, jsonb, bigint);

CREATE FUNCTION mailer.enqueue_event(
    p_event_type varchar,
    p_recipient_user_id bigint DEFAULT NULL,
    p_payload_json jsonb DEFAULT '{}'::jsonb,
    p_created_by_user_id bigint DEFAULT NULL,
    p_deduplication_key varchar DEFAULT NULL
) RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    v_event_id bigint;
    v_template_code varchar(100);
    v_recipient_email varchar(255);
    v_recipient_name varchar(120);
    v_deduplication_key varchar(255);
BEGIN
    SELECT nt.code
      INTO v_template_code
      FROM mailer.notification_template nt
     WHERE nt.code = p_event_type
       AND nt.channel = 'EMAIL'
       AND nt.is_active = true
     LIMIT 1;

    IF v_template_code IS NULL THEN
        RAISE EXCEPTION 'Активный шаблон для события % не найден', p_event_type;
    END IF;

    IF p_recipient_user_id IS NOT NULL THEN
        SELECT u.email, u.name
          INTO v_recipient_email, v_recipient_name
          FROM work.w_user_login u
         WHERE u.id = p_recipient_user_id;
    END IF;

    v_recipient_email := coalesce(v_recipient_email, nullif(trim(coalesce(p_payload_json ->> 'recipientEmail', '')), ''));
    v_recipient_name := coalesce(v_recipient_name, nullif(trim(coalesce(p_payload_json ->> 'recipientName', '')), ''));
    v_deduplication_key := nullif(trim(coalesce(p_deduplication_key, '')), '');

    IF v_recipient_email IS NULL THEN
        RAISE EXCEPTION 'Не удалось определить email получателя для события %', p_event_type;
    END IF;

    INSERT INTO mailer.notification_event (
        event_type,
        template_code,
        recipient_user_id,
        recipient_email,
        recipient_name,
        payload_json,
        status,
        created_by_user_id,
        deduplication_key
    ) VALUES (
        p_event_type,
        v_template_code,
        p_recipient_user_id,
        v_recipient_email,
        v_recipient_name,
        coalesce(p_payload_json, '{}'::jsonb),
        'NEW',
        p_created_by_user_id,
        v_deduplication_key
    )
    ON CONFLICT (deduplication_key) WHERE deduplication_key IS NOT NULL
    DO NOTHING
    RETURNING id INTO v_event_id;

    IF v_event_id IS NULL THEN
        SELECT e.id
          INTO v_event_id
          FROM mailer.notification_event e
         WHERE e.deduplication_key = v_deduplication_key;
        RETURN v_event_id;
    END IF;

    INSERT INTO mailer.notification_event_log (event_id, status, message)
    VALUES (v_event_id, 'NEW', 'Событие поставлено в очередь уведомлений');

    RETURN v_event_id;
END;
$$;
