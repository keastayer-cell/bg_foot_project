UPDATE work.w_api_access_rule
SET active = FALSE
WHERE url_pattern = '/api/**' AND active = TRUE;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/seasons', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('SUPER_ADMIN', 'TEAM_REP', 'USER', 'GUEST')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/seasons', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/seasons/*', m.http_method, TRUE
FROM work.w_role r
CROSS JOIN (
    VALUES ('PUT'), ('DELETE')
) AS m(http_method)
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;