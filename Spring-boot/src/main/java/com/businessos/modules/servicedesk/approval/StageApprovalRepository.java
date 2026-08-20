package com.businessos.modules.servicedesk.approval;

import com.businessos.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface StageApprovalRepository extends JpaRepository<StageApproval, Long> {
    boolean existsByServiceRequestIdAndWorkflowStageIdAndStatus(Long serviceRequestId, Long stageId, ApprovalStatus status);
    Page<StageApproval> findByCompanyIdAndStatus(Long companyId, ApprovalStatus status, Pageable pageable);
    List<StageApproval> findByServiceRequestId(Long serviceRequestId);
    Optional<StageApproval> findByIdAndCompanyId(Long id, Long companyId);
}
