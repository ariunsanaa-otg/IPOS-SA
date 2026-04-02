# IPOS-SA — InfoPharma Ordering System

**Live:** [https://ipos-sa.up.railway.app/](https://ipos-sa.up.railway.app/)

A full-stack pharmaceutical ordering and account management system built as a coursework project. The backend exposes a RESTful API covering the full order lifecycle, merchant account management, stock control, invoicing, payments, and business reporting.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend framework | Spring Boot 3.4.3 |
| Language | Java 21 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL (hosted on Railway) |
| Object mapping | ModelMapper 3.2.4 |
| Boilerplate reduction | Lombok 1.18.38 |
| Frontend | Vite + React |
| Build tool | Maven |

---

## Architecture

The backend follows a strict layered architecture:

```
Controller  →  Service (interface)  →  ServiceImpl  →  Repository (Spring Data JPA)
                                            ↕
                                     Mapper<A, B>
                                  (dedicated impl classes)
```

- **Controllers** — thin REST layer, delegates all logic to services
- **Services** — business logic, transaction boundaries, cascade operations
- **Repositories** — Spring Data JPA with derived query methods
- **Mappers** — dedicated `Mapper<Entity, DTO>` implementation classes wrapping ModelMapper; no raw `ModelMapper` injection in controllers or services
- **Scheduler** — nightly `@Scheduled` job auto-updates merchant account statuses based on payment age
- **Global exception handler** — `@RestControllerAdvice` maps `EntityNotFoundException` → 404, `IllegalStateException` → 400

---

## Features

### Account Management
- Create user and merchant accounts (MERCHANT / ADMIN / MANAGER roles)
- Update contact details, credit limits, and discount plan assignments
- Cascade delete — removing an account cleans up all payments, invoices, and orders
- Automatic nightly status updates: accounts > 30 days overdue → `IN_DEFAULT`, > 15 days → `SUSPENDED`
- Manager-only status restore endpoint

### Catalogue & Stock
- Full CRUD for catalogue items
- Stock delivery recording with `StockDelivery` audit trail
- Low-stock report using configurable `minStockLevel` + `reorderBufferPct` buffer formula
- Keyword search across item ID and description

### Orders
- Place orders with real-time stock validation and reduction
- Invoice auto-generated on order placement (6-digit ID)
- Order lifecycle: `ACCEPTED` → `BEING_PROCESSED` → `DISPATCHED` → `DELIVERED`
- Dispatch records courier, courier reference, and expected delivery date
- Merchant's own order history endpoint

### Payments & Invoices
- Record payments against specific invoices
- Balance automatically reduced on payment
- Account auto-restored `SUSPENDED` → `NORMAL` when balance clears
- `IN_DEFAULT` accounts require manual manager restore

### Discount Plans
- Create tiered discount plans (FIXED or FLEXIBLE)
- Each tier defines a min/max order value range and a discount rate
- Assign plans to merchant accounts

### Reporting (6 report types)
| Report | Endpoint |
|---|---|
| Sales turnover | `GET /api/reports/turnover` |
| Merchant order summary | `GET /api/reports/merchant/{id}/orders` |
| Merchant detailed orders (line items) | `GET /api/reports/merchant/{id}/orders/detailed` |
| Merchant invoices | `GET /api/reports/merchant/{id}/invoices` |
| All invoices | `GET /api/reports/invoices` |
| Stock turnover (received vs sold) | `GET /api/reports/stock-turnover` |
| Overdue debtor reminders | `GET /api/accounts/debtors` |

---

## API Summary

**41 endpoints** across 7 resource groups. All dates use ISO 8601 (`YYYY-MM-DD`).

| Group | Base path | Endpoints |
|---|---|---|
| Accounts | `/api/accounts` | 11 |
| Discount Plans | `/api/discount-plans` | 5 |
| Catalogue | `/api/catalogue` | 8 |
| Orders | `/api/orders` | 7 |
| Invoices | `/api/invoices` | 3 |
| Payments | `/api/payments` | 1 |
| Reports | `/api/reports` | 6 |

> Full request/response examples are documented in `IPOS-SA API Reference.docx`.

---

## Getting Started

The API is live at **https://ipos-sa.up.railway.app/** — all endpoints below are available against that base URL.

### Prerequisites (local development)
- Java 21+
- Maven 3.9+
- PostgreSQL database (local or remote)

### Run locally

```bash
# Clone the repo
git clone https://github.com/Neil-21/IPOS-SA.git
cd IPOS-SA/backend

# Set your database connection in:
# src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://<host>:<port>/railway
spring.datasource.username=postgres
spring.datasource.password=yourpassword

# Run
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Example request

```bash
# Create a merchant account
curl -X POST https://ipos-sa.up.railway.app/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jsmith",
    "password": "secret123",
    "accountType": "MERCHANT",
    "accountStatus": "NORMAL",
    "contactName": "John Smith",
    "companyName": "Smith Pharmacy",
    "phone": "07712345678",
    "email": "john@smithpharma.co.uk"
  }'

# Place an order
curl -X POST https://ipos-sa.up.railway.app/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "items": [
      { "itemId": "100 00042", "quantity": 10 }
    ]
  }'
```

---

## Project Structure

```
backend/
├── controller/        # REST endpoints
├── service/           # Business logic interfaces
│   └── impl/          # Service implementations + AccountScheduler
├── repository/        # Spring Data JPA repositories
├── entity/            # JPA entities
├── dto/               # Request / response DTOs
├── mapper/            # Mapper<A,B> interface
│   └── impl/          # Dedicated mapper implementations
└── exception/         # GlobalExceptionHandler

frontend/              # Vite + React frontend
```

---

## Enum Reference

| Enum | Values |
|---|---|
| `AccountType` | `MERCHANT` `ADMIN` `MANAGER` |
| `AccountStatus` | `NORMAL` `SUSPENDED` `IN_DEFAULT` |
| `PlanType` | `FIXED` `FLEXIBLE` |
| `PaymentMethod` | `BANK_TRANSFER` `CARD` |
| `OrderStatus` | `ACCEPTED` `BEING_PROCESSED` `DISPATCHED` `DELIVERED` |
| `PaymentStatus` | `PENDING` `PARTIAL` `PAID` |

---

## Pending

- Spring Security (JWT authentication, role-based endpoint access, BCrypt password hashing)
