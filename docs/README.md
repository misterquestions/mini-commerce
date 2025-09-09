# Documentation Index

This directory houses architecture and API documentation for Mini-Commerce.

Structure:
- adr/ : Architecture Decision Records (lightweight, sequential)
- api/ : OpenAPI specs and event schemas
  - events/ : JSON/Avro schemas for Kafka domain events
- diagrams/ : (planned) PlantUML / PNG diagrams (system, sequence, ERD)

## ADR Process
1. Copy `adr/0000-template.md` to next sequential number.
2. Keep title concise (imperative verb + object).
3. Status values: Proposed | Accepted | Superseded | Deprecated.
4. Reference ADR numbers in commit messages / PR descriptions when relevant.

## Event Schema Guidelines
- File naming: `<event>.<version>.json` (e.g., `order.created.v1.json`).
- Version bump when backward-incompatible change.
- Keys: always use aggregate identifier (`orderId`) for ordering.
- Envelope (future): eventId, type, version, occurredAt, producer, traceId, data.

## OpenAPI
Service-specific OpenAPI specs live within each service (e.g., `order-service/src/main/resources/openapi`). Aggregate spec (future) will be produced via the gateway build.

## Roadmap Docs (Planned Additions)
- diagrams/system-overview.puml
- diagrams/order-lifecycle-sequence.puml
- api/events/*.avsc (Avro)
- metrics/ (Prometheus rules, SLOs)

---
See root README for high-level overview and each service README for deep dives.

