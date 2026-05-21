package knu.dykf.landom.config;

import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClickHouseSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final String schemaPath;

    public ClickHouseSchemaInitializer(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
            ResourceLoader resourceLoader,
            @Value("${clickhouse.schema-path}") String schemaPath) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.schemaPath = schemaPath;
    }

    @Override
    @SneakyThrows
    public void run(ApplicationArguments args) {
        Resource resource = resourceLoader.getResource(schemaPath);

        if (!resource.exists()) {
            throw new CustomException(ErrorCode.CLICKHOUSE_SCHEMA_FILE_NOT_FOUND);
        }

        String schemaSql = resource.getContentAsString(StandardCharsets.UTF_8);
        List<String> statements = splitStatements(schemaSql);

        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }

        log.info("Applied {} ClickHouse schema statement(s) from {}", statements.size(), schemaPath);
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char currentChar = sql.charAt(i);
            char nextChar = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (currentChar == '\n') {
                    inLineComment = false;
                    current.append(currentChar);
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick && currentChar == '-' && nextChar == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (currentChar == '\'' && !inDoubleQuote && !inBacktick && !isEscaped(sql, i)) {
                inSingleQuote = !inSingleQuote;
            } else if (currentChar == '"' && !inSingleQuote && !inBacktick && !isEscaped(sql, i)) {
                inDoubleQuote = !inDoubleQuote;
            } else if (currentChar == '`' && !inSingleQuote && !inDoubleQuote && !isEscaped(sql, i)) {
                inBacktick = !inBacktick;
            }

            if (currentChar == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        addStatement(statements, current);
        return statements;
    }

    private boolean isEscaped(String sql, int index) {
        int backslashCount = 0;

        for (int i = index - 1; i >= 0 && sql.charAt(i) == '\\'; i--) {
            backslashCount++;
        }

        return backslashCount % 2 == 1;
    }

    private void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();

        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
