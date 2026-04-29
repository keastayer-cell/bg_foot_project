INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/transfers', method_name, TRUE
FROM work.w_role r
CROSS JOIN (VALUES ('GET'), ('POST')) AS methods(method_name)
WHERE r.code = 'REFEREE'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/transfer-candidates/*', 'GET', TRUE
FROM work.w_role r
WHERE r.code = 'REFEREE'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/transfers/*/approve', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'REFEREE'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/transfers/*/reject', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'REFEREE'
ON CONFLICT DO NOTHING;