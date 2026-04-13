-- Read access parity for GUEST / USER / TEAM_REP on currently used public domain APIs
INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/teams', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('GUEST', 'USER', 'TEAM_REP')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/players', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('GUEST', 'USER', 'TEAM_REP')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/players/*/history', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('GUEST', 'USER', 'TEAM_REP')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/teams/*/players', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('GUEST', 'USER', 'TEAM_REP')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/admin/access/me', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('GUEST', 'USER', 'TEAM_REP')
ON CONFLICT DO NOTHING;

-- TEAM_REP mutate rights are scoped later by team scope checks in business logic.
INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/teams/*/players/*', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/teams/*/players/*', 'DELETE', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;