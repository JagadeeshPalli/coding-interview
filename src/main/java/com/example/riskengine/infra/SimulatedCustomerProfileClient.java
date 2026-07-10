package com.example.riskengine.infra;

import com.example.riskengine.domain.CustomerProfile;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public final class SimulatedCustomerProfileClient implements CustomerProfileClient {
    private final Executor executor;
    private final ScheduledExecutorService scheduler;
    private final Duration latency;

    public SimulatedCustomerProfileClient(Executor executor, ScheduledExecutorService scheduler, Duration latency) {
        this.executor = executor;
        this.scheduler = scheduler;
        this.latency = latency;
    }

    @Override
    public CompletableFuture<CustomerProfile> fetch(String customerId) {
        CompletableFuture<CustomerProfile> profile = CompletableFuture.supplyAsync(() -> {
            sleep(latency);
            boolean highRisk = customerId.endsWith("9");
            return new CustomerProfile(customerId, "US", Set.of("merchant-trusted"), highRisk);
        }, executor);

        return TimeoutSupport.withTimeout(profile, Duration.ofMillis(500), scheduler);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching customer profile", e);
        }
    }
}
