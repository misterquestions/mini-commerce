# Order Service

The **Order Service** is the authoritative source for order management in Mini-Commerce. It owns the order domain, enforces status transitions, and reliably publishes domain events to Kafka using the transactional outbox pattern.

## Purpose & Responsibilities
- **Owns**: Orders, status machine
- **Publishes**: order.created, order.paid, order.fulfillment_requested, order.cancelled, order.completed
- **Consumes**: payment.approved|declined, inventory.reserved|rejected, fulfillment.shipped|failed
- **DB**: PostgreSQL (orders, order_items)

## API Endpoints
- `POST /orders` → Creates order, emits `order.created`
- `GET /orders/{id}` → Fetch order
- `PATCH /orders/{id}/cancel` → Cancels order, emits `order.cancelled`

See [OpenAPI spec](src/main/resources/openapi/order-service.yaml).

## Event Publishing & Reliability

Events are published to Kafka using a **transactional outbox** pattern:
- Order changes and event records are committed atomically in the DB
- A background process reads outbox rows and publishes to Kafka
- Ensures exactly-once, reliable event delivery even if Kafka is temporarily unavailable
- See [ADR: Transactional Outbox](../docs/adr/0001-transactional-outbox-for-domain-events.md)

![Outbox Flow](../docs/diagrams/outbox-flow.puml)

## Error Handling & Resilience
- If Kafka is down, events are retried from the outbox
- No silent fail: events are guaranteed to be published, with observability and alerting
- Outbox rows are only deleted after successful publish
- Metrics and traces are emitted for all event operations

## Testing & Local Development
- **Unit & Integration Tests**: JUnit5, Testcontainers (Kafka, Postgres)
- **Run tests**:
  ```sh
  ./gradlew test
  ```
- **Local dev**: See [../README.md](../README.md) for Docker Compose setup

## Diagrams
- [Order Service Context](../docs/diagrams/order-service-context.puml)
- [Order Lifecycle Sequence](../docs/diagrams/order-lifecycle-sequence.puml)
- [Outbox Flow](../docs/diagrams/outbox-flow.puml)

## Observability
- OpenTelemetry traces, correlation IDs (orderId)
- Centralized logging, metrics

## Further Reading
- [Transactional Outbox ADR](../docs/adr/0001-transactional-outbox-for-domain-events.md)
- [System Overview](../docs/diagrams/system-overview.puml)
- [OpenAPI Spec](src/main/resources/openapi/order-service.yaml)

---

For questions, see the global [README](../README.md) or reach out to the team.

