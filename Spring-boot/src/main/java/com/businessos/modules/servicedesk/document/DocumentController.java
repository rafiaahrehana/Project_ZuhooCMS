package com.businessos.modules.servicedesk.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Documents attached to a service request. Any authenticated tenant user can
 * upload/view (the client who owns the request, or staff working it) - the
 * same open pattern used by comments/tasks on ServiceRequestController.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-requests/{requestId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateDocumentRequest request) {
        return new ResponseEntity<>(documentService.upload(requestId, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable Long requestId) {
        return ResponseEntity.ok(documentService.listForRequest(requestId));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Long requestId, @PathVariable Long documentId) {
        documentService.delete(requestId, documentId);
        return ResponseEntity.noContent().build();
    }
}
