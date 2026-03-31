CREATE TABLE IF NOT EXISTS default.events (
    project_key String,
    session_id String,
    user_agent String,
    url String,
    event_type String,
    event_timestamp DateTime64(3, 'Asia/Seoul'),
    payload String,
    created_at DateTime DEFAULT now()
)
    ENGINE = MergeTree()
    PARTITION BY toYYYYMM(event_timestamp)
ORDER BY (project_key, event_type, event_timestamp);