package com.businessos.modules.servicedesk.proposal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceProposalRepository extends JpaRepository<ServiceProposal, Long> {
    Optional<ServiceProposal> findByServiceRequestIdAndCompanyId(Long serviceRequestId, Long companyId);
    Optional<ServiceProposal> findByIdAndCompanyId(Long id, Long companyId);
}
