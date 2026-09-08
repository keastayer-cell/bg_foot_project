INSERT INTO work.w_site_notification_template (
    code, title_template, summary_template, body_html_template, severity,
    action_url_template, requires_acknowledgement, audience_scope, email_enabled
) VALUES
    ('PLAYER_SUSPENDED_YELLOW', 'Дисквалификация: {{playerName}}',
     '{{playerName}} пропустит {{suspensionMatchesText}} турнира «{{tournamentName}}» из-за перебора ЖК.',
     '<p>Игрок <strong>{{playerName}}</strong> команды <strong>«{{teamName}}»</strong> пропустит <strong>{{suspensionMatchesText}}</strong> турнира <strong>«{{tournamentName}}»</strong> в связи с дисквалификацией за перебор жёлтых карточек.</p><p>Дисквалификация наступила после матча <strong>{{matchName}}</strong>.</p>',
     'WARNING', '/matches/{{matchId}}', TRUE, 'RECIPIENTS', TRUE),
    ('PLAYER_SUSPENDED_RED', 'Дисквалификация: {{playerName}}',
     '{{playerName}} пропустит {{suspensionMatchesText}} турнира «{{tournamentName}}» из-за КК.',
     '<p>Игрок <strong>{{playerName}}</strong> команды <strong>«{{teamName}}»</strong> пропустит <strong>{{suspensionMatchesText}}</strong> турнира <strong>«{{tournamentName}}»</strong> в связи с дисквалификацией за красную карточку.</p><p>Дисквалификация наступила после матча <strong>{{matchName}}</strong>.</p>',
     'WARNING', '/matches/{{matchId}}', TRUE, 'RECIPIENTS', TRUE)
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
    ('PLAYER_SUSPENDED_YELLOW', 'EMAIL', 'Дисквалификация игрока {{playerName}} за перебор ЖК',
     '<!doctype html><html lang="ru"><body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:28px"><table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:100%;background:#fff;border-radius:16px"><tr><td style="padding:28px;background:#111a3b;color:#fff;border-radius:16px 16px 0 0"><small>ФУТБОЛ БОГОРОДСК</small><h1 style="margin:10px 0 0">Дисквалификация игрока</h1></td></tr><tr><td style="padding:28px"><p>Здравствуйте, {{recipientName}}!</p><p>Игрок <strong>{{playerName}}</strong> команды <strong>«{{teamName}}»</strong> пропустит <strong>{{suspensionMatchesText}}</strong> турнира <strong>«{{tournamentName}}»</strong> из-за перебора жёлтых карточек.</p><p>Дисквалификация наступила после матча <strong>{{matchName}}</strong>.</p><p><a href="{{actionUrl}}" style="display:inline-block;padding:12px 18px;background:#087f5b;color:#fff;text-decoration:none;border-radius:8px">Открыть матч</a></p></td></tr></table></td></tr></table></body></html>',
     'HTML', TRUE),
    ('PLAYER_SUSPENDED_RED', 'EMAIL', 'Дисквалификация игрока {{playerName}} за КК',
     '<!doctype html><html lang="ru"><body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:28px"><table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:100%;background:#fff;border-radius:16px"><tr><td style="padding:28px;background:#111a3b;color:#fff;border-radius:16px 16px 0 0"><small>ФУТБОЛ БОГОРОДСК</small><h1 style="margin:10px 0 0">Дисквалификация игрока</h1></td></tr><tr><td style="padding:28px"><p>Здравствуйте, {{recipientName}}!</p><p>Игрок <strong>{{playerName}}</strong> команды <strong>«{{teamName}}»</strong> пропустит <strong>{{suspensionMatchesText}}</strong> турнира <strong>«{{tournamentName}}»</strong> из-за красной карточки.</p><p>Дисквалификация наступила после матча <strong>{{matchName}}</strong>.</p><p><a href="{{actionUrl}}" style="display:inline-block;padding:12px 18px;background:#b42318;color:#fff;text-decoration:none;border-radius:8px">Открыть матч</a></p></td></tr></table></td></tr></table></body></html>',
     'HTML', TRUE)
ON CONFLICT (code) DO UPDATE
SET channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    body_format = EXCLUDED.body_format,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

COMMENT ON COLUMN work.w_site_notification.source_type IS 'Тип бизнес-источника; для дисквалификации включает причину и игрока, чтобы исключить повторную доставку';
