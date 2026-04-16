INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/players', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;