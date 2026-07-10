package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public final class FraudSignalRule implements RiskRule {
    @Override
    public RiskResult evaluate(RiskContext context, RiskConfig config) {
        if (context.fraudSignal().listedMerchant()) {
            return new RiskResult("FRAUD_PROVIDER", 80, "Merchant is listed by fraud provider");
        }
        int score = Math.min(context.fraudSignal().providerScore(), 60);
        return new RiskResult("FRAUD_PROVIDER", score, context.fraudSignal().reason());
    }
}
