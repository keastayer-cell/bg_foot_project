ALTER TABLE work.w_competition
    DROP CONSTRAINT IF EXISTS uq_w_competition_season_name;

CREATE UNIQUE INDEX IF NOT EXISTS ux_w_competition_active_season_name
    ON work.w_competition (season_id, LOWER(name))
    WHERE active = TRUE;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/seasons/*/competitions/**', method_name, TRUE
FROM work.w_role role
CROSS JOIN (VALUES ('POST'), ('PUT'), ('DELETE')) methods(method_name)
WHERE role.code IN ('SUPER_ADMIN', 'REFEREE')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/seasons/*/competitions/*/roster/**', method_name, TRUE
FROM work.w_role role
CROSS JOIN (VALUES ('POST'), ('DELETE')) methods(method_name)
WHERE role.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;
