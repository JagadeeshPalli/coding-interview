package com.example.riskengine.engine;

import com.example.riskengine.domain.RiskConfig;

import java.util.Objects;

public final class RiskConfigProvider {
    private volatile RiskConfig current;

    public RiskConfigProvider(RiskConfig initialConfig) {
        this.current = Objects.requireNonNull(initialConfig, "initialConfig");
    }

    public RiskConfig current() {
        return current;
    }

    public void update(RiskConfig config) {
        this.current = Objects.requireNonNull(config, "config");
    }
}
