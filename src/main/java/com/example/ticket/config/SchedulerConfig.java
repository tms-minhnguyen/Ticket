package com.example.ticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks (Payment Worker, Order Expiration).
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Enables @Scheduled annotations
}
