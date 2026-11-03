package com.lilamaris.lilserve.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Runs the reservation context as one executable application: the web controllers, the application
 * services, and the JPA adapters are scanned together and Flyway migrates the schema at startup.
 *
 * <p>The class sits at the context package root, so the scan root covers every module of the
 * context: entities in domain, repositories in persistence, services in application, controllers in
 * web. No entity scan or repository scan is declared, because auto-configuration takes both from
 * this package.
 */
@SpringBootApplication
public class LauncherApplication {

    /**
     * Starts the reservation service.
     *
     * @param args the command line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(LauncherApplication.class, args);
    }
}
