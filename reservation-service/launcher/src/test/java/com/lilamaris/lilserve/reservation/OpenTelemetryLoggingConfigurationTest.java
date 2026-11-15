package com.lilamaris.lilserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies that the OTLP log appender is wired only when an export endpoint is configured, so the
 * service starts without OpenTelemetry destinations.
 */
@DisplayName("OTLP 로그 내보내기 설정")
class OpenTelemetryLoggingConfigurationTest {
    private static final String ENDPOINT = "management.opentelemetry.logging.export.otlp.endpoint";

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            OpenTelemetryLoggingConfiguration.class, TestOpenTelemetry.class);

    @Test
    @DisplayName("엔드포인트가 없으면 앱렌더 설정을 만들지 않는다")
    void backsOffWithoutEndpoint() {
        runner.run(
                context ->
                        assertThat(context)
                                .doesNotHaveBean(OpenTelemetryLoggingConfiguration.class));
    }

    @Test
    @DisplayName("엔드포인트가 있으면 앱렌더 설정을 만든다")
    void installsAppenderWhenEndpointConfigured() {
        runner.withPropertyValues(ENDPOINT + "=http://localhost:4318/v1/logs")
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(OpenTelemetryLoggingConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestOpenTelemetry {

        @Bean
        OpenTelemetry openTelemetry() {
            return OpenTelemetry.noop();
        }
    }
}
