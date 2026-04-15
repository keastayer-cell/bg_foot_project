DELETE FROM work.w_season_standings_row;

WITH base_teams AS (
    SELECT
        st.season_id,
        team.id AS team_id,
        team.name AS team_name
    FROM work.w_season_team st
    JOIN work.w_team team ON team.id = st.team_id
    WHERE team.active = TRUE
),
verified_matches AS (
    SELECT
        tour.season_id,
        match.home_team_id,
        match.away_team_id,
        protocol.home_score,
        protocol.away_score
    FROM work.w_tour_match match
    JOIN work.w_tour tour ON tour.id = match.tour_id
    JOIN work.w_match_protocol protocol ON protocol.match_id = match.id
    WHERE match.active = TRUE
      AND tour.active = TRUE
      AND tour.published = TRUE
      AND protocol.status = 'VERIFIED'
      AND protocol.home_score IS NOT NULL
      AND protocol.away_score IS NOT NULL
),
team_results AS (
    SELECT
        season_id,
        home_team_id AS team_id,
        1 AS matches_played,
        CASE WHEN home_score > away_score THEN 1 ELSE 0 END AS wins,
        CASE WHEN home_score = away_score THEN 1 ELSE 0 END AS draws,
        CASE WHEN home_score < away_score THEN 1 ELSE 0 END AS losses,
        home_score AS goals_for,
        away_score AS goals_against,
        CASE WHEN home_score > away_score THEN 3 WHEN home_score = away_score THEN 1 ELSE 0 END AS points
    FROM verified_matches

    UNION ALL

    SELECT
        season_id,
        away_team_id AS team_id,
        1 AS matches_played,
        CASE WHEN away_score > home_score THEN 1 ELSE 0 END AS wins,
        CASE WHEN away_score = home_score THEN 1 ELSE 0 END AS draws,
        CASE WHEN away_score < home_score THEN 1 ELSE 0 END AS losses,
        away_score AS goals_for,
        home_score AS goals_against,
        CASE WHEN away_score > home_score THEN 3 WHEN away_score = home_score THEN 1 ELSE 0 END AS points
    FROM verified_matches
),
aggregated AS (
    SELECT
        team_results.season_id,
        team_results.team_id,
        SUM(team_results.matches_played) AS matches_played,
        SUM(team_results.wins) AS wins,
        SUM(team_results.draws) AS draws,
        SUM(team_results.losses) AS losses,
        SUM(team_results.goals_for) AS goals_for,
        SUM(team_results.goals_against) AS goals_against,
        SUM(team_results.points) AS points
    FROM team_results
    GROUP BY team_results.season_id, team_results.team_id
),
prepared AS (
    SELECT
        base_teams.season_id,
        base_teams.team_id,
        base_teams.team_name,
        COALESCE(aggregated.matches_played, 0) AS matches_played,
        COALESCE(aggregated.wins, 0) AS wins,
        COALESCE(aggregated.draws, 0) AS draws,
        COALESCE(aggregated.losses, 0) AS losses,
        COALESCE(aggregated.goals_for, 0) AS goals_for,
        COALESCE(aggregated.goals_against, 0) AS goals_against,
        COALESCE(aggregated.points, 0) AS points,
        COALESCE(aggregated.goals_for, 0) - COALESCE(aggregated.goals_against, 0) AS goal_difference
    FROM base_teams
    LEFT JOIN aggregated
        ON aggregated.season_id = base_teams.season_id
       AND aggregated.team_id = base_teams.team_id
),
ranked AS (
    SELECT
        prepared.*,
        ROW_NUMBER() OVER (
            PARTITION BY prepared.season_id
            ORDER BY prepared.points DESC,
                     prepared.goal_difference DESC,
                     prepared.goals_for DESC,
                     prepared.team_name ASC
        ) AS position
    FROM prepared
)
INSERT INTO work.w_season_standings_row (
    season_id,
    team_id,
    position,
    matches_played,
    wins,
    draws,
    losses,
    goals_for,
    goals_against,
    goal_difference,
    points,
    created_at,
    updated_at
)
SELECT
    ranked.season_id,
    ranked.team_id,
    ranked.position,
    ranked.matches_played,
    ranked.wins,
    ranked.draws,
    ranked.losses,
    ranked.goals_for,
    ranked.goals_against,
    ranked.goal_difference,
    ranked.points,
    NOW(),
    NOW()
FROM ranked;

UPDATE work.w_season_standings_config config
SET last_calculated_at = NOW(),
    updated_at = NOW()
WHERE EXISTS (
    SELECT 1
    FROM work.w_season_standings_row row
    WHERE row.season_id = config.season_id
);