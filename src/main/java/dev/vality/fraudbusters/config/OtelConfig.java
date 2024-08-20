package dev.vality.fraudbusters.config;

import dev.vality.fraudbusters.config.properties.OtelProperties;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OtelConfig {

    private final OtelProperties otelProperties;
    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public OpenTelemetry openTelemetryConfig() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, appName)));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpHttpSpanExporter.builder()
                                .setEndpoint(otelProperties.getResource())
                                .setTimeout(Duration.ofMillis(otelProperties.getTimeout()))
                                .build())
                        .build())
                .setSampler(Sampler.alwaysOff())
                .setResource(resource)
                .build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        registerGlobalOpenTelemetry(openTelemetrySdk);
        return openTelemetrySdk;
    }

    private static void registerGlobalOpenTelemetry(OpenTelemetry openTelemetry) {
        try {
            GlobalOpenTelemetry.set(openTelemetry);
        } catch (Exception e) {
            log.warn("please initialize the ObservabilitySdk before starting the application");
            // will throw an exception if it was already set - problem is that we cannot check if was set
            // by a third-party library before
            GlobalOpenTelemetry.resetForTest();
            try {
                GlobalOpenTelemetry.set(openTelemetry);
            } catch (Exception ex) {
                log.warn("unable to set GlobalOpenTelemetry", ex);
                //now we give up - must be a race condition
            }
        }
    }
}
