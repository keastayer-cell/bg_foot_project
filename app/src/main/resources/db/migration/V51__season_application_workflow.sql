create table if not exists work.w_season_application (
    id bigserial primary key,
    season_id bigint not null references work.w_season(id),
    team_id bigint not null references work.w_team(id),
    representative_user_id bigint references work.w_user_login(id),
    status varchar(32) not null,
    submitted_at timestamptz,
    decision_at timestamptz,
    decision_by_user_id bigint references work.w_user_login(id),
    decision_comment text,
    created_by_user_id bigint references work.w_user_login(id),
    created_at timestamptz not null default now(),
    updated_by_user_id bigint references work.w_user_login(id),
    updated_at timestamptz not null default now(),
    constraint uq_w_season_application unique (season_id, team_id),
    constraint chk_w_season_application_status check (status in ('DRAFT', 'SUBMITTED', 'RETURNED', 'APPROVED', 'REJECTED'))
);

create index if not exists ix_w_season_application_status
    on work.w_season_application(status, updated_at desc);

create index if not exists ix_w_season_application_team
    on work.w_season_application(team_id, season_id);

create table if not exists work.w_season_application_player (
    id bigserial primary key,
    application_id bigint not null references work.w_season_application(id) on delete cascade,
    player_id bigint not null references work.w_player(id),
    created_by_user_id bigint references work.w_user_login(id),
    created_at timestamptz not null default now(),
    constraint uq_w_season_application_player unique (application_id, player_id)
);

create index if not exists ix_w_season_application_player_app
    on work.w_season_application_player(application_id);

comment on table work.w_season_application is 'Заявка команды на участие в сезоне с модерацией со стороны рефери/админа';
comment on table work.w_season_application_player is 'Игроки, включённые в сезонную заявку команды';

insert into mailer.notification_template (
    code,
    channel,
    subject_template,
    body_template,
    body_format,
    is_active
)
values
(
    'SEASON_APPLICATION_SUBMITTED_TO_REFEREE',
    'EMAIL',
    'Новая заявка на сезон «{{seasonName}}» от команды «{{teamName}}»',
    '<p>Добрый день.</p><p>Представитель команды "{{teamName}}" отправил в вашу сторону заявку на сезон "{{seasonName}}". Просим проверить заявку и вынести решение о допуске.</p>',
    'HTML',
    true
),
(
    'SEASON_APPLICATION_APPROVED',
    'EMAIL',
    'Заявка на сезон «{{seasonName}}» одобрена',
    '<p>Добрый день.</p><p>Ваша заявка на сезон "{{seasonName}}" одобрена. Ваша команда допущена до участия в турнире.</p>',
    'HTML',
    true
),
(
    'SEASON_APPLICATION_RETURNED',
    'EMAIL',
    'Заявка на сезон «{{seasonName}}» возвращена на доработку',
    '<p>Добрый день.</p><p>Ваша заявка на сезон "{{seasonName}}" возвращена на доработку. Пройдите в личный кабинет, чтобы узнать замечания и внести правки.</p><p>{{decisionComment}}</p>',
    'HTML',
    true
),
(
    'SEASON_APPLICATION_REJECTED',
    'EMAIL',
    'Заявка на сезон «{{seasonName}}» отклонена',
    '<p>Добрый день.</p><p>Ваша заявка на сезон "{{seasonName}}" отклонена. Пройдите в личный кабинет чтобы узнать причины.</p><p>{{decisionComment}}</p>',
    'HTML',
    true
)
on conflict (code) do update
set subject_template = excluded.subject_template,
    body_template = excluded.body_template,
    body_format = excluded.body_format,
    is_active = excluded.is_active,
    updated_at = now();

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'POST', '/api/team-rep/seasons/*/submit', true
from work.w_role r
where r.code = 'TEAM_REP'
on conflict do nothing;

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'GET', '/api/season-applications', true
from work.w_role r
where r.code = 'REFEREE'
on conflict do nothing;

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'GET', '/api/season-applications/*', true
from work.w_role r
where r.code = 'REFEREE'
on conflict do nothing;

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'POST', '/api/season-applications/*/approve', true
from work.w_role r
where r.code = 'REFEREE'
on conflict do nothing;

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'POST', '/api/season-applications/*/return', true
from work.w_role r
where r.code = 'REFEREE'
on conflict do nothing;

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'POST', '/api/season-applications/*/reject', true
from work.w_role r
where r.code = 'REFEREE'
on conflict do nothing;
