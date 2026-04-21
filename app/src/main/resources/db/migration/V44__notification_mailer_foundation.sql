create schema if not exists notify;

create table if not exists notify.notification_template (
    id bigserial primary key,
    code varchar(100) not null,
    channel varchar(20) not null default 'EMAIL',
    subject_template text not null,
    body_template text not null,
    body_format varchar(10) not null default 'TEXT',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_notify_notification_template_code unique (code),
    constraint chk_notify_notification_template_channel check (channel in ('EMAIL')),
    constraint chk_notify_notification_template_body_format check (body_format in ('TEXT', 'HTML'))
);

create table if not exists notify.notification_event (
    id bigserial primary key,
    event_type varchar(100) not null,
    template_code varchar(100) not null,
    recipient_user_id bigint,
    recipient_email varchar(255) not null,
    recipient_name varchar(120),
    payload_json jsonb not null default '{}'::jsonb,
    status varchar(20) not null default 'NEW',
    attempt_count integer not null default 0,
    next_retry_at timestamptz not null default now(),
    locked_at timestamptz,
    processing_started_at timestamptz,
    processed_at timestamptz,
    last_error text,
    created_by_user_id bigint,
    created_at timestamptz not null default now(),
    constraint fk_notify_notification_event_template_code
        foreign key (template_code) references notify.notification_template (code),
    constraint fk_notify_notification_event_recipient_user
        foreign key (recipient_user_id) references work.w_user_login (id),
    constraint fk_notify_notification_event_created_by_user
        foreign key (created_by_user_id) references work.w_user_login (id),
    constraint chk_notify_notification_event_status
        check (status in ('NEW', 'PROCESSING', 'FAILED', 'SENT', 'DEAD'))
);

create index if not exists idx_notify_notification_event_status_retry
    on notify.notification_event (status, next_retry_at, created_at);

create index if not exists idx_notify_notification_event_recipient_user_id
    on notify.notification_event (recipient_user_id);

create table if not exists notify.notification_event_log (
    id bigserial primary key,
    event_id bigint not null,
    status varchar(20) not null,
    message text,
    created_at timestamptz not null default now(),
    constraint fk_notify_notification_event_log_event
        foreign key (event_id) references notify.notification_event (id) on delete cascade
);

create index if not exists idx_notify_notification_event_log_event_id
    on notify.notification_event_log (event_id, created_at);

create table if not exists notify.notification_delivery_log (
    id bigserial primary key,
    event_id bigint not null,
    attempt_number integer not null,
    recipient_email varchar(255) not null,
    subject_rendered text,
    body_rendered text,
    transport_type varchar(20) not null,
    provider_message_id varchar(255),
    status varchar(20) not null,
    error_text text,
    sent_at timestamptz not null default now(),
    constraint fk_notify_notification_delivery_log_event
        foreign key (event_id) references notify.notification_event (id) on delete cascade,
    constraint chk_notify_notification_delivery_log_status
        check (status in ('SENT', 'FAILED', 'DEAD'))
);

create index if not exists idx_notify_notification_delivery_log_event_id
    on notify.notification_delivery_log (event_id, sent_at desc);

create or replace function notify.enqueue_event(
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
      from notify.notification_template nt
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

    insert into notify.notification_event (
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

    insert into notify.notification_event_log (event_id, status, message)
    values (v_event_id, 'NEW', 'Событие поставлено в очередь уведомлений');

    return v_event_id;
end;
$$;

insert into notify.notification_template (
    code,
    channel,
    subject_template,
    body_template,
    body_format,
    is_active
) values (
    'USER_REGISTERED',
    'EMAIL',
    'Добро пожаловать в Football Stats',
    'Добрый день! Рады что Вы с нами. Если вы представитель команды, обратитесь к Администратору ресурса, для назначения.',
    'TEXT',
    true
)
on conflict (code) do update
set channel = excluded.channel,
    subject_template = excluded.subject_template,
    body_template = excluded.body_template,
    body_format = excluded.body_format,
    is_active = excluded.is_active,
    updated_at = now();

comment on schema notify is 'Схема уведомлений и очереди событий для отдельного mailer-сервиса';
comment on table notify.notification_template is 'Шаблоны уведомлений по типам событий';
comment on table notify.notification_event is 'Очередь событий уведомлений для обработки mailer-сервисом';
comment on table notify.notification_event_log is 'Журнал смены состояний события уведомления';
comment on table notify.notification_delivery_log is 'Журнал фактических попыток отправки уведомлений';
