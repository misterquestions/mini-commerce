package com.minicommerce.orders;

import com.minicommerce.orders.events.Topics;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import com.minicommerce.orders.web.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TestRestTemplate http;

    @BeforeEach
    void enablePatchSupport() {
        http.getRestTemplate()
                .setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));
    }

    @Test
    void create_and_cancel_order_happy_path() {
        var req = new CreateOrderRequest(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "USD",
                List.of(new OrderItemRequest("SKU-1", "Mouse", 2, new BigDecimal("19.99")))
        );

        ObjectMapper mapper = new ObjectMapper();

        // Configure consumer (use random group so we always read from beginning)
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(Topics.ORDER_CREATED, Topics.ORDER_CANCELLED));

            ResponseEntity<OrderResponse> created = http.postForEntity("/api/v1/orders", req, OrderResponse.class);
            Assertions.assertEquals(HttpStatus.CREATED, created.getStatusCode());
            var order = created.getBody();
            Assertions.assertNotNull(order);
            Assertions.assertEquals("created", order.status());
            Assertions.assertEquals(new BigDecimal("39.98"), order.total());

            ConsumerRecord<String, String> createdEvent = pollForEvent(consumer, Topics.ORDER_CREATED, order.id().toString());
            Assertions.assertNotNull(createdEvent, "ORDER_CREATED event not received");
            JsonNode createdJson = mapper.readTree(createdEvent.value());
            Assertions.assertEquals("order.created", createdJson.get("type").asText());
            Assertions.assertEquals(order.id().toString(), createdJson.get("data").get("orderId").asText());

            // cancel
            ResponseEntity<OrderResponse> cancelled = http.exchange(
                    "/api/v1/orders/{id}/cancel", HttpMethod.PATCH, HttpEntity.EMPTY, OrderResponse.class,
                    Map.of("id", order.id())
            );
            Assertions.assertEquals(HttpStatus.OK, cancelled.getStatusCode());
            Assertions.assertEquals("cancelled", cancelled.getBody().status());

            ConsumerRecord<String, String> cancelledEvent = pollForEvent(consumer, Topics.ORDER_CANCELLED, order.id().toString());
            Assertions.assertNotNull(cancelledEvent, "ORDER_CANCELLED event not received");
            JsonNode cancelledJson = mapper.readTree(cancelledEvent.value());
            Assertions.assertEquals("order.cancelled", cancelledJson.get("type").asText());
            Assertions.assertEquals(order.id().toString(), cancelledJson.get("data").get("orderId").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConsumerRecord<String, String> pollForEvent(KafkaConsumer<String, String> consumer, String topic, String key) {
        long deadline = System.currentTimeMillis() + 10_000; // 10s

        while (System.currentTimeMillis() < deadline) {
            var records = consumer.poll(java.time.Duration.ofMillis(250));
            for (ConsumerRecord<String, String> r : records) {
                if (r.topic().equals(topic) && key.equals(r.key())) {
                    return r;
                }
            }
        }

        return null;
    }
}
