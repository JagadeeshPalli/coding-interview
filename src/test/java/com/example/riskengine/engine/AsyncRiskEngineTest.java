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
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static Transaction transaction(String id, BigDecimal amount, String merchantId, String country) {
        return new Transaction(
                id,
                "acct-1",
                "cust-1",
                merchantId,
                amount,
                "USD",
                country,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
