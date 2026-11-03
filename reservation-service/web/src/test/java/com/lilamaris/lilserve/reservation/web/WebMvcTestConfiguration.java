package com.lilamaris.lilserve.reservation.web;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Anchor configuration for the web slice tests of this module.
 *
 * <p>The slice annotation disables full auto-configuration, so this class re-enables it for the
 * test context alone, keeping the scope to what the web layer needs.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class WebMvcTestConfiguration {}
