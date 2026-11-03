package com.lilamaris.lilserve.reservation.application.config;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.model.id.UuidGenerator;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application module's beans: the {@link ApplicationProperties} binding, the {@link
 * Clock} services read the current time from, and the {@link IdGenerator} this layer owns.
 */
@Configuration
@EnableConfigurationProperties({ApplicationProperties.class})
public class ApplicationConfiguration {

    /**
     * Creates the system clock for the configured timezone.
     *
     * @param properties the bound properties
     * @return the clock used for creation, update, and deletion timestamps
     */
    @Bean
    Clock clock(ApplicationProperties properties) {
        return Clock.system(properties.timezone());
    }

    /**
     * Creates the default identifier generator: the {@link UuidGenerator} of time-ordered UUIDs, so
     * new room rows append to the primary key index instead of scattering it.
     *
     * @return the UUID generator
     */
    @Bean
    IdGenerator<UUID> uuidGenerator() {
        return new UuidGenerator();
    }
}
