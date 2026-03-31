package knu.dykf.landom.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.host}")
    private String host;

    @Value("${clickhouse.port}")
    private int port;

    @Value("${clickhouse.database}")
    private String database;

    @Value("${clickhouse.username}")
    private String username;

    @Value("${clickhouse.password}")
    private String password;

    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate() throws SQLException {
        String url = String.format("jdbc:clickhouse://%s:%d/%s", host, port, database);

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        properties.setProperty("compress", "0");
        properties.setProperty("http_connection_provider", "HTTP_URL_CONNECTION");

        ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);
        return new JdbcTemplate(dataSource);
    }
}
