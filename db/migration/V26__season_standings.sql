CREATE TABLE work.w_season_standings_config (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL UNIQUE REFERENCES work.w_season (id) ON DELETE CASCADE,
    win_points INTEGER NOT NULL DEFAULT 3,
    draw_points INTEGER NOT NULL DEFAULT 1,
    loss_points INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NULL,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_season_standings_config_win_points_check CHECK (win_points >= 0),
    CONSTRAINT w_season_standings_config_draw_points_check CHECK (draw_points >= 0),
    CONSTRAINT w_season_standings_config_loss_points_check CHECK (loss_points >= 0)
);

CREATE TABLE work.w_season_standings_row (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES work.w_season (id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES work.w_team (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    matches_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    goals_for INTEGER NOT NULL DEFAULT 0,
    goals_against INTEGER NOT NULL DEFAULT 0,
    goal_difference INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_season_standings_row_season_team UNIQUE (season_id, team_id),
    CONSTRAINT uq_w_season_standings_row_season_position UNIQUE (season_id, position),
    CONSTRAINT w_season_standings_row_position_check CHECK (position >= 1),
    CONSTRAINT w_season_standings_row_matches_played_check CHECK (matches_played >= 0),
    CONSTRAINT w_season_standings_row_wins_check CHECK (wins >= 0),
    CONSTRAINT w_season_standings_row_draws_check CHECK (draws >= 0),
    CONSTRAINT w_season_standings_row_losses_check CHECK (losses >= 0),
    CONSTRAINT w_season_standings_row_goals_for_check CHECK (goals_for >= 0),
    CONSTRAINT w_season_standings_row_goals_against_check CHECK (goals_against >= 0)
);

CREATE INDEX idx_w_season_standings_row_season_position
    ON work.w_season_standings_row (season_id, position);

INSERT INTO work.w_season_standings_config (season_id)
SELECT season.id
FROM work.w_season season
WHERE NOT EXISTS (
    SELECT 1
    FROM work.w_season_standings_config config
    WHERE config.season_id = season.id
);

COMMENT ON TABLE work.w_season_standings_config IS 'Конфигурация правил турнирной таблицы сезона';
COMMENT ON COLUMN work.w_season_standings_config.win_points IS 'Очки за победу';
COMMENT ON COLUMN work.w_season_standings_config.draw_points IS 'Очки за ничью';
COMMENT ON COLUMN work.w_season_standings_config.loss_points IS 'Очки за поражение';
COMMENT ON COLUMN work.w_season_standings_config.last_calculated_at IS 'Когда турнирная таблица была пересчитана последний раз';

COMMENT ON TABLE work.w_season_standings_row IS 'Предрассчитанные строки турнирной таблицы сезона';
COMMENT ON COLUMN work.w_season_standings_row.position IS 'Место команды в таблице';
COMMENT ON COLUMN work.w_season_standings_row.matches_played IS 'Количество сыгранных матчей';
COMMENT ON COLUMN work.w_season_standings_row.goals_for IS 'Забитые мячи';
COMMENT ON COLUMN work.w_season_standings_row.goals_against IS 'Пропущенные мячи';
COMMENT ON COLUMN work.w_season_standings_row.goal_difference IS 'Разница мячей';
COMMENT ON COLUMN work.w_season_standings_row.points IS 'Набранные очки';