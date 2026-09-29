# AgriShield Portal: Enterprise Architecture & Production Implementation

AgriShield is an enterprise food chain safety platform enforcing Maximum Residue Limits (MRL) and Antimicrobial Usage (AMU) compliance. It combines high-throughput relational processing with immutable audit logging on Hyperledger Fabric 2.5.

---

## 1. System Directory Structure

```
d:\farm\
├── docker-compose.yml              # Complete Multi-Container Topology
├── .env.example                    # Environment variable configuration template
├── README.md                       # Operations & Runbook Documentation
├── index.html                      # Standalone All-In-One Client Application
├── database/
│   └── init/
│       └── 01-init.sql             # PostgreSQL 15 DDL & Seed Schema
├── backend/                        # Java 17 / Spring Boot 3.1.5 Microservice
│   ├── Dockerfile                  # Multi-stage Container Build
│   ├── pom.xml                     # Maven dependencies (JPA, Security, Fabric SDK)
│   └── src/
│       ├── main/
│       │   ├── java/com/agrishield/
│       │   │   ├── AgriShieldApplication.java
│       │   │   ├── config/         # SecurityConfig, JwtTokenProvider, FabricGatewayConfig
│       │   │   ├── controller/     # Auth, Animal, DataCleaner, Shipment controllers
│       │   │   ├── dto/            # Data Transfer Objects
│       │   │   ├── entity/         # JPA Entities (User, Animal, Treatment, VetShift, etc.)
│       │   │   ├── exception/      # GlobalExceptionHandler, PermitDeniedException (400)
│       │   │   ├── repository/     # Spring Data JPA Repositories
│       │   │   └── service/        # AuthService, AnimalMrlService, DataCleanerService, etc.
│       │   └── resources/
│       │       └── application.yml
│       └── test/                   # JUnit 5 & Mockito Integration Tests
├── chaincode/                      # Hyperledger Fabric 2.5 Smart Contract (Chaincode)
│   ├── pom.xml
│   └── src/main/java/org/hyperledger/fabric/chaincode/
│       ├── MrlAmuContract.java     # recordTreatment, verifyShipmentPermit, queryTreatmentHistory
│       └── model/                  # TreatmentRecord, ShipmentPermitRecord
├── frontend/                       # React 18 SPA + Tailwind CSS + Lucide Icons
│   ├── Dockerfile                  # Nginx production build container
│   ├── package.json
│   ├── tailwind.config.js
│   └── src/
│       ├── api/client.js           # Axios with JWT Bearer Interceptors
│       ├── context/AuthContext.js  # JWT Auth State Management
│       ├── components/             # Navbar, Sidebar, ProtectedRoute
│       └── pages/                  # ScannerView, DataCleanerConsole, GatekeeperView, Explorer
├── scripts/
│   ├── init-fabric.sh              # Channel genesis & peer enrollment
│   └── deploy-chaincode.sh         # Package, install, approve, and commit lifecycle
└── tests/
    └── performance/
        ├── k6-rfid-scan.js         # 1,000 VU load test asserting p99 < 20ms
        └── k6-fabric-commit.js     # Fabric commit latency asserting < 2000ms
```

---

## 2. Infrastructure Setup & Docker Compose

### Prerequisites
* Docker 24.0+ and Docker Compose v2.20+
* 8 GB+ RAM allocated to Docker

### Bring Up the Complete Environment
```bash
# 1. Copy environment template
cp .env.example .env

# 2. Build and launch all containers in detached mode
docker compose up --build -d

# 3. Verify container health status
docker compose ps
```

### Services & Port Allocation
* **React 18 Frontend:** `http://localhost:3000`
* **Spring Boot REST API:** `http://localhost:8080/api/v1`
* **PostgreSQL 15:** `localhost:5432` (Database: `agrishield_db`)
* **Peer0 Org1:** `localhost:7051`
* **Peer0 Org2:** `localhost:9051`
* **Raft Orderer:** `localhost:7050`
* **CouchDB State Store:** `http://localhost:5984` and `http://localhost:7984`

---

## 3. Pre-Seeded User Accounts & RBAC Matrix

| Role | Username | Password | License ID | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **FARMHAND** | `farmhand_jake` | `password123` | `FARM-EMP-91` | RFID scanner, livestock directory (Read-Only) |
| **VETERINARIAN** | `vet_sarah` | `password123` | `VET-USA-4402` | Prescriptions, two-pass data cleaner, X.509 signing |
| **MANAGER** | `manager_john` | `password123` | `MGR-HQ-01` | Full operations, shipment gatekeeper, user admin |
| **AUDITOR** | `auditor_elena` | `password123` | `GOV-AUDIT-55` | Read-only ledger explorer, cryptographic verification |

---

## 4. Deterministic Business Logic Conditions

### Condition Set 1: Authentication & RBAC
* Sessions issue an HMAC-SHA256 signed JWT with a strict 15-minute lifespan (`900,000 ms`).
* Route guards enforce that FARMHAND requests to write APIs return **HTTP 403 Forbidden**.

### Condition Set 2: Data Ingestion & Deduplication Pipeline (`/api/v1/data/clean`)
* **Pass 1:** Computes `Key = SHA256(Animal_ID + Drug_ID + Timestamp)`. If exists in memory cache or PostgreSQL, returns `[REJECTED_DUPLICATE]`.
* **Pass 2:** Cross-references `vet_shifts`. If $\text{Shift Start} \le \text{Timestamp} \le \text{Shift End}$ is violated, marks `[FLAGGED_VET_ANOMALY]`.
* Clean records are approved and committed to Hyperledger Fabric.

### Condition Set 3: Sub-10ms RFID Scan & Automatic MRL Status Decay
* $\text{Safe Release Time} = T_{\text{admin}} + (W_h \times 3600\text{ seconds})$.
* Dynamic evaluation: $\text{Status} = (T_{\text{current}} < \text{Safe Release Time}) \ ? \ \text{LOCKED} : \text{SAFE}$.

### Condition Set 4: Deterministic Shipment Permit Gatekeeper (`/api/v1/shipments/permit`)
$$\text{Permit Status} = \begin{cases} \text{DENIED (HTTP 400)}, & \text{if } \exists \, a \in \{A_1, \dots, A_N\} \text{ where Status}(a) = \text{LOCKED} \\ \text{APPROVED (HTTP 200)}, & \text{if } \forall \, a \in \{A_1, \dots, A_N\}, \text{ Status}(a) = \text{SAFE} \end{cases}$$
* Zero tolerance: A single locked animal immediately halts commercial transport permit issuance.

### Condition Set 5: AMU Threshold Analytics
* $\text{AMU Index} = \frac{\sum \text{Dosage (mg)}}{\sum \text{Biomass (kg)}}$.
* Normal ($\le 2.5$), Warning ($2.5 - 3.5$), Breach ($> 3.5$).

---

## 5. Automated Tests & Benchmarks

### Running JUnit 5 & Mockito Suite
```bash
cd backend
./mvnw clean test
```
Validates:
1. `DataCleanerServiceTest`: Off-duty vet shift rejection and composite key deduplication.
2. `ShipmentGatekeeperServiceTest`: Deterministic denial on locked animals and approval on all-safe batches.
3. `ShipmentControllerIntegrationTest`: HTTP 400 status assertion with offending locked tags in the response payload.

### Running k6 Performance Benchmarks
```bash
# 1. Benchmark 1,000 concurrent RFID scan queries (asserting p99 < 20ms)
k6 run tests/performance/k6-rfid-scan.js

# 2. Benchmark Hyperledger Fabric transaction commit throughput (asserting latency < 2s)
k6 run tests/performance/k6-fabric-commit.js
```
