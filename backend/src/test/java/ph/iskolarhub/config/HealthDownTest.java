package ph.iskolarhub.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import ph.iskolarhub.support.PostgresTestConfig;

/**
 * Simulates a failing database check by replacing the "db" health indicator. This verifies that readiness
 * includes the database and reports 503; it does not stop a real PostgreSQL instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class HealthDownTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class DownDatabase {
        @Bean
        HealthIndicator dbHealthIndicator() {
            return () -> Health.down().build();
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void readinessReportsServiceUnavailableWhenDatabaseCheckFails() throws Exception {
        mvc.perform(get("/api/v1/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void livenessStaysUpWhenDatabaseCheckFails() throws Exception {
        mvc.perform(get("/api/v1/actuator/health/liveness"))
                .andExpect(status().isOk());
    }
}
