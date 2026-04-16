ALTER TABLE work.w_player
    ADD COLUMN is_goalkeeper boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN work.w_player.is_goalkeeper IS 'Признак, что игрок является вратарем';