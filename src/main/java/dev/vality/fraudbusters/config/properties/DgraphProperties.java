package dev.vality.fraudbusters.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dgraph")
public class DgraphProperties {

    private String host;
    private int port;
    private boolean auth;
    private String login;
    private String password;

}
