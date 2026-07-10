# Realtime Risk Engine

This project is a Java 21 Maven implementation of a real-time financial transaction risk engine. It is designed as an interview-ready coding exercise that demonstrates modern Java concurrency, thread-safe programming, asynchronous task composition, and a modular rule-based architecture.

The engine receives financial transactions, enriches them with simulated customer and fraud-provider data, evaluates multiple risk rules, calculates a score, and returns a final decision:

```text
APPROVE
REVIEW
DECLINE
```

## What This Project Demonstrates

- **Java 21**
  - Uses Java 21 source/target configuration through Maven.
  - Uses records for immutable domain objects such as `Transaction`, `RiskContext`, `RiskResult`, and `RiskDecision`.

- **Virtual Threads / Project Loom**
  - Uses `Executors.newVirtualThreadPerTaskExecutor()` for high-concurrency transaction processing.
  - Demonstrates request/task-style concurrency without manually managing large platform-thread pools.

- **CompletableFuture Asynchronous Flow**
  - Uses `CompletableFuture` to compose customer profile lookup and fraud signal lookup.
  - Uses `thenCombine`, `thenCompose`, `allOf`, and async rule execution to avoid blocking the main orchestration flow.

- **Java Memory Model and Thread Safety**
  - Immutable records safely carry transaction and risk data across threads.
  - `final` fields safely publish engine dependencies.
  - `volatile` is used in `RiskConfigProvider` so config updates are visible across threads.
  - Shared mutable state is isolated inside dedicated concurrent components.

- **Lock-Free / Low-Lock Programming**
  - `LongAdder` is used for high-throughput metrics.
  - `AtomicReference` with compare-and-set is used for idempotency state.
  - `AtomicLong`, `ConcurrentHashMap`, and `ConcurrentLinkedQueue` are used for in-memory velocity tracking.

- **Production-Oriented Modularity**
  - Domain model, rules, engine orchestration, infrastructure clients, and concurrency utilities are separated into packages.
  - Rules are independently testable.
  - External services are represented behind interfaces.
  - Timeout support is included for simulated external calls.

## Architecture

```text
Transaction
    |
    v
AsyncRiskEngine
    |
    +--> CustomerProfileClient  -- async customer enrichment
    |
    +--> FraudSignalClient      -- async fraud-provider signal
    |
    +--> VelocityStore          -- lock-free in-memory velocity state
    |
    v
RiskContext
    |
    v
RiskRule implementations
    |
    v
RiskDecisionPolicy
    |
    v
RiskDecision
```

## Package Layout

```text
com.example.riskengine.app          Demo application and wiring
com.example.riskengine.domain       Immutable domain model
com.example.riskengine.engine       Risk engine orchestration and decision policy
com.example.riskengine.rules        Risk rule interface and rule implementations
com.example.riskengine.infra        Simulated async external service clients
com.example.riskengine.concurrent   Lock-free stores, idempotency, and metrics
```

## Implemented Risk Rules

- `AmountRiskRule`
  - Adds risk for large transaction amounts.

- `CountryRiskRule`
  - Declines sanctioned countries.
  - Adds risk when transaction country differs from customer home country.

- `VelocityRiskRule`
  - Adds risk when an account has high spend or high transaction count in a rolling window.

- `MerchantTrustRule`
  - Reduces risk for trusted merchants.
  - Adds risk for high-risk customer profiles.

- `FraudSignalRule`
  - Uses simulated fraud-provider signals and watchlist results.

## How To Run

From the project root:

```powershell
cd E:\coding-interview
mvn test
mvn exec:java
```

Important: the Maven command uses a colon:

```powershell
mvn exec:java
```

Do not use:

```powershell
mvn exec.java
```

## Expected Demo Output

The exact Maven timestamps may differ, but the application should produce decisions similar to:

```text
txn-1 -> APPROVE score=0
txn-2 -> APPROVE score=36
txn-3 -> DECLINE score=115
txn-4 -> DECLINE score=108
txn-5 -> DECLINE score=114
processed=5 approved=2 review=0 declined=3 failed=0
BUILD SUCCESS
```

## Test Coverage

The project includes JUnit 5 tests for:

- low-risk approval
- sanctioned-country decline
- duplicate/idempotency handling
- concurrent transaction processing with virtual threads
- concurrent duplicate transaction race handling
- review and decline scoring edge cases
- external dependency failure metrics
- invalid transactions
- concurrent metrics updates
- volatile config visibility across threads
- concurrent velocity-store updates

Run all tests:

```powershell
mvn test
```

Current expected result:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Important Classes

- `Transaction`
  - Immutable input transaction record.

- `AsyncRiskEngine`
  - Main orchestration layer.
  - Composes async enrichment calls and risk rule execution.

- `RiskRule`
  - Rule contract for adding new risk checks.

- `RiskDecisionPolicy`
  - Converts final score into `APPROVE`, `REVIEW`, or `DECLINE`.

- `VelocityStore`
  - In-memory rolling account velocity tracker using concurrent data structures.

- `IdempotencyStore`
  - Uses CAS through `AtomicReference` to avoid processing duplicate transactions.

- `RiskMetrics`
  - Uses `LongAdder` for concurrent counters.

- `RiskConfigProvider`
  - Uses `volatile` for safely publishing runtime config changes.

## Design Notes For Interview Discussion

This implementation is intentionally in-memory and dependency-light so the concurrency and design choices are easy to inspect. In a production system, the simulated infrastructure clients would be replaced by real HTTP/database/cache integrations.

Good points to discuss:

- Virtual threads are useful for request-per-task and blocking I/O style workloads.
- `CompletableFuture` is useful for composing independent async operations.
- Immutable records reduce shared mutable state and simplify thread safety.
- Lock-free structures are used where they make sense, not everywhere.
- Idempotency and velocity tracking are isolated from the core engine logic.
- Rules are modular and can be added without changing the engine orchestration.

## What More Can Be Done

For a real production-grade financial risk engine, the next improvements would be:

- Replace in-memory idempotency with distributed idempotency using Redis, DynamoDB, PostgreSQL, or another durable store.
- Replace in-memory velocity tracking with Redis sorted sets, Kafka Streams, Flink, or another distributed stream-processing approach.
- Add backpressure and bulkheads so one slow dependency cannot create unlimited virtual-thread work.
- Add circuit breakers, retries with jitter, and fallback decisions for external service failures.
- Return deterministic `REVIEW` or `DECLINE` decisions for dependency failures instead of completing exceptionally.
- Add structured logging with transaction ID, account ID, decision, score, latency, and rule-level reasons.
- Add metrics using Micrometer/OpenTelemetry.
- Add tracing across customer lookup, fraud lookup, rule execution, and decision publishing.
- Use a proper `Money` value object with minor units instead of directly converting `BigDecimal` to `long` in velocity tracking.
- Add configuration from application properties or a dynamic config service.
- Add REST/Kafka adapters for real-time ingestion.
- Add persistence for final decisions and audit history.
- Add load tests and latency percentiles.
- Add mutation tests and property-based tests for scoring rules.

## Summary

This project demonstrates the core of a real-time risk engine using Java 21, virtual threads, `CompletableFuture`, thread-safe programming, and lock-free concurrency primitives. It is modular, testable, and intentionally structured so an interviewer can clearly see where each requirement is implemented.
