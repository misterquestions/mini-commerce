package com.minicommerce.orders.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.orders.outbox.OutboxEventRepository;
import com.minicommerce.orders.outbox.OutboxStatus;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderServiceEventPublishingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OutboxEventRepository outboxRepository;
    CreateOrderRequest createRequest;

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // speed up relay for test
        registry.add("outbox.relay-interval-ms", () -> "500");
    }

    @BeforeEach
    void setup() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
        createRequest = new CreateOrderRequest(UUID.randomUUID(), "USD", List.of(new OrderItemRequest("SKU1", "Mouse", 1, new BigDecimal("10.00"))));
    }

    @Test
    void create_persists_outbox_envelope() throws Exception {
        orderService.create(createRequest);
        assertThat(orderRepository.count()).isEqualTo(1);
        var rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.NEW);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(row.getPayload());
        assertThat(json.get("type").asText()).isEqualTo("order.created");
        assertThat(json.get("data").get("orderId").asText()).isNotBlank();
    }

    @Test
    void cancel_adds_second_outbox_event() throws Exception {
        var order = orderService.create(createRequest);
        orderService.cancel(order.getId());
        var rows = outboxRepository.findAll();
        assertThat(rows).hasSize(2);
        ObjectMapper mapper = new ObjectMapper();
        long cancelledCount = rows.stream().filter(r -> {
            try {
                return mapper.readTree(r.getPayload()).get("type").asText().equals("order.cancelled");
            } catch (Exception e) {
                return false;
            }
        }).count();
        assertThat(cancelledCount).isEqualTo(1);
    }
}
