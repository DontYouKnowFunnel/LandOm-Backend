package knu.dykf.landom.repository;

import knu.dykf.landom.dto.request.SdkEventRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

        List<Object[]> batchArgs = request.events().stream().map(event -> {
            try {
                return new Object[]{
                        request.sessionId(),
                        event.type(),
                        new Timestamp(event.timestamp()),
                        objectMapper.writeValueAsString(event.payload()),
                        event.cssSelector()
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        jdbcTemplate.batchUpdate(eventSql, batchArgs);
    }

    public Map<String, Object> getSectionStats(String apiKey, String targetSelector, LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT 
                count(DISTINCT session_id) AS reached_count,
                avg(duration_seconds) AS avg_duration
            FROM (
                SELECT 
                    session_id,
                    dateDiff('second', min(timestamp), max(timestamp)) AS duration_seconds
                FROM event_details
                WHERE session_id IN (SELECT session_id FROM event_sessions WHERE api_key = ?)
                  AND css_selector LIKE concat(?, '%')
                  AND timestamp BETWEEN ? AND ?
                GROUP BY session_id
            )
        """;

        return jdbcTemplate.queryForMap(sql, apiKey, targetSelector, start, end);
    }

    public long getTotalSessionCount(String apiKey, LocalDateTime start, LocalDateTime end) {
        String sql = """
            SELECT count(DISTINCT session_id) 
            FROM event_sessions 
            WHERE api_key = ? 
              AND session_id IN (
                  SELECT DISTINCT session_id FROM event_details WHERE timestamp BETWEEN ? AND ?
              )
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, apiKey, start, end);
    }
}