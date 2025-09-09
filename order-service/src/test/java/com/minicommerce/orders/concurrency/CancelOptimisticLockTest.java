package com.minicommerce.orders.concurrency;

import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.service.OrderService;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
@Testcontainers
class CancelOptimisticLockTest {
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
        r.add("outbox.relay-interval-ms", () -> "500");
    }

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    UUID orderId;

    @BeforeEach
    void setup(){
        orderRepository.deleteAll();
        var order = orderService.create(new CreateOrderRequest(UUID.randomUUID(), "USD", List.of(
                new OrderItemRequest("SKU-C", "Mouse", 1, new BigDecimal("10.00"))
        )));
        orderId = order.getId();
    }

    @Test
    void second_cancel_attempt_hits_optimistic_lock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> task = () -> {
                try {
                    orderService.cancel(orderId);
                    return true;
                } catch (OptimisticLockingFailureException e){
                    return false; // expected for one thread
                }
            };
            Future<Boolean> f1 = pool.submit(task);
            Future<Boolean> f2 = pool.submit(task);
            boolean r1 = get(f1);
            boolean r2 = get(f2);
            org.assertj.core.api.Assertions.assertThat(r1 ^ r2).as("Exactly one cancel should succeed").isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean get(Future<Boolean> f) throws ExecutionException, InterruptedException { return f.get(); }
}
