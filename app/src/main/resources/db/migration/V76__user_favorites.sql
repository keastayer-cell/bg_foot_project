CREATE TABLE work.w_user_favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work.w_user_login(id) ON DELETE CASCADE,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_user_favorite UNIQUE (user_id, target_type, target_id),
    CONSTRAINT chk_w_user_favorite_type CHECK (target_type IN ('TEAM', 'PLAYER'))
);
CREATE INDEX idx_w_user_favorite_user ON work.w_user_favorite(user_id, created_at DESC);
COMMENT ON TABLE work.w_user_favorite IS 'Команды и игроки, добавленные пользователем в избранное';
COMMENT ON COLUMN work.w_user_favorite.id IS 'Идентификатор записи избранного';
COMMENT ON COLUMN work.w_user_favorite.user_id IS 'Владелец списка избранного';
COMMENT ON COLUMN work.w_user_favorite.target_type IS 'Тип объекта: TEAM или PLAYER';
COMMENT ON COLUMN work.w_user_favorite.target_id IS 'Идентификатор команды или игрока';
COMMENT ON COLUMN work.w_user_favorite.created_at IS 'Дата добавления в избранное';
