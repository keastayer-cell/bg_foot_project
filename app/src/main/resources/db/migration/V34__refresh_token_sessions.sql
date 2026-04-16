create table if not exists work.w_refresh_token_session (
    id bigserial primary key,
    user_id bigint not null references work.w_user_login(id) on delete cascade,
    token_hash varchar(128) not null unique,
    token_version integer not null default 0,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    last_used_at timestamptz,
    revoked_at timestamptz,
    replaced_by_token_hash varchar(128),
    user_agent varchar(255),
    ip_address varchar(64)
);

create index if not exists idx_w_refresh_token_session_user on work.w_refresh_token_session(user_id);
create index if not exists idx_w_refresh_token_session_expires on work.w_refresh_token_session(expires_at);

comment on table work.w_refresh_token_session is 'Refresh-токены для долгоживущих пользовательских сессий';
comment on column work.w_refresh_token_session.token_hash is 'SHA-256 хеш refresh token, исходный токен в БД не хранится';
comment on column work.w_refresh_token_session.token_version is 'Снимок версии токена пользователя на момент выдачи refresh token';
comment on column work.w_refresh_token_session.replaced_by_token_hash is 'Хеш нового токена после ротации текущего';