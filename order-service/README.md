# Mini-Commerce — M1: Order Service (Spring Boot + Gradle)

## Run (local)
1. Start Postgres:
   ```bash
   docker run --name pg -e POSTGRES_PASSWORD=minicommerce -e POSTGRES_USER=minicommerce -e POSTGRES_DB=minicommerce -p 5432:5432 -d postgres:16
   ```
2. Build & run:
   ```bash
   ./gradlew bootRun
   ```
3. Open API docs:
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI YAML: `src/main/resources/openapi/order-service.yaml`

## Example
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId":"00000000-0000-0000-0000-000000000001",
    "currency":"USD",
    "items":[{"sku":"SKU-1","name":"Mouse","qty":2,"unitPrice":19.99}]
  }'
```
