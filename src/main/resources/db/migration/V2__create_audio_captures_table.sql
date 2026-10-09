CREATE TABLE audio_captures
(
    id                UUID                           NOT NULL,
    user_id           UUID                           NOT NULL,
    content_type      VARCHAR(100)                   NOT NULL,
    size_bytes        BIGINT                         NOT NULL,
    duration_seconds  NUMERIC(6, 2),
    original_filename VARCHAR(255),
    storage_path      VARCHAR(500)                   NOT NULL,
    transcript        TEXT,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_audio_captures PRIMARY KEY (id)
);

CREATE INDEX idx_audio_captures_user_id ON audio_captures (user_id);
CREATE INDEX idx_audio_captures_created_at ON audio_captures (created_at DESC);

ALTER TABLE expenses
    ALTER COLUMN capture_id DROP NOT NULL;

UPDATE expenses
SET capture_id = NULL
WHERE origin = 'MANUAL';

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_audio_capture
    FOREIGN KEY (capture_id) REFERENCES audio_captures (id)
    ON DELETE SET NULL;
