package com.sigmoid.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single entry point for POC 01 — Module B (provider stubs).
 *
 * <p>One application hosts all three rails as packages:
 * {@code upi}, {@code neft}, {@code card}, sharing the {@code common} package
 * (error envelope, enums, trace filter, metric names). Component scanning starts
 * at this package, so every controller, service, and {@code @ConfigurationProperties}
 * bean below it is picked up automatically.
 *
 * <p>{@code @EnableScheduling} drives the NEFT batch settler.
 */
@SpringBootApplication
@EnableScheduling
public class PaymentGatewayPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayPocApplication.class, args);
    }
}
