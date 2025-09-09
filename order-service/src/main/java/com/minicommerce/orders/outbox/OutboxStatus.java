package com.minicommerce.orders.outbox;

public enum OutboxStatus {
    NEW, RETRY, SENT, FAILED
}

