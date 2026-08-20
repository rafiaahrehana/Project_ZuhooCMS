package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NavItemRepository extends JpaRepository<NavItem, Long> {
    List<NavItem> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
