# 🚀 Landom Backend

랜딩 페이지 행동 로그를 수집하고, 동적 섹션 매핑을 통해 실시간 퍼널 인사이트를 제공하는 분석 엔진입니다.

## 🏗️ 1. Core Architecture
*   **Hybrid Data Layer**: 정형 데이터(MySQL)와 비정형 로그(ClickHouse)의 분리 운영
*   **OLAP Optimized**: 수백만 건의 이벤트를 초단위로 집계하기 위한 ClickHouse 기반 통계 구조
*   **Decoupled Analytics**: 수집 시점의 raw 로그와 조회 시점의 분석 기준(Section)을 분리하여 유연성 확보

## 🌊 2. Data Flow (Pipeline)
1.  **Ingest**: 클라이언트 SDK → Spring Boot 서버 (Event Logging)
2.  **Clean**: `ReplacingMergeTree`를 이용한 중복 세션 실시간 병합 (Deduplication)
3.  **Store**: `event_sessions`(세션 메타) / `event_details`(행동 로그) 분리 저장
4.  **Map**: API 요청 시 MySQL의 섹션 설정(Selector)을 로그 데이터에 동적 투영
5.  **Aggregate**: `argMax`, `dateDiff` 등 ClickHouse 함수를 활용한 실시간 지표(전환율, 체류시간) 산출

## 🧠 3. Key Technical Points
*   **Prefix Match Logic**: `startsWith` 기반의 Selector 매핑으로 섹션 내 모든 하위 요소 클릭 추적
*   **Dynamic Funneling**: 코드 수정 없이 API 설정만으로 과거 데이터에 대한 퍼널 재구성 가능
*   **Session Status Tracking**: 마지막 이벤트 시각 및 도달 섹션을 기준으로 실시간 상태(Exploring/Converted/Drop) 판별
*   **Weekly Bucketing**: `toRelativeWeekNum`을 활용한 주차별(YYYY-MM-WX) 지표 트렌드 가공

## 🛠️ Tech Stack
*   **Language**: Java 21 (Spring Boot 3.4)
*   **Storage**: ClickHouse (Analytics), MySQL (Metadata)
*   **Interface**: Spring JDBC (JdbcTemplate), Spring Data JPA
*   **Documentation**: Swagger (SpringDoc OpenAPI)
