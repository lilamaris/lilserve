package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.RoomRepository;
import com.lilamaris.lilserve.reservation.persistence.testfixture.PostgresTestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Anchor configuration for the JPA slice tests of this module.
 *
 * <p>Entities live in the domain module and repositories in this one, so a package scan rooted at
 * this module finds neither. Both package roots are declared from the classes themselves so a
 * rename fails at startup instead of silently dropping the entity or the repository.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(PostgresTestConfiguration.class)
@EntityScan(basePackageClasses = Room.class)
@EnableJpaRepositories(basePackageClasses = RoomRepository.class)
class PersistenceJpaTestConfiguration {}
