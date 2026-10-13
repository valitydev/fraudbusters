# Fraudbusters 

Service for fraud detection.

It is a tool for describing behavioral patterns for fraudulent transactions, it provides the ability 
to check for compliance with the described patterns.

### Template Description Language

[Fraudo DSL](https://github.com/valitydev/fraudo)

### The following protocols are implemented for interaction
[Apache Thrift](https://thrift.apache.org/) is used to describe the protocols.

- [Proxy Inspector](https://github.com/valitydev/damsel/blob/master/proto/proxy_inspector.thrift) - 
protocol for inspection payment for mismatch
- [Fraudbusters Proto](https://github.com/valitydev/fraudbusters-proto/blob/master/proto/fraudbusters.thrift) -
protocol for managing a set of patterns and bindings

### Installation and testing

Build with JDK 25 and Maven. The service uses Spring Boot 4.1 via Vality's
`service-parent-pom` 4.0.1. CI also runs on Java 25; shared resources provide the
Java 25 container runtime and OpenTelemetry agent.

Run `mvn clean verify` to build and test. Container tests require Docker with
enough free disk space to start Kafka and ClickHouse.

Instruction for testing in https://github.com/valitydev/fraudbusters-compose

### Tracing

The Docker runtime starts the OpenTelemetry Java agent. Configure tracing through
environment variables in the Helm values, independently of Spring's `application.yml`:

```properties
OTEL_JAVAAGENT_ENABLED=true
OTEL_SERVICE_NAME=fraudbusters
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
OTEL_EXPORTER_OTLP_TRACES_TIMEOUT=60000
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_TRACES_SAMPLER=always_on
OTEL_PROPAGATORS=tracecontext
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
OTEL_INSTRUMENTATION_LOGBACK_MDC_ENABLED=false
```

The endpoint must address the collector reachable from the running service.
`OTEL_JAVAAGENT_ENABLED=false` disables the agent. Prometheus metrics continue to
use Micrometer. The former `otel.resource` and `otel.timeout` Spring properties
are no longer used; no custom OpenTelemetry SDK is created by the application.

Deploy the updated service image together with the Helm environment changes.
Older images still require the former `otel.resource` application property.

When running the JAR outside Docker, attach the agent explicitly:

```shell
java -javaagent:target/maven-shared-archive-resources/opentelemetry-javaagent.jar -jar target/fraudbusters-1.0-SNAPSHOT.jar
```

### License
[Apache 2.0 License.](/LICENSE)

