CREATE TABLE work.w_competition (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    competition_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    roster_mode VARCHAR(24) NOT NULL DEFAULT 'SEASON_SHARED',
    max_roster_size INTEGER,
    match_roster_size INTEGER,
    players_on_field INTEGER NOT NULL DEFAULT 11,
    regular_tie_legs INTEGER NOT NULL DEFAULT 1,
    final_legs INTEGER NOT NULL DEFAULT 1,
    third_place_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    third_place_legs INTEGER NOT NULL DEFAULT 1,
    extra_time_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    extra_time_minutes INTEGER NOT NULL DEFAULT 30,
    penalties_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    yellow_cards_for_suspension INTEGER NOT NULL DEFAULT 0,
    yellow_suspension_matches INTEGER NOT NULL DEFAULT 1,
    red_suspension_matches INTEGER NOT NULL DEFAULT 1,
    draw_status VARCHAR(24) NOT NULL DEFAULT 'NOT_DRAWN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_competition_season_name UNIQUE (season_id, name),
    CONSTRAINT chk_w_competition_type CHECK (competition_type IN ('CHAMPIONSHIP', 'CUP')),
    CONSTRAINT chk_w_competition_status CHECK (status IN ('DRAFT', 'ACTIVE', 'FINISHED')),
    CONSTRAINT chk_w_competition_roster_mode CHECK (roster_mode IN ('SEASON_SHARED', 'OWN')),
    CONSTRAINT chk_w_competition_draw_status CHECK (draw_status IN ('NOT_DRAWN', 'DRAFT', 'CONFIRMED')),
    CONSTRAINT chk_w_competition_players_on_field CHECK (players_on_field > 0),
    CONSTRAINT chk_w_competition_legs CHECK (regular_tie_legs BETWEEN 1 AND 2 AND final_legs BETWEEN 1 AND 2 AND third_place_legs BETWEEN 1 AND 2),
    CONSTRAINT chk_w_competition_roster_limits CHECK (max_roster_size IS NULL OR max_roster_size > 0),
    CONSTRAINT chk_w_competition_match_roster CHECK (match_roster_size IS NULL OR match_roster_size >= players_on_field),
    CONSTRAINT chk_w_competition_extra_time CHECK (extra_time_minutes >= 0),
    CONSTRAINT chk_w_competition_discipline CHECK (
        yellow_cards_for_suspension >= 0 AND yellow_suspension_matches >= 1 AND red_suspension_matches >= 1
    )
);

CREATE UNIQUE INDEX ux_w_competition_one_championship
    ON work.w_competition (season_id)
    WHERE competition_type = 'CHAMPIONSHIP' AND active = TRUE;

INSERT INTO work.w_competition (
    season_id, name, competition_type, status, roster_mode, max_roster_size,
    players_on_field, third_place_enabled, active, created_by_user_id,
    updated_by_user_id, created_at, updated_at
)
SELECT
    s.id,
    'Чемпионат',
    'CHAMPIONSHIP',
    CASE WHEN s.status = 'CLOSED' THEN 'FINISHED' WHEN s.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'DRAFT' END,
    'SEASON_SHARED',
    s.max_roster_size,
    s.players_on_field,
    COALESCE(pc.third_place_enabled, FALSE),
    s.active,
    s.created_by_user_id,
    s.updated_by_user_id,
    s.created_at,
    s.updated_at
FROM work.w_season s
LEFT JOIN work.w_season_playoff_config pc ON pc.season_id = s.id;

CREATE TABLE work.w_competition_team (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    seed_number INTEGER,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_competition_team UNIQUE (competition_id, team_id)
);

INSERT INTO work.w_competition_team (competition_id, team_id, created_by_user_id, created_at)
SELECT c.id, st.team_id, st.created_by_user_id, st.created_at
FROM work.w_competition c
JOIN work.w_season_team st ON st.season_id = c.season_id
WHERE c.competition_type = 'CHAMPIONSHIP';

CREATE TABLE work.w_competition_roster_player (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_competition_roster_player UNIQUE (competition_id, player_id)
);

CREATE TABLE work.w_cup_tie (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id) ON DELETE CASCADE,
    round_code VARCHAR(40) NOT NULL,
    round_order INTEGER NOT NULL,
    slot_order INTEGER NOT NULL,
    leg_count INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(160) NOT NULL,
    home_source_tie_id BIGINT REFERENCES work.w_cup_tie(id),
    away_source_tie_id BIGINT REFERENCES work.w_cup_tie(id),
    home_source_result VARCHAR(16),
    away_source_result VARCHAR(16),
    home_team_id BIGINT REFERENCES work.w_team(id),
    away_team_id BIGINT REFERENCES work.w_team(id),
    winner_team_id BIGINT REFERENCES work.w_team(id),
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
    aggregate_home_score INTEGER,
    aggregate_away_score INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_cup_tie_slot UNIQUE (competition_id, round_order, slot_order),
    CONSTRAINT chk_w_cup_tie_status CHECK (status IN ('PLANNED', 'BYE', 'READY', 'IN_PROGRESS', 'FINISHED'))
);

ALTER TABLE work.w_tour
    ADD COLUMN competition_id BIGINT REFERENCES work.w_competition(id) ON DELETE CASCADE;

UPDATE work.w_tour tour
SET competition_id = competition.id
FROM work.w_competition competition
WHERE competition.season_id = tour.season_id
  AND competition.competition_type = 'CHAMPIONSHIP';

ALTER TABLE work.w_tour
    DROP CONSTRAINT w_tour_name_per_season_unique;

CREATE UNIQUE INDEX ux_w_tour_name_per_competition
    ON work.w_tour (competition_id, name)
    WHERE competition_id IS NOT NULL;

CREATE UNIQUE INDEX ux_w_tour_legacy_name_per_season
    ON work.w_tour (season_id, name)
    WHERE competition_id IS NULL;

CREATE TABLE work.w_cup_tie_match (
    id BIGSERIAL PRIMARY KEY,
    tie_id BIGINT NOT NULL REFERENCES work.w_cup_tie(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL UNIQUE REFERENCES work.w_tour_match(id) ON DELETE CASCADE,
    leg_number INTEGER NOT NULL,
    CONSTRAINT uq_w_cup_tie_match_leg UNIQUE (tie_id, leg_number)
);

CREATE INDEX idx_w_competition_season ON work.w_competition(season_id, active, competition_type);
CREATE INDEX idx_w_competition_team_competition ON work.w_competition_team(competition_id, team_id);
CREATE INDEX idx_w_competition_roster_team ON work.w_competition_roster_player(competition_id, team_id, active);
CREATE INDEX idx_w_cup_tie_competition ON work.w_cup_tie(competition_id, round_order, slot_order);

COMMENT ON TABLE work.w_competition IS 'Отдельное соревнование внутри сезона: чемпионат или кубок';
COMMENT ON TABLE work.w_competition_team IS 'Команды-участники отдельного соревнования';
COMMENT ON TABLE work.w_competition_roster_player IS 'Собственная заявка соревнования';
COMMENT ON TABLE work.w_cup_tie IS 'Пара кубковой сетки с источниками участников';
COMMENT ON COLUMN work.w_tour.competition_id IS 'Соревнование, которому принадлежит тур или кубковый раунд';

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/seasons/*/competitions*', method_name, TRUE
FROM work.w_role role
CROSS JOIN (VALUES ('GET'), ('POST'), ('PUT'), ('DELETE')) methods(method_name)
WHERE role.code IN ('SUPER_ADMIN', 'REFEREE')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/seasons/*/competitions/*/roster', method_name, TRUE
FROM work.w_role role
CROSS JOIN (VALUES ('GET'), ('POST')) methods(method_name)
WHERE role.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;
