package com.businessos.modules.servicedesk.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {
    List<RequiredDocument> findByCompanyIdAndServiceIdOrderBySortOrderAsc(Long companyId, Long serviceId);
    Optional<RequiredDocument> findByIdAndCompanyId(Long id, Long companyId);
}
