package com.sigmoid.paymentgateway.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Wiring for the outbound RestClient used to call the provider stubs. */
@Configuration
public class GatewayBeans {

    @Bean
    public RestClient stubsClient(RoutingProperties props) {
        return RestClient.builder()
                .baseUrl(props.getStubsBaseUrl())
                .build();
    }
}
