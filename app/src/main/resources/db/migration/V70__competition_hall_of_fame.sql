ALTER TABLE work.w_competition
    ADD COLUMN IF NOT EXISTS honors_published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS honors_formation VARCHAR(40),
    ADD COLUMN IF NOT EXISTS honors_updated_at TIMESTAMPTZ;

CREATE TABLE work.w_competition_award (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id) ON DELETE CASCADE,
    award_code VARCHAR(48),
    title VARCHAR(120) NOT NULL,
    winner_type VARCHAR(16) NOT NULL,
    player_id BIGINT REFERENCES work.w_player(id),
    team_id BIGINT REFERENCES work.w_team(id),
    winner_name_snapshot VARCHAR(180) NOT NULL,
    team_name_snapshot VARCHAR(180),
    stat_value INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_competition_award_winner_type CHECK (winner_type IN ('PLAYER', 'TEAM'))
);

CREATE INDEX ix_competition_award_competition ON work.w_competition_award(competition_id, sort_order, id);

CREATE TABLE work.w_competition_selection_slot (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES work.w_competition(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES work.w_player(id),
    player_name_snapshot VARCHAR(180) NOT NULL,
    team_name_snapshot VARCHAR(180),
    position_label VARCHAR(40) NOT NULL,
    x_percent INTEGER NOT NULL,
    y_percent INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_selection_slot_x CHECK (x_percent BETWEEN 5 AND 95),
    CONSTRAINT ck_selection_slot_y CHECK (y_percent BETWEEN 5 AND 95)
);

CREATE INDEX ix_competition_selection_slot_competition ON work.w_competition_selection_slot(competition_id, sort_order, id);

COMMENT ON COLUMN work.w_competition.honors_published IS 'Признак публикации итогов соревнования в Зале славы';
COMMENT ON COLUMN work.w_competition.honors_formation IS 'Название схемы символической сборной';
COMMENT ON COLUMN work.w_competition.honors_updated_at IS 'Дата последнего сохранения наград';
COMMENT ON TABLE work.w_competition_award IS 'Зафиксированные командные и индивидуальные награды соревнования';
COMMENT ON COLUMN work.w_competition_award.competition_id IS 'Соревнование, в котором вручена награда';
COMMENT ON COLUMN work.w_competition_award.award_code IS 'Системный код награды, если он задан';
COMMENT ON COLUMN work.w_competition_award.title IS 'Отображаемое название награды';
COMMENT ON COLUMN work.w_competition_award.winner_type IS 'Тип победителя: игрок или команда';
COMMENT ON COLUMN work.w_competition_award.player_id IS 'Игрок-победитель для индивидуальной награды';
COMMENT ON COLUMN work.w_competition_award.team_id IS 'Команда-победитель для командной награды';
COMMENT ON COLUMN work.w_competition_award.winner_name_snapshot IS 'Имя победителя на момент публикации';
COMMENT ON COLUMN work.w_competition_award.team_name_snapshot IS 'Название команды игрока на момент публикации';
COMMENT ON COLUMN work.w_competition_award.stat_value IS 'Числовой результат для автоматически рассчитанной награды';
COMMENT ON COLUMN work.w_competition_award.sort_order IS 'Порядок показа награды';
COMMENT ON TABLE work.w_competition_selection_slot IS 'Позиции игроков символической сборной соревнования';
COMMENT ON COLUMN work.w_competition_selection_slot.competition_id IS 'Соревнование, для которого составлена сборная';
COMMENT ON COLUMN work.w_competition_selection_slot.player_id IS 'Игрок символической сборной';
COMMENT ON COLUMN work.w_competition_selection_slot.player_name_snapshot IS 'Имя игрока на момент публикации';
COMMENT ON COLUMN work.w_competition_selection_slot.team_name_snapshot IS 'Название команды игрока на момент публикации';
COMMENT ON COLUMN work.w_competition_selection_slot.position_label IS 'Короткое обозначение позиции на поле';
COMMENT ON COLUMN work.w_competition_selection_slot.x_percent IS 'Горизонтальная координата игрока на схеме в процентах';
COMMENT ON COLUMN work.w_competition_selection_slot.y_percent IS 'Вертикальная координата игрока на схеме в процентах';
COMMENT ON COLUMN work.w_competition_selection_slot.sort_order IS 'Порядок игроков в редакторе и выдаче';
