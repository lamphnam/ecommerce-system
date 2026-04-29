package com.techlab.ecommerce.order;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test: verifies the full Order Service application context boots with
 * RabbitAutoConfiguration excluded. {@link RabbitTemplate} is mocked so beans
 * that depend on it (e.g. {@code OrderEventPublisher}) can be wired in tests
 * without a live broker.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }
}
