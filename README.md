# Realtime Risk Engine

A Java 21 Maven project that demonstrates a modular real-time financial transaction risk engine.

## Highlights

- Java 21 records for immutable, safely published domain objects.
- Virtual threads with `Executors.newVirtualThreadPerTaskExecutor()`.
- `CompletableFuture` pipelines for asynchronous composition.
- Thread-safe programming using immutable data, `volatile` config snapshots, `ConcurrentHashMap`, `AtomicReference`, `AtomicLong`, and `LongAdder`.
- Lock-free state where appropriate for metrics, velocity tracking, and idempotency.
- Focused unit tests for scoring, idempotency, and concurrent processing.

## Run

```bash
mvn test
mvn exec:java
```

## Package Layout

```text
com.example.riskengine.app          Demo application
com.example.riskengine.domain       Immutable domain model
com.example.riskengine.engine       Risk engine orchestration
com.example.riskengine.rules        Risk rule implementations
com.example.riskengine.infra        Async external service adapters
com.example.riskengine.concurrent   Lock-free stores and metrics
```
