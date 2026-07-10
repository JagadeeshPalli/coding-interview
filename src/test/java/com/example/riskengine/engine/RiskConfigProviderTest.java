package com.example.riskengine.engine;

import com.example.riskengine.domain.RiskConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskConfigProviderTest {
    @Test
    void publishesConfigUpdatesAcrossThreads() throws Exception {
        RiskConfigProvider provider = new RiskConfigProvider(RiskConfig.defaults());
        RiskConfig updated = new RiskConfig(
                BigDecimal.valueOf(2_000),
                BigDecimal.valueOf(20_000),
                30,
                70,
                9_000,
                9,
                Set.of("AA")
        );
        CountDownLatch updatedLatch = new CountDownLatch(1);
        AtomicReference<RiskConfig> observed = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var reader = executor.submit(() -> {
                updatedLatch.await();
                observed.set(provider.current());
                return null;
            });

            provider.update(updated);
            updatedLatch.countDown();
            reader.get();
        }

        assertEquals(updated, observed.get());
    }
}
