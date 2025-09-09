package com.minicommerce.orders.outbox;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_due", columnList = "status,next_attempt_at")
})
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String key;

    @Column(columnDefinition = "text", nullable = false)
    private String payload; // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @PrePersist
    public void prePersist(){
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if(id==null) id = UUID.randomUUID();
        if(status==null) status = OutboxStatus.NEW;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public String getAggregateId(){ return aggregateId; }
    public void setAggregateId(String aggregateId){ this.aggregateId = aggregateId; }
    public String getAggregateType(){ return aggregateType; }
    public void setAggregateType(String aggregateType){ this.aggregateType = aggregateType; }
    public String getTopic(){ return topic; }
    public void setTopic(String topic){ this.topic = topic; }
    public String getKey(){ return key; }
    public void setKey(String key){ this.key = key; }
    public String getPayload(){ return payload; }
    public void setPayload(String payload){ this.payload = payload; }
    public OutboxStatus getStatus(){ return status; }
    public void setStatus(OutboxStatus status){ this.status = status; }
    public int getAttempts(){ return attempts; }
    public void setAttempts(int attempts){ this.attempts = attempts; }
    public OffsetDateTime getNextAttemptAt(){ return nextAttemptAt; }
    public void setNextAttemptAt(OffsetDateTime nextAttemptAt){ this.nextAttemptAt = nextAttemptAt; }
    public OffsetDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt){ this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt(){ return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt){ this.updatedAt = updatedAt; }
    public String getLastError(){ return lastError; }
    public void setLastError(String lastError){ this.lastError = lastError; }
}

