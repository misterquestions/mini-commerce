package com.minicommerce.orders.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status IN ('NEW','RETRY') AND (next_attempt_at IS NULL OR next_attempt_at <= :now) ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findBatchDue(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    long countByStatus(OutboxStatus status);
}

