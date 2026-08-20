package com.businessos.modules.servicedesk.kb;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kb/articles")
@RequiredArgsConstructor
public class KbArticleController {

    private final KbArticleService articleService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<KbArticleResponse> create(@Valid @RequestBody KbArticleRequest request) {
        return ResponseEntity.ok(articleService.create(request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}")
    public ResponseEntity<KbArticleResponse> update(@PathVariable Long id, @Valid @RequestBody KbArticleRequest request) {
        return ResponseEntity.ok(articleService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KbArticleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<KbArticleResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.list(keyword, status, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<KbArticleResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.publish(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<KbArticleResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.archive(id));
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<KbArticleResponse> markHelpful(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.markHelpful(id));
    }
}
