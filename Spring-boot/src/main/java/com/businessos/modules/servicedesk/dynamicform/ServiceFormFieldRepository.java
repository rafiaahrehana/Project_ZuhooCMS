package com.businessos.modules.servicedesk.dynamicform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceFormFieldRepository extends JpaRepository<ServiceFormField, Long> {
    List<ServiceFormField> findByCompanyIdAndServiceIdOrderBySortOrderAsc(Long companyId, Long serviceId);
    Optional<ServiceFormField> findByIdAndCompanyId(Long id, Long companyId);
    void deleteByCompanyIdAndServiceId(Long companyId, Long serviceId);
}
