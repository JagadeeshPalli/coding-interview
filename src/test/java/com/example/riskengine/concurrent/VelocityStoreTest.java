package com.example.riskengine.concurrent;

import com.example.riskengine.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityStoreTest {
    @Test
    void recordsConcurrentSpendWithoutLocks() {
        VelocityStore store = new VelocityStore(Duration.ofMinutes(10));
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<?> tasks = IntStream.range(0, 100)
                    .mapToObj(i -> executor.submit(() -> store.record(transaction("txn-" + i, now))))
                    .toList();

            tasks.forEach(task -> {
                try {
                    ((java.util.concurrent.Future<?>) task).get();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }

        VelocityStore.Snapshot snapshot = store.record(transaction("txn-final", now));
        assertEquals(101, snapshot.count());
        assertEquals(1_010, snapshot.spend());
    }

    private static Transaction transaction(String id, Instant timestamp) {
        return new Transaction(
                id,
                "acct-velocity",
                "cust-1",
                "merchant-normal",
                BigDecimal.TEN,
                "USD",
                "US",
                timestamp
        );
    }
}
