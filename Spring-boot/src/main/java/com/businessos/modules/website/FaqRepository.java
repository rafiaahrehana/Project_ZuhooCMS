package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByCompanyId(Long companyId);
    List<Faq> findByCompanyIdAndCategory(Long companyId, String category);
}
