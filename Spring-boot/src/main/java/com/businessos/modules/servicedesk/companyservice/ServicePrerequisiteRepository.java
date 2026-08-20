package com.businessos.modules.servicedesk.companyservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePrerequisiteRepository extends JpaRepository<ServicePrerequisite, Long> {
    List<ServicePrerequisite> findByServiceIdOrderByIdAsc(Long serviceId);
}
