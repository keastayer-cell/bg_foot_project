create table if not exists app_user (
    id bigserial primary key,
    email varchar(255) not null unique,
    name varchar(120) not null,
    password_hash varchar(255) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_app_user_email on app_user(email);
