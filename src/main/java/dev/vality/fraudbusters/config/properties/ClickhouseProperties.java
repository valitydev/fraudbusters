package dev.vality.fraudbusters.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "clickhouse.db")
public class ClickhouseProperties {

    private String url;
    private String user;
    private String password;
    private String compress;
    private String connectionTimeout;
    private String socketTimeout;

}
