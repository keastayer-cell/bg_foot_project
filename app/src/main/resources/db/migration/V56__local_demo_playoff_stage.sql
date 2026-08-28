ALTER TABLE work.w_demo_dataset
    DROP CONSTRAINT chk_w_demo_dataset_stage;

ALTER TABLE work.w_demo_dataset
    ADD CONSTRAINT chk_w_demo_dataset_stage
        CHECK (stage IN ('BASE', 'SCHEDULE', 'RESULTS', 'TRANSFERS', 'PLAYOFF'));
