ALTER TABLE work.w_cup_tie
    ADD COLUMN home_penalty_score INTEGER,
    ADD COLUMN away_penalty_score INTEGER;

ALTER TABLE work.w_cup_tie
    ADD CONSTRAINT chk_w_cup_tie_penalty_scores CHECK (
        (home_penalty_score IS NULL AND away_penalty_score IS NULL)
        OR (home_penalty_score >= 0 AND away_penalty_score >= 0 AND home_penalty_score <> away_penalty_score)
    );
