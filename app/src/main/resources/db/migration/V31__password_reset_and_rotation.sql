alter table work.w_user_login
    add column if not exists must_change_password boolean not null default false;

alter table work.w_user_login
    add column if not exists token_version integer not null default 0;

alter table work.w_user_login
    add column if not exists password_changed_at timestamptz;

update work.w_user_login
set password_changed_at = coalesce(password_changed_at, created_at)
where password_changed_at is null;

comment on column work.w_user_login.must_change_password is 'Признак, что пользователь обязан сменить пароль после входа';
comment on column work.w_user_login.token_version is 'Версия токена для принудительной инвалидции старых сессий';
comment on column work.w_user_login.password_changed_at is 'Когда пользователь в последний раз сменил пароль';