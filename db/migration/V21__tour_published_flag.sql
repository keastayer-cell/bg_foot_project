ALTER TABLE work.w_tour
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE work.w_tour t
SET published = TRUE
WHERE EXISTS (
    SELECT 1
    FROM work.w_tour_match tm
    WHERE tm.tour_id = t.id
      AND tm.active = TRUE
);

COMMENT ON COLUMN work.w_tour.published IS 'Признак опубликованности тура для публичного отображения';

CREATE INDEX idx_w_tour_season_published_active
    ON work.w_tour (season_id, published, active, sort_order, id);