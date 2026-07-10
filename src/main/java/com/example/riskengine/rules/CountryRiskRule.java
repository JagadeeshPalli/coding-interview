package com.example.riskengine.rules;

import com.example.riskengine.domain.RiskConfig;
import com.example.riskengine.domain.RiskContext;
import com.example.riskengine.domain.RiskResult;

public final class CountryRiskRule implements RiskRule {
    @Override
    public RiskResult evaluate(RiskContext context, RiskConfig config) {
        String country = context.transaction().country();
        if (config.sanctionedCountries().contains(country)) {
            return new RiskResult("COUNTRY", 100, "Country is sanctioned");
        }
        if (!country.equals(context.customerProfile().homeCountry())) {
            return new RiskResult("COUNTRY", 20, "Country differs from customer home country");
        }
        return new RiskResult("COUNTRY", 0, "Country matches expected profile");
    }
}
