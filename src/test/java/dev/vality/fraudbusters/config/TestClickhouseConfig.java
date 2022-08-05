package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import dev.vality.fraudbusters.extension.ClickHouseContainerExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseConnectionSettings;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;

import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@TestConfiguration
@RequiredArgsConstructor
public class TestClickhouseConfig {

    @Bean
    @Primary
    public ClickHouseDataSource clickHouseDataSource() {
        Integer firstMappedPort = ClickHouseContainerExtension.CLICKHOUSE_CONTAINER.getFirstMappedPort();
        log.info("{}", firstMappedPort);
        Properties info = new Properties();
        info.put(ClickHouseQueryParam.USER.getKey(), "default");
        info.put(ClickHouseQueryParam.COMPRESS.getKey(), false);
        return new ClickHouseDataSource("jdbc:clickhouse://localhost:" + firstMappedPort + "/default", info);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }

    @Bean
    public JdbcTemplate longQueryJdbcTemplate() {
        Properties info = new Properties();
        info.put(ClickHouseQueryParam.USER.getKey(), "default");
        info.put(ClickHouseQueryParam.COMPRESS.getKey(), true);
        info.put(ClickHouseQueryParam.CONNECT_TIMEOUT.getKey(), 10000);
        info.put(ClickHouseConnectionSettings.CONNECTION_TIMEOUT.getKey(), 10000);
        info.put(ClickHouseConnectionSettings.SOCKET_TIMEOUT.getKey(), 10000);
        Integer firstMappedPort = ClickHouseContainerExtension.CLICKHOUSE_CONTAINER.getFirstMappedPort();
        return new JdbcTemplate(
                new ClickHouseDataSource("jdbc:clickhouse://localhost:" + firstMappedPort + "/default", info));
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(ClickHouseDataSource clickHouseDataSource) {
        return new NamedParameterJdbcTemplate(clickHouseDataSource);
    }

}
