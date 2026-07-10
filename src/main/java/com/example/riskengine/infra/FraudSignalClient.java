package com.example.riskengine.infra;

import com.example.riskengine.domain.FraudSignal;
import com.example.riskengine.domain.Transaction;

import java.util.concurrent.CompletableFuture;

public interface FraudSignalClient {
    CompletableFuture<FraudSignal> check(Transaction transaction);
}
