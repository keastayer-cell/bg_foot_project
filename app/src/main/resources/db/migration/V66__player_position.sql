ALTER TABLE work.w_player
    ADD COLUMN position VARCHAR(24);

UPDATE work.w_player
SET position = 'GOALKEEPER'
WHERE is_goalkeeper = TRUE;

ALTER TABLE work.w_player
    ADD CONSTRAINT w_player_position_check
    CHECK (position IS NULL OR position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'));

COMMENT ON COLUMN work.w_player.position IS
    'Основная игровая позиция: GOALKEEPER, DEFENDER, MIDFIELDER или FORWARD; NULL — позиция не указана';
