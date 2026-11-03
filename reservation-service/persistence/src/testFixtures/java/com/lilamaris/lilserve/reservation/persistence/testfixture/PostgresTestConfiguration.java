package com.lilamaris.lilserve.reservation.persistence.testfixture;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a disposable PostgreSQL database for the tests of the reservation service. V2 uses
 * PostgreSQL-only objects, so the schema cannot be applied to an embedded database. The connection
 * details are registered as {@code spring.datasource} properties, so Flyway and Hibernate run
 * against the same database the tests write to.
 */
@Configuration(proxyBeanMethods = false)
public class PostgresTestConfiguration {
    private static final String POSTGRES_IMAGE = "postgres:18-alpine";

    /**
     * Declares the container bean whose connection Spring registers as the test datasource.
     *
     * @return the PostgreSQL container
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
