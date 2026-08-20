package com.businessos.modules.servicedesk.servicetemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateFormFieldRepository extends JpaRepository<TemplateFormField, Long> {
    List<TemplateFormField> findByServiceTemplateIdOrderBySortOrderAsc(Long serviceTemplateId);
}
