package com.businessos.modules.servicedesk.servicetemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRequiredDocumentRepository extends JpaRepository<TemplateRequiredDocument, Long> {
    List<TemplateRequiredDocument> findByServiceTemplateIdOrderBySortOrderAsc(Long serviceTemplateId);
}
