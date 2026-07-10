package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public interface RiskRule {
    RiskResult evaluate(RiskContext context, RiskConfig config);
}
