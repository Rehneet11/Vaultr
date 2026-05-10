package com.example.vaultr.services;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = Resilience4jKafkaIntegrationTest.TestConfig.class, properties = {
        "resilience4j.retry.instances.kafkaPublisher.max-attempts=3",
        "resilience4j.retry.instances.kafkaPublisher.wait-duration=1s",
        "resilience4j.circuitbreaker.instances.kafkaPublisher.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.kafkaPublisher.failure-rate-threshold=50",
        "spring.kafka.producer.properties.max.block.ms=500"
})
@Testcontainers
public class Resilience4jKafkaIntegrationTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @org.springframework.context.annotation.Import(KafkaPublisher.class)
    static class TestConfig {
    }

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private KafkaPublisher kafkaPublisher;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    public void setup() {
        // Reset metrics
        circuitBreakerRegistry.circuitBreaker("kafkaPublisher").transitionToClosedState();
    }

    @Test
    public void testRetryWhenKafkaIsDown() {
        // Stop Kafka to simulate failure
        kafkaContainer.stop();

        Exception exception = assertThrows(Exception.class, () -> {
            kafkaPublisher.publishEvent("test-topic", "key1", "payload");
        });

        // The retry should have attempted 3 times (1 initial + 2 retries)
        io.github.resilience4j.retry.Retry retry = retryRegistry.retry("kafkaPublisher");
        long failedCalls = retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt();
        assertTrue(failedCalls > 0, "Should have recorded failed calls with retries");
    }

    @Test
    public void testCircuitBreakerOpensAfterFailures() {
        if (kafkaContainer.isRunning()) {
            kafkaContainer.stop();
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("kafkaPublisher");

        // The sliding window size is 10, and failure rate is 50%
        for (int i = 0; i < 10; i++) {
            try {
                kafkaPublisher.publishEvent("test-topic", "key" + i, "payload");
            } catch (Exception e) {
                // Expected
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.getState(), "Circuit breaker should be OPEN");

        // Subsequent calls should throw CallNotPermittedException
        assertThrows(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class, () -> {
            kafkaPublisher.publishEvent("test-topic", "key-open", "payload");
        });
    }
}
