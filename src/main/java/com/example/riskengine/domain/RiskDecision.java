package com.example.riskengine.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RiskDecision(
        String transactionId,
        DecisionType type,
        int score,
        List<RiskResult> results,
        Instant decidedAt
) {
    public RiskDecision {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(type, "type");
        results = List.copyOf(Objects.requireNonNull(results, "results"));
        Objects.requireNonNull(decidedAt, "decidedAt");
    }

    public static RiskDecision duplicate(String transactionId) {
        return new RiskDecision(
                transactionId,
                DecisionType.REVIEW,
                0,
                List.of(new RiskResult("IDEMPOTENCY", 0, "Duplicate transaction ignored")),
                Instant.now()
        );
    }
}
