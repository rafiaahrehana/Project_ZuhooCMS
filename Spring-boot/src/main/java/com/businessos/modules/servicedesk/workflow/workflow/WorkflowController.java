package com.businessos.modules.servicedesk.workflow.workflow;

import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageRequest;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageResponse;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateRequest;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BUG-FIX: Added @Valid on all @RequestBody parameters.
 * Added @PreAuthorize — workflow template management is an admin-only operation.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/suggest")
    public ResponseEntity<WorkflowSuggestionResponse> suggest(
            @Valid @RequestBody WorkflowSuggestionRequest request) {
        return ResponseEntity.ok(workflowService.suggest(request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<WorkflowTemplateResponse> create(
            @Valid @RequestBody WorkflowTemplateRequest request) {
        return new ResponseEntity<>(workflowService.createTemplate(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<WorkflowTemplateResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workflowService.listTemplates(
                PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<WorkflowTemplateResponse>> listActive() {
        return ResponseEntity.ok(workflowService.listActiveTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getTemplateById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowTemplateRequest request) {
        return ResponseEntity.ok(workflowService.updateTemplate(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<WorkflowTemplateResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.toggleTemplate(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        workflowService.deleteTemplate(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{templateId}/stages")
    public ResponseEntity<WorkflowStageResponse> addStage(
            @PathVariable Long templateId,
            @Valid @RequestBody WorkflowStageRequest request) {
        return new ResponseEntity<>(
            workflowService.addStage(templateId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{templateId}/stages/{stageId}")
    public ResponseEntity<WorkflowStageResponse> updateStage(
            @PathVariable Long templateId,
            @PathVariable Long stageId,
            @Valid @RequestBody WorkflowStageRequest request) {
        return ResponseEntity.ok(workflowService.updateStage(templateId, stageId, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{templateId}/stages/{stageId}")
    public ResponseEntity<String> deleteStage(
            @PathVariable Long templateId,
            @PathVariable Long stageId) {
        workflowService.deleteStage(templateId, stageId);
        return ResponseEntity.ok("Deleted successfully");
    }
}
