alter table work.w_user_login
    add column if not exists password_reset_token_hash varchar(128);

alter table work.w_user_login
    add column if not exists password_reset_expires_at timestamptz;

comment on column work.w_user_login.password_reset_token_hash is 'SHA-256 хеш одноразового токена сброса пароля';
comment on column work.w_user_login.password_reset_expires_at is 'Срок действия одноразовой ссылки на сброс пароля';