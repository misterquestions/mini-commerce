package com.minicommerce.orders.events;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EventPublisherTest {

    @Test
    void retries_on_failure() {
        KafkaTemplate<String, Object> kafka = Mockito.mock(KafkaTemplate.class);
        EventPublisher publisher = new EventPublisher(kafka);

        CompletableFuture<SendResult<String, Object>> fail = new CompletableFuture<>();
        fail.completeExceptionally(new RuntimeException("fail"));
        CompletableFuture<SendResult<String, Object>> success = new CompletableFuture<>();
        success.complete(null);

        Mockito.when(kafka.send(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(fail, fail, success);

        publisher.publish("t", "k", new Object());
        Mockito.verify(kafka, Mockito.times(3)).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void throws_after_max_attempts() {
        KafkaTemplate<String, Object> kafka = Mockito.mock(KafkaTemplate.class);
        EventPublisher publisher = new EventPublisher(kafka);

        CompletableFuture<SendResult<String, Object>> fail = new CompletableFuture<>();
        fail.completeExceptionally(new RuntimeException("fail"));
        Mockito.when(kafka.send(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(fail);

        assertThrows(RuntimeException.class, () -> publisher.publish("t", "k", new Object()));
        Mockito.verify(kafka, Mockito.times(3)).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }
}
