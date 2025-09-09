package com.minicommerce.orders.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.orders.outbox.OutboxEventRepository;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.service.OrderService;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.assertj.core.api.Assertions;
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Set;

@SpringBootTest
@Testcontainers
class OrderCreatedSchemaValidationTest {
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
        r.add("outbox.relay-interval-ms", () -> "250");
    }

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OutboxEventRepository outboxRepository;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void clean(){
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void created_event_matches_schema() throws Exception {
        orderService.create(new CreateOrderRequest(UUID.randomUUID(), "USD", List.of(
                new OrderItemRequest("SKU-S", "Mouse", 1, new BigDecimal("7.50"))
        )));
        var row = outboxRepository.findAll().get(0);
        JsonNode payload = mapper.readTree(row.getPayload());

        Path schemaPath = Path.of("..", "docs", "api", "events", "order.created.v1.json");
        try (InputStream in = Files.newInputStream(schemaPath)) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            JsonSchema schema = factory.getSchema(in);
            Set<ValidationMessage> errors = schema.validate(payload);
            Assertions.assertThat(errors).as("Schema violations" + errors).isEmpty();
        }
    }
}

