package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    List<PricingPlan> findByCompanyId(Long companyId);
}
