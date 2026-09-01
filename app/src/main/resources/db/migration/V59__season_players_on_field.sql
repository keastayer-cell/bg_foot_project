ALTER TABLE work.w_season
    ADD COLUMN players_on_field INTEGER NOT NULL DEFAULT 11;

ALTER TABLE work.w_season
    ADD CONSTRAINT chk_w_season_players_on_field
        CHECK (players_on_field > 0);

COMMENT ON COLUMN work.w_season.players_on_field IS
    'Количество игроков команды на поле, включая вратаря; определяет размер стартового состава';
