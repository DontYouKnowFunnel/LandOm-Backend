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
            INSERT INTO event_details (session_id, event_type, timestamp, payload, css_selector) 
            VALUES (?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = request.events().stream()
                .map(event -> new Object[]{
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
                    countIf(event_type = 'click' AND ? != '' AND ifNull(css_selector, '') LIKE concat(?, '%')) > 0
                    OR (
                        countIf(? != '' AND ifNull(css_selector, '') LIKE concat(?, '%')) > 0
                        AND countIf(event_type = 'click') > 0
                        AND maxIf(timestamp, event_type = 'click') >= minIf(timestamp, ? != '' AND ifNull(css_selector, '') LIKE concat(?, '%'))
                    )
                ),
                'CONVERTED',
                countIf(event_type = 'exit') > 0,
                'DROP',
                'EXPLORING'
            ) AS status
            FROM event_details
            WHERE session_id = ?
            AND session_id IN (
                SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
                UNION DISTINCT
                SELECT ? AS session_id
            )
            AND event_type != 'replay'
        """;

        String status = jdbcTemplate.queryForObject(sql, String.class,
                ctaSectionSelector, ctaSectionSelector,
                ctaSectionSelector, ctaSectionSelector,
                ctaSectionSelector, ctaSectionSelector,
                sessionId,
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

    public long getTotalSessionCount(String apiKey) {
        String sql = """
            SELECT count(DISTINCT session_id)
            FROM event_details
            WHERE session_id IN (
                SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
            )
            AND event_type != 'replay'
        """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, apiKey);
        return count == null ? 0L : count;
    }

    public Map<String, Object> getSectionStats(String apiKey, List<String> reachedSectionSelectors) {
        if (reachedSectionSelectors.isEmpty()) {
            return Map.of("reached_count", 0L, "avg_duration", 0.0);
        }

        String selectorConditions = reachedSectionSelectors.stream()
                .map(selector -> "ifNull(css_selector, '') LIKE concat(?, '%')")
                .reduce((left, right) -> left + " OR " + right)
                .orElse("false");

        String sql = """
            SELECT 
                count(DISTINCT session_id) AS reached_count,
                avg(duration_seconds) AS avg_duration
            FROM (
                SELECT 
                    session_id,
                    dateDiff('second', min(timestamp), max(timestamp)) AS duration_seconds
                FROM event_details
                WHERE session_id IN (
                    SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
                )
                AND event_type != 'replay'
                AND (%s)
                GROUP BY session_id
            )
        """.formatted(selectorConditions);

        List<Object> params = new ArrayList<>();
        params.add(apiKey);
        params.addAll(reachedSectionSelectors);

        return jdbcTemplate.queryForMap(sql, params.toArray());
    }

    public Map<String, Object> getSummaryStats(String apiKey) {
        String sql = """
        SELECT 
            count(*) AS total_sessions,
            avg(duration_seconds) AS avg_total_duration,
            countIf(status = 'CONVERTED') AS converted_sessions
        FROM (
                SELECT 
                    d.session_id,
                    dateDiff('second', min(d.timestamp), max(d.timestamp)) AS duration_seconds,
                    any(s.status) AS status
                FROM event_details AS d
                JOIN (
                    SELECT
                        session_id,
                        argMax(status, status_updated_at) AS status
                    FROM event_sessions
                    WHERE api_key = ?
                    GROUP BY session_id
                ) AS s ON d.session_id = s.session_id
                WHERE d.event_type != 'replay'
                GROUP BY d.session_id
                )
    """;

        return jdbcTemplate.queryForMap(sql, apiKey);
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
                d.session_id,
                concat(toString(toYear(min(d.timestamp))), '-',
                       leftPad(toString(toMonth(min(d.timestamp))), 2, '0'), '-W',
                       toString(toRelativeWeekNum(min(d.timestamp)) - toRelativeWeekNum(toStartOfMonth(min(d.timestamp))) + 1)) AS period,
                count(*) AS event_count,
                any(s.status) = 'CONVERTED' AS is_converted
            FROM event_details AS d
            JOIN (
                SELECT
                    session_id,
                    argMax(status, status_updated_at) AS status
                FROM event_sessions
                WHERE api_key = ?
                GROUP BY session_id
            ) AS s ON d.session_id = s.session_id
            WHERE d.event_type != 'replay'
            GROUP BY d.session_id
            )
        GROUP BY period
        ORDER BY period ASC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TrendRawDto(
                rs.getString("period"),
                rs.getInt("avg_score"),
                rs.getLong("total_sessions"),
                rs.getLong("converted_sessions")
        ), apiKey);
    }

    public record TrendRawDto(String period, int score, long totalSessions, long convertedSessions) {}

    public List<SessionSummaryDto> getRecentSessions(String apiKey, int limit) {
        String sql = """
        SELECT 
            s.session_id,
            s.user_agent,
            min(d.timestamp) as start_time,
            max(d.timestamp) as end_time,
            dateDiff('second', min(d.timestamp), max(d.timestamp)) as duration_seconds,
            argMax(d.css_selector, d.timestamp) as last_selector,
            s.status
        FROM (
            SELECT
                session_id,
                argMax(user_agent, status_updated_at) AS user_agent,
                argMax(status, status_updated_at) AS status
            FROM event_sessions
            WHERE api_key = ?
            GROUP BY session_id
        ) AS s
        JOIN event_details AS d ON s.session_id = d.session_id
        WHERE d.event_type != 'replay'
        GROUP BY s.session_id, s.user_agent, s.status
        ORDER BY start_time DESC
        LIMIT ?
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SessionSummaryDto(
                rs.getString("session_id"),
                rs.getString("user_agent"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getLong("duration_seconds"),
                rs.getString("last_selector"),
                rs.getString("status")
        ),
                apiKey,
                limit);
    }

    public record SessionSummaryDto(
            String sessionId,
            String userAgent,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationSeconds,
            String lastCssSelector,
            String status
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
                    max(d.timestamp) AS last_event_time
                FROM event_sessions AS s
                JOIN event_details AS d ON s.session_id = d.session_id
                GROUP BY s.api_key, s.session_id
                HAVING status = 'EXPLORING'
                AND last_event_time < subtractMinutes(now64(3), 10)
            )
        """;

        jdbcTemplate.execute(sql);
    }

    public List<JsonNode> getReplayEvents(String apiKey, String sessionId) {
        String sql = """
        SELECT payload
        FROM event_details
        WHERE session_id = ?
        AND event_type = 'replay'
        AND session_id IN (
            SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
        )
        ORDER BY timestamp ASC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            JsonNode payload = readReplayPayload(rs.getString("payload"));
            JsonNode event = payload.get("event");
            if (event == null || event.isNull()) {
                throw new CustomException(ErrorCode.EVENT_QUERY_FAILED);
            }
            return event;
        }, sessionId, apiKey);
    }

    @SneakyThrows
    private JsonNode readReplayPayload(String payload) {
        return objectMapper.readTree(payload);
    }

}
