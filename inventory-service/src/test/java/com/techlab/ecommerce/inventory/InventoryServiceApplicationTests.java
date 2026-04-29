package com.techlab.ecommerce.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test: verifies the full Inventory Service context boots with AMQP
 * auto-configuration excluded. RabbitTemplate is mocked for publisher beans.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceApplicationTests {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }
}
