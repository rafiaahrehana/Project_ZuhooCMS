package com.businessos.modules.finance.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/finance/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Per-category spending budgets with live budget-vs-actual")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a budget for a category + fiscal year")
    public ResponseEntity<BudgetDtos.BudgetResponse> create(@Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return new ResponseEntity<>(budgetService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget")
    public ResponseEntity<BudgetDtos.BudgetResponse> update(@PathVariable Long id,
            @Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @GetMapping
    @Operation(summary = "Budgets for a fiscal year with actual spend, remaining, and % used")
    public ResponseEntity<List<BudgetDtos.BudgetResponse>> listForYear(@RequestParam int fiscalYear) {
        return ResponseEntity.ok(budgetService.listForYear(fiscalYear));
    }

    @GetMapping("/categories")
    @Operation(summary = "Distinct budget category names, for suggesting matches on the Expense form")
    public ResponseEntity<List<String>> listCategories() {
        return ResponseEntity.ok(budgetService.listCategories());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
