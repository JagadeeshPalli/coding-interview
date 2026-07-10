package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public final class AmountRiskRule implements RiskRule {
    @Override
    public RiskResult evaluate(RiskContext context, RiskConfig config) {
        int score = 0;
        String reason = "Amount is within normal range";

        if (context.transaction().amount().compareTo(config.declineAmountThreshold()) >= 0) {
            score = 70;
            reason = "Amount exceeds decline threshold";
        } else if (context.transaction().amount().compareTo(config.reviewAmountThreshold()) >= 0) {
            score = 25;
            reason = "Amount exceeds review threshold";
        }

        return new RiskResult("AMOUNT", score, reason);
    }
}
