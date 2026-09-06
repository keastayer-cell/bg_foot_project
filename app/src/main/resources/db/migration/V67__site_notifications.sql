CREATE TABLE work.w_site_notification (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    action_url VARCHAR(500),
    source_type VARCHAR(60),
    source_id BIGINT,
    requires_acknowledgement BOOLEAN NOT NULL DEFAULT FALSE,
    audience_type VARCHAR(20) NOT NULL DEFAULT 'USERS',
    audience_value VARCHAR(120),
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    CONSTRAINT chk_w_site_notification_severity
        CHECK (severity IN ('INFO', 'IMPORTANT', 'WARNING')),
    CONSTRAINT chk_w_site_notification_audience_type
        CHECK (audience_type IN ('ALL', 'ROLE', 'TEAM', 'USERS')),
    CONSTRAINT chk_w_site_notification_expiration
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE TABLE work.w_site_notification_recipient (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES work.w_site_notification(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES work.w_user_login(id) ON DELETE CASCADE,
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    CONSTRAINT uq_w_site_notification_recipient UNIQUE (notification_id, user_id),
    CONSTRAINT chk_w_site_notification_recipient_ack
        CHECK (acknowledged_at IS NULL OR read_at IS NOT NULL)
);

CREATE INDEX idx_w_site_notification_created_at
    ON work.w_site_notification (created_at DESC, id DESC);

CREATE INDEX idx_w_site_notification_recipient_user_unread
    ON work.w_site_notification_recipient (user_id, read_at, delivered_at DESC);

CREATE INDEX idx_w_site_notification_recipient_notification
    ON work.w_site_notification_recipient (notification_id);

COMMENT ON TABLE work.w_site_notification IS 'Уведомления внутри сайта, созданные автоматически или вручную';
COMMENT ON COLUMN work.w_site_notification.id IS 'Технический идентификатор уведомления';
COMMENT ON COLUMN work.w_site_notification.event_type IS 'Код события или тип ручного объявления';
COMMENT ON COLUMN work.w_site_notification.title IS 'Краткая тема уведомления';
COMMENT ON COLUMN work.w_site_notification.body IS 'Основной текст уведомления';
COMMENT ON COLUMN work.w_site_notification.severity IS 'Важность уведомления: INFO, IMPORTANT или WARNING';
COMMENT ON COLUMN work.w_site_notification.action_url IS 'Внутренний путь сайта, открываемый при выборе уведомления';
COMMENT ON COLUMN work.w_site_notification.source_type IS 'Тип связанного бизнес-объекта';
COMMENT ON COLUMN work.w_site_notification.source_id IS 'Идентификатор связанного бизнес-объекта';
COMMENT ON COLUMN work.w_site_notification.requires_acknowledgement IS 'Признак необходимости явного подтверждения ознакомления';
COMMENT ON COLUMN work.w_site_notification.audience_type IS 'Тип аудитории: все, роль, команда или явный список пользователей';
COMMENT ON COLUMN work.w_site_notification.audience_value IS 'Человекочитаемое значение выбранной аудитории';
COMMENT ON COLUMN work.w_site_notification.created_by_user_id IS 'Пользователь, создавший ручное объявление или инициировавший событие';
COMMENT ON COLUMN work.w_site_notification.created_at IS 'Дата и время формирования уведомления';
COMMENT ON COLUMN work.w_site_notification.expires_at IS 'Дата и время окончания показа уведомления';

COMMENT ON TABLE work.w_site_notification_recipient IS 'Состояние доставки и ознакомления с уведомлением для конкретного пользователя';
COMMENT ON COLUMN work.w_site_notification_recipient.id IS 'Технический идентификатор записи получателя';
COMMENT ON COLUMN work.w_site_notification_recipient.notification_id IS 'Ссылка на уведомление';
COMMENT ON COLUMN work.w_site_notification_recipient.user_id IS 'Пользователь-получатель уведомления';
COMMENT ON COLUMN work.w_site_notification_recipient.delivered_at IS 'Дата и время появления уведомления у пользователя';
COMMENT ON COLUMN work.w_site_notification_recipient.read_at IS 'Дата и время первого открытия уведомления';
COMMENT ON COLUMN work.w_site_notification_recipient.acknowledged_at IS 'Дата и время явного подтверждения ознакомления';

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, endpoint.url_pattern, 'POST', TRUE
FROM work.w_role role
CROSS JOIN (
    VALUES
        ('/api/notifications/*/read'),
        ('/api/notifications/*/acknowledge'),
        ('/api/notifications/read-all')
) AS endpoint(url_pattern)
WHERE role.code IN ('GUEST', 'USER', 'TEAM_REP', 'REFEREE')
ON CONFLICT DO NOTHING;

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT role.id, '/api/notifications/admin', 'POST', TRUE
FROM work.w_role role
WHERE role.code = 'REFEREE'
ON CONFLICT DO NOTHING;
