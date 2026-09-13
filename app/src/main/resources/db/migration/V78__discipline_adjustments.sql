CREATE TABLE work.w_discipline_adjustment (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id),
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    team_id BIGINT NOT NULL REFERENCES work.w_team(id),
    old_remaining_matches INTEGER NOT NULL,
    new_remaining_matches INTEGER NOT NULL,
    adjustment_type VARCHAR(24) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_by_user_id BIGINT NOT NULL REFERENCES work.w_user_login(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_discipline_adjustment_values CHECK (old_remaining_matches >= 0 AND new_remaining_matches >= 0),
    CONSTRAINT chk_discipline_adjustment_type CHECK (adjustment_type IN ('ADDED', 'REDUCED', 'CANCELLED', 'CORRECTED'))
);

CREATE INDEX ix_discipline_adjustment_lookup
    ON work.w_discipline_adjustment (competition_id, player_id, created_at DESC, id DESC);

COMMENT ON TABLE work.w_discipline_adjustment IS 'Неизменяемая история ручных корректировок дисквалификаций';
COMMENT ON COLUMN work.w_discipline_adjustment.reason IS 'Обязательное основание решения организатора';

INSERT INTO work.w_site_notification_template (
    code, title_template, summary_template, body_html_template, severity,
    action_url_template, requires_acknowledgement, audience_scope, email_enabled
) VALUES (
    'PLAYER_SUSPENSION_ADJUSTED', 'Изменено наказание: {{playerName}}',
    'Дисквалификация игрока {{playerName}} изменена: осталось матчей — {{remainingMatches}}.',
    '<p>Организатор изменил дисквалификацию игрока <strong>{{playerName}}</strong> команды <strong>«{{teamName}}»</strong> в турнире <strong>«{{tournamentName}}»</strong>.</p><p>Осталось матчей: <strong>{{remainingMatches}}</strong>.</p><p>Причина: {{reason}}</p>',
    'WARNING', '/discipline?season={{seasonId}}&competition={{competitionId}}', TRUE, 'RECIPIENTS', TRUE
)
ON CONFLICT (code) DO UPDATE SET title_template=EXCLUDED.title_template, summary_template=EXCLUDED.summary_template,
 body_html_template=EXCLUDED.body_html_template, action_url_template=EXCLUDED.action_url_template,
 requires_acknowledgement=TRUE, email_enabled=TRUE, active=TRUE, updated_at=NOW();

INSERT INTO mailer.notification_template (code, channel, subject_template, body_template, body_format, is_active)
VALUES ('PLAYER_SUSPENSION_ADJUSTED', 'EMAIL', 'Изменено наказание игрока {{playerName}}',
 '<h2>Изменение дисквалификации</h2><p>Игрок <strong>{{playerName}}</strong>, команда «{{teamName}}», турнир «{{tournamentName}}».</p><p>Осталось матчей: <strong>{{remainingMatches}}</strong>.</p><p>Причина: {{reason}}</p><p><a href="{{actionUrl}}">Открыть дисциплинарный центр</a></p>',
 'HTML', TRUE)
ON CONFLICT (code) DO UPDATE SET subject_template=EXCLUDED.subject_template, body_template=EXCLUDED.body_template,
 body_format='HTML', is_active=TRUE, updated_at=NOW();

