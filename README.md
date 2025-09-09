# Mini-Commerce

**Mini-Commerce** is a polyglot, event-driven e-commerce portfolio system demonstrating modern microservices architecture, SDLC, and analytics. It showcases:

- **Polyglot microservices**: Java (Spring Boot), Node.js (Nest), Python (FastAPI), Go
- **Event-driven architecture**: Kafka (order lifecycle, audit, analytics)
- **APIs**: REST + OpenAPI, data persistence (Postgres, MongoDB), dbt analytics, RAG for explainable LLM answers
- **DevEx/SDLC**: Code quality, tests, CI/CD (GitHub Actions), IaC (Terraform), Azure (AKS, ACR, Event Hubs Kafka), Docker, Helm, Observability
- **End-to-end order lifecycle**: From order creation to fulfillment, with real-time status and analytics

## Architecture Overview

![System Overview](docs/diagrams/image/system-overview.png)

- **Gateway**: API aggregation, auth mock, rate limiting
- **Order Service**: Authoritative order domain, event publishing, transactional outbox
- **Payment Service**: Idempotent payments, retries, circuit breaker
- **Inventory Service**: Stock checks, reservations, Kafka consumer
- **Fulfillment Service**: Event-driven shipping, webhooks, idempotency
- **Analytics Service**: Metrics agent, RAG, dbt marts
- **Admin UI**: React dashboard, chat with LLM

See [docs/diagrams](docs/diagrams/) for more diagrams.

## Key Features

- **Transactional Outbox** for reliable event publishing ([ADR](docs/adr/0001-transactional-outbox-for-domain-events.md))
- **Observability**: OpenTelemetry, correlation IDs, Grafana/Loki
- **CI/CD & IaC**: GitHub Actions, Helm, Terraform, Azure
- **Explainable Analytics**: dbt, pgvector, RAG, provenance

## Quickstart

1. **Prerequisites**: Docker, Docker Compose
2. **Run locally**:
   ```sh
   docker-compose up --build
   ```
3. **Access services**:
   - Gateway: http://localhost:8080
   - Admin UI: http://localhost:3000
   - Service APIs: see [docs/api](docs/api/)

## Service READMEs
- [Order Service](order-service/README.md)
- [Payment Service](payment-service/README.md)
- [Inventory Service](inventory-service/README.md)
- [Fulfillment Service](fulfillment-service/README.md)
- [Analytics Service](analytics-service/README.md)

## Documentation & Diagrams
- [Architecture Decision Records](docs/adr/)
- [Diagrams](docs/diagrams/)
- [OpenAPI Specs](docs/api/)
- [dbt Analytics](analytics/dbt/)

## User Journeys
1. **Create Order** → Payment → Inventory → Fulfillment → Status in UI
2. **Admin UI**: Orders, statuses, chat with LLM (metrics, RAG)
3. **Data Ops**: Mongo → Postgres → dbt → Analytics
4. **Webhooks/Integrations**: Fulfillment, carrier simulation

## Tech Stack
- **Languages**: Java 21, Node.js 20, Python 3.11, Go 1.22
- **Data**: PostgreSQL, MongoDB, Kafka
- **Analytics**: dbt, pgvector
- **Testing**: JUnit, Jest, pytest, testify, Playwright
- **Platform**: Azure, Docker, Helm, Terraform

---

For deep technical details, see each service's README and [docs/adr](docs/adr/).
