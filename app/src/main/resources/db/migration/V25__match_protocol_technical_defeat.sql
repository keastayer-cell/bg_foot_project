ALTER TABLE work.w_match_protocol
    ADD COLUMN home_technical_defeat BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN away_technical_defeat BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE work.w_match_protocol
    ADD CONSTRAINT w_match_protocol_single_technical_defeat CHECK (
        NOT (home_technical_defeat AND away_technical_defeat)
    );

COMMENT ON COLUMN work.w_match_protocol.home_technical_defeat IS 'Признак технического поражения хозяев';
COMMENT ON COLUMN work.w_match_protocol.away_technical_defeat IS 'Признак технического поражения гостей';