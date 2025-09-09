package com.minicommerce.orders.outbox;

import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.service.OrderService;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OutboxRequeueEndpointTest {
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
        r.add("outbox.relay-interval-ms", () -> "5000"); // slow relay to avoid races
    }

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OutboxEventRepository outboxRepository;
    @Autowired TestRestTemplate rest;

    @BeforeEach
    void clean(){
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void requeue_failed_endpoint_resets_failed_rows() {
        // create order -> NEW outbox row
        orderService.create(new CreateOrderRequest(UUID.randomUUID(), "USD", List.of(
                new OrderItemRequest("SKU-RQ", "Mouse", 1, new BigDecimal("11.00"))
        )));
        var row = outboxRepository.findAll().get(0);
        // force FAILED state
        row.setStatus(OutboxStatus.FAILED);
        row.setAttempts(3);
        row.setLastError("forced");
        outboxRepository.save(row);

        ResponseEntity<Map> resp = rest.exchange("/api/v1/outbox/requeue-failed", HttpMethod.POST, null, Map.class);
        Assertions.assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        var updated = outboxRepository.findAll().get(0);
        Assertions.assertThat(updated.getStatus()).isEqualTo(OutboxStatus.RETRY);
        Assertions.assertThat(updated.getAttempts()).isZero();
        Assertions.assertThat(updated.getLastError()).isNull();
    }
}

