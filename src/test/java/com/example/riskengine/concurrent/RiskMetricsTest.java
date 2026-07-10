package com.example.riskengine.concurrent;

import com.example.riskengine.domain.DecisionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskMetricsTest {
    @Test
    void recordsConcurrentDecisionCountersSafely() throws Exception {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<? extends Future<?>> tasks = IntStream.range(0, 1_000)
                    .mapToObj(i -> executor.submit(() -> metrics.recordDecision(DecisionType.APPROVE)))
                    .toList();

            for (Future<?> task : tasks) {
                task.get();
            }
        }

        assertEquals(1_000, metrics.processed());
        assertEquals(1_000, metrics.decisions(DecisionType.APPROVE));
        assertEquals(0, metrics.failed());
    }
}
