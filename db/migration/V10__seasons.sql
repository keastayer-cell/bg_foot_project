CREATE TABLE work.w_season (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(120) NOT NULL,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_season_name_key UNIQUE (name)
);

COMMENT ON TABLE work.w_season IS 'Сезоны соревнований';
COMMENT ON COLUMN work.w_season.id IS 'Идентификатор сезона';
COMMENT ON COLUMN work.w_season.name IS 'Название сезона';
COMMENT ON COLUMN work.w_season.created_by_user_id IS 'Пользователь, создавший сезон';
COMMENT ON COLUMN work.w_season.active IS 'Признак активности сезона';
COMMENT ON COLUMN work.w_season.created_at IS 'Дата создания сезона';
COMMENT ON COLUMN work.w_season.updated_at IS 'Дата последнего обновления сезона';
