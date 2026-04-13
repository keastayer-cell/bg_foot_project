CREATE TABLE IF NOT EXISTS work.w_season_player (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season(id),
    team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    created_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_season_player UNIQUE (season_id, team_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_w_season_player_season_team_active
    ON work.w_season_player (season_id, team_id, active, player_id);

CREATE INDEX IF NOT EXISTS idx_w_season_player_team_active
    ON work.w_season_player (team_id, active, player_id);

INSERT INTO work.w_season_player (season_id, team_id, player_id, created_by_user_id, updated_by_user_id, active)
SELECT DISTINCT p.season_id, pt.team_id, p.id, p.created_by_user_id, p.updated_by_user_id, TRUE
FROM work.w_player p
JOIN work.w_player_team pt
  ON pt.player_id = p.id
 AND pt.active = TRUE
JOIN work.w_season_team st
  ON st.season_id = p.season_id
 AND st.team_id = pt.team_id
WHERE p.season_id IS NOT NULL
  AND p.active = TRUE
ON CONFLICT (season_id, team_id, player_id) DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons', 'GET', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/players', method_name, TRUE
FROM work.w_role r
CROSS JOIN (VALUES ('GET'), ('POST')) AS methods(method_name)
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/players/*', 'PUT', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/players', method_name, TRUE
FROM work.w_role r
CROSS JOIN (VALUES ('GET'), ('PUT')) AS methods(method_name)
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/players/*', method_name, TRUE
FROM work.w_role r
CROSS JOIN (VALUES ('POST'), ('DELETE')) AS methods(method_name)
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;