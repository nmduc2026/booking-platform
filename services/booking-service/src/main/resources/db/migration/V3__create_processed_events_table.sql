CREATE TABLE processed_events (
    event_id      VARCHAR(120) PRIMARY KEY,
    processed_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
