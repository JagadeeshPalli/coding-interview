package com.example.riskengine.infra;

import com.example.riskengine.domain.CustomerProfile;

import java.util.concurrent.CompletableFuture;

public interface CustomerProfileClient {
    CompletableFuture<CustomerProfile> fetch(String customerId);
}
