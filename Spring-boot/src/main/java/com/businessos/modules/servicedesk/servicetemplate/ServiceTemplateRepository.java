package com.businessos.modules.servicedesk.servicetemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, Long> {
    Page<ServiceTemplate> findByActive(boolean active, Pageable pageable);
    List<ServiceTemplate> findByCategoryIdAndActiveTrue(Long categoryId);
}
