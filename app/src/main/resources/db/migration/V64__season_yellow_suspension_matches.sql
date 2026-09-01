ALTER TABLE work.w_season_standings_config
    ADD COLUMN yellow_suspension_matches integer NOT NULL DEFAULT 1;

UPDATE work.w_season_standings_config
SET red_cards_for_suspension = 1
WHERE red_cards_for_suspension < 1;

ALTER TABLE work.w_season_standings_config
    ALTER COLUMN red_cards_for_suspension SET DEFAULT 1;

COMMENT ON COLUMN work.w_season_standings_config.yellow_suspension_matches IS 'Количество матчей дисквалификации после достижения порога ЖК';
COMMENT ON COLUMN work.w_season_standings_config.red_cards_for_suspension IS 'Количество матчей дисквалификации за одну КК';
