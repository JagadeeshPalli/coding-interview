package com.example.riskengine.domain;

public record FraudSignal(int providerScore, boolean listedMerchant, String reason) {
}
