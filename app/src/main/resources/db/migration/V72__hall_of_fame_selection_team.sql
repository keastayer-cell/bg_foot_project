ALTER TABLE work.w_competition_selection_slot
    ADD COLUMN IF NOT EXISTS team_id BIGINT REFERENCES work.w_team(id),
    ADD COLUMN IF NOT EXISTS team_short_name_snapshot VARCHAR(80);

COMMENT ON COLUMN work.w_competition_selection_slot.team_id IS 'Команда игрока в момент включения в символическую сборную';
COMMENT ON COLUMN work.w_competition_selection_slot.team_short_name_snapshot IS 'Короткое название команды игрока на момент публикации';
