ALTER TABLE work.w_site_notification
    ADD COLUMN summary VARCHAR(300);

UPDATE work.w_site_notification
SET summary = LEFT(REGEXP_REPLACE(body, '<[^>]+>', ' ', 'g'), 300)
WHERE summary IS NULL;

ALTER TABLE work.w_site_notification
    ALTER COLUMN summary SET NOT NULL;

COMMENT ON COLUMN work.w_site_notification.summary IS 'Краткое содержание уведомления для компактного списка';

CREATE TABLE work.w_site_notification_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    title_template VARCHAR(180) NOT NULL,
    summary_template VARCHAR(300) NOT NULL,
    body_html_template TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    action_url_template VARCHAR(500),
    requires_acknowledgement BOOLEAN NOT NULL DEFAULT FALSE,
    audience_scope VARCHAR(20) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_site_notification_template_code UNIQUE (code),
    CONSTRAINT chk_w_site_notification_template_severity
        CHECK (severity IN ('INFO', 'IMPORTANT', 'WARNING')),
    CONSTRAINT chk_w_site_notification_template_audience_scope
        CHECK (audience_scope IN ('PUBLIC', 'RECIPIENTS'))
);

COMMENT ON TABLE work.w_site_notification_template IS 'HTML-шаблоны автоматических уведомлений внутри сайта';
COMMENT ON COLUMN work.w_site_notification_template.id IS 'Технический идентификатор шаблона';
COMMENT ON COLUMN work.w_site_notification_template.code IS 'Уникальный код бизнес-события';
COMMENT ON COLUMN work.w_site_notification_template.title_template IS 'Шаблон краткой темы уведомления';
COMMENT ON COLUMN work.w_site_notification_template.summary_template IS 'Шаблон краткого содержания для списка уведомлений';
COMMENT ON COLUMN work.w_site_notification_template.body_html_template IS 'Шаблон полного тела уведомления в HTML-разметке';
COMMENT ON COLUMN work.w_site_notification_template.severity IS 'Важность формируемого уведомления';
COMMENT ON COLUMN work.w_site_notification_template.action_url_template IS 'Шаблон внутренней ссылки на связанный раздел сайта';
COMMENT ON COLUMN work.w_site_notification_template.requires_acknowledgement IS 'Признак необходимости явного подтверждения ознакомления';
COMMENT ON COLUMN work.w_site_notification_template.audience_scope IS 'Область видимости события: публичная или только заданным получателям';
COMMENT ON COLUMN work.w_site_notification_template.email_enabled IS 'Признак необходимости дополнительно поставить email в очередь mailer';
COMMENT ON COLUMN work.w_site_notification_template.active IS 'Признак доступности шаблона для формирования новых уведомлений';
COMMENT ON COLUMN work.w_site_notification_template.created_at IS 'Дата и время создания шаблона';
COMMENT ON COLUMN work.w_site_notification_template.updated_at IS 'Дата и время последнего изменения шаблона';

CREATE INDEX idx_w_site_notification_template_active
    ON work.w_site_notification_template (active, code);

INSERT INTO work.w_site_notification_template (
    code, title_template, summary_template, body_html_template, severity,
    action_url_template, requires_acknowledgement, audience_scope, email_enabled
) VALUES
    ('SEASON_STARTED', 'Сезон «{{seasonName}}» начался',
     'Расписание опубликовано, сезон открыт.',
     '<p>Сезон <strong>«{{seasonName}}»</strong> открыт.</p><p>Расписание матчей и актуальные результаты доступны на сайте.</p>',
     'IMPORTANT', '/', FALSE, 'PUBLIC', TRUE),
    ('SEASON_CLOSED', 'Сезон «{{seasonName}}» завершён',
     'Опубликованы итоговые результаты сезона.',
     '<p>Сезон <strong>«{{seasonName}}»</strong> завершён.</p><p>Итоговая таблица и результаты матчей доступны на сайте.</p>',
     'INFO', '/', FALSE, 'PUBLIC', TRUE),
    ('TOUR_PUBLISHED', 'Опубликован тур «{{tourName}}»',
     'Стало доступно расписание матчей сезона «{{seasonName}}».',
     '<p>Опубликован тур <strong>«{{tourName}}»</strong> сезона <strong>«{{seasonName}}»</strong>.</p><p>Откройте расписание, чтобы посмотреть пары команд и время начала матчей.</p>',
     'INFO', '/', FALSE, 'PUBLIC', FALSE),
    ('TRANSFER_REQUEST_CREATED', 'Новая заявка на трансфер',
     '{{playerName}}: {{fromTeamName}} → {{toTeamName}}. Требуется решение.',
     '<p>Клуб <strong>«{{toTeamName}}»</strong> запросил переход игрока <strong>{{playerName}}</strong> из команды <strong>«{{fromTeamName}}»</strong>.</p><p>Откройте раздел трансферов и примите решение.</p>',
     'IMPORTANT', '/team-rep-transfers?season={{seasonId}}', TRUE, 'RECIPIENTS', TRUE),
    ('TRANSFER_REQUEST_APPROVED', 'Трансфер подтверждён',
     '{{playerName}}: {{fromTeamName}} → {{toTeamName}}.',
     '<p>Трансфер игрока <strong>{{playerName}}</strong> из команды <strong>«{{fromTeamName}}»</strong> в <strong>«{{toTeamName}}»</strong> подтверждён.</p>',
     'INFO', '/team-rep-transfers?season={{seasonId}}', FALSE, 'RECIPIENTS', TRUE),
    ('TRANSFER_REQUEST_REJECTED', 'Трансфер отклонён',
     '{{playerName}}: {{fromTeamName}} → {{toTeamName}}.',
     '<p>Трансфер игрока <strong>{{playerName}}</strong> из команды <strong>«{{fromTeamName}}»</strong> в <strong>«{{toTeamName}}»</strong> отклонён.</p><p><strong>Комментарий:</strong> {{decisionComment}}</p>',
     'WARNING', '/team-rep-transfers?season={{seasonId}}', FALSE, 'RECIPIENTS', TRUE),
    ('TRANSFER_REQUEST_REVOKED', 'Трансфер отозван',
     '{{playerName}}: {{fromTeamName}} → {{toTeamName}}.',
     '<p>Заявка на трансфер игрока <strong>{{playerName}}</strong> из команды <strong>«{{fromTeamName}}»</strong> в <strong>«{{toTeamName}}»</strong> отозвана.</p><p><strong>Комментарий:</strong> {{decisionComment}}</p>',
     'WARNING', '/team-rep-transfers?season={{seasonId}}', FALSE, 'RECIPIENTS', TRUE),
    ('SEASON_APPLICATION_SUBMITTED_TO_REFEREE', 'Новая заявка на сезон',
     'Команда «{{teamName}}» отправила состав на сезон «{{seasonName}}».',
     '<p>Команда <strong>«{{teamName}}»</strong> отправила заявку на сезон <strong>«{{seasonName}}»</strong>.</p><p>Проверьте состав и примите решение.</p>',
     'IMPORTANT', '/season-applications-review', TRUE, 'RECIPIENTS', FALSE),
    ('SEASON_APPLICATION_APPROVED', 'Заявка на сезон одобрена',
     'Состав команды «{{teamName}}» допущен к сезону «{{seasonName}}».',
     '<p>Заявка команды <strong>«{{teamName}}»</strong> на сезон <strong>«{{seasonName}}»</strong> одобрена.</p>',
     'INFO', '/team-rep-dashboard', FALSE, 'RECIPIENTS', FALSE),
    ('SEASON_APPLICATION_RETURNED', 'Заявка возвращена на доработку',
     'Состав команды «{{teamName}}» нужно скорректировать.',
     '<p>Заявка команды <strong>«{{teamName}}»</strong> на сезон <strong>«{{seasonName}}»</strong> возвращена на доработку.</p><p><strong>Комментарий:</strong> {{decisionComment}}</p>',
     'WARNING', '/team-rep-dashboard', TRUE, 'RECIPIENTS', FALSE),
    ('SEASON_APPLICATION_REJECTED', 'Заявка на сезон отклонена',
     'Заявка команды «{{teamName}}» не принята.',
     '<p>Заявка команды <strong>«{{teamName}}»</strong> на сезон <strong>«{{seasonName}}»</strong> отклонена.</p><p><strong>Комментарий:</strong> {{decisionComment}}</p>',
     'WARNING', '/team-rep-dashboard', TRUE, 'RECIPIENTS', FALSE)
ON CONFLICT (code) DO UPDATE
SET title_template = EXCLUDED.title_template,
    summary_template = EXCLUDED.summary_template,
    body_html_template = EXCLUDED.body_html_template,
    severity = EXCLUDED.severity,
    action_url_template = EXCLUDED.action_url_template,
    requires_acknowledgement = EXCLUDED.requires_acknowledgement,
    audience_scope = EXCLUDED.audience_scope,
    email_enabled = EXCLUDED.email_enabled,
    active = TRUE,
    updated_at = NOW();

INSERT INTO mailer.notification_template (
    code, channel, subject_template, body_template, body_format, is_active
) VALUES
    ('SEASON_STARTED', 'EMAIL', 'Сезон «{{seasonName}}» начался',
     '<!doctype html><html lang="ru"><body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:28px"><table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:100%;background:#fff;border-radius:16px"><tr><td style="padding:28px;background:#111a3b;color:#fff;border-radius:16px 16px 0 0"><small>ФУТБОЛ БОГОРОДСК</small><h1 style="margin:10px 0 0">Сезон начался</h1></td></tr><tr><td style="padding:28px"><p>Здравствуйте, {{recipientName}}!</p><p>Сезон <strong>«{{seasonName}}»</strong> открыт. Расписание и результаты доступны на сайте.</p><p><a href="{{siteUrl}}" style="color:#087f5b">Открыть сайт</a></p></td></tr></table></td></tr></table></body></html>',
     'HTML', TRUE),
    ('SEASON_CLOSED', 'EMAIL', 'Сезон «{{seasonName}}» завершён',
     '<!doctype html><html lang="ru"><body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%"><tr><td align="center" style="padding:28px"><table role="presentation" width="600" style="max-width:100%;background:#fff;border-radius:16px"><tr><td style="padding:28px;background:#111a3b;color:#fff"><h1>Сезон завершён</h1></td></tr><tr><td style="padding:28px"><p>Здравствуйте, {{recipientName}}!</p><p>Сезон <strong>«{{seasonName}}»</strong> завершён. Итоговые результаты доступны на сайте.</p><p><a href="{{siteUrl}}" style="color:#087f5b">Посмотреть итоги</a></p></td></tr></table></td></tr></table></body></html>',
     'HTML', TRUE),
    ('TRANSFER_REQUEST_CREATED', 'EMAIL', 'Требуется решение по трансферу игрока {{playerName}}',
     '<!doctype html><html lang="ru"><body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%"><tr><td align="center" style="padding:28px"><table role="presentation" width="600" style="max-width:100%;background:#fff;border-radius:16px"><tr><td style="padding:28px;background:#111a3b;color:#fff"><h1>Новая заявка на трансфер</h1></td></tr><tr><td style="padding:28px"><p>Здравствуйте, {{recipientName}}!</p><p>Запрошен переход игрока <strong>{{playerName}}</strong>: «{{fromTeamName}}» → «{{toTeamName}}».</p><p><a href="{{actionUrl}}" style="color:#087f5b">Принять решение</a></p></td></tr></table></td></tr></table></body></html>',
     'HTML', TRUE),
    ('TRANSFER_REQUEST_APPROVED', 'EMAIL', 'Трансфер игрока {{playerName}} подтверждён',
     '<!doctype html><html lang="ru"><body style="font-family:Arial,sans-serif;color:#172033"><h1>Трансфер подтверждён</h1><p>Здравствуйте, {{recipientName}}!</p><p>{{playerName}}: «{{fromTeamName}}» → «{{toTeamName}}».</p><p><a href="{{actionUrl}}">Открыть трансферы</a></p></body></html>', 'HTML', TRUE),
    ('TRANSFER_REQUEST_REJECTED', 'EMAIL', 'Трансфер игрока {{playerName}} отклонён',
     '<!doctype html><html lang="ru"><body style="font-family:Arial,sans-serif;color:#172033"><h1>Трансфер отклонён</h1><p>Здравствуйте, {{recipientName}}!</p><p>{{playerName}}: «{{fromTeamName}}» → «{{toTeamName}}».</p><p><strong>Комментарий:</strong> {{decisionComment}}</p><p><a href="{{actionUrl}}">Открыть трансферы</a></p></body></html>', 'HTML', TRUE),
    ('TRANSFER_REQUEST_REVOKED', 'EMAIL', 'Заявка на трансфер игрока {{playerName}} отозвана',
     '<!doctype html><html lang="ru"><body style="font-family:Arial,sans-serif;color:#172033"><h1>Трансфер отозван</h1><p>Здравствуйте, {{recipientName}}!</p><p>{{playerName}}: «{{fromTeamName}}» → «{{toTeamName}}».</p><p><strong>Комментарий:</strong> {{decisionComment}}</p><p><a href="{{actionUrl}}">Открыть трансферы</a></p></body></html>', 'HTML', TRUE)
ON CONFLICT (code) DO UPDATE
SET channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    body_format = EXCLUDED.body_format,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();
