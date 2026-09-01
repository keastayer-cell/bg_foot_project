ALTER TABLE work.w_match_lineup_player
    ADD COLUMN is_starter BOOLEAN NOT NULL DEFAULT TRUE;

WITH ranked_players AS (
    SELECT
        mlp.id,
        ROW_NUMBER() OVER (PARTITION BY mlp.lineup_id ORDER BY mlp.sort_order, mlp.id) AS position,
        s.players_on_field
    FROM work.w_match_lineup_player mlp
    JOIN work.w_match_lineup ml ON ml.id = mlp.lineup_id
    JOIN work.w_tour_match tm ON tm.id = ml.match_id
    JOIN work.w_tour t ON t.id = tm.tour_id
    JOIN work.w_season s ON s.id = t.season_id
)
UPDATE work.w_match_lineup_player mlp
SET is_starter = ranked.position <= ranked.players_on_field
FROM ranked_players ranked
WHERE ranked.id = mlp.id;

COMMENT ON COLUMN work.w_match_lineup_player.is_starter IS
    'TRUE для игрока основного состава, FALSE для запасного';
