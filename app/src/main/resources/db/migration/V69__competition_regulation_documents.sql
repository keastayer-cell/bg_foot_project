ALTER TABLE work.w_competition
    ADD COLUMN regulation_media_id BIGINT REFERENCES work.w_media_asset(id),
    ADD COLUMN regulation_updated_at TIMESTAMPTZ;

COMMENT ON COLUMN work.w_competition.regulation_media_id IS 'PDF регламента конкретного соревнования; не заменяет общий документ сезона';
COMMENT ON COLUMN work.w_competition.regulation_updated_at IS 'Время последней загрузки PDF регламента соревнования';
