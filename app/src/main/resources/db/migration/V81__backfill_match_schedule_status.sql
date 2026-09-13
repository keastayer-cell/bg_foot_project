UPDATE work.w_tour_match match
SET schedule_status = CASE
        WHEN protocol.home_technical_defeat OR protocol.away_technical_defeat THEN 'TECHNICAL_RESULT'
        ELSE 'COMPLETED'
    END,
    schedule_change_reason = CASE
        WHEN (protocol.home_technical_defeat OR protocol.away_technical_defeat)
            THEN COALESCE(match.schedule_change_reason, NULLIF(BTRIM(protocol.notes), ''))
        ELSE match.schedule_change_reason
    END
FROM work.w_match_protocol protocol
WHERE protocol.match_id = match.id
  AND protocol.status = 'VERIFIED'
  AND match.schedule_status NOT IN ('COMPLETED', 'TECHNICAL_RESULT');

COMMENT ON COLUMN work.w_tour_match.schedule_status IS
    'Состояние матча: запланирован, перенесён, отменён, завершён или технический результат';
