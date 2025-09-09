package com.minicommerce.orders.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    public EventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publish(String topic, String key, Object payload) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                kafka.send(topic, key, payload).get();
                return;
            } catch (Exception e) {
                last = new RuntimeException("Failed to publish to %s (attempt %d)".formatted(topic, attempt), e);
            }
        }
        throw last;
    }
}
