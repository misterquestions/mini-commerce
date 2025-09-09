package com.minicommerce.orders.events;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventPublisherTest {

    @Test
    void publish_invokes_single_send_only() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        EventPublisher publisher = new EventPublisher(kafkaTemplate);

        publisher.publish("topic", "key", "payload");
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }

    @Test
    void publishWithHeaders_sends_envelope() {
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        EventPublisher publisher = new EventPublisher(kafkaTemplate);
        var envelope = new EventEnvelope<>(EventPublisher.newEventId(), "order.created", "v1", java.time.OffsetDateTime.now(), "order", "agg-1", "trace-1", new Object());
        publisher.publishWithHeaders("topic", "key", envelope);
        // Explicitly match Message<?> overload to avoid ambiguity with ProducerRecord send()
        verify(kafkaTemplate, times(1)).send(Mockito.<Message<?>>any());
    }
}
