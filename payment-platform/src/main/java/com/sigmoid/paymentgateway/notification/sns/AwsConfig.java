package com.sigmoid.paymentgateway.notification.sns;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Creates a real SnsClient only when notification.sns.enabled=true, so local runs
 * don't require AWS credentials. Uses the default credentials provider chain
 * (env vars, profile, or IAM role on ECS).
 */
@Configuration
public class AwsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "notification.sns", name = "enabled", havingValue = "true")
    public SnsClient snsClient(NotificationProperties props) {
        return SnsClient.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }
}
