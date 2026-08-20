package com.businessos.modules.servicedesk.proposal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-requests/{requestId}/proposal")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @GetMapping
    public ResponseEntity<ProposalResponse> get(@PathVariable Long requestId) {
        return ResponseEntity.ok(proposalService.get(requestId));
    }

    @PutMapping
    public ResponseEntity<ProposalResponse> save(@PathVariable Long requestId, @Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.ok(proposalService.save(requestId, request));
    }

    @PostMapping("/send")
    public ResponseEntity<ProposalResponse> send(@PathVariable Long requestId) {
        return ResponseEntity.ok(proposalService.send(requestId));
    }

    @PostMapping("/accept")
    public ResponseEntity<ProposalResponse> accept(@PathVariable Long requestId) {
        return ResponseEntity.ok(proposalService.accept(requestId));
    }

    @PostMapping("/request-changes")
    public ResponseEntity<ProposalResponse> requestChanges(@PathVariable Long requestId, @RequestBody(required = false) RequestChangesBody body) {
        return ResponseEntity.ok(proposalService.requestChanges(requestId, body != null ? body.getFeedback() : null));
    }

    @PostMapping("/attachments")
    public ResponseEntity<ProposalAttachmentResponse> addAttachment(@PathVariable Long requestId, @Valid @RequestBody ProposalAttachmentRequest request) {
        return ResponseEntity.ok(proposalService.addAttachment(requestId, request));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long requestId, @PathVariable Long attachmentId) {
        proposalService.deleteAttachment(requestId, attachmentId);
        return ResponseEntity.ok().build();
    }

    @lombok.Getter @lombok.Setter
    public static class RequestChangesBody {
        private String feedback;
    }
}
