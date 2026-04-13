ALTER TABLE work.w_season
    ADD COLUMN updated_by_user_id BIGINT REFERENCES work.w_user_login(id);

UPDATE work.w_season
SET updated_by_user_id = created_by_user_id
WHERE updated_by_user_id IS NULL;

COMMENT ON COLUMN work.w_season.updated_by_user_id IS 'Пользователь, который последним изменял сезон';