package com.example.riskengine.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Transaction(
        String id,
        String accountId,
        String customerId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String country,
        Instant timestamp
) {
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(timestamp, "timestamp");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
