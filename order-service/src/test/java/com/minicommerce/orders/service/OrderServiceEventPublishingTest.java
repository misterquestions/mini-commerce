package com.minicommerce.orders.service;

import com.minicommerce.orders.events.EventPublisher;
import com.minicommerce.orders.events.OrderCreatedEvent;
import com.minicommerce.orders.events.Topics;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class OrderServiceEventPublishingTest {

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
    OrderService service;
    @Autowired
    OrderRepository orders;

    CreateOrderRequest request;

    @BeforeEach
    void setup() {
        orders.deleteAll();
        request = new CreateOrderRequest(
                UUID.randomUUID(),
                "USD",
                List.of(new OrderItemRequest("SKU1", "Mouse", 1, new BigDecimal("10.00")))
        );
    }

    @Test
    void create_publishes_event_after_commit() {
        service.create(request);

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        Mockito.verify(publisher).publish(Mockito.eq(Topics.ORDER_CREATED), Mockito.anyString(), event.capture());
        assertThat(event.getValue().type()).isEqualTo("order.created");
        assertThat(event.getValue().version()).isEqualTo("v1");
    }

    @Test
    void create_throws_when_publish_fails_but_order_persisted() {
        Mockito.doThrow(new RuntimeException("kafka down"))
                .when(publisher).publish(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        assertThrows(RuntimeException.class, () -> service.create(request));
        assertEquals(1, orders.count());
    }

    @Test
    void cancel_publishes_event() {
        service.create(request);
        Mockito.reset(publisher);

        var order = orders.findAll().get(0);
        service.cancel(order.getId());

        Mockito.verify(publisher)
                .publish(Mockito.eq(Topics.ORDER_CANCELLED),
                        Mockito.eq(order.getId().toString()),
                        Mockito.any());
    }
}
