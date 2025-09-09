package com.minicommerce.orders;

import com.minicommerce.orders.events.EventPublisher;
import com.minicommerce.orders.events.Topics;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import com.minicommerce.orders.web.dto.OrderResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.mockito.Mockito;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    TestRestTemplate http;

    @org.springframework.boot.test.mock.mockito.MockBean
    EventPublisher publisher;

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

        ResponseEntity<OrderResponse> created = http.postForEntity("/api/v1/orders", req, OrderResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, created.getStatusCode());
        var order = created.getBody();
        Assertions.assertNotNull(order);
        Assertions.assertEquals("created", order.status());
        Assertions.assertEquals(new BigDecimal("39.98"), order.total());

        // cancel
        ResponseEntity<OrderResponse> cancelled =
                http.exchange("/api/v1/orders/{id}/cancel", HttpMethod.PATCH, HttpEntity.EMPTY, OrderResponse.class,
                        Map.of("id", order.id()));
        Assertions.assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        Assertions.assertEquals("cancelled", cancelled.getBody().status());

        Mockito.verify(publisher).publish(Mockito.eq(Topics.ORDER_CREATED), Mockito.eq(order.id().toString()), Mockito.any());
        Mockito.verify(publisher).publish(Mockito.eq(Topics.ORDER_CANCELLED), Mockito.eq(order.id().toString()), Mockito.any());
    }
}
