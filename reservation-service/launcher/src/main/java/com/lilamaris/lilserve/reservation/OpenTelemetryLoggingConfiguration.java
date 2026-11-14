package com.lilamaris.lilserve.reservation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Installs the Logback appender that turns log events into OTLP log records. Spring Boot does not
 * install log appenders itself, and the appender is only useful once
 * MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT is set, so this configuration backs off
 * without that property.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "management.opentelemetry.logging.export.otlp.endpoint")
class OpenTelemetryLoggingConfiguration {

    /**
     * Binds the OTel SDK to the Logback appender declared in logback-spring.xml.
     *
     * @param openTelemetry SDK to attach log records to
     * @return callback that installs the appender during bean initialization
     */
    @Bean
    InitializingBean openTelemetryAppender(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
