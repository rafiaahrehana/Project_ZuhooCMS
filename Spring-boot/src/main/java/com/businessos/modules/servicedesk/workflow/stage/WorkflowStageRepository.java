package com.businessos.modules.servicedesk.workflow.stage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * BUG-FIX SUMMARY
 * ───────────────
 * Added findByIdAndCompanyId — ServiceRequestServiceImpl.addTask() calls:
 *   workflowStageRepository.findByIdAndCompanyId(request.getWorkflowStageId(), companyId)
 * Without this method the repository has no matching derived query and the service
 * fails to compile.
 *
 * This also enforces tenant isolation on WorkflowStage lookups: a stage from
 * another company cannot be linked to a task in this company.
 */
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

    List<WorkflowStage> findByWorkflowTemplateIdOrderByStageOrderAsc(Long templateId);

    boolean existsByWorkflowTemplateIdAndStageOrder(Long templateId, Integer stageOrder);

    Optional<WorkflowStage> findByIdAndCompanyId(Long id, Long companyId); // added
}
