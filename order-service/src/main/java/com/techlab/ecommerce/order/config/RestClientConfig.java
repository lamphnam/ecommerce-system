package com.techlab.ecommerce.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient beans for synchronous HTTP calls to downstream services.
 * Used only by the sync order flow (experiment baseline).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient inventoryRestClient(
            @Value("${sync.inventory-url:http://localhost:8083}") String inventoryUrl) {
        return RestClient.builder()
                .baseUrl(inventoryUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    @Bean
    public RestClient paymentRestClient(
            @Value("${sync.payment-url:http://localhost:8082}") String paymentUrl) {
        return RestClient.builder()
                .baseUrl(paymentUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
