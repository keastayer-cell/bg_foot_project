create schema if not exists work;

grant usage on schema work to football_app;
grant create on schema work to football_app;

do
$$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'app_bootstrap'
    ) then
        execute 'alter table public.app_bootstrap set schema work';
    end if;

    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'app_user'
    ) then
        execute 'alter table public.app_user set schema work';
    end if;
end
$$;
