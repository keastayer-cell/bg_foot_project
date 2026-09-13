ALTER TABLE work.w_tour_match DROP CONSTRAINT chk_w_tour_match_schedule_status;
ALTER TABLE work.w_tour_match
    ADD CONSTRAINT chk_w_tour_match_schedule_status
    CHECK (schedule_status IN ('SCHEDULED','RESCHEDULED','CANCELLED','COMPLETED','TECHNICAL_RESULT'));

INSERT INTO work.w_site_notification_template (
 code,title_template,summary_template,body_html_template,severity,action_url_template,
 requires_acknowledgement,audience_scope,email_enabled
) VALUES (
 'MATCH_SCHEDULE_CHANGED','Изменение матча {{matchName}}','{{changeSummary}}',
 '<p><strong>{{matchName}}</strong></p><p>{{changeSummary}}</p><p>Площадка: {{venueName}}.</p><p>Причина: {{reason}}</p>',
 'WARNING','/matches/{{matchId}}',FALSE,'RECIPIENTS',TRUE
)
ON CONFLICT (code) DO UPDATE SET title_template=EXCLUDED.title_template,summary_template=EXCLUDED.summary_template,
 body_html_template=EXCLUDED.body_html_template,action_url_template=EXCLUDED.action_url_template,
 email_enabled=TRUE,active=TRUE,updated_at=NOW();

INSERT INTO mailer.notification_template (code,channel,subject_template,body_template,body_format,is_active)
VALUES ('MATCH_SCHEDULE_CHANGED','EMAIL','Изменение матча {{matchName}}',
 '<h2>Изменение расписания</h2><p><strong>{{matchName}}</strong></p><p>{{changeSummary}}</p><p>Площадка: {{venueName}}.</p><p>Причина: {{reason}}</p><p><a href="{{actionUrl}}">Открыть матч</a></p>',
 'HTML',TRUE)
ON CONFLICT (code) DO UPDATE SET subject_template=EXCLUDED.subject_template,body_template=EXCLUDED.body_template,
 body_format='HTML',is_active=TRUE,updated_at=NOW();

