package com.businessos.shared.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanDefinitionRepository extends JpaRepository<SubscriptionPlanDefinition, Long> {

    Optional<SubscriptionPlanDefinition> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<SubscriptionPlanDefinition> findAllByOrderByPriceAsc();

    List<SubscriptionPlanDefinition> findByActiveTrueOrderByPriceAsc();
}
