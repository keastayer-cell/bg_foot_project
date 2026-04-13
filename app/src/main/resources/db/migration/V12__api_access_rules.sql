CREATE TABLE IF NOT EXISTS work.w_api_access_rule (
    id           BIGSERIAL PRIMARY KEY,
    role_id      BIGINT NOT NULL REFERENCES work.w_role(id) ON DELETE CASCADE,
    url_pattern  VARCHAR(255) NOT NULL,
    http_method  VARCHAR(16) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_w_api_access_rule_active
    ON work.w_api_access_rule(role_id, url_pattern, http_method)
    WHERE active = TRUE;

INSERT INTO work.w_role(code, name_ru)
VALUES ('GUEST', 'Гость')
ON CONFLICT (code) DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/**', '*', TRUE
FROM work.w_role r
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/**', m.http_method, TRUE
FROM work.w_role r
CROSS JOIN (
    VALUES ('GET'), ('POST'), ('PUT'), ('PATCH'), ('DELETE')
) AS m(http_method)
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/**', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('USER', 'GUEST')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE work.w_api_access_rule IS 'Матрица доступа к API по роли, URL и HTTP-методу';
COMMENT ON COLUMN work.w_api_access_rule.id IS 'Идентификатор правила доступа к API';
COMMENT ON COLUMN work.w_api_access_rule.role_id IS 'Идентификатор роли';
COMMENT ON COLUMN work.w_api_access_rule.url_pattern IS 'URL или шаблон URL (Ant style)';
COMMENT ON COLUMN work.w_api_access_rule.http_method IS 'HTTP-метод (GET/POST/PUT/PATCH/DELETE или *)';
COMMENT ON COLUMN work.w_api_access_rule.active IS 'Признак активности правила';
COMMENT ON COLUMN work.w_api_access_rule.created_at IS 'Дата создания правила';