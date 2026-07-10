package com.example.riskengine.domain;

import java.util.Objects;

public record RiskResult(String ruleName, int score, String reason) {
    public RiskResult {
        Objects.requireNonNull(ruleName, "ruleName");
        Objects.requireNonNull(reason, "reason");
    }
}
