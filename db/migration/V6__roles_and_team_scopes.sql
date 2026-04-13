create table if not exists work.w_role (
    id bigserial primary key,
    code varchar(50) not null unique,
    name_ru varchar(120) not null,
    created_at timestamptz not null default now()
);

create table if not exists work.w_team (
    id bigserial primary key,
    name varchar(120) not null unique,
    short_name varchar(60),
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists work.w_user_role (
    id bigserial primary key,
    user_id bigint not null references work.w_user_login(id) on delete cascade,
    role_id bigint not null references work.w_role(id) on delete cascade,
    granted_by_user_id bigint references work.w_user_login(id),
    granted_at timestamptz not null default now(),
    active boolean not null default true
);

create table if not exists work.w_user_team_scope (
    id bigserial primary key,
    user_id bigint not null references work.w_user_login(id) on delete cascade,
    team_id bigint not null references work.w_team(id) on delete cascade,
    granted_by_user_id bigint references work.w_user_login(id),
    granted_at timestamptz not null default now(),
    valid_from timestamptz,
    valid_to timestamptz,
    can_edit_roster boolean not null default true,
    can_edit_application boolean not null default true,
    active boolean not null default true
);

create table if not exists work.w_auth_audit_log (
    id bigserial primary key,
    actor_user_id bigint references work.w_user_login(id),
    target_user_id bigint references work.w_user_login(id),
    action_code varchar(80) not null,
    payload_json jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create unique index if not exists ux_w_user_role_active
    on work.w_user_role(user_id, role_id)
    where active = true;

create unique index if not exists ux_w_user_team_scope_active
    on work.w_user_team_scope(user_id, team_id)
    where active = true;

insert into work.w_role(code, name_ru)
values
    ('SUPER_ADMIN', 'Супер администратор'),
    ('USER', 'Обычный пользователь'),
    ('TEAM_REP', 'Представитель команды')
on conflict (code) do nothing;

insert into work.w_user_role(user_id, role_id, granted_by_user_id, active)
select u.id, r.id, null, true
from work.w_user_login u
join work.w_role r on r.code = 'USER'
where not exists (
    select 1
    from work.w_user_role ur
    where ur.user_id = u.id and ur.role_id = r.id and ur.active = true
);

comment on table work.w_role is 'Справочник ролей доступа';
comment on column work.w_role.id is 'Идентификатор роли';
comment on column work.w_role.code is 'Код роли';
comment on column work.w_role.name_ru is 'Название роли на русском';
comment on column work.w_role.created_at is 'Дата создания роли';

comment on table work.w_team is 'Команды для разграничения прав представителей';
comment on column work.w_team.id is 'Идентификатор команды';
comment on column work.w_team.name is 'Полное название команды';
comment on column work.w_team.short_name is 'Короткое название команды';
comment on column work.w_team.active is 'Признак активности команды';
comment on column work.w_team.created_at is 'Дата создания команды';

comment on table work.w_user_role is 'Назначенные роли пользователей';
comment on column work.w_user_role.id is 'Идентификатор назначения роли';
comment on column work.w_user_role.user_id is 'Пользователь, которому назначили роль';
comment on column work.w_user_role.role_id is 'Назначенная роль';
comment on column work.w_user_role.granted_by_user_id is 'Кто назначил роль';
comment on column work.w_user_role.granted_at is 'Дата назначения роли';
comment on column work.w_user_role.active is 'Признак активного назначения';

comment on table work.w_user_team_scope is 'Область доступа представителя к команде';
comment on column work.w_user_team_scope.id is 'Идентификатор назначения области доступа';
comment on column work.w_user_team_scope.user_id is 'Пользователь-представитель';
comment on column work.w_user_team_scope.team_id is 'Команда доступа';
comment on column work.w_user_team_scope.granted_by_user_id is 'Кто выдал доступ к команде';
comment on column work.w_user_team_scope.granted_at is 'Дата выдачи доступа';
comment on column work.w_user_team_scope.valid_from is 'Начало срока действия';
comment on column work.w_user_team_scope.valid_to is 'Окончание срока действия';
comment on column work.w_user_team_scope.can_edit_roster is 'Можно редактировать состав';
comment on column work.w_user_team_scope.can_edit_application is 'Можно редактировать заявку';
comment on column work.w_user_team_scope.active is 'Признак активного доступа';

comment on table work.w_auth_audit_log is 'Журнал действий по правам доступа';
comment on column work.w_auth_audit_log.id is 'Идентификатор записи журнала';
comment on column work.w_auth_audit_log.actor_user_id is 'Кто выполнил действие';
comment on column work.w_auth_audit_log.target_user_id is 'Для какого пользователя выполнено действие';
comment on column work.w_auth_audit_log.action_code is 'Код действия';
comment on column work.w_auth_audit_log.payload_json is 'Детали действия в JSON';
comment on column work.w_auth_audit_log.created_at is 'Дата и время действия';
