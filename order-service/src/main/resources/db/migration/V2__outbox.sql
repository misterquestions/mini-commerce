-- V2__outbox.sql
-- Transactional outbox table for reliable event publishing
CREATE TABLE IF NOT EXISTS public.outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_id    TEXT NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    topic           TEXT NOT NULL,
    event_key       TEXT NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(16) NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error      TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_due ON public.outbox_events(status, next_attempt_at);

