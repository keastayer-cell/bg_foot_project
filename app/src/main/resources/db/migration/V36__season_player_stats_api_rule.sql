INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/seasons/*/player-stats', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('TEAM_REP', 'USER', 'GUEST')
ON CONFLICT DO NOTHING;