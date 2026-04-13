create table if not exists app_bootstrap (
    id bigserial primary key,
    created_at timestamptz not null default now(),
    note varchar(255) not null
);
