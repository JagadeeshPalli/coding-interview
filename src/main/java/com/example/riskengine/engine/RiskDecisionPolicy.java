package com.example.riskengine.engine;

import com.example.riskengine.domain.DecisionType;
import com.example.riskengine.domain.RiskConfig;

public final class RiskDecisionPolicy {
    public DecisionType decide(int score, RiskConfig config) {
        if (score >= config.declineScoreThreshold()) {
            return DecisionType.DECLINE;
        }
        if (score >= config.reviewScoreThreshold()) {
            return DecisionType.REVIEW;
        }
        return DecisionType.APPROVE;
    }
}
