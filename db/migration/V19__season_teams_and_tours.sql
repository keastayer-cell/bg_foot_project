CREATE TABLE work.w_season_team (
    id                 BIGSERIAL PRIMARY KEY,
    season_id          BIGINT NOT NULL REFERENCES work.w_season(id),
    team_id            BIGINT NOT NULL REFERENCES work.w_team(id),
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_season_team_unique UNIQUE (season_id, team_id)
);

COMMENT ON TABLE work.w_season_team IS 'Связь команд с сезонами';
COMMENT ON COLUMN work.w_season_team.season_id IS 'Сезон участия';
COMMENT ON COLUMN work.w_season_team.team_id IS 'Команда, закрепленная за сезоном';

CREATE TABLE work.w_tour (
    id                 BIGSERIAL PRIMARY KEY,
    season_id          BIGINT NOT NULL REFERENCES work.w_season(id),
    name               VARCHAR(120) NOT NULL,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_tour_name_per_season_unique UNIQUE (season_id, name)
);

COMMENT ON TABLE work.w_tour IS 'Туры сезона';
COMMENT ON COLUMN work.w_tour.season_id IS 'Сезон, к которому относится тур';
COMMENT ON COLUMN work.w_tour.name IS 'Название тура';

CREATE TABLE work.w_tour_match (
    id                 BIGSERIAL PRIMARY KEY,
    tour_id            BIGINT NOT NULL REFERENCES work.w_tour(id),
    home_team_id       BIGINT NOT NULL REFERENCES work.w_team(id),
    away_team_id       BIGINT NOT NULL REFERENCES work.w_team(id),
    kickoff_at         TIMESTAMPTZ NOT NULL,
    created_by_user_id BIGINT REFERENCES work.w_user_login(id),
    updated_by_user_id BIGINT REFERENCES work.w_user_login(id),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_tour_match_teams_must_differ CHECK (home_team_id <> away_team_id)
);

COMMENT ON TABLE work.w_tour_match IS 'Матчи внутри тура';
COMMENT ON COLUMN work.w_tour_match.tour_id IS 'Тур, в котором проводится матч';
COMMENT ON COLUMN work.w_tour_match.home_team_id IS 'Домашняя команда';
COMMENT ON COLUMN work.w_tour_match.away_team_id IS 'Гостевая команда';
COMMENT ON COLUMN work.w_tour_match.kickoff_at IS 'Дата и время начала матча';