package com.cloudpulse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("CloudPulse Application Context Tests")
class CloudPulseApplicationTest {

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // Verify the Spring context starts without errors
    }
}
