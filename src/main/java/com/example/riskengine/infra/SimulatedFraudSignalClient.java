package com.example.riskengine.infra;

import com.example.riskengine.domain.FraudSignal;
import com.example.riskengine.domain.Transaction;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SimulatedFraudSignalClient implements FraudSignalClient {
    private static final Set<String> LISTED_MERCHANTS = Set.of("merchant-blocked", "merchant-watchlist");

    private final Executor executor;
    private final ScheduledExecutorService scheduler;
    private final Duration latency;

    public SimulatedFraudSignalClient(Executor executor, ScheduledExecutorService scheduler, Duration latency) {
        this.executor = executor;
        this.scheduler = scheduler;
        this.latency = latency;
    }

    @Override
    public CompletableFuture<FraudSignal> check(Transaction transaction) {
        CompletableFuture<FraudSignal> signal = CompletableFuture.supplyAsync(() -> {
            sleep(latency);
            boolean listed = LISTED_MERCHANTS.contains(transaction.merchantId());
            int score = listed ? 90 : Math.abs(transaction.id().hashCode() % 25);
            return new FraudSignal(score, listed, listed ? "Merchant is on provider watchlist" : "Provider risk is low");
        }, executor);

        return TimeoutSupport.withTimeout(signal, Duration.ofMillis(500), scheduler);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while checking fraud signal", e);
        }
    }
}
