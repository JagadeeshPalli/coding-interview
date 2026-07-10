package com.example.riskengine.engine;

import com.example.riskengine.concurrent.IdempotencyStore;
import com.example.riskengine.concurrent.RiskMetrics;
import com.example.riskengine.concurrent.VelocityStore;
import com.example.riskengine.domain.CustomerProfile;
import com.example.riskengine.domain.FraudSignal;
import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskDecision;
import com.example.riskengine.domain.RiskResult;
import com.example.riskengine.domain.Transaction;
import com.example.riskengine.infra.CustomerProfileClient;
import com.example.riskengine.infra.FraudSignalClient;
import com.example.riskengine.rules.RiskRule;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class AsyncRiskEngine implements RiskEngine {
    private final List<RiskRule> rules;
    private final CustomerProfileClient customerProfileClient;
    private final FraudSignalClient fraudSignalClient;
    private final VelocityStore velocityStore;
    private final IdempotencyStore idempotencyStore;
    private final RiskMetrics metrics;
    private final RiskConfigProvider configProvider;
    private final RiskDecisionPolicy decisionPolicy;
    private final Executor ruleExecutor;
    private final Clock clock;

    public AsyncRiskEngine(
            List<RiskRule> rules,
            CustomerProfileClient customerProfileClient,
            FraudSignalClient fraudSignalClient,
            VelocityStore velocityStore,
            IdempotencyStore idempotencyStore,
            RiskMetrics metrics,
            RiskConfigProvider configProvider,
            RiskDecisionPolicy decisionPolicy,
            Executor ruleExecutor,
            Clock clock
    ) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        this.customerProfileClient = Objects.requireNonNull(customerProfileClient, "customerProfileClient");
        this.fraudSignalClient = Objects.requireNonNull(fraudSignalClient, "fraudSignalClient");
        this.velocityStore = Objects.requireNonNull(velocityStore, "velocityStore");
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.configProvider = Objects.requireNonNull(configProvider, "configProvider");
        this.decisionPolicy = Objects.requireNonNull(decisionPolicy, "decisionPolicy");
        this.ruleExecutor = Objects.requireNonNull(ruleExecutor, "ruleExecutor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CompletableFuture<RiskDecision> evaluate(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");

        if (!idempotencyStore.markInProgress(transaction.id())) {
            return CompletableFuture.completedFuture(RiskDecision.duplicate(transaction.id()));
        }

        VelocityStore.Snapshot velocity = velocityStore.record(transaction);
        CompletableFuture<CustomerProfile> customer = customerProfileClient.fetch(transaction.customerId());
        CompletableFuture<FraudSignal> fraud = fraudSignalClient.check(transaction);

        return customer
                .thenCombine(fraud, (profile, signal) -> new RiskContext(
                        transaction,
                        profile,
                        signal,
                        velocity.spend(),
                        velocity.count()
                ))
                .thenCompose(this::runRules)
                .thenApply(results -> toDecision(transaction.id(), results, configProvider.current()))
                .whenComplete((decision, failure) -> {
                    if (failure == null) {
                        idempotencyStore.markCompleted(transaction.id());
                        metrics.recordDecision(decision.type());
                    } else {
                        metrics.recordFailure();
                    }
                });
    }

    private CompletableFuture<List<RiskResult>> runRules(RiskContext context) {
        RiskConfig config = configProvider.current();
        List<CompletableFuture<RiskResult>> futures = rules.stream()
                .map(rule -> CompletableFuture.supplyAsync(() -> rule.evaluate(context, config), ruleExecutor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    private RiskDecision toDecision(String transactionId, List<RiskResult> results, RiskConfig config) {
        int score = Math.max(0, results.stream().mapToInt(RiskResult::score).sum());
        return new RiskDecision(
                transactionId,
                decisionPolicy.decide(score, config),
                score,
                results,
                Instant.now(clock)
        );
    }
}
