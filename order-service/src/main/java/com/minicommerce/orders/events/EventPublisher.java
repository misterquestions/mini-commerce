package com.minicommerce.orders.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public static UUID newEventId() {
        return UUID.randomUUID();
    }

    public void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload); // single attempt; outbox handles retries
    }

    public void publishWithHeaders(String topic, String key, EventEnvelope<?> envelope) {
        var message = MessageBuilder.withPayload(envelope).setHeader(KafkaHeaders.TOPIC, topic).setHeader(KafkaHeaders.KEY, key).setHeader("eventId", envelope.eventId().toString()).setHeader("aggregateId", envelope.aggregateId()).setHeader("aggregateType", envelope.aggregateType()).setHeader("eventType", envelope.type()).setHeader("eventVersion", envelope.version()).setHeader("traceId", envelope.traceId()).build();
        kafkaTemplate.send(message);
    }
}
