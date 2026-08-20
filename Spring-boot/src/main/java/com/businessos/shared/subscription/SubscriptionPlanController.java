package com.businessos.shared.subscription;

import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Super Admin-managed plan catalog - the Company Owner's upgrade picker and the
 * self-service checkout (SslCommerzServiceImpl) both price off these rows. */
@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanDefinitionRepository repository;

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanDefinition>> getAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly
                ? repository.findByActiveTrueOrderByPriceAsc()
                : repository.findAllByOrderByPriceAsc());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping
    public ResponseEntity<SubscriptionPlanDefinition> create(@Valid @RequestBody SubscriptionPlanRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("A plan with code \"" + code + "\" already exists.");
        }
        SubscriptionPlanDefinition plan = SubscriptionPlanDefinition.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .billingCycle(request.getBillingCycle())
                .price(request.getPrice())
                .active(true)
                .build();
        return new ResponseEntity<>(repository.save(plan), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<SubscriptionPlanDefinition> update(
            @PathVariable Long id, @Valid @RequestBody SubscriptionPlanRequest request) {
        // code is immutable once created - it's the identifier Company.subscriptionPlan
        // and SubscriptionHistory.fromPlan/toPlan already reference.
        SubscriptionPlanDefinition plan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription());
        plan.setBillingCycle(request.getBillingCycle());
        plan.setPrice(request.getPrice());
        plan.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(repository.save(plan));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<SubscriptionPlanDefinition> toggleActive(@PathVariable Long id) {
        SubscriptionPlanDefinition plan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        plan.setActive(!plan.isActive());
        plan.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(repository.save(plan));
    }
}
