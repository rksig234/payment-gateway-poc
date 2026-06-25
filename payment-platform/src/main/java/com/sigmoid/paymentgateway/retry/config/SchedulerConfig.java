package com.sigmoid.paymentgateway.retry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A TaskScheduler for the non-blocking backoff: instead of sleeping the Kafka
 * consumer thread, the next retry is scheduled to publish after the delay.
 * Also backs the @Scheduled daily DLQ review.
 */
@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("retry-sched-");
        scheduler.initialize();
        return scheduler;
    }
}
