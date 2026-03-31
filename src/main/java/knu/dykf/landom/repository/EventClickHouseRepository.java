package knu.dykf.landom.repository;

import knu.dykf.landom.dto.request.SdkEventRequest;
import knu.dykf.landom.dto.response.FunnelResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;

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
        String sql = """
            INSERT INTO events (project_key, session_id, user_agent, url, event_type, event_timestamp, payload) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = request.events().stream().map(event -> {
            try {
                return new Object[]{
                        projectKey,
                        request.sessionId(),
                        request.userAgent(),
                        request.url(),
                        event.type(),
                        new Timestamp(event.timestamp()),
                        objectMapper.writeValueAsString(event.payload())
                };
            } catch (Exception e) {
                throw new RuntimeException("JSON 파싱 에러", e);
            }
        }).toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public List<FunnelResponse.FunnelData> getScrollFunnel(String projectKey) {
        String sql = """
        SELECT 
            b.bucket * 10 as funnel_id,
            round(countIf(JSONExtractInt(payload, 'maxDepth') >= (b.bucket * 10)) / count(), 2) as ratio
        FROM events
        CROSS JOIN (SELECT arrayJoin(range(0, 11)) AS bucket) AS b
        WHERE project_key = ? AND event_type = 'exit'
        GROUP BY funnel_id
        ORDER BY funnel_id
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new FunnelResponse.FunnelData(
                        rs.getInt("funnel_id"),
                        rs.getDouble("ratio")
                ), projectKey);
    }

}