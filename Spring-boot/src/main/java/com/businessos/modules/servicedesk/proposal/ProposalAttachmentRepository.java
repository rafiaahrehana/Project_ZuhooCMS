package com.businessos.modules.servicedesk.proposal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalAttachmentRepository extends JpaRepository<ProposalAttachment, Long> {
    List<ProposalAttachment> findByProposalIdOrderByIdAsc(Long proposalId);
    Optional<ProposalAttachment> findByIdAndCompanyId(Long id, Long companyId);
}
