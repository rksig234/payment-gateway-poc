package com.sigmoid.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The payment platform — a modular monolith hosting the gateway (Module A) and the
 * lifecycle services (Module C) as packages:
 * <ul>
 *   <li>{@code gateway} — routing, idempotency, circuit breakers, event publishing</li>
 *   <li>{@code status} — Postgres state machine + timeline (owns payment reads)</li>
 *   <li>{@code retry} — exponential backoff + DLQ</li>
 *   <li>{@code notification} — channels + SNS</li>
 * </ul>
 * Domains still communicate through real Kafka topics; the provider stubs run as a
 * separate app and are called over HTTP.
 */
@SpringBootApplication(scanBasePackages = "com.sigmoid.paymentgateway")
@ConfigurationPropertiesScan
@EnableScheduling
public class PaymentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentPlatformApplication.class, args);
    }
}
