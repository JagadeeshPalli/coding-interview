package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public final class MerchantTrustRule implements RiskRule {
    @Override
    public RiskResult evaluate(RiskContext context, RiskConfig config) {
        if (context.customerProfile().trustedMerchants().contains(context.transaction().merchantId())) {
            return new RiskResult("MERCHANT_TRUST", -15, "Merchant is trusted by customer");
        }
        if (context.customerProfile().highRiskCustomer()) {
            return new RiskResult("MERCHANT_TRUST", 15, "Customer profile is high risk");
        }
        return new RiskResult("MERCHANT_TRUST", 0, "No merchant trust adjustment");
    }
}
