CREATE TABLE IF NOT EXISTS work.w_match_lineup (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES work.w_tour_match(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    submitted_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_match_lineup_match_team UNIQUE (match_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_w_match_lineup_match_id ON work.w_match_lineup(match_id);
CREATE INDEX IF NOT EXISTS idx_w_match_lineup_team_id ON work.w_match_lineup(team_id);

CREATE TABLE IF NOT EXISTS work.w_match_lineup_player (
    id BIGSERIAL PRIMARY KEY,
    lineup_id BIGINT NOT NULL REFERENCES work.w_match_lineup(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    sort_order INTEGER NOT NULL DEFAULT 1,
    created_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_match_lineup_player UNIQUE (lineup_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_w_match_lineup_player_lineup_id ON work.w_match_lineup_player(lineup_id);
CREATE INDEX IF NOT EXISTS idx_w_match_lineup_player_player_id ON work.w_match_lineup_player(player_id);