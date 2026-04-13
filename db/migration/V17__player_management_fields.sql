ALTER TABLE work.w_player
    ADD COLUMN IF NOT EXISTS birth_date DATE,
    ADD COLUMN IF NOT EXISTS residence VARCHAR(120),
    ADD COLUMN IF NOT EXISTS season_id BIGINT REFERENCES work.w_season(id),
    ADD COLUMN IF NOT EXISTS goals INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS yellow_cards INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS red_cards INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE work.w_player
SET updated_at = COALESCE(updated_at, created_at);

COMMENT ON COLUMN work.w_player.birth_date IS 'Дата рождения игрока';
COMMENT ON COLUMN work.w_player.residence IS 'Прописка/место проживания игрока';
COMMENT ON COLUMN work.w_player.season_id IS 'Сезон, к которому относится карточка игрока';
COMMENT ON COLUMN work.w_player.goals IS 'Количество голов';
COMMENT ON COLUMN work.w_player.yellow_cards IS 'Количество желтых карточек';
COMMENT ON COLUMN work.w_player.red_cards IS 'Количество красных карточек';
COMMENT ON COLUMN work.w_player.created_by_user_id IS 'Пользователь, создавший игрока';
COMMENT ON COLUMN work.w_player.updated_by_user_id IS 'Пользователь, который последним изменял игрока';
COMMENT ON COLUMN work.w_player.updated_at IS 'Дата последнего изменения игрока';