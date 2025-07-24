# order-flow

Event-driven order service built with Java 21 and Spring Boot. It is a modular monolith: the
modules communicate only through Spring application events, with no broker or external infrastructure.
Data lives in an in-memory H2 database.

## How an order flows

```
POST /orders ─▶ OrderCreated
                  └─▶ inventory ─▶ InventoryReserved ─▶ order: STOCK_RESERVED ─▶ PaymentRequested
                                 └▶ InventoryRejected ─▶ order: REJECTED
PaymentRequested ─▶ payment ─▶ PaymentCompleted ─▶ order: CONFIRMED ─▶ OrderConfirmed ─▶ shipping ─▶ OrderShipped ─▶ order: SHIPPED
                           └▶ PaymentFailed ─▶ order: CANCELLED ─▶ CompensationRequested
CompensationRequested ─▶ inventory releases stock, payment refunds (both idempotent)
```

| Package | Responsibility |
|---|---|
| `order` | REST API, order status state machine, saga handlers, event timeline |
| `inventory` | Stock levels, all-or-nothing reservations, release on compensation |
| `payment` | Simulated gateway (declines amounts above `orderflow.payment.max-amount`), refunds |
| `shipping` | Simulated fulfilment that returns a tracking number |
| `shared` | Event contracts (`OrderEvents`), `@SagaListener`, simulated latency |

### Design notes
- `@SagaListener` = `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`. A handler only runs once the
  publishing transaction has committed, and it runs on its own thread and transaction.
- Every event is written to `order_event_log` in the publisher's transaction, which backs `GET /orders/{id}/events`.
- Handlers are idempotent (reservation, charge, release and refund each check whether they already happened).
- A step that finishes for an already-cancelled order triggers `CompensationRequested` again, so a cancel racing the
  saga still releases stock and refunds.
- Persistence is plain state (JPA, optimistic locking on orders, pessimistic locks on stock rows). No outbox or
  event sourcing, because in-process events cannot survive a crash between commit and handling.
  If the app dies mid-saga, in-flight orders stay in their last status.

## API

| Method | Path | |
|---|---|---|
| POST | `/orders` | Create an order (202 Accepted, status `PENDING`) |
| GET | `/orders/{id}` | Current state |
| GET | `/orders/{id}/events` | Event timeline |
| POST | `/orders/{id}/cancel` | Cancel while `PENDING`, `STOCK_RESERVED` or `CONFIRMED` (409 otherwise) |
| GET | `/inventory` | Stock levels (seeded: `BOOK-001`, `PEN-001`, `LAPTOP-001`) |

## Run

```bash
mvn spring-boot:run
```

```bash
curl -s -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"customerId":"c1","items":[{"sku":"BOOK-001","quantity":2,"unitPrice":12.50}]}'
```

Then `GET /orders/{id}` and `GET /orders/{id}/events` with the returned id. An order totalling more than 10000
is declined and cancelled; a SKU that is unknown or short on stock is rejected.

## Test

```bash
mvn test
```
