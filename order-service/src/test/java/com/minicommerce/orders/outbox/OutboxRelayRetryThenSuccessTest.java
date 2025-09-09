package com.minicommerce.orders.outbox;

import com.minicommerce.orders.events.EventPublisher;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.service.OrderService;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@Testcontainers
class OutboxRelayRetryThenSuccessTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.flyway.enabled", () -> "false");
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("outbox.relay-interval-ms", () -> "150");
        r.add("outbox.max-attempts", () -> "4");
        r.add("outbox.initial-backoff-ms", () -> "50");
    }

    @TestConfiguration
    static class FlakyPublisherConfig {
        @Bean
        @Primary
        EventPublisher flakyPublisher(){
            AtomicInteger counter = new AtomicInteger();
            return new EventPublisher(null) {
                @Override
                public void publish(String topic, String key, Object payload) {
                    if(counter.getAndIncrement() < 2){
                        throw new RuntimeException("transient failure");
                    }
                    // success after 2 failures
                }
            };
        }
    }

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OutboxEventRepository outboxRepository;

    @BeforeEach
    void clean(){
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void transient_failures_eventually_sent() {
        orderService.create(new CreateOrderRequest(UUID.randomUUID(), "USD", List.of(new OrderItemRequest("SKU-R", "Mouse",1,new BigDecimal("5.00")))));
        await().atMost(Duration.ofSeconds(6))
                .pollInterval(Duration.ofMillis(125))
                .untilAsserted(() -> {
                    var row = outboxRepository.findAll().get(0);
                    org.assertj.core.api.Assertions.assertThat(row.getStatus()).isEqualTo(OutboxStatus.SENT);
                });
    }
}
