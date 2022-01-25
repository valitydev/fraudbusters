package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.aspect.SimpleMeasureAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfiguration {

    @Bean
    SimpleMeasureAspect simpleMeasureAspect(MeterRegistry registry) {
        return new SimpleMeasureAspect(registry);
    }

}