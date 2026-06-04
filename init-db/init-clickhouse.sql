-- 1. 세션 정보 테이블 (중복 방지용 ReplacingMergeTree 사용)
-- api_key와 session_id가 같으면 나중에 하나로 병합되어 중복 저장을 방지합니다.
CREATE TABLE IF NOT EXISTS event_sessions (
    session_id String,
    user_agent String,
    url String,
    api_key String,
    status String DEFAULT 'EXPLORING',
    status_updated_at DateTime64(3, 'Asia/Seoul') DEFAULT now64(3)
) ENGINE = ReplacingMergeTree()
    ORDER BY (api_key, session_id);

ALTER TABLE event_sessions
    ADD COLUMN IF NOT EXISTS status String DEFAULT 'EXPLORING';

ALTER TABLE event_sessions
    ADD COLUMN IF NOT EXISTS status_updated_at DateTime64(3, 'Asia/Seoul') DEFAULT now64(3);

-- 2. 이벤트 상세 정보 테이블 (MergeTree 사용)
-- rrweb 세션 리플레이도 event_type = 'replay', payload = rrweb event JSON 문자열로 함께 저장합니다.
-- css_selector는 Nullable(String)로 설정하여 SDK에서 값이 없어도 저장 가능합니다.
CREATE TABLE IF NOT EXISTS event_details (
    api_key String,
    session_id String,
    event_type String,
    timestamp DateTime64(3, 'Asia/Seoul'),
    payload String,
    css_selector Nullable(String),
    INDEX idx_event_details_api_key api_key TYPE bloom_filter(0.01) GRANULARITY 1
) ENGINE = MergeTree()
    ORDER BY (api_key, session_id, timestamp);

ALTER TABLE event_details
    ADD COLUMN IF NOT EXISTS api_key String DEFAULT '';

ALTER TABLE event_details
    ADD INDEX IF NOT EXISTS idx_event_details_api_key api_key TYPE bloom_filter(0.01) GRANULARITY 1;

CREATE TABLE IF NOT EXISTS event_session_event_summary (
    api_key String,
    session_id String,
    start_time_state AggregateFunction(min, DateTime64(3, 'Asia/Seoul')),
    end_time_state AggregateFunction(max, DateTime64(3, 'Asia/Seoul')),
    ping_count_state AggregateFunction(sum, UInt64),
    event_count_state AggregateFunction(count),
    last_selector_state AggregateFunction(argMax, Nullable(String), DateTime64(3, 'Asia/Seoul'))
) ENGINE = AggregatingMergeTree()
    ORDER BY (api_key, session_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS event_session_event_summary_mv
TO event_session_event_summary
AS
SELECT
    api_key,
    session_id,
    minState(timestamp) AS start_time_state,
    maxState(timestamp) AS end_time_state,
    sumState(toUInt64(event_type = 'ping')) AS ping_count_state,
    countState() AS event_count_state,
    argMaxState(css_selector, timestamp) AS last_selector_state
FROM event_details
WHERE event_type != 'replay'
GROUP BY api_key, session_id;
