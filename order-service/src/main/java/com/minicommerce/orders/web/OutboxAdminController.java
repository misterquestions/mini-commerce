package com.minicommerce.orders.web;

import com.minicommerce.orders.outbox.OutboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outbox")
public class OutboxAdminController {
    private final OutboxService outboxService;

    public OutboxAdminController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @PostMapping("/requeue-failed")
    public Map<String, Object> requeueFailed() {
        int count = outboxService.requeueFailed();
        return Map.of("requeued", count);
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<?> requeueSingle(@PathVariable UUID id) {
        boolean ok = outboxService.requeueSingle(id);
        if (ok) return ResponseEntity.ok(Map.of("requeued", id));
        return ResponseEntity.notFound().build();
    }
}

