package com.example.riskengine.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {
    @Test
    void rejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> transaction(BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> transaction(BigDecimal.valueOf(-1)));
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(NullPointerException.class, () -> new Transaction(
                null,
                "acct-1",
                "cust-1",
                "merchant-normal",
                BigDecimal.TEN,
                "USD",
                "US",
                Instant.parse("2026-01-01T00:00:00Z")
        ));
    }

    @Test
    void acceptsPositiveFractionalAmount() {
        assertDoesNotThrow(() -> transaction(BigDecimal.valueOf(10.50)));
    }

    private static Transaction transaction(BigDecimal amount) {
        return new Transaction(
                "txn-domain",
                "acct-1",
                "cust-1",
                "merchant-normal",
                amount,
                "USD",
                "US",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
