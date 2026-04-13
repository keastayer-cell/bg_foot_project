CREATE TABLE IF NOT EXISTS work.w_media_asset (
    id                 BIGSERIAL PRIMARY KEY,
    owner_type         VARCHAR(20) NOT NULL,
    owner_id           BIGINT NOT NULL,
    media_kind         VARCHAR(30) NOT NULL,
    data_url           TEXT NOT NULL,
    mime_type          VARCHAR(120),
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_w_media_asset_owner
    ON work.w_media_asset(owner_type, owner_id, media_kind, active, id DESC);

ALTER TABLE work.w_team
    ADD COLUMN IF NOT EXISTS logo_media_id BIGINT REFERENCES work.w_media_asset(id);

ALTER TABLE work.w_player
    ADD COLUMN IF NOT EXISTS photo_media_id BIGINT REFERENCES work.w_media_asset(id);

-- Migrate old in-table team logos into media storage.
WITH inserted AS (
    INSERT INTO work.w_media_asset(owner_type, owner_id, media_kind, data_url, created_by_user_id, active)
    SELECT 'TEAM', t.id, 'TEAM_LOGO', t.logo_data_url, t.updated_by_user_id, TRUE
    FROM work.w_team t
    WHERE t.logo_data_url IS NOT NULL AND btrim(t.logo_data_url) <> ''
    RETURNING id, owner_id
)
UPDATE work.w_team t
SET logo_media_id = i.id
FROM inserted i
WHERE t.id = i.owner_id AND t.logo_media_id IS NULL;

COMMENT ON TABLE work.w_media_asset IS 'Хранилище медиа-ресурсов (эмблемы команд и фото игроков)';
COMMENT ON COLUMN work.w_media_asset.owner_type IS 'Тип владельца медиа: TEAM/PLAYER';
COMMENT ON COLUMN work.w_media_asset.owner_id IS 'ID владельца медиа';
COMMENT ON COLUMN work.w_media_asset.media_kind IS 'Тип медиа: TEAM_LOGO/PLAYER_PHOTO';
COMMENT ON COLUMN work.w_media_asset.data_url IS 'Данные изображения (data URL)';
COMMENT ON COLUMN work.w_media_asset.mime_type IS 'MIME-тип изображения';
COMMENT ON COLUMN work.w_media_asset.created_by_user_id IS 'Кто загрузил медиа';
COMMENT ON COLUMN work.w_media_asset.active IS 'Активно ли медиа';
COMMENT ON COLUMN work.w_media_asset.created_at IS 'Дата загрузки медиа';
COMMENT ON COLUMN work.w_team.logo_media_id IS 'Ссылка на активную эмблему команды';
COMMENT ON COLUMN work.w_player.photo_media_id IS 'Ссылка на активное фото игрока';