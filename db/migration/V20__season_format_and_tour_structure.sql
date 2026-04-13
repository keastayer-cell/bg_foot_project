ALTER TABLE work.w_season
    ADD COLUMN rounds_count INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN playoff_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN playoff_team_count INTEGER NULL;

ALTER TABLE work.w_season
    ADD CONSTRAINT w_season_rounds_count_check CHECK (rounds_count >= 1),
    ADD CONSTRAINT w_season_playoff_team_count_check CHECK (playoff_team_count IS NULL OR playoff_team_count >= 2);

COMMENT ON COLUMN work.w_season.rounds_count IS 'Количество кругов регулярного этапа';
COMMENT ON COLUMN work.w_season.playoff_enabled IS 'Включен ли плей-офф';
COMMENT ON COLUMN work.w_season.playoff_team_count IS 'Количество команд, выходящих в плей-офф';

ALTER TABLE work.w_tour
    ADD COLUMN stage_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    ADD COLUMN round_number INTEGER NULL,
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

UPDATE work.w_tour
SET stage_type = 'REGULAR',
    sort_order = id
WHERE stage_type = 'REGULAR';

COMMENT ON COLUMN work.w_tour.stage_type IS 'Тип стадии: REGULAR или PLAYOFF';
COMMENT ON COLUMN work.w_tour.round_number IS 'Номер тура для регулярного этапа';
COMMENT ON COLUMN work.w_tour.sort_order IS 'Порядок отображения тура внутри сезона';

CREATE INDEX idx_w_tour_season_stage_sort
    ON work.w_tour (season_id, stage_type, sort_order, id);