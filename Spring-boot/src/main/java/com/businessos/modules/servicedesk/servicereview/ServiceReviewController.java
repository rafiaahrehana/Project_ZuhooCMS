package com.businessos.modules.servicedesk.servicereview;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ServiceReviewController {

    private final ServiceReviewService reviewService;

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ServiceReviewResponse> submitOrUpdate(
            @Valid @RequestBody ServiceReviewRequest request) {
        return new ResponseEntity<>(reviewService.submitOrUpdate(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceReviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<ServiceReviewResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.listAll(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/service/{hubServiceId}")
    public ResponseEntity<Page<ServiceReviewResponse>> listByService(
            @PathVariable Long hubServiceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.listByService(
            hubServiceId,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/service/{hubServiceId}/average-rating")
    public ResponseEntity<Double> averageRatingByService(
            @PathVariable Long hubServiceId) {
        return ResponseEntity.ok(reviewService.getAverageRatingByService(hubServiceId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/average-rating")
    public ResponseEntity<Double> averageRatingByCompany() {
        return ResponseEntity.ok(reviewService.getAverageRatingByCompany());
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
