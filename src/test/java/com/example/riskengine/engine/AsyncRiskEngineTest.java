package com.example.riskengine.engine;

import com.example.riskengine.concurrent.IdempotencyStore;
import com.example.riskengine.concurrent.RiskMetrics;
import com.example.riskengine.concurrent.VelocityStore;
import com.example.riskengine.domain.CustomerProfile;
import com.example.riskengine.domain.DecisionType;
import com.example.riskengine.domain.FraudSignal;
import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskDecision;
import com.example.riskengine.domain.Transaction;
import com.example.riskengine.rules.AmountRiskRule;
import com.example.riskengine.rules.CountryRiskRule;
import com.example.riskengine.rules.FraudSignalRule;
import com.example.riskengine.rules.MerchantTrustRule;
import com.example.riskengine.rules.RiskRule;
import com.example.riskengine.rules.VelocityRiskRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncRiskEngineTest {
    @Test
    void approvesLowRiskTransaction() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);

            RiskDecision decision = engine.evaluate(transaction("txn-low", BigDecimal.valueOf(100), "merchant-trusted", "US"))
                    .join();

            assertEquals(DecisionType.APPROVE, decision.type());
            assertEquals(1, metrics.processed());
            assertEquals(1, metrics.decisions(DecisionType.APPROVE));
        }
    }

    @Test
    void declinesSanctionedCountryTransaction() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);

            RiskDecision decision = engine.evaluate(transaction("txn-country", BigDecimal.valueOf(100), "merchant-normal", "IR"))
                    .join();

            assertEquals(DecisionType.DECLINE, decision.type());
            assertEquals(1, metrics.decisions(DecisionType.DECLINE));
        }
    }

    @Test
    void duplicateTransactionIsReviewedAndNotProcessedTwice() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);
            Transaction transaction = transaction("txn-dupe", BigDecimal.valueOf(100), "merchant-normal", "US");

            RiskDecision first = engine.evaluate(transaction).join();
            RiskDecision second = engine.evaluate(transaction).join();

            assertEquals(DecisionType.APPROVE, first.type());
            assertEquals(DecisionType.REVIEW, second.type());
            assertEquals(1, metrics.processed());
        }
    }

    @Test
    void processesManyTransactionsConcurrentlyOnVirtualThreads() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);

            List<CompletableFuture<RiskDecision>> decisions = IntStream.range(0, 250)
                    .mapToObj(i -> engine.evaluate(transaction(
                            "txn-concurrent-" + i,
                            "acct-concurrent-" + i,
                            BigDecimal.valueOf(25 + i),
                            i % 7 == 0 ? "merchant-trusted" : "merchant-normal",
                            "US"
                    )))
                    .toList();

            CompletableFuture.allOf(decisions.toArray(CompletableFuture[]::new)).join();

            long approved = decisions.stream()
                    .map(CompletableFuture::join)
                    .filter(decision -> decision.type() == DecisionType.APPROVE)
                    .count();

            assertEquals(250, approved);
            assertEquals(250, metrics.processed());
            assertEquals(250, metrics.decisions(DecisionType.APPROVE));
        }
    }

    @Test
    void onlyOneConcurrentDuplicateIsFullyEvaluated() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);
            Transaction transaction = transaction("txn-race", BigDecimal.valueOf(100), "merchant-normal", "US");

            List<CompletableFuture<RiskDecision>> decisions = IntStream.range(0, 50)
                    .mapToObj(ignored -> engine.evaluate(transaction))
                    .toList();

            CompletableFuture.allOf(decisions.toArray(CompletableFuture[]::new)).join();

            long approved = decisions.stream()
                    .map(CompletableFuture::join)
                    .filter(decision -> decision.type() == DecisionType.APPROVE)
                    .count();
            long duplicateReviews = decisions.stream()
                    .map(CompletableFuture::join)
                    .filter(decision -> decision.type() == DecisionType.REVIEW)
                    .count();

            assertEquals(1, approved);
            assertEquals(49, duplicateReviews);
            assertEquals(1, metrics.processed());
        }
    }

    @Test
    void combinedScoreAtReviewThresholdIsReviewed() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);

            RiskDecision decision = engine.evaluate(transaction(
                    "txn-review-threshold",
                    BigDecimal.valueOf(1_000),
                    "merchant-normal",
                    "GB"
            )).join();

            assertEquals(DecisionType.REVIEW, decision.type());
            assertEquals(50, decision.score());
        }
    }

    @Test
    void amountAtDeclineThresholdIsDeclined() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engine(metrics, executor);

            RiskDecision decision = engine.evaluate(transaction(
                    "txn-decline-threshold",
                    BigDecimal.valueOf(10_000),
                    "merchant-normal",
                    "US"
            )).join();

            assertEquals(DecisionType.DECLINE, decision.type());
            assertEquals(100, decision.score());
        }
    }

    @Test
    void recordsFailureMetricWhenDependencyFails() {
        RiskMetrics metrics = new RiskMetrics();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AsyncRiskEngine engine = engineWithFailingCustomerClient(metrics, executor);

            CompletionException failure = assertThrows(
                    CompletionException.class,
                    () -> engine.evaluate(transaction("txn-failure", BigDecimal.valueOf(100), "merchant-normal", "US")).join()
            );

            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(0, metrics.processed());
            assertEquals(1, metrics.failed());
        }
    }

    private static AsyncRiskEngine engine(RiskMetrics metrics, java.util.concurrent.ExecutorService executor) {
        List<RiskRule> rules = List.of(
                new AmountRiskRule(),
                new CountryRiskRule(),
                new VelocityRiskRule(),
                new MerchantTrustRule(),
                new FraudSignalRule()
        );

        return new AsyncRiskEngine(
                rules,
                ignored -> CompletableFuture.completedFuture(new CustomerProfile("cust-1", "US", Set.of("merchant-trusted"), false)),
                ignored -> CompletableFuture.completedFuture(new FraudSignal(5, false, "low provider risk")),
                new VelocityStore(Duration.ofMinutes(10)),
                new IdempotencyStore(),
                metrics,
                new RiskConfigProvider(RiskConfig.defaults()),
                new RiskDecisionPolicy(),
                executor,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static AsyncRiskEngine engineWithFailingCustomerClient(
            RiskMetrics metrics,
            java.util.concurrent.ExecutorService executor
    ) {
        List<RiskRule> rules = List.of(
                new AmountRiskRule(),
                new CountryRiskRule(),
                new VelocityRiskRule(),
                new MerchantTrustRule(),
                new FraudSignalRule()
        );

        return new AsyncRiskEngine(
                rules,
                ignored -> CompletableFuture.failedFuture(new IllegalStateException("customer service unavailable")),
                ignored -> CompletableFuture.completedFuture(new FraudSignal(5, false, "low provider risk")),
                new VelocityStore(Duration.ofMinutes(10)),
                new IdempotencyStore(),
                metrics,
                new RiskConfigProvider(RiskConfig.defaults()),
                new RiskDecisionPolicy(),
                executor,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static Transaction transaction(String id, BigDecimal amount, String merchantId, String country) {
        return transaction(id, "acct-1", amount, merchantId, country);
    }

    private static Transaction transaction(
            String id,
            String accountId,
            BigDecimal amount,
            String merchantId,
            String country
    ) {
        return new Transaction(
                id,
                accountId,
                "cust-1",
                merchantId,
                amount,
                "USD",
                country,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
