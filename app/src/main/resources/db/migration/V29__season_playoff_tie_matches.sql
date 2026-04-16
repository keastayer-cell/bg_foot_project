CREATE TABLE work.w_season_playoff_tie_match (
    id BIGSERIAL PRIMARY KEY,
    tie_id BIGINT NOT NULL REFERENCES work.w_season_playoff_tie (id) ON DELETE CASCADE,
    leg_number INTEGER NOT NULL,
    match_id BIGINT NOT NULL REFERENCES work.w_tour_match (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_season_playoff_tie_match_leg UNIQUE (tie_id, leg_number),
    CONSTRAINT uq_w_season_playoff_tie_match_match UNIQUE (match_id),
    CONSTRAINT w_season_playoff_tie_match_leg_number_check CHECK (leg_number IN (1, 2))
);

CREATE INDEX idx_w_season_playoff_tie_match_tie_id
    ON work.w_season_playoff_tie_match (tie_id, leg_number);

COMMENT ON TABLE work.w_season_playoff_tie_match IS 'Связка пары плей-офф с конкретными матчами по играм';
COMMENT ON COLUMN work.w_season_playoff_tie_match.leg_number IS 'Номер матча внутри пары: 1 или 2';