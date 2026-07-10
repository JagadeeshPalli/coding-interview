package com.example.riskengine.app;

import com.example.riskengine.concurrent.IdempotencyStore;
import com.example.riskengine.concurrent.RiskMetrics;
import com.example.riskengine.concurrent.VelocityStore;
import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.engine.AsyncRiskEngine;
import com.example.riskengine.engine.RiskConfigProvider;
import com.example.riskengine.engine.RiskDecisionPolicy;
import com.example.riskengine.infra.CustomerProfileClient;
import com.example.riskengine.infra.FraudSignalClient;
import com.example.riskengine.infra.SimulatedCustomerProfileClient;
import com.example.riskengine.infra.SimulatedFraudSignalClient;
import com.example.riskengine.rules.AmountRiskRule;
import com.example.riskengine.rules.CountryRiskRule;
import com.example.riskengine.rules.FraudSignalRule;
import com.example.riskengine.rules.MerchantTrustRule;
import com.example.riskengine.rules.RiskRule;
import com.example.riskengine.rules.VelocityRiskRule;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public final class RiskEngineFactory {
    private RiskEngineFactory() {
    }

    public static AsyncRiskEngine create(
            ExecutorService virtualThreadExecutor,
            ScheduledExecutorService scheduler,
            RiskMetrics metrics
    ) {
        CustomerProfileClient customerClient = new SimulatedCustomerProfileClient(
                virtualThreadExecutor,
                scheduler,
                Duration.ofMillis(25)
        );
        FraudSignalClient fraudClient = new SimulatedFraudSignalClient(
                virtualThreadExecutor,
                scheduler,
                Duration.ofMillis(35)
        );
        List<RiskRule> rules = List.of(
                new AmountRiskRule(),
                new CountryRiskRule(),
                new VelocityRiskRule(),
                new MerchantTrustRule(),
                new FraudSignalRule()
        );

        return new AsyncRiskEngine(
                rules,
                customerClient,
                fraudClient,
                new VelocityStore(Duration.ofMinutes(10)),
                new IdempotencyStore(),
                metrics,
                new RiskConfigProvider(RiskConfig.defaults()),
                new RiskDecisionPolicy(),
                virtualThreadExecutor,
                Clock.systemUTC()
        );
    }
}
