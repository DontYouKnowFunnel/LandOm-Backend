package knu.dykf.landom.repository;

import knu.dykf.landom.dto.request.SdkEventRequest;
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
}