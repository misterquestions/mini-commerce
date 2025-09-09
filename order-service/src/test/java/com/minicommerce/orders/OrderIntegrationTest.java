package com.minicommerce.orders;

import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import com.minicommerce.orders.web.dto.OrderResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("minicommerce")
            .withUsername("minicommerce")
            .withPassword("minicommerce");

    @BeforeAll
    static void init() {
        System.setProperty("spring.datasource.url", pg.getJdbcUrl());
        System.setProperty("spring.datasource.username", pg.getUsername());
        System.setProperty("spring.datasource.password", pg.getPassword());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        System.setProperty("spring.flyway.enabled", "true");
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
    }
}
