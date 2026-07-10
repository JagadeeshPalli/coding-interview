package com.example.riskengine.engine;

import com.example.riskengine.domain.RiskDecision;
import com.example.riskengine.domain.Transaction;

import java.util.concurrent.CompletableFuture;

public interface RiskEngine {
    CompletableFuture<RiskDecision> evaluate(Transaction transaction);
}
