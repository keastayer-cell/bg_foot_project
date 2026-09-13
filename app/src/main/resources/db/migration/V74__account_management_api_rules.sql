INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/account/**', method_name, TRUE
FROM work.w_role role
CROSS JOIN (VALUES ('POST'), ('PUT'), ('PATCH'), ('DELETE')) AS methods(method_name)
WHERE role.code IN ('USER', 'TEAM_REP', 'REFEREE', 'SUPER_ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM work.w_api_access_rule existing
      WHERE existing.role_id = role.id
        AND existing.url_pattern = '/api/account/**'
        AND existing.http_method = methods.method_name
        AND existing.active = TRUE
  );
