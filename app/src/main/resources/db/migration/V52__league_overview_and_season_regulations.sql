create table if not exists work.w_league_official (
    id bigserial primary key,
    full_name varchar(160) not null,
    position_title varchar(160) not null,
    bio text,
    photo_media_id bigint,
    sort_order integer not null default 100,
    active boolean not null default true,
    created_by_user_id bigint,
    updated_by_user_id bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ix_w_league_official_active_sort
    on work.w_league_official (active, sort_order, id);

create table if not exists work.w_league_venue (
    id bigserial primary key,
    name varchar(160) not null,
    short_label varchar(24),
    address varchar(255) not null,
    description text,
    photo_media_id bigint,
    sort_order integer not null default 100,
    active boolean not null default true,
    created_by_user_id bigint,
    updated_by_user_id bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ix_w_league_venue_active_sort
    on work.w_league_venue (active, sort_order, id);

alter table work.w_season
    add column if not exists regulation_media_id bigint,
    add column if not exists regulation_updated_at timestamptz;

comment on table work.w_league_official is 'Публичный справочник руководства лиги';
comment on table work.w_league_venue is 'Публичный справочник мест проведения турниров';
comment on column work.w_season.regulation_media_id is 'Последний активный PDF-файл положения сезона';
comment on column work.w_season.regulation_updated_at is 'Когда положение сезона было загружено или обновлено';

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'GET', '/api/league/overview', true
from work.w_role r
where r.code in ('USER', 'GUEST', 'TEAM_REP', 'REFEREE')
  and not exists (
    select 1 from work.w_api_access_rule ar
    where ar.role_id = r.id
      and ar.http_method = 'GET'
      and ar.url_pattern = '/api/league/overview'
  );

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, 'GET', '/api/seasons/*/regulation/pdf', true
from work.w_role r
where r.code in ('USER', 'GUEST', 'TEAM_REP', 'REFEREE')
  and not exists (
    select 1 from work.w_api_access_rule ar
    where ar.role_id = r.id
      and ar.http_method = 'GET'
      and ar.url_pattern = '/api/seasons/*/regulation/pdf'
  );

insert into work.w_api_access_rule (role_id, http_method, url_pattern, active)
select r.id, ar.http_method, ar.url_pattern, true
from work.w_role r
cross join (
    values
      ('GET', '/api/admin/league/officials'),
      ('POST', '/api/admin/league/officials'),
      ('PUT', '/api/admin/league/officials/*'),
      ('DELETE', '/api/admin/league/officials/*'),
      ('GET', '/api/admin/league/venues'),
      ('POST', '/api/admin/league/venues'),
      ('PUT', '/api/admin/league/venues/*'),
      ('DELETE', '/api/admin/league/venues/*'),
      ('PUT', '/api/admin/league/seasons/*/regulation'),
      ('DELETE', '/api/admin/league/seasons/*/regulation')
) as ar(http_method, url_pattern)
where r.code = 'REFEREE'
  and not exists (
    select 1 from work.w_api_access_rule existing
    where existing.role_id = r.id
      and existing.http_method = ar.http_method
      and existing.url_pattern = ar.url_pattern
  );