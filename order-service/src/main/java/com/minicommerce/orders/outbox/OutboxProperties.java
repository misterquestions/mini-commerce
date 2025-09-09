package com.minicommerce.orders.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {
    /** Batch size for each relay poll */
    private int batchSize = 50;
    /** Max publish attempts before marking FAILED */
    private int maxAttempts = 8;
    /** Initial backoff in milliseconds */
    private long initialBackoffMs = 500;
    /** Exponential multiplier */
    private double backoffMultiplier = 2.0d;
    /** Scheduler fixed delay ms */
    private long relayIntervalMs = 1000;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getInitialBackoffMs() { return initialBackoffMs; }
    public void setInitialBackoffMs(long initialBackoffMs) { this.initialBackoffMs = initialBackoffMs; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    public long getRelayIntervalMs() { return relayIntervalMs; }
    public void setRelayIntervalMs(long relayIntervalMs) { this.relayIntervalMs = relayIntervalMs; }
}
