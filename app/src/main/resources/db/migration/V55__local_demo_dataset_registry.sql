CREATE TABLE work.w_demo_dataset (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    season_id BIGINT,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_w_demo_dataset_stage
        CHECK (stage IN ('BASE', 'SCHEDULE', 'RESULTS', 'TRANSFERS'))
);

CREATE TABLE work.w_demo_dataset_object (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES work.w_demo_dataset(id) ON DELETE CASCADE,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_w_demo_dataset_object UNIQUE (dataset_id, object_type, object_id)
);

CREATE INDEX ix_w_demo_dataset_object_lookup
    ON work.w_demo_dataset_object(dataset_id, object_type, object_id);

COMMENT ON TABLE work.w_demo_dataset IS 'Реестр локальных наборов данных для ручной продуктовой проверки';
COMMENT ON TABLE work.w_demo_dataset_object IS 'Идентификаторы объектов, созданных локальным генератором демо-лиги';

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/admin/demo/**', '*', TRUE
FROM work.w_role r
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
