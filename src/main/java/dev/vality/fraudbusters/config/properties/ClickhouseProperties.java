package dev.vality.fraudbusters.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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
