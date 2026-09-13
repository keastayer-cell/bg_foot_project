ALTER TABLE work.w_tour_match
    ADD COLUMN venue_id BIGINT REFERENCES work.w_league_venue(id),
    ADD COLUMN schedule_status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED',
    ADD COLUMN original_kickoff_at TIMESTAMPTZ,
    ADD COLUMN schedule_change_reason VARCHAR(500);

ALTER TABLE work.w_tour_match
    ADD CONSTRAINT chk_w_tour_match_schedule_status
    CHECK (schedule_status IN ('SCHEDULED', 'RESCHEDULED', 'CANCELLED'));

CREATE INDEX ix_w_tour_match_public_calendar
    ON work.w_tour_match (kickoff_at, schedule_status, venue_id)
    WHERE active = TRUE;

COMMENT ON COLUMN work.w_tour_match.venue_id IS 'Площадка проведения матча';
COMMENT ON COLUMN work.w_tour_match.schedule_status IS 'Состояние расписания: запланирован, перенесён или отменён';
COMMENT ON COLUMN work.w_tour_match.original_kickoff_at IS 'Исходные дата и время до переноса';
COMMENT ON COLUMN work.w_tour_match.schedule_change_reason IS 'Причина переноса или отмены';

