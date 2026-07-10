package com.example.riskengine.concurrent;

import com.example.riskengine.domain.DecisionType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class RiskMetrics {
    private final LongAdder processed = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final Map<DecisionType, LongAdder> decisions = new EnumMap<>(DecisionType.class);

    public RiskMetrics() {
        for (DecisionType type : DecisionType.values()) {
            decisions.put(type, new LongAdder());
        }
    }

    public void recordDecision(DecisionType type) {
        processed.increment();
        decisions.get(type).increment();
    }

    public void recordFailure() {
        failed.increment();
    }

    public long processed() {
        return processed.sum();
    }

    public long failed() {
        return failed.sum();
    }

    public long decisions(DecisionType type) {
        return decisions.get(type).sum();
    }
}
