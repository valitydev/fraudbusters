package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.config.properties.ClickhouseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseConnectionSettings;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class ClickhouseConfig {

    private final ClickhouseProperties clickhouseProperties;

    @Bean
    public ClickHouseDataSource clickHouseDataSource() {
        Properties info = new Properties();
        info.put(ClickHouseQueryParam.USER.getKey(), clickhouseProperties.getUser());
        info.put(ClickHouseQueryParam.PASSWORD.getKey(), clickhouseProperties.getPassword());
        info.put(ClickHouseQueryParam.COMPRESS.getKey(), clickhouseProperties.getCompress());
        return new ClickHouseDataSource(clickhouseProperties.getUrl(), info);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }

    @Bean
    public JdbcTemplate longQueryJdbcTemplate() {
        Properties info = new Properties();
        info.put(ClickHouseQueryParam.USER.getKey(), clickhouseProperties.getUser());
        info.put(ClickHouseQueryParam.PASSWORD.getKey(), clickhouseProperties.getPassword());
        info.put(ClickHouseQueryParam.COMPRESS.getKey(), true);
        info.put(ClickHouseQueryParam.CONNECT_TIMEOUT.getKey(), clickhouseProperties.getConnectionTimeout());
        info.put(ClickHouseConnectionSettings.CONNECTION_TIMEOUT.getKey(),
                Integer.parseInt(clickhouseProperties.getConnectionTimeout()));
        info.put(ClickHouseConnectionSettings.SOCKET_TIMEOUT.getKey(),
                Integer.parseInt(clickhouseProperties.getSocketTimeout()));
        return new JdbcTemplate(new ClickHouseDataSource(clickhouseProperties.getUrl(), info));
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(ClickHouseDataSource clickHouseDataSource) {
        return new NamedParameterJdbcTemplate(clickHouseDataSource);
    }

}
