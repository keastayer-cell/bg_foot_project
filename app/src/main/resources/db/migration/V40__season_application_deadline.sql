ALTER TABLE work.w_season
    ADD COLUMN application_deadline date;

COMMENT ON COLUMN work.w_season.application_deadline IS 'Дата включительно, до которой представители команды могут добавлять игроков в заявку сезона';