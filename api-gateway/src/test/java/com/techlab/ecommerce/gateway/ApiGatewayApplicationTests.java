package com.techlab.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "gateway.jwt.secret=test-secret-for-unit-tests-must-be-long-enough-32-chars"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
