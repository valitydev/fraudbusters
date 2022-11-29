package dev.vality.fraudbusters.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "dgraph")
public class DgraphProperties {

    private List<String> targets;
    private boolean auth;
    private String login;
    private String password;
    private int maxAttempts;
    private long backoffPeriod;
    private String trustCertCollectionFile;
    private String keyCertChainFile;
    private String keyFile;
    private String negotiationType;
}
