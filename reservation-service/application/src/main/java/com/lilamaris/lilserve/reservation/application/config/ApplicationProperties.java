package com.lilamaris.lilserve.reservation.application.config;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings of the reservation context, bound from the {@code lilserve} prefix.
 *
 * @param timezone the timezone the application clock reads, defaults to UTC
 */
@Validated
@ConfigurationProperties(prefix = "lilserve")
public record ApplicationProperties(@DefaultValue("UTC") @NotNull ZoneId timezone) {}
