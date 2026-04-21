ALTER TABLE work.w_season_transfer_request
    DROP CONSTRAINT IF EXISTS chk_w_season_transfer_request_status;

ALTER TABLE work.w_season_transfer_request
    ADD CONSTRAINT chk_w_season_transfer_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED'));

COMMENT ON COLUMN work.w_season_transfer_request.status IS 'Статус заявки: PENDING, APPROVED, REJECTED, REVOKED';

INSERT INTO work.w_api_access_rule(role_id, url_pattern, http_method, active)
SELECT r.id, '/api/team-rep/transfers/*/revoke', 'POST', TRUE
FROM work.w_role r
WHERE r.code = 'TEAM_REP'
ON CONFLICT DO NOTHING;