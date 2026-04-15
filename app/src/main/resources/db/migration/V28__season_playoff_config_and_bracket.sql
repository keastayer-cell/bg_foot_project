ALTER TABLE work.w_season_standings_config
    ADD COLUMN technical_defeat_winner_goals INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN technical_defeat_loser_goals INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ranking_rules_json TEXT NOT NULL DEFAULT '["POINTS","GOAL_DIFFERENCE","GOALS_FOR","ALPHABETICAL"]';

ALTER TABLE work.w_season_standings_config
    ADD CONSTRAINT w_season_standings_config_technical_defeat_winner_goals_check CHECK (technical_defeat_winner_goals >= 0),
    ADD CONSTRAINT w_season_standings_config_technical_defeat_loser_goals_check CHECK (technical_defeat_loser_goals >= 0);

COMMENT ON COLUMN work.w_season_standings_config.technical_defeat_winner_goals IS 'Сколько голов получает победитель при техническом поражении соперника';
COMMENT ON COLUMN work.w_season_standings_config.technical_defeat_loser_goals IS 'Сколько голов получает проигравший при техническом поражении';
COMMENT ON COLUMN work.w_season_standings_config.ranking_rules_json IS 'Упорядоченный список правил сортировки турнирной таблицы';

CREATE TABLE work.w_season_playoff_config (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL UNIQUE REFERENCES work.w_season (id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    team_count INTEGER NULL,
    seeding_mode VARCHAR(32) NOT NULL DEFAULT 'TABLE_POSITION',
    pairing_mode VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    pairing_json TEXT NULL,
    third_place_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    round_of_16_legs INTEGER NOT NULL DEFAULT 1,
    quarterfinal_legs INTEGER NOT NULL DEFAULT 1,
    semifinal_legs INTEGER NOT NULL DEFAULT 1,
    final_legs INTEGER NOT NULL DEFAULT 1,
    third_place_legs INTEGER NOT NULL DEFAULT 1,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_season_playoff_config_team_count_check CHECK (team_count IS NULL OR team_count IN (2, 4, 8, 16)),
    CONSTRAINT w_season_playoff_config_round_of_16_legs_check CHECK (round_of_16_legs IN (1, 2)),
    CONSTRAINT w_season_playoff_config_quarterfinal_legs_check CHECK (quarterfinal_legs IN (1, 2)),
    CONSTRAINT w_season_playoff_config_semifinal_legs_check CHECK (semifinal_legs IN (1, 2)),
    CONSTRAINT w_season_playoff_config_final_legs_check CHECK (final_legs IN (1, 2)),
    CONSTRAINT w_season_playoff_config_third_place_legs_check CHECK (third_place_legs IN (1, 2))
);

CREATE TABLE work.w_season_playoff_bracket (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL UNIQUE REFERENCES work.w_season (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    regular_season_completed BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMPTZ NULL,
    based_on_standings_calculated_at TIMESTAMPTZ NULL,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE work.w_season_playoff_tie (
    id BIGSERIAL PRIMARY KEY,
    bracket_id BIGINT NOT NULL REFERENCES work.w_season_playoff_bracket (id) ON DELETE CASCADE,
    round_code VARCHAR(32) NOT NULL,
    round_order INTEGER NOT NULL,
    slot_order INTEGER NOT NULL,
    leg_count INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(120) NULL,
    home_seed INTEGER NULL,
    away_seed INTEGER NULL,
    home_source_tie_id BIGINT NULL REFERENCES work.w_season_playoff_tie (id) ON DELETE SET NULL,
    home_source_result VARCHAR(16) NULL,
    away_source_tie_id BIGINT NULL REFERENCES work.w_season_playoff_tie (id) ON DELETE SET NULL,
    away_source_result VARCHAR(16) NULL,
    home_team_id BIGINT NULL REFERENCES work.w_team (id) ON DELETE SET NULL,
    away_team_id BIGINT NULL REFERENCES work.w_team (id) ON DELETE SET NULL,
    winner_team_id BIGINT NULL REFERENCES work.w_team (id) ON DELETE SET NULL,
    aggregate_home_score INTEGER NULL,
    aggregate_away_score INTEGER NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_w_season_playoff_tie_round_slot UNIQUE (bracket_id, round_code, slot_order),
    CONSTRAINT w_season_playoff_tie_leg_count_check CHECK (leg_count IN (1, 2)),
    CONSTRAINT w_season_playoff_tie_home_source_result_check CHECK (home_source_result IS NULL OR home_source_result IN ('WINNER', 'LOSER')),
    CONSTRAINT w_season_playoff_tie_away_source_result_check CHECK (away_source_result IS NULL OR away_source_result IN ('WINNER', 'LOSER'))
);

CREATE INDEX idx_w_season_playoff_tie_bracket_round_order
    ON work.w_season_playoff_tie (bracket_id, round_order, slot_order);

INSERT INTO work.w_season_playoff_config (season_id, enabled, team_count)
SELECT season.id, season.playoff_enabled, season.playoff_team_count
FROM work.w_season season
WHERE NOT EXISTS (
    SELECT 1
    FROM work.w_season_playoff_config config
    WHERE config.season_id = season.id
);

COMMENT ON TABLE work.w_season_playoff_config IS 'Конфигурация плей-офф внутри сезона';
COMMENT ON COLUMN work.w_season_playoff_config.team_count IS 'Сколько команд попадает в плей-офф';
COMMENT ON COLUMN work.w_season_playoff_config.seeding_mode IS 'Как заполняются участники сетки';
COMMENT ON COLUMN work.w_season_playoff_config.pairing_mode IS 'Как формируются пары стартового раунда';
COMMENT ON COLUMN work.w_season_playoff_config.pairing_json IS 'Кастомные пары стартового раунда в JSON';
COMMENT ON COLUMN work.w_season_playoff_config.third_place_enabled IS 'Нужен ли матч за третье место';

COMMENT ON TABLE work.w_season_playoff_bracket IS 'Текущий snapshot сетки плей-офф сезона';
COMMENT ON COLUMN work.w_season_playoff_bracket.regular_season_completed IS 'Завершен ли регулярный этап к моменту генерации';
COMMENT ON COLUMN work.w_season_playoff_bracket.based_on_standings_calculated_at IS 'На каком расчете таблицы строилась сетка';

COMMENT ON TABLE work.w_season_playoff_tie IS 'Отдельная пара внутри сетки плей-офф';
COMMENT ON COLUMN work.w_season_playoff_tie.round_code IS 'Раунд плей-офф: ROUND_OF_16, QUARTERFINAL, SEMIFINAL, FINAL, THIRD_PLACE';
COMMENT ON COLUMN work.w_season_playoff_tie.slot_order IS 'Порядок пары внутри раунда';
COMMENT ON COLUMN work.w_season_playoff_tie.leg_count IS 'Количество матчей в паре';
COMMENT ON COLUMN work.w_season_playoff_tie.home_seed IS 'Посев домашней команды для стартового раунда';
COMMENT ON COLUMN work.w_season_playoff_tie.away_seed IS 'Посев гостевой команды для стартового раунда';