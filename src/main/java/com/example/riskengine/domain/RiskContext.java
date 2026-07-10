package com.example.riskengine.domain;

import java.util.Objects;

public record RiskContext(
        Transaction transaction,
        CustomerProfile customerProfile,
        FraudSignal fraudSignal,
        long accountSpendAfterTransaction,
        int transactionsInCurrentWindow
) {
    public RiskContext {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(customerProfile, "customerProfile");
        Objects.requireNonNull(fraudSignal, "fraudSignal");
    }
}
