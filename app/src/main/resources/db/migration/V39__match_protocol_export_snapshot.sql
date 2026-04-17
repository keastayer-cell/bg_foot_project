CREATE TABLE IF NOT EXISTS work.w_match_protocol_export_snapshot (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL UNIQUE REFERENCES work.w_tour_match(id) ON DELETE CASCADE,
    tour_sort_order INTEGER NOT NULL DEFAULT 0,
    kickoff_at TIMESTAMPTZ,
    tour_name VARCHAR(120) NOT NULL,
    home_team_name VARCHAR(160) NOT NULL,
    away_team_name VARCHAR(160) NOT NULL,
    home_score INTEGER,
    away_score INTEGER,
    home_technical_defeat BOOLEAN NOT NULL DEFAULT FALSE,
    away_technical_defeat BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    referees_json TEXT NOT NULL,
    teams_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_w_match_protocol_export_snapshot_season
    ON work.w_match_protocol_export_snapshot(season_id);

CREATE INDEX IF NOT EXISTS idx_w_match_protocol_export_snapshot_order
    ON work.w_match_protocol_export_snapshot(season_id, tour_sort_order, kickoff_at, match_id);

COMMENT ON TABLE work.w_match_protocol_export_snapshot IS 'Снимок подтвержденного протокола для быстрой сезонной выгрузки';
COMMENT ON COLUMN work.w_match_protocol_export_snapshot.match_id IS 'Матч, к которому относится снимок подтвержденного протокола';
COMMENT ON COLUMN work.w_match_protocol_export_snapshot.referees_json IS 'JSON со списком арбитров протокола';
COMMENT ON COLUMN work.w_match_protocol_export_snapshot.teams_json IS 'JSON с составами и статистикой игроков протокола';