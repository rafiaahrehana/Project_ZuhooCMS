package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository("websiteServiceRequestRepository")
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    Optional<ServiceRequest> findByCodeAndCompanyId(String code, Long companyId);
}
