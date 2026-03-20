# Access Risk Monitoring Platform

An enterprise Identity Access Governance (IAG) backend modelled on real-world GRC concepts — Segregation of Duties (SoD) enforcement, risk rule management, audit logging, and violation lifecycle tracking.

Built with Java 21, Spring Boot 3.2, and PostgreSQL.

---

## Domain context

In enterprise IAG systems (SAP GRC, SAP IAG), a core control objective is **Segregation of Duties**: no single user should hold permissions that together allow them to initiate and approve the same business transaction. This system implements that control:

- **Permissions** map to business actions (e.g. `CREATE_PURCHASE_ORDER`, `APPROVE_PURCHASE_ORDER`)
- **Roles** bundle permissions and are assigned to users
- **Risk rules** define which permission pairs create an SoD conflict and at what severity (CRITICAL / HIGH / MEDIUM / LOW)
- **Risk analysis** scans users' effective permissions (derived through their roles) and raises **violations** when conflicts are detected
- Every state change is written to an immutable **audit log**

---

## Architecture

```
┌────────────────────────────────────────────┐
│               REST API (8 controllers)      │
│  Spring MVC · Bean Validation · Springdoc  │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│             Service Layer (8 services)      │
│  Business logic · Risk engine · Audit log  │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│         Repository Layer (8 repos)          │
│         Spring Data JPA · JPQL queries     │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│              PostgreSQL                     │
└────────────────────────────────────────────┘
```

**Package layout:**

| Package | Contents |
|---|---|
| `entity` | User, Role, Permission, UserRole, RolePermission, RiskRule, RiskViolation, AuditLog |
| `dto` | Request/response DTOs with Bean Validation; static `from()` factory methods |
| `service` | One service per aggregate; `RiskAnalysisService` is the core engine |
| `controller` | Thin REST controllers; no business logic |
| `exception` | `AccessRiskException` hierarchy + `GlobalExceptionHandler` |
| `config` | `DataSeeder` (seed data) + `StartupRunner` (initial risk scan) |
| `enums` | `Severity`, `ViolationStatus`, `AuditAction`, `ErrorCode` |

---

## Key design decisions

| Decision | Rationale |
|---|---|
| Join tables as explicit `@Entity` classes | `UserRole` and `RolePermission` store raw FKs — no `@ManyToMany`. Allows future fields (validFrom, assignedBy) and removes cascade surprises |
| No bidirectional JPA mappings | Service layer owns all joins; prevents accidental lazy-load and N+1 issues |
| `GenerationType.IDENTITY` on all entities | Reliable with PostgreSQL BIGSERIAL; avoids Hibernate 6 sequence fragility |
| `AuditLogService` with `REQUIRES_NEW` propagation | Audit writes commit independently — survive a parent transaction rollback |
| `AuditAction` and `ErrorCode` enums | All audit events and error codes are compile-safe; no magic strings |
| `ErrorResponse` with `errorCode` field | Clients can switch on a machine-readable code, not parse message strings |
| Risk engine loads permissions once per run | Builds a permission name cache upfront — prevents N+1 inside the rule evaluation loop |
| SoD idempotency check | `findByUserIdAndRuleIdAndStatus(OPEN)` prevents duplicate violations on repeated scans |

---

## Running locally

**Prerequisites:** Docker and Docker Compose.

```bash
git clone https://github.com/<your-username>/access-risk-platform.git
cd access-risk-platform
docker-compose up --build
```

The app starts on port `8080`. On first boot, seed data is loaded automatically:
- 8 permissions, 4 roles, 4 users, 4 risk rules
- Initial risk scan runs and detects **6 SoD violations** across 2 risky users

**Swagger UI:** http://localhost:8080/swagger-ui.html
**OpenAPI JSON:** http://localhost:8080/api-docs

To stop and remove containers:
```bash
docker-compose down
```

To wipe the database volume and start fresh:
```bash
docker-compose down -v
```

---

## API overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users` | List all users |
| `POST` | `/api/users` | Create a user |
| `PUT` | `/api/users/{id}` | Update a user |
| `DELETE` | `/api/users/{id}` | Delete a user |
| `GET` | `/api/roles` | List all roles |
| `POST` | `/api/roles` | Create a role |
| `GET` | `/api/permissions` | List all permissions |
| `POST` | `/api/permissions` | Create a permission |
| `POST` | `/api/users/{userId}/roles/{roleId}` | Assign role to user |
| `POST` | `/api/roles/{roleId}/permissions/{permissionId}` | Assign permission to role |
| `GET` | `/api/risk-rules` | List SoD risk rules |
| `POST` | `/api/risk-rules` | Create a risk rule |
| `POST` | `/api/risk/analyze` | Run risk analysis (all users or `?userId=`) |
| `GET` | `/api/risk/violations` | List violations (filter by `?status=OPEN`) |
| `GET` | `/api/dashboard/summary` | System-wide risk summary |
| `GET` | `/api/audit-logs` | Paginated audit log |
| `GET` | `/api/audit-logs/{entityType}/{entityId}` | Audit history for a specific entity |

All error responses follow a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with id: 99",
  "path": "/api/users/99",
  "traceId": "a1b2c3d4e5f6g7h8",
  "timestamp": "2026-03-19T10:30:00"
}
```

---

## Seed data and expected state

After first boot, the system contains:

**Users and their risk profile:**

| User | Roles | Risk |
|---|---|---|
| Alice Johnson | AP_CLERK | Clean |
| Bob Williams | PROCUREMENT_OFFICER | Clean |
| Carol Martinez | AP_CLERK + FINANCE_MANAGER | **3 violations** (2 CRITICAL, 1 HIGH) |
| David Chen | FINANCE_MANAGER + PROCUREMENT_OFFICER | **3 violations** (1 CRITICAL, 2 HIGH) |

**Risk rules:**

| Rule | Permissions in conflict | Severity |
|---|---|---|
| Vendor-Payment SoD | CREATE_VENDOR + PROCESS_PAYMENT | CRITICAL |
| PO Self-Approval | CREATE_PURCHASE_ORDER + APPROVE_PURCHASE_ORDER | HIGH |
| Payment 4-eyes | APPROVE_PAYMENT + PROCESS_PAYMENT | CRITICAL |
| Vendor + Payment Auth | APPROVE_VENDOR + APPROVE_PAYMENT | HIGH |

---

## Tech stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 16 |
| Validation | Jakarta Bean Validation (JSR-380) |
| API docs | Springdoc OpenAPI 2.3 (Swagger UI) |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose |
