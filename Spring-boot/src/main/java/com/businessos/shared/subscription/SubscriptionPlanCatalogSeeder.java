package com.businessos.shared.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ensures the four legacy tiers (Free/Starter/Pro/Enterprise) exist as real
 * SubscriptionPlanDefinition rows on every boot. Existing companies already
 * have "FREE"/"STARTER"/"PRO"/"ENTERPRISE" strings in their subscriptionPlan
 * column (from when it was a Java enum) - those codes must resolve to a
 * catalog row immediately, or company plan lookups would break on upgrade.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPlanCatalogSeeder implements CommandLineRunner {

    private final SubscriptionPlanDefinitionRepository repository;

    @Override
    public void run(String... args) {
        seed("FREE", "Free", "Basic support, limited users, community access.", BillingCycle.MONTHLY, BigDecimal.ZERO);
        seed("STARTER", "Starter", "Email support, up to 10 users, standard integrations.", BillingCycle.MONTHLY, new BigDecimal("4900"));
        seed("PRO", "Pro", "Priority support, unlimited users, advanced integrations.", BillingCycle.MONTHLY, new BigDecimal("9900"));
        seed("ENTERPRISE", "Enterprise", "24/7 phone support, dedicated account manager, custom development.", BillingCycle.MONTHLY, new BigDecimal("19900"));
    }

    private void seed(String code, String name, String description, BillingCycle cycle, BigDecimal price) {
        if (!repository.existsByCodeIgnoreCase(code)) {
            repository.save(SubscriptionPlanDefinition.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .billingCycle(cycle)
                    .price(price)
                    .active(true)
                    .build());
        }
    }
}
