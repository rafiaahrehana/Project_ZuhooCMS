package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.modules.servicedesk.requestcomment.AddCommentRequest;
import com.businessos.modules.servicedesk.requeststatus.ChangeRequestStatusRequest;
import com.businessos.modules.servicedesk.task.CreateTaskRequest;
import com.businessos.modules.servicedesk.task.UpdateTaskRequest;
import com.businessos.modules.servicedesk.requestcomment.RequestCommentResponse;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistoryResponse;
import com.businessos.modules.servicedesk.task.TaskResponse;
import com.businessos.enums.ServiceRequestStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final com.businessos.modules.servicedesk.task.TaskService taskService;

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @Valid @RequestBody CreateServiceRequestRequest request) {
        return new ResponseEntity<>(serviceRequestService.create(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<ServiceRequestResponse>> listAll(
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(serviceRequestService.listAll(status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ServiceRequestResponse>> listMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(serviceRequestService.listMyRequests(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/assigned-to-me")
    public ResponseEntity<Page<ServiceRequestResponse>> listAssignedToMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(serviceRequestService.listAssignedToMe(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}/summary")
    public ResponseEntity<ServiceRequestResponse> summarise(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.summarise(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{id}/draft-reply")
    public ResponseEntity<ServiceRequestReplyDraftResponse> draftReply(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestReplyDraftRequest request) {
        return ResponseEntity.ok(serviceRequestService.draftReply(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestRequest request) {
        return ResponseEntity.ok(serviceRequestService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRequestStatusRequest request) {
        return ResponseEntity.ok(serviceRequestService.changeStatus(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/assign/{employeeId}")
    public ResponseEntity<ServiceRequestResponse> assign(
            @PathVariable Long id,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(serviceRequestService.assign(id, employeeId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long id,
            @RequestBody(required = false) CancelRequest request) {
        serviceRequestService.cancel(id, request != null ? request.getReason() : null);
        return ResponseEntity.ok("Request cancelled");
    }

    @lombok.Getter @lombok.Setter
    public static class CancelRequest {
        private String reason;
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> addTask(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRequest request) {
        return new ResponseEntity<>(taskService.addTask(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTasks(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, taskId, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<String> deleteTask(
            @PathVariable Long id,
            @PathVariable Long taskId) {
        taskService.deleteTask(id, taskId);
        return ResponseEntity.ok("Deleted successfully");
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<RequestCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        return new ResponseEntity<>(
                serviceRequestService.addComment(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<RequestCommentResponse>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                serviceRequestService.getComments(id, PageRequest.of(page, size)));
    }

    // Clients included: getStatusHistory() resolves the request through findInTenant(), whose
    // guardAccess() already refuses a CLIENT any request that isn't their own. The status
    // timeline is the client's own history — there's nothing staff-internal in it.
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'CLIENT')")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequestStatusHistoryResponse>> getHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getStatusHistory(id));
    }

    // Workflow stage progress: was implemented in the service layer but never
    // reached a route, so a service with a workflow attached had no way to
    // actually move a request through its stages unless every stage happened
    // to require approval (the only other caller is StageApprovalServiceImpl,
    // after an approval decision).
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{id}/advance-stage")
    public ResponseEntity<ServiceRequestResponse> advanceStage(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.advanceStage(id));
    }

    // Clients included: this is the same "which stage is my request in" view
    // as getHistory() above, just shaped around the workflow's stages instead
    // of the status timeline.
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'CLIENT')")
    @GetMapping("/{id}/stage-progress")
    public ResponseEntity<StageProgressResponse> getStageProgress(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getStageProgress(id));
    }

    // Quotation Endpoints

    @PostMapping("/{id}/quotation")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Submit a quotation for a service request")
    public ResponseEntity<ServiceRequestResponse> submitQuotation(
            @PathVariable Long id,
            @Valid @RequestBody SubmitQuotationRequest request) {
        return ResponseEntity.ok(serviceRequestService.submitQuotation(id, request));
    }

    @PostMapping("/{id}/quotation/accept")
    @PreAuthorize("hasAnyRole('CLIENT', 'COMPANY_OWNER')")
    @Operation(summary = "Accept a quotation")
    public ResponseEntity<ServiceRequestResponse> acceptQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.acceptQuotation(id));
    }

    @PostMapping("/{id}/quotation/reject")
    @PreAuthorize("hasAnyRole('CLIENT', 'COMPANY_OWNER')")
    @Operation(summary = "Reject a quotation")
    public ResponseEntity<ServiceRequestResponse> rejectQuotation(
            @PathVariable Long id,
            @Valid @RequestBody RejectQuotationRequest request) {
        return ResponseEntity.ok(serviceRequestService.rejectQuotation(id, request));
    }
}
