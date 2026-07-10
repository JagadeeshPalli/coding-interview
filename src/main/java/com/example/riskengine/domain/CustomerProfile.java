package com.example.riskengine.domain;

import java.util.Objects;
import java.util.Set;

public record CustomerProfile(
        String customerId,
        String homeCountry,
        Set<String> trustedMerchants,
        boolean highRiskCustomer
) {
    public CustomerProfile {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(homeCountry, "homeCountry");
        trustedMerchants = Set.copyOf(Objects.requireNonNull(trustedMerchants, "trustedMerchants"));
    }
}
