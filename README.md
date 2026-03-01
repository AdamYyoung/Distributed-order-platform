# Distributed Order Platform

A distributed microservice-based order system designed to handle high-concurrency scenarios such as flash sales.  

The system focuses on scalability, eventual consistency, and separation of business and infrastructure concerns.

---

## Architecture Overview

The system consists of the following modules:

- API Gateway
- Auth Service
- Order Service
- Product Service
- Inventory Service
- Common Library (Infrastructure Layer)

Tech Stack:

- Java 11
- Spring Boot / Spring Cloud (Eureka)
- MySQL
- Redis
- Kafka
- Docker
- Kubernetes

---

# API Gateway

### Role
Single entry point of the distributed system.

### Responsibilities
- JWT validation and identity injection
- IP-based rate limiting (Redis + Token Bucket)
- Request routing
- TraceId generation

### Key Design Decisions

- Centralized authentication to keep downstream services stateless
- Token Bucket algorithm to allow burst traffic with controlled refill rate
- Shared Redis for distributed rate-limiting consistency

### Trade-offs
- Cleaner separation of concerns
- Small additional latency
- Requires high availability for gateway

---

# Auth Service

### Role
Security boundary and identity provider.

### Responsibilities
- User registration & login
- JWT issuance
- RBAC-based authorization (User ↔ Role many-to-many)
- Credential verification

### Design Highlights
- Stateless authentication using JWT
- Factory Method pattern to decouple instance creation
- Short TTL + optional blacklist strategy for token revocation

### Trade-offs
- JWT is scalable but hard to revoke
- Centralized auth simplifies logic but requires HA deployment

---

# Product Service

### Role
Manage product metadata.

### Strategy
- Cache-Aside pattern (read-heavy, write-light)
- @Transactional for DB consistency
- Redis caching via Spring Cache abstraction

### Cache Design
- Accept eventual consistency
- Short TTL to reduce stale window
- Distributed lock to prevent cache breakdown
- Preheating for hot products during flash sales

---

# Inventory Service

### Role
High-concurrency stock deduction with consistency guarantees.

### Core Strategy
- Write-Behind pattern for high throughput
- Redis Lua scripts for atomic stock deduction
- Decoupled cache deduction and DB persistence
- Saga-based compensation

### Reliability Mechanisms
- Startup warm-up (load MySQL stock into Redis)
- Periodic reconciliation job (scan recently updated records)
- Distributed lock to ensure idempotent compensation

### Trade-offs
- Higher throughput
- Eventual consistency
- Durability risk mitigated via Kafka + compensation

---

# Order Service

### Daily Orders
- Asynchronous product queries (CompletableFuture)
- TransactionalEventListener to publish events post-commit
- Resilience4j circuit breaker + fallback

### Flash Sale (Seckill) Workflow

Designed using Saga pattern:

1. Redis pre-deduction
2. Order placeholder creation
3. Kafka decoupled DB persistence
4. Compensation logic on failure

### Consistency Mechanisms
- Distributed lock (one user one order)
- Idempotency checks
- Main-thread rollback
- Async Kafka compensation
- Frontend polling via orderToken

### Read/Write Separation
- Redis → high-speed read & deduction
- MySQL → final durable write

---

# Common Library (Infrastructure Layer)

### Purpose
Handle cross-cutting concerns.

### Includes
- JwtUtils
- Snowflake ID generator
- Standardized ApiResponse wrapper
- Global exception handling
- Redis serialization optimization
- Random TTL to prevent cache avalanche
- Context propagation (ThreadLocal + MDC)
- Custom conditional annotations

### Design Principle
Keep business logic out. Infrastructure only.

---

# 🚀 High-Concurrency Design Focus

- Redis Lua atomic operations
- Distributed locks (Redisson)
- Token bucket rate limiting
- Cache preheating
- Write-behind strategy
- Saga-based eventual consistency
- Partition strategy planning for Kafka (same SKU → same partition)

---

# Known Trade-offs & Future Improvements

- Kafka strict ordering requires careful partition strategy
- Asynchronous convergence logic can be further enhanced
- Flash-sale traffic may require Redis Cluster scaling
- Vertical separation of hot SKU tables if traffic grows 10x

---

# Design Philosophy

- Separate business logic from infrastructure concerns
- Prefer scalability over strict consistency when acceptable
- Use compensation over distributed locking when possible
- Design for failure and recovery

---
