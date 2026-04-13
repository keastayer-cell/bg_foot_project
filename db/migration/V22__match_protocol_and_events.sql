CREATE TABLE work.w_match_protocol (
    id                 BIGSERIAL PRIMARY KEY,
    match_id           BIGINT NOT NULL UNIQUE REFERENCES work.w_tour_match(id) ON DELETE CASCADE,
    status             VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    home_score         INTEGER,
    away_score         INTEGER,
    best_player_id     BIGINT REFERENCES work.w_player(id),
    notes              TEXT,
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_match_protocol_score_non_negative CHECK (
        (home_score IS NULL OR home_score >= 0) AND (away_score IS NULL OR away_score >= 0)
    )
);

COMMENT ON TABLE work.w_match_protocol IS 'Протокол матча: итоговый статус, счет и служебные данные';
COMMENT ON COLUMN work.w_match_protocol.match_id IS 'Матч, к которому относится протокол';
COMMENT ON COLUMN work.w_match_protocol.status IS 'Статус матча: SCHEDULED, LIVE, FINISHED и т.д.';
COMMENT ON COLUMN work.w_match_protocol.home_score IS 'Счет хозяев';
COMMENT ON COLUMN work.w_match_protocol.away_score IS 'Счет гостей';
COMMENT ON COLUMN work.w_match_protocol.best_player_id IS 'Лучший игрок матча';

CREATE TABLE work.w_match_event (
    id                 BIGSERIAL PRIMARY KEY,
    match_id           BIGINT NOT NULL REFERENCES work.w_tour_match(id) ON DELETE CASCADE,
    team_id            BIGINT REFERENCES work.w_team(id),
    player_id          BIGINT REFERENCES work.w_player(id),
    related_player_id  BIGINT REFERENCES work.w_player(id),
    event_type         VARCHAR(40) NOT NULL,
    minute             INTEGER NOT NULL,
    extra_minute       INTEGER,
    value_text         VARCHAR(255),
    sort_order         INTEGER NOT NULL DEFAULT 0,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE work.w_match_event IS 'События матча: голы, карточки, замены и другие отметки протокола';
COMMENT ON COLUMN work.w_match_event.match_id IS 'Матч, к которому относится событие';
COMMENT ON COLUMN work.w_match_event.team_id IS 'Команда, к которой относится событие';
COMMENT ON COLUMN work.w_match_event.player_id IS 'Основной игрок события';
COMMENT ON COLUMN work.w_match_event.related_player_id IS 'Связанный игрок, например вышедший/ушедший при замене';
COMMENT ON COLUMN work.w_match_event.event_type IS 'Тип события: GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION и т.д.';
COMMENT ON COLUMN work.w_match_event.minute IS 'Минута матча';
COMMENT ON COLUMN work.w_match_event.extra_minute IS 'Добавленная минута, если есть';
COMMENT ON COLUMN work.w_match_event.sort_order IS 'Порядок события внутри матча';

INSERT INTO work.w_match_protocol (match_id, status, created_at, updated_at)
SELECT tm.id, 'SCHEDULED', NOW(), NOW()
FROM work.w_tour_match tm
LEFT JOIN work.w_match_protocol mp ON mp.match_id = tm.id
WHERE mp.id IS NULL;

CREATE INDEX idx_w_match_protocol_match ON work.w_match_protocol (match_id);
CREATE INDEX idx_w_match_event_match_sort ON work.w_match_event (match_id, sort_order, minute, id);

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/matches/*', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('SUPER_ADMIN', 'TEAM_REP', 'USER', 'GUEST')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/seasons/*/overview', 'GET', TRUE
FROM work.w_role r
WHERE r.code IN ('SUPER_ADMIN', 'TEAM_REP', 'USER', 'GUEST')
ON CONFLICT DO NOTHING;
