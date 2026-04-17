INSERT INTO work.w_role(code, name_ru)
VALUES ('REFEREE', 'Рефери')
ON CONFLICT (code) DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/**', '*', TRUE
FROM work.w_role r
WHERE r.code = 'REFEREE'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS work.w_referee (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL UNIQUE,
    city VARCHAR(120),
    birth_date DATE,
    photo_media_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS work.w_season_referee (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season(id) ON DELETE CASCADE,
    referee_id BIGINT NOT NULL REFERENCES work.w_referee(id) ON DELETE CASCADE,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_w_season_referee UNIQUE (season_id, referee_id)
);

ALTER TABLE work.w_match_protocol
    ADD COLUMN IF NOT EXISTS chief_referee_id BIGINT REFERENCES work.w_referee(id),
    ADD COLUMN IF NOT EXISTS assistant_referee_one_id BIGINT REFERENCES work.w_referee(id),
    ADD COLUMN IF NOT EXISTS assistant_referee_two_id BIGINT REFERENCES work.w_referee(id);

CREATE INDEX IF NOT EXISTS idx_w_season_referee_season ON work.w_season_referee(season_id);
CREATE INDEX IF NOT EXISTS idx_w_season_referee_referee ON work.w_season_referee(referee_id);

COMMENT ON TABLE work.w_referee IS 'Справочник судей';
COMMENT ON COLUMN work.w_referee.full_name IS 'ФИО судьи';
COMMENT ON COLUMN work.w_referee.city IS 'Город судьи';
COMMENT ON COLUMN work.w_referee.birth_date IS 'Дата рождения судьи';

COMMENT ON TABLE work.w_season_referee IS 'Привязка судей к сезону';
COMMENT ON COLUMN work.w_match_protocol.chief_referee_id IS 'Главный арбитр матча';
COMMENT ON COLUMN work.w_match_protocol.assistant_referee_one_id IS 'Первый помощник арбитра';
COMMENT ON COLUMN work.w_match_protocol.assistant_referee_two_id IS 'Второй помощник арбитра';