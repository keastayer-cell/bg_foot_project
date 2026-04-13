do
$$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'usr'
    ) then
        execute 'alter table work.usr rename to w_user_login';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'app_user'
    ) then
        execute 'alter table work.app_user rename to w_user_login';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'btp_log'
    ) then
        execute 'alter table work.btp_log rename to w_bootstrap_log';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'app_bootstrap'
    ) then
        execute 'alter table work.app_bootstrap rename to w_bootstrap_log';
    end if;

    if exists (
        select 1
        from pg_indexes
        where schemaname = 'work'
          and indexname = 'idx_usr_email'
    ) then
        execute 'alter index work.idx_usr_email rename to idx_w_user_login_email';
    elsif exists (
        select 1
        from pg_indexes
        where schemaname = 'work'
          and indexname = 'idx_app_user_email'
    ) then
        execute 'alter index work.idx_app_user_email rename to idx_w_user_login_email';
    end if;
end
$$;

comment on table work.w_user_login is 'Пользователи входа на сайт';
comment on column work.w_user_login.id is 'Идентификатор пользователя';
comment on column work.w_user_login.email is 'Email для входа';
comment on column work.w_user_login.name is 'Имя пользователя';
comment on column work.w_user_login.password_hash is 'Хэш пароля';
comment on column work.w_user_login.created_at is 'Дата и время регистрации';

comment on table work.w_bootstrap_log is 'Технический журнал инициализации';
comment on column work.w_bootstrap_log.id is 'Идентификатор записи';
comment on column work.w_bootstrap_log.created_at is 'Дата и время создания записи';
comment on column work.w_bootstrap_log.note is 'Служебная заметка';
