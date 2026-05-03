-- 1. 세션 정보 테이블 (중복 방지용 ReplacingMergeTree 사용)
-- api_key와 session_id가 같으면 나중에 하나로 병합되어 중복 저장을 방지합니다.
CREATE TABLE IF NOT EXISTS event_sessions (
    session_id String,
    user_agent String,
    url String,
    api_key String
) ENGINE = ReplacingMergeTree()
    ORDER BY (api_key, session_id);

-- 2. 이벤트 상세 정보 테이블 (MergeTree 사용)
-- css_selector는 Nullable(String)로 설정하여 SDK에서 값이 없어도 저장 가능합니다.
CREATE TABLE IF NOT EXISTS event_details (
    session_id String,
    event_type String,
    timestamp DateTime64(3, 'Asia/Seoul'),
    payload String,
    css_selector Nullable(String)
) ENGINE = MergeTree()
    ORDER BY (session_id, timestamp);