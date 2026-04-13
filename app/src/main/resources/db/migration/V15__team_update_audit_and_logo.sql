ALTER TABLE work.w_team
    ADD COLUMN IF NOT EXISTS city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS logo_data_url TEXT,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE work.w_team
SET updated_at = COALESCE(updated_at, created_at);

COMMENT ON COLUMN work.w_team.city IS 'Город команды';
COMMENT ON COLUMN work.w_team.logo_data_url IS 'Логотип команды (data URL или ссылка)';
COMMENT ON COLUMN work.w_team.created_by_user_id IS 'Пользователь, создавший команду';
COMMENT ON COLUMN work.w_team.updated_by_user_id IS 'Пользователь, который последним изменял команду';
COMMENT ON COLUMN work.w_team.updated_at IS 'Дата последнего изменения команды';