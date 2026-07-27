DROP INDEX IF EXISTS work.ux_w_season_transfer_request_pending_player;

CREATE UNIQUE INDEX IF NOT EXISTS ux_w_season_transfer_request_blocking_player
    ON work.w_season_transfer_request (season_id, player_id)
    WHERE status IN ('PENDING', 'APPROVED');

COMMENT ON INDEX work.ux_w_season_transfer_request_blocking_player IS
    'Не допускает параллельную или повторную трансферную заявку для игрока с незавершенным переходом';
