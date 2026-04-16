ALTER TABLE work.w_season_standings_config
    ADD COLUMN yellow_cards_for_suspension integer NOT NULL DEFAULT 0,
    ADD COLUMN red_cards_for_suspension integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN work.w_season_standings_config.yellow_cards_for_suspension IS 'Количество ЖК в рамках сезона, после которого игрок пропускает следующий матч';
COMMENT ON COLUMN work.w_season_standings_config.red_cards_for_suspension IS 'Количество КК в рамках сезона, после которого игрок пропускает следующий матч';