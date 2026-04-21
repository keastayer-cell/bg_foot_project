do $$
begin
    if exists (select 1 from information_schema.schemata where schema_name = 'notify') then
        if exists (select 1 from information_schema.schemata where schema_name = 'mailer') then
            if exists (select 1 from information_schema.tables where table_schema = 'notify' and table_name = 'notification_template') then
                execute 'alter table notify.notification_template set schema mailer';
            end if;
            if exists (select 1 from information_schema.tables where table_schema = 'notify' and table_name = 'notification_event') then
                execute 'alter table notify.notification_event set schema mailer';
            end if;
            if exists (select 1 from information_schema.tables where table_schema = 'notify' and table_name = 'notification_event_log') then
                execute 'alter table notify.notification_event_log set schema mailer';
            end if;
            if exists (select 1 from information_schema.tables where table_schema = 'notify' and table_name = 'notification_delivery_log') then
                execute 'alter table notify.notification_delivery_log set schema mailer';
            end if;
            if exists (select 1 from information_schema.routines where routine_schema = 'notify' and routine_name = 'enqueue_event') then
                execute 'drop function if exists notify.enqueue_event(varchar, bigint, jsonb, bigint)';
            end if;
            execute 'drop schema if exists notify';
        else
            execute 'alter schema notify rename to mailer';
        end if;
    else
        execute 'create schema if not exists mailer';
    end if;
end;
$$;

comment on schema mailer is 'Схема почтового сервиса, очереди событий и логов отправки';

comment on table mailer.notification_template is 'Шаблоны писем и других уведомлений по типам событий';
comment on column mailer.notification_template.id is 'Технический идентификатор шаблона';
comment on column mailer.notification_template.code is 'Уникальный код шаблона, обычно совпадает с типом события';
comment on column mailer.notification_template.channel is 'Канал доставки уведомления';
comment on column mailer.notification_template.subject_template is 'Шаблон темы письма с макроподстановками';
comment on column mailer.notification_template.body_template is 'Шаблон тела письма с макроподстановками';
comment on column mailer.notification_template.body_format is 'Формат тела сообщения: TEXT или HTML';
comment on column mailer.notification_template.is_active is 'Признак активного шаблона';
comment on column mailer.notification_template.created_at is 'Дата и время создания шаблона';
comment on column mailer.notification_template.updated_at is 'Дата и время последнего обновления шаблона';

comment on table mailer.notification_event is 'Очередь событий уведомлений для обработки mailer-сервисом';
comment on column mailer.notification_event.id is 'Технический идентификатор события уведомления';
comment on column mailer.notification_event.event_type is 'Тип бизнес-события, которое должно быть обработано';
comment on column mailer.notification_event.template_code is 'Код шаблона, по которому нужно сформировать письмо';
comment on column mailer.notification_event.recipient_user_id is 'Идентификатор пользователя-получателя, если он известен в системе';
comment on column mailer.notification_event.recipient_email is 'Email получателя письма';
comment on column mailer.notification_event.recipient_name is 'Имя получателя для персонализации письма';
comment on column mailer.notification_event.payload_json is 'JSON-полезная нагрузка события для макроподстановок';
comment on column mailer.notification_event.status is 'Текущее состояние обработки события';
comment on column mailer.notification_event.attempt_count is 'Количество попыток отправки по событию';
comment on column mailer.notification_event.next_retry_at is 'Момент времени, после которого разрешена следующая попытка';
comment on column mailer.notification_event.locked_at is 'Время блокировки события обработчиком mailer';
comment on column mailer.notification_event.processing_started_at is 'Время начала текущей обработки события';
comment on column mailer.notification_event.processed_at is 'Время окончательной успешной или финальной неуспешной обработки';
comment on column mailer.notification_event.last_error is 'Текст последней ошибки обработки';
comment on column mailer.notification_event.created_by_user_id is 'Пользователь, от имени которого событие было поставлено в очередь';
comment on column mailer.notification_event.created_at is 'Дата и время создания события в очереди';

comment on table mailer.notification_event_log is 'Журнал смены состояний события уведомления';
comment on column mailer.notification_event_log.id is 'Технический идентификатор записи журнала события';
comment on column mailer.notification_event_log.event_id is 'Ссылка на событие уведомления';
comment on column mailer.notification_event_log.status is 'Состояние события на момент записи журнала';
comment on column mailer.notification_event_log.message is 'Служебное сообщение о переходе состояния';
comment on column mailer.notification_event_log.created_at is 'Дата и время записи в журнал события';

comment on table mailer.notification_delivery_log is 'Журнал фактических попыток отправки уведомлений';
comment on column mailer.notification_delivery_log.id is 'Технический идентификатор записи журнала отправки';
comment on column mailer.notification_delivery_log.event_id is 'Ссылка на событие уведомления';
comment on column mailer.notification_delivery_log.attempt_number is 'Номер попытки отправки';
comment on column mailer.notification_delivery_log.recipient_email is 'Email, на который выполнялась отправка';
comment on column mailer.notification_delivery_log.subject_rendered is 'Итоговая тема письма после рендеринга';
comment on column mailer.notification_delivery_log.body_rendered is 'Итоговое тело письма после рендеринга';
comment on column mailer.notification_delivery_log.transport_type is 'Тип транспорта, использованного для отправки';
comment on column mailer.notification_delivery_log.provider_message_id is 'Идентификатор сообщения на стороне SMTP-провайдера';
comment on column mailer.notification_delivery_log.status is 'Итог попытки отправки';
comment on column mailer.notification_delivery_log.error_text is 'Текст ошибки доставки, если попытка завершилась неуспешно';
comment on column mailer.notification_delivery_log.sent_at is 'Дата и время попытки отправки';

create or replace function mailer.enqueue_event(
    p_event_type varchar,
    p_recipient_user_id bigint default null,
    p_payload_json jsonb default '{}'::jsonb,
    p_created_by_user_id bigint default null
) returns bigint
language plpgsql
as $$
declare
    v_event_id bigint;
    v_template_code varchar(100);
    v_recipient_email varchar(255);
    v_recipient_name varchar(120);
begin
    select nt.code
      into v_template_code
      from mailer.notification_template nt
     where nt.code = p_event_type
       and nt.channel = 'EMAIL'
       and nt.is_active = true
     limit 1;

    if v_template_code is null then
        raise exception 'Активный шаблон для события % не найден', p_event_type;
    end if;

    if p_recipient_user_id is not null then
        select u.email, u.name
          into v_recipient_email, v_recipient_name
          from work.w_user_login u
         where u.id = p_recipient_user_id;
    end if;

    v_recipient_email := coalesce(v_recipient_email, nullif(trim(coalesce(p_payload_json ->> 'recipientEmail', '')), ''));
    v_recipient_name := coalesce(v_recipient_name, nullif(trim(coalesce(p_payload_json ->> 'recipientName', '')), ''));

    if v_recipient_email is null then
        raise exception 'Не удалось определить email получателя для события %', p_event_type;
    end if;

    insert into mailer.notification_event (
        event_type,
        template_code,
        recipient_user_id,
        recipient_email,
        recipient_name,
        payload_json,
        status,
        created_by_user_id
    ) values (
        p_event_type,
        v_template_code,
        p_recipient_user_id,
        v_recipient_email,
        v_recipient_name,
        coalesce(p_payload_json, '{}'::jsonb),
        'NEW',
        p_created_by_user_id
    )
    returning id into v_event_id;

    insert into mailer.notification_event_log (event_id, status, message)
    values (v_event_id, 'NEW', 'Событие поставлено в очередь уведомлений');

    return v_event_id;
end;
$$;

drop function if exists notify.enqueue_event(varchar, bigint, jsonb, bigint);
