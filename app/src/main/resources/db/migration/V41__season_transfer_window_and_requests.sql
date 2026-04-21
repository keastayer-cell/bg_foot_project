ALTER TABLE work.w_season
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN max_roster_size INTEGER,
    ADD COLUMN transfer_window_start_date DATE,
    ADD COLUMN transfer_window_end_date DATE;

UPDATE work.w_season
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'CLOSED' END
WHERE status IS NULL OR status = '';

ALTER TABLE work.w_season
    ADD CONSTRAINT chk_w_season_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED')),
    ADD CONSTRAINT chk_w_season_max_roster_size
        CHECK (max_roster_size IS NULL OR max_roster_size > 0),
    ADD CONSTRAINT chk_w_season_transfer_window
        CHECK (
            transfer_window_start_date IS NULL
            OR transfer_window_end_date IS NULL
            OR transfer_window_start_date <= transfer_window_end_date
        );

COMMENT ON COLUMN work.w_season.status IS 'Статус сезона: DRAFT, ACTIVE, CLOSED';
COMMENT ON COLUMN work.w_season.max_roster_size IS 'Максимальное количество игроков в заявке команды на сезон';
COMMENT ON COLUMN work.w_season.transfer_window_start_date IS 'Дата начала окна трансферов внутри сезона';
COMMENT ON COLUMN work.w_season.transfer_window_end_date IS 'Дата окончания окна трансферов внутри сезона';

CREATE TABLE IF NOT EXISTS work.w_season_transfer_request (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season(id),
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    from_team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    to_team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    requested_by_user_id BIGINT,
    processed_by_user_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_comment TEXT,
    decision_comment TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT chk_w_season_transfer_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_w_season_transfer_request_teams
        CHECK (from_team_id <> to_team_id)
);

CREATE INDEX IF NOT EXISTS idx_w_season_transfer_request_season_status
    ON work.w_season_transfer_request (season_id, status, requested_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_w_season_transfer_request_from_team
    ON work.w_season_transfer_request (from_team_id, status, requested_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_w_season_transfer_request_to_team
    ON work.w_season_transfer_request (to_team_id, status, requested_at DESC, id DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_w_season_transfer_request_pending_player
    ON work.w_season_transfer_request (season_id, player_id)
    WHERE status = 'PENDING';

COMMENT ON TABLE work.w_season_transfer_request IS 'Заявки на трансфер игрока между командами в рамках одного сезона';
COMMENT ON COLUMN work.w_season_transfer_request.season_id IS 'Сезон, внутри которого выполняется трансфер';
COMMENT ON COLUMN work.w_season_transfer_request.player_id IS 'Игрок, по которому создана заявка';
COMMENT ON COLUMN work.w_season_transfer_request.from_team_id IS 'Команда, из которой игрок уходит';
COMMENT ON COLUMN work.w_season_transfer_request.to_team_id IS 'Команда, в которую игрок переходит';
COMMENT ON COLUMN work.w_season_transfer_request.requested_by_user_id IS 'Пользователь, создавший заявку';
COMMENT ON COLUMN work.w_season_transfer_request.processed_by_user_id IS 'Пользователь, который подтвердил или отклонил заявку';
COMMENT ON COLUMN work.w_season_transfer_request.status IS 'Статус заявки: PENDING, APPROVED, REJECTED';
COMMENT ON COLUMN work.w_season_transfer_request.request_comment IS 'Комментарий инициатора заявки';
COMMENT ON COLUMN work.w_season_transfer_request.decision_comment IS 'Комментарий при подтверждении или отклонении заявки';
COMMENT ON COLUMN work.w_season_transfer_request.requested_at IS 'Дата и время создания заявки';
COMMENT ON COLUMN work.w_season_transfer_request.processed_at IS 'Дата и время обработки заявки';

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/transfers', method_name, TRUE
FROM work.w_role r
CROSS JOIN (VALUES ('GET'), ('POST')) AS methods(method_name)
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/seasons/*/transfer-candidates/*', 'GET', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/transfers/*/approve', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/transfers/*/reject', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;