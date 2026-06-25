package com.sigmoid.paymentgateway.notification.sns;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SNS settings. Disabled by default so the service runs locally without AWS
 * credentials (channels still fire). Set enabled=true + a topic ARN to publish for real.
 */
@ConfigurationProperties(prefix = "notification.sns")
public class NotificationProperties {

    private boolean enabled = false;
    private String topicArn = "";
    private String region = "ap-south-1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
