do
$$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'app_user'
    ) then
        execute 'alter table work.app_user rename to usr';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'work'
          and table_name = 'app_bootstrap'
    ) then
        execute 'alter table work.app_bootstrap rename to btp_log';
    end if;

    if exists (
        select 1
        from pg_indexes
        where schemaname = 'work'
          and indexname = 'idx_app_user_email'
    ) then
        execute 'alter index work.idx_app_user_email rename to idx_usr_email';
    end if;
end
$$;

comment on table work.usr is 'Site users';
comment on column work.usr.id is 'User id';
comment on column work.usr.email is 'User email';
comment on column work.usr.name is 'Display name';
comment on column work.usr.password_hash is 'Password hash';
comment on column work.usr.created_at is 'Create timestamp';

comment on table work.btp_log is 'Bootstrap log';
comment on column work.btp_log.id is 'Record id';
comment on column work.btp_log.created_at is 'Create timestamp';
comment on column work.btp_log.note is 'Bootstrap note';
