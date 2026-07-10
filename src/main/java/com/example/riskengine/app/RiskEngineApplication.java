package com.example.riskengine.app;

import com.example.riskengine.concurrent.RiskMetrics;
import com.example.riskengine.domain.DecisionType;
import com.example.riskengine.domain.RiskDecision;
import com.example.riskengine.domain.Transaction;
import com.example.riskengine.engine.RiskEngine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public final class RiskEngineApplication {
    private RiskEngineApplication() {
    }

    public static void main(String[] args) {
        RiskMetrics metrics = new RiskMetrics();

        try (var virtualThreads = Executors.newVirtualThreadPerTaskExecutor()) {
            ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.setRemoveOnCancelPolicy(true);
            try {
                RiskEngine riskEngine = RiskEngineFactory.create(virtualThreads, scheduler, metrics);

                List<CompletableFuture<RiskDecision>> decisions = sampleTransactions().stream()
                        .map(riskEngine::evaluate)
                        .toList();

                CompletableFuture.allOf(decisions.toArray(CompletableFuture[]::new)).join();

                decisions.stream()
                        .map(CompletableFuture::join)
                        .forEach(decision -> System.out.printf(
                                "%s -> %s score=%d%n",
                                decision.transactionId(),
                                decision.type(),
                                decision.score()
                        ));
            } finally {
                scheduler.shutdownNow();
            }
        }

        System.out.printf(
                "processed=%d approved=%d review=%d declined=%d failed=%d%n",
                metrics.processed(),
                metrics.decisions(DecisionType.APPROVE),
                metrics.decisions(DecisionType.REVIEW),
                metrics.decisions(DecisionType.DECLINE),
                metrics.failed()
        );
    }

    private static List<Transaction> sampleTransactions() {
        Instant now = Instant.now();
        return List.of(
                new Transaction("txn-1", "acct-1", "cust-1", "merchant-trusted", BigDecimal.valueOf(120), "USD", "US", now),
                new Transaction("txn-2", "acct-1", "cust-1", "merchant-normal", BigDecimal.valueOf(1_500), "USD", "US", now),
                new Transaction("txn-3", "acct-2", "cust-9", "merchant-watchlist", BigDecimal.valueOf(250), "USD", "GB", now),
                new Transaction("txn-4", "acct-3", "cust-2", "merchant-normal", BigDecimal.valueOf(15_000), "USD", "US", now),
                new Transaction("txn-5", "acct-4", "cust-3", "merchant-normal", BigDecimal.valueOf(50), "USD", "IR", now)
        );
    }
}
