package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public final class VelocityRiskRule implements RiskRule {
    @Override
    public RiskResult evaluate(RiskContext context, RiskConfig config) {
        boolean highSpend = context.accountSpendAfterTransaction() >= config.velocitySpendThreshold();
        boolean highCount = context.transactionsInCurrentWindow() >= config.velocityTransactionThreshold();

        if (highSpend && highCount) {
            return new RiskResult("VELOCITY", 45, "High spend and transaction count in velocity window");
        }
        if (highSpend || highCount) {
            return new RiskResult("VELOCITY", 25, "Elevated velocity detected");
        }
        return new RiskResult("VELOCITY", 0, "Velocity is normal");
    }
}
