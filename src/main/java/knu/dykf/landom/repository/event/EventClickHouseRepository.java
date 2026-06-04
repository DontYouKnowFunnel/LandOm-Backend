package knu.dykf.landom.repository.event;

import knu.dykf.landom.dto.request.event.SdkEventRequest;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Repository
public class EventClickHouseRepository {

    private static final int SECTION_BEHAVIOR_SESSION_LIMIT = 10;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EventClickHouseRepository(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveAll(String projectKey, SdkEventRequest request, String ctaSectionSelector) {
        String eventSql = """
            INSERT INTO event_details (api_key, session_id, event_type, timestamp, payload, css_selector) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = request.events().stream()
                .map(event -> new Object[]{
                        projectKey,
                        request.sessionId(),
                        event.type(),
                        new Timestamp(event.timestamp()),
                        serializePayload(event),
                        event.cssSelector()
                })
                .toList();

        jdbcTemplate.batchUpdate(eventSql, batchArgs);
        saveSessionSnapshot(projectKey, request, ctaSectionSelector);
    }

    private void saveSessionSnapshot(String projectKey, SdkEventRequest request, String ctaSectionSelector) {
        String status = resolveSessionStatus(projectKey, request.sessionId(), ctaSectionSelector);
        Timestamp statusUpdatedAt = new Timestamp(System.currentTimeMillis());

        String sessionSql = """
            INSERT INTO event_sessions (session_id, user_agent, url, api_key, status, status_updated_at) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(sessionSql,
                request.sessionId(),
                request.userAgent(),
                request.url(),
                projectKey,
                status,
                statusUpdatedAt);
    }

    private String resolveSessionStatus(String projectKey, String sessionId, String ctaSectionSelector) {
        String sql = """
            SELECT multiIf(
                (
                    countIf(event_type = 'click' AND ? != '' AND %s) > 0
                    OR (
                        countIf(? != '' AND %s) > 0
                        AND countIf(event_type = 'click') > 0
                        AND maxIf(timestamp, event_type = 'click') >= minIf(timestamp, ? != '' AND %s)
                    )
                ),
                'CONVERTED',
                countIf(event_type = 'exit') > 0,
                'DROP',
                'EXPLORING'
            ) AS status
            FROM event_details
            WHERE api_key = ?
            AND session_id = ?
            AND event_type != 'replay'
        """.formatted(
                sectionSelectorCondition(),
                sectionSelectorCondition(),
                sectionSelectorCondition()
        );

        String status = jdbcTemplate.queryForObject(sql, String.class,
                ctaSectionSelector, ctaSectionSelector, ctaSectionSelector,
                ctaSectionSelector, ctaSectionSelector, ctaSectionSelector,
                ctaSectionSelector, ctaSectionSelector, ctaSectionSelector,
                projectKey,
                sessionId);

        return status == null ? "EXPLORING" : status;
    }

    @SneakyThrows
    private String serializePayload(SdkEventRequest.EventDetail event) {
        if ("replay".equals(event.type()) && isCompressedGzipPayload(event.payload())) {
            return decompressReplayPayload(event.payload());
        }

        return objectMapper.writeValueAsString(event.payload());
    }

    private boolean isCompressedGzipPayload(Map<String, Object> payload) {
        if (payload == null) {
            return false;
        }

        return Boolean.TRUE.equals(payload.get("compressed"))
                && "gzip".equals(payload.get("compression"))
                && "base64".equals(payload.get("encoding"))
                && payload.get("data") instanceof String;
    }

    @SneakyThrows
    private String decompressReplayPayload(Map<String, Object> payload) {
        String encodedData = (String) payload.get("data");
        if (!isBase64(encodedData)) {
            throw new CustomException(ErrorCode.EVENT_PAYLOAD_INVALID);
        }

        byte[] compressedBytes = Base64.getDecoder().decode(encodedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
        String json = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
        JsonNode restoredPayload = objectMapper.readTree(json);

        JsonNode event = restoredPayload.get("event");
        if (event == null || event.isNull()) {
            throw new CustomException(ErrorCode.EVENT_PAYLOAD_INVALID);
        }

        return objectMapper.writeValueAsString(restoredPayload);
    }

    private boolean isBase64(String encodedData) {
        return encodedData != null
                && encodedData.length() % 4 == 0
                && encodedData.matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    private String sectionSelectorCondition() {
        return sectionSelectorCondition("css_selector");
    }

    private String sectionSelectorCondition(String selectorColumn) {
        return "(ifNull(%s, '') = ? OR ifNull(%s, '') LIKE concat(?, ' > %%'))"
                .formatted(selectorColumn, selectorColumn);
    }

    public long getTotalSessionCount(String apiKey) {
        String sql = """
            SELECT count()
            FROM (
                SELECT session_id
                FROM event_session_event_summary
                WHERE api_key = ?
                GROUP BY session_id
            )
        """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, apiKey);
        return count == null ? 0L : count;
    }

    private String latestSessionStatusSql() {
        return """
                SELECT
                    session_id,
                    argMax(user_agent, status_updated_at) AS user_agent,
                    argMax(url, status_updated_at) AS url,
                    argMax(status, status_updated_at) AS status
                FROM event_sessions
                WHERE api_key = ?
                GROUP BY session_id
                """;
    }

    private String eventSessionSummarySql() {
        return """
                SELECT
                    session_id,
                    minMerge(start_time_state) AS start_time,
                    maxMerge(end_time_state) AS end_time,
                    greatest(
                        toFloat64(dateDiff('second', start_time, end_time)),
                        toFloat64(sumMerge(ping_count_state) * 5)
                    ) AS duration_seconds,
                    countMerge(event_count_state) AS event_count,
                    argMaxMerge(last_selector_state) AS last_selector
                FROM event_session_event_summary
                WHERE api_key = ?
                GROUP BY session_id
                """;
    }

    private String eventSessionSummaryForInactiveTimeoutSql() {
        return """
                SELECT
                    api_key,
                    session_id,
                    maxMerge(end_time_state) AS last_event_time
                FROM event_session_event_summary
                GROUP BY api_key, session_id
                """;
    }

    public Map<String, Object> getSummaryStats(String apiKey) {
        String sql = """
        SELECT
            count(*) AS total_sessions,
            avg(e.duration_seconds) AS avg_total_duration,
            countIf(s.status = 'CONVERTED') AS converted_sessions
        FROM (
            %s
        ) AS e
        JOIN (
            %s
        ) AS s ON e.session_id = s.session_id
    """.formatted(eventSessionSummarySql(), latestSessionStatusSql());

        return jdbcTemplate.queryForMap(sql, apiKey, apiKey);
    }

    public List<TrendRawDto> getWeeklyTrends(String apiKey) {
        String sql = """
        SELECT
            period,
            round(sum(event_count) / count(*), 0) AS avg_score,
            count(*) AS total_sessions,
            countIf(is_converted) AS converted_sessions
        FROM (
            SELECT
                concat(toString(toYear(e.start_time)), '-',
                       leftPad(toString(toMonth(e.start_time)), 2, '0'), '-W',
                       toString(toRelativeWeekNum(e.start_time) - toRelativeWeekNum(toStartOfMonth(e.start_time)) + 1)) AS period,
                e.event_count AS event_count,
                s.status = 'CONVERTED' AS is_converted
            FROM (
                %s
            ) AS e
            JOIN (
                %s
            ) AS s ON e.session_id = s.session_id
            )
        GROUP BY period
        ORDER BY period ASC
    """.formatted(eventSessionSummarySql(), latestSessionStatusSql());

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TrendRawDto(
                rs.getString("period"),
                rs.getInt("avg_score"),
                rs.getLong("total_sessions"),
                rs.getLong("converted_sessions")
        ), apiKey, apiKey);
    }

    public List<SessionSummaryDto> getRecentSessions(
            String apiKey,
            String sectionSelector,
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            String status,
            int limit
    ) {
        SessionQuery query = buildRecentSessionQuery(
                apiKey,
                sectionSelector,
                startDateTime,
                endDateTimeExclusive,
                status,
                limit
        );

        return jdbcTemplate.query(query.sql(), (rs, rowNum) -> new SessionSummaryDto(
                rs.getString("session_id"),
                rs.getString("user_agent"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getLong("duration_seconds"),
                rs.getString("last_selector"),
                rs.getString("status")
        ),
                query.params().toArray());
    }

    private SessionQuery buildRecentSessionQuery(
            String apiKey,
            String sectionSelector,
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            String status,
            int limit
    ) {
        String sessionSummarySql = """
            SELECT
                e.session_id,
                s.user_agent,
                e.start_time,
                e.end_time,
                e.duration_seconds,
                e.last_selector,
                s.status
            FROM (
                %s
            ) AS e
            JOIN (
                %s
            ) AS s ON e.session_id = s.session_id
            """.formatted(eventSessionSummarySql(), latestSessionStatusSql());

        List<Object> params = new ArrayList<>();
        params.add(apiKey);
        params.add(apiKey);

        StringBuilder conditions = new StringBuilder("WHERE 1 = 1");
        if (sectionSelector != null) {
            conditions.append(" AND ").append(sectionSelectorCondition("last_selector"));
            params.add(sectionSelector);
            params.add(sectionSelector);
        }
        if (startDateTime != null) {
            conditions.append(" AND start_time >= parseDateTime64BestEffort(?, 3, 'Asia/Seoul')");
            params.add(startDateTime.toString());
        }
        if (endDateTimeExclusive != null) {
            conditions.append(" AND start_time < parseDateTime64BestEffort(?, 3, 'Asia/Seoul')");
            params.add(endDateTimeExclusive.toString());
        }
        if (status != null) {
            conditions.append(" AND status = ?");
            params.add(status);
        }
        params.add(limit);

        String sql = """
            SELECT
                session_id,
                user_agent,
                start_time,
                end_time,
                duration_seconds,
                last_selector,
                status
            FROM (
                %s
            ) AS session_summary
            %s
            ORDER BY start_time DESC
            LIMIT ?
            """.formatted(sessionSummarySql, conditions);

        return new SessionQuery(sql, params);
    }
    public Map<String, Object> getSectionStats(
            String apiKey,
            List<String> reachedSectionSelectors,
            String durationSectionSelector
    ) {
        if (reachedSectionSelectors.isEmpty()) {
            return Map.of("reached_count", 0L, "avg_duration", 0.0);
        }

        String selectorConditions = reachedSectionSelectors.stream()
                .map(selector -> sectionSelectorCondition())
                .reduce((left, right) -> left + " OR " + right)
                .orElse("false");

        String durationSelectorCondition = sectionSelectorCondition();
        String scanConditions = "(%s OR %s)".formatted(selectorConditions, durationSelectorCondition);

        String sql = """
            SELECT
                countIf(reached_section) AS reached_count,
                ifNull(avgIf(duration_seconds, duration_section), 0.0) AS avg_duration
            FROM (
                SELECT
                    session_id,
                    countIf(%s) > 0 AS reached_section,
                    countIf(%s) > 0 AS duration_section,
                    greatest(
                        toFloat64(dateDiff('second', minIf(timestamp, %s), maxIf(timestamp, %s))),
                        toFloat64(countIf(event_type = 'ping' AND %s) * 5)
                    ) AS duration_seconds
                FROM event_details
                WHERE api_key = ?
                AND event_type != 'replay'
                AND %s
                GROUP BY session_id
            ) AS session_stats
        """.formatted(
                selectorConditions,
                durationSelectorCondition,
                durationSelectorCondition,
                durationSelectorCondition,
                durationSelectorCondition,
                scanConditions
        );

        List<Object> params = new ArrayList<>();
        addSectionSelectorParams(params, reachedSectionSelectors);
        addSectionSelectorParams(params, durationSectionSelector);
        addSectionSelectorParams(params, durationSectionSelector);
        addSectionSelectorParams(params, durationSectionSelector);
        addSectionSelectorParams(params, durationSectionSelector);
        params.add(apiKey);
        addSectionSelectorParams(params, reachedSectionSelectors);
        addSectionSelectorParams(params, durationSectionSelector);

        return jdbcTemplate.queryForMap(sql, params.toArray());
    }

    public List<FunnelSectionStats> getFunnelSectionStats(String apiKey, List<String> sectionSelectors) {
        if (sectionSelectors.isEmpty()) {
            return List.of();
        }

        String outerColumns = buildFunnelStatsOuterColumns(sectionSelectors.size());
        String sessionColumns = buildFunnelStatsSessionColumns(sectionSelectors);
        String scanCondition = sectionSelectors.stream()
                .map(ignored -> sectionSelectorCondition())
                .reduce((left, right) -> left + " OR " + right)
                .orElse("false");

        String sql = """
            SELECT
                %s
            FROM (
                SELECT
                    session_id,
                    %s
                FROM event_details
                WHERE api_key = ?
                AND event_type != 'replay'
                AND (%s)
                GROUP BY session_id
            ) AS session_stats
        """.formatted(outerColumns, sessionColumns, scanCondition);

        List<Object> params = new ArrayList<>();
        for (int index = 0; index < sectionSelectors.size(); index++) {
            addSectionSelectorParams(params, sectionSelectors.subList(index, sectionSelectors.size()));
            addSectionSelectorParams(params, sectionSelectors.get(index));
            addSectionSelectorParams(params, sectionSelectors.get(index));
            addSectionSelectorParams(params, sectionSelectors.get(index));
            addSectionSelectorParams(params, sectionSelectors.get(index));
        }
        params.add(apiKey);
        addSectionSelectorParams(params, sectionSelectors);

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, params.toArray());
        List<FunnelSectionStats> stats = new ArrayList<>();
        for (int index = 0; index < sectionSelectors.size(); index++) {
            stats.add(new FunnelSectionStats(
                    getLong(row, "reached_count_" + index),
                    getDouble(row, "avg_duration_" + index)
            ));
        }
        return stats;
    }

    private String buildFunnelStatsOuterColumns(int sectionCount) {
        List<String> columns = new ArrayList<>();
        for (int index = 0; index < sectionCount; index++) {
            columns.add("""
                    countIf(reached_%d) AS reached_count_%d,
                    ifNull(avgIf(duration_seconds_%d, duration_%d), 0.0) AS avg_duration_%d
                    """.formatted(index, index, index, index, index));
        }
        return String.join(",\n", columns);
    }

    private String buildFunnelStatsSessionColumns(List<String> sectionSelectors) {
        List<String> columns = new ArrayList<>();
        for (int index = 0; index < sectionSelectors.size(); index++) {
            String reachedCondition = sectionSelectors.subList(index, sectionSelectors.size()).stream()
                    .map(ignored -> sectionSelectorCondition())
                    .reduce((left, right) -> left + " OR " + right)
                    .orElse("false");
            String durationCondition = sectionSelectorCondition();

            columns.add("""
                    countIf(%s) > 0 AS reached_%d,
                    countIf(%s) > 0 AS duration_%d,
                    greatest(
                        toFloat64(dateDiff('second', minIf(timestamp, %s), maxIf(timestamp, %s))),
                        toFloat64(countIf(event_type = 'ping' AND %s) * 5)
                    ) AS duration_seconds_%d
                    """.formatted(
                    reachedCondition,
                    index,
                    durationCondition,
                    index,
                    durationCondition,
                    durationCondition,
                    durationCondition,
                    index
            ));
        }
        return String.join(",\n", columns);
    }

    private void addSectionSelectorParams(List<Object> params, List<String> selectors) {
        selectors.forEach(selector -> addSectionSelectorParams(params, selector));
    }

    private void addSectionSelectorParams(List<Object> params, String selector) {
        params.add(selector);
        params.add(selector);
    }

    private long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double getDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    public SectionBehaviorData getSectionBehaviorData(
            String apiKey,
            String sectionSelector,
            LocalDateTime crawledAt
    ) {
        String crawledAtParam = crawledAt.toString();

        String sessionSql = """
                SELECT
                    d.session_id,
                    min(d.timestamp) AS first_event_at,
                    max(d.timestamp) AS last_event_at,
                    dateDiff('second', min(d.timestamp), max(d.timestamp)) AS duration_seconds
                FROM event_details
                AS d
                JOIN (
                    SELECT
                        session_id,
                        argMax(status, status_updated_at) AS status
                    FROM event_sessions
                    WHERE api_key = ?
                    GROUP BY session_id
                ) AS s ON d.session_id = s.session_id
                WHERE d.api_key = ?
                AND d.timestamp >= parseDateTime64BestEffort(?, 3, 'Asia/Seoul')
                AND d.event_type != 'replay'
                AND s.status = 'DROP'
                AND %s
                GROUP BY d.session_id
                ORDER BY last_event_at DESC
                LIMIT ?
                """.formatted(sectionSelectorCondition("d.css_selector"));

        List<SectionSessionWindow> sessionWindows = jdbcTemplate.query(
                sessionSql,
                (rs, rowNum) -> new SectionSessionWindow(
                        rs.getString("session_id"),
                        rs.getTimestamp("first_event_at").toLocalDateTime(),
                        rs.getTimestamp("last_event_at").toLocalDateTime(),
                        rs.getLong("duration_seconds")
                ),
                apiKey,
                apiKey,
                crawledAtParam,
                sectionSelector,
                sectionSelector,
                SECTION_BEHAVIOR_SESSION_LIMIT
        );

        if (sessionWindows.isEmpty()) {
            return new SectionBehaviorData(SECTION_BEHAVIOR_SESSION_LIMIT, List.of());
        }

        String scrollWindowConditions = sessionWindows.stream()
                .map(session -> """
                        (session_id = ?
                        AND timestamp BETWEEN parseDateTime64BestEffort(?, 3, 'Asia/Seoul')
                        AND parseDateTime64BestEffort(?, 3, 'Asia/Seoul'))
                        """)
                .reduce((left, right) -> left + " OR " + right)
                .orElseThrow();

        String eventSql = """
                SELECT
                    session_id,
                    event_type,
                    timestamp,
                    payload
                FROM event_details
                WHERE api_key = ?
                AND timestamp >= parseDateTime64BestEffort(?, 3, 'Asia/Seoul')
                AND event_type != 'replay'
                AND session_id IN (%s)
                AND (
                    %s
                    OR (event_type = 'scroll' AND (%s))
                )
                ORDER BY session_id, timestamp ASC
                """.formatted(
                sessionWindows.stream()
                        .map(session -> "?")
                        .reduce((left, right) -> left + ", " + right)
                        .orElseThrow(),
                sectionSelectorCondition(),
                scrollWindowConditions
        );

        List<Object> eventParams = new ArrayList<>();
        eventParams.add(apiKey);
        eventParams.add(crawledAtParam);
        sessionWindows.stream()
                .map(SectionSessionWindow::sessionId)
                .forEach(eventParams::add);
        eventParams.add(sectionSelector);
        eventParams.add(sectionSelector);
        sessionWindows.forEach(session -> {
            eventParams.add(session.sessionId());
            eventParams.add(session.firstEventAt().toString());
            eventParams.add(session.lastEventAt().toString());
        });

        Map<String, List<SectionBehaviorEvent>> eventsBySession = jdbcTemplate.query(
                eventSql,
                rs -> {
                    Map<String, List<SectionBehaviorEvent>> events = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        String sessionId = rs.getString("session_id");
                        events.computeIfAbsent(sessionId, ignored -> new ArrayList<>())
                                .add(new SectionBehaviorEvent(
                                        rs.getString("event_type"),
                                        rs.getTimestamp("timestamp").toLocalDateTime(),
                                        rs.getString("payload")
                                ));
                    }
                    return events;
                },
                eventParams.toArray()
        );

        List<SectionSessionBehavior> sessionBehaviors = sessionWindows.stream()
                .map(session -> new SectionSessionBehavior(
                        session.sessionId(),
                        session.durationSeconds(),
                        eventsBySession.getOrDefault(session.sessionId(), List.of())
                ))
                .toList();

        return new SectionBehaviorData(SECTION_BEHAVIOR_SESSION_LIMIT, sessionBehaviors);
    }

    public record TrendRawDto(String period, int score, long totalSessions, long convertedSessions) {}

    public record FunnelSectionStats(long reachedCount, double avgDurationSeconds) {}

    public record SectionBehaviorData(
            int latestSessionLimit,
            List<SectionSessionBehavior> sessions
    ) {}

    public record SectionSessionBehavior(
            String sessionId,
            long durationSeconds,
            List<SectionBehaviorEvent> events
    ) {}

    public record SectionBehaviorEvent(
            String eventType,
            LocalDateTime timestamp,
            String payload
    ) {}

    private record SectionSessionWindow(
            String sessionId,
            LocalDateTime firstEventAt,
            LocalDateTime lastEventAt,
            long durationSeconds
    ) {}

    public record SessionSummaryDto(
            String sessionId,
            String userAgent,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationSeconds,
            String lastCssSelector,
            String status
    ) {}

    private record SessionQuery(
            String sql,
            List<Object> params
    ) {}

    public void markInactiveExploringSessionsAsDrop() {
        String sql = """
            INSERT INTO event_sessions (session_id, user_agent, url, api_key, status, status_updated_at)
            SELECT
                session_id,
                user_agent,
                url,
                api_key,
                'DROP' AS status,
                now64(3) AS status_updated_at
            FROM (
                SELECT
                    s.session_id AS session_id,
                    s.api_key AS api_key,
                    argMax(s.user_agent, s.status_updated_at) AS user_agent,
                    argMax(s.url, s.status_updated_at) AS url,
                    argMax(s.status, s.status_updated_at) AS status,
                    any(e.last_event_time) AS last_event_time
                FROM event_sessions AS s
                JOIN (
                    %s
                ) AS e ON s.api_key = e.api_key AND s.session_id = e.session_id
                GROUP BY s.api_key, s.session_id
                HAVING status = 'EXPLORING'
                AND last_event_time < subtractMinutes(now64(3), 10)
            )
        """.formatted(eventSessionSummaryForInactiveTimeoutSql());

        jdbcTemplate.execute(sql);
    }

    public List<JsonNode> getReplayEvents(String apiKey, String sessionId) {
        String sql = """
        SELECT payload
        FROM event_details
        WHERE api_key = ?
        AND session_id = ?
        AND event_type = 'replay'
        ORDER BY timestamp ASC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            JsonNode payload = readReplayPayload(rs.getString("payload"));
            JsonNode event = payload.get("event");
            if (event == null || event.isNull()) {
                throw new CustomException(ErrorCode.EVENT_QUERY_FAILED);
            }
            return event;
        }, apiKey, sessionId);
    }

    @SneakyThrows
    private JsonNode readReplayPayload(String payload) {
        return objectMapper.readTree(payload);
    }

}
