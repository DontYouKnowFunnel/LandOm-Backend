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

    public void saveAll(String projectKey, SdkEventRequest request) {
        String sessionSql = """
            INSERT INTO event_sessions (session_id, user_agent, url, api_key) 
            VALUES (?, ?, ?, ?)
        """;
        jdbcTemplate.update(sessionSql,
                request.sessionId(), request.userAgent(), request.url(), projectKey);

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

    public Map<String, Object> getSectionStats(String apiKey, String cssSelector) {
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
                AND css_selector LIKE concat(?, '%')
                GROUP BY session_id
            )
        """;
        return jdbcTemplate.queryForMap(sql, apiKey, cssSelector);
    }

    public Map<String, Object> getSummaryStats(String apiKey, String ctaSectionSelector) {
        String sql = """
        SELECT 
            count(*) AS total_sessions,
            avg(duration_seconds) AS avg_total_duration,
            countIf(is_converted > 0) AS converted_sessions
        FROM (
                SELECT 
                    session_id,
                    dateDiff('second', min(timestamp), max(timestamp)) AS duration_seconds,
                    countIf(event_type = 'click' AND ? != '' AND ifNull(css_selector, '') LIKE concat(?, '%')) AS is_converted
                FROM event_details
                WHERE session_id IN (
                    SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
                )
                AND event_type != 'replay'
            GROUP BY session_id
        )
    """;

        return jdbcTemplate.queryForMap(sql, ctaSectionSelector, ctaSectionSelector, apiKey);
    }

    public List<TrendRawDto> getWeeklyTrends(String apiKey, String ctaSectionSelector) {
        String sql = """
        SELECT 
            concat(toString(toYear(timestamp)), '-', 
                   leftPad(toString(toMonth(timestamp)), 2, '0'), '-W', 
                   toString(toRelativeWeekNum(timestamp) - toRelativeWeekNum(toStartOfMonth(timestamp)) + 1)) AS period,
            -- 점수: 세션당 평균 이벤트 발생 수 (예시)
            round(count(*) / count(DISTINCT session_id), 0) AS avg_score,
            -- 전환율 계산용 데이터
            count(DISTINCT session_id) AS total_sessions,
            uniqExactIf(session_id, event_type = 'click' AND ? != '' AND ifNull(css_selector, '') LIKE concat(?, '%')) AS converted_sessions
        FROM event_details
        WHERE session_id IN (
            SELECT session_id FROM event_sessions FINAL WHERE api_key = ?
        )
        AND event_type != 'replay'
        GROUP BY period
        ORDER BY period ASC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TrendRawDto(
                rs.getString("period"),
                rs.getInt("avg_score"),
                rs.getLong("total_sessions"),
                rs.getLong("converted_sessions")
        ), ctaSectionSelector, ctaSectionSelector, apiKey);
    }

    public record TrendRawDto(String period, int score, long totalSessions, long convertedSessions) {}

    public List<SessionSummaryDto> getRecentSessions(String apiKey, String ctaSectionSelector, int limit) {
        String sql = """
        SELECT 
            s.session_id,
            s.user_agent,
            min(d.timestamp) as start_time,
            max(d.timestamp) as end_time,
            dateDiff('second', min(d.timestamp), max(d.timestamp)) as duration_seconds,
            argMax(d.css_selector, d.timestamp) as last_selector,
            countIf(d.event_type = 'exit') > 0 as has_exit,
            countIf(d.event_type = 'click' AND ? != '' AND ifNull(d.css_selector, '') LIKE concat(?, '%')) > 0 as has_cta_click
        FROM (SELECT * FROM event_sessions FINAL WHERE api_key = ?) AS s
        JOIN event_details AS d ON s.session_id = d.session_id
        WHERE d.event_type != 'replay'
        GROUP BY s.session_id, s.user_agent
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
                rs.getBoolean("has_exit"),
                rs.getBoolean("has_cta_click")
        ), ctaSectionSelector, ctaSectionSelector, apiKey, limit);
    }

    public record SessionSummaryDto(
            String sessionId,
            String userAgent,
            LocalDateTime startTime,
            LocalDateTime endTime,
            long durationSeconds,
            String lastCssSelector,
            boolean hasExit,
            boolean hasCtaClick
    ) {}

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
