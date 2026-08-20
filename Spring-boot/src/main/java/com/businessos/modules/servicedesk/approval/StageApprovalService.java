package com.businessos.modules.servicedesk.approval;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StageApprovalService {
    Page<StageApprovalResponse> getPending(Pageable pageable);
    List<StageApprovalResponse> getForRequest(Long serviceRequestId);
    StageApprovalResponse approve(Long id, DecisionRequest request);
    StageApprovalResponse reject(Long id, DecisionRequest request);
}
