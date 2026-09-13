CREATE TABLE work.w_user_notification_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work.w_user_login(id) ON DELETE CASCADE,
    category VARCHAR(24) NOT NULL,
    bell_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_user_notification_preference UNIQUE (user_id, category),
    CONSTRAINT chk_w_user_notification_preference_category
        CHECK (category IN ('MATCHES', 'TOURNAMENTS', 'TRANSFERS', 'DISCIPLINE', 'NEWS'))
);

COMMENT ON TABLE work.w_user_notification_preference IS 'Настройки каналов уведомлений пользователя по продуктовым категориям';
COMMENT ON COLUMN work.w_user_notification_preference.id IS 'Идентификатор набора настроек категории';
COMMENT ON COLUMN work.w_user_notification_preference.user_id IS 'Пользователь, которому принадлежат настройки';
COMMENT ON COLUMN work.w_user_notification_preference.category IS 'Категория событий: матчи, турниры, трансферы, дисциплина или новости';
COMMENT ON COLUMN work.w_user_notification_preference.bell_enabled IS 'Показывать необязательные события категории в колокольчике';
COMMENT ON COLUMN work.w_user_notification_preference.email_enabled IS 'Отправлять необязательные события категории по email';
COMMENT ON COLUMN work.w_user_notification_preference.updated_at IS 'Дата последнего изменения настроек категории';
