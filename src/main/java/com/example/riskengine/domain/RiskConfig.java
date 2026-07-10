package com.example.riskengine.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record RiskConfig(
        BigDecimal reviewAmountThreshold,
        BigDecimal declineAmountThreshold,
        int reviewScoreThreshold,
        int declineScoreThreshold,
        long velocitySpendThreshold,
        int velocityTransactionThreshold,
        Set<String> sanctionedCountries
) {
    public RiskConfig {
        Objects.requireNonNull(reviewAmountThreshold, "reviewAmountThreshold");
        Objects.requireNonNull(declineAmountThreshold, "declineAmountThreshold");
        sanctionedCountries = Set.copyOf(Objects.requireNonNull(sanctionedCountries, "sanctionedCountries"));
    }

    public static RiskConfig defaults() {
        return new RiskConfig(
                BigDecimal.valueOf(1_000),
                BigDecimal.valueOf(10_000),
                40,
                80,
                5_000,
                5,
                Set.of("IR", "KP", "SY")
        );
    }
}
