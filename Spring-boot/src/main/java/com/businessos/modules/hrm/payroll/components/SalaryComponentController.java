package com.businessos.modules.hrm.payroll.components;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Salary component catalog, reusable structure templates, and per-employee
 * extra components. Same access rule as SalaryStructureController: the
 * fine-grained checks are permission-service driven inside the services.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/salary-components")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class SalaryComponentController {

    private final SalaryComponentService service;

    // ── Catalog ────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<SalaryComponent>> catalog() {
        return ResponseEntity.ok(service.listCatalog());
    }

    @PostMapping
    public ResponseEntity<SalaryComponent> create(@RequestBody SalaryComponent input) {
        return new ResponseEntity<>(service.createComponent(input), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryComponent> update(@PathVariable Long id, @RequestBody SalaryComponent input) {
        return ResponseEntity.ok(service.updateComponent(id, input));
    }

    // ── Templates ──────────────────────────────────────────────
    @GetMapping("/templates")
    public ResponseEntity<List<SalaryStructureTemplate>> templates() {
        return ResponseEntity.ok(service.listTemplates());
    }

    @PostMapping("/templates")
    public ResponseEntity<SalaryStructureTemplate> createTemplate(@RequestBody SalaryStructureTemplate input) {
        return new ResponseEntity<>(service.saveTemplate(null, input), HttpStatus.CREATED);
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<SalaryStructureTemplate> updateTemplate(
            @PathVariable Long id, @RequestBody SalaryStructureTemplate input) {
        return ResponseEntity.ok(service.saveTemplate(id, input));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates/{id}/breakdown")
    public ResponseEntity<Map<String, BigDecimal>> breakdown(
            @PathVariable Long id, @RequestParam BigDecimal gross) {
        return ResponseEntity.ok(service.breakdown(id, gross));
    }

    // ── Extra components on one employee's structure ───────────
    @GetMapping("/structure/{structureId}")
    public ResponseEntity<List<ExtraDto>> extras(@PathVariable Long structureId) {
        return ResponseEntity.ok(service.listExtras(structureId).stream().map(ExtraDto::of).toList());
    }

    @PutMapping("/structure/{structureId}")
    public ResponseEntity<List<ExtraDto>> setExtras(
            @PathVariable Long structureId,
            @RequestBody List<SalaryComponentService.ExtraLine> lines) {
        return ResponseEntity.ok(service.setExtras(structureId, lines).stream().map(ExtraDto::of).toList());
    }

    public record ExtraDto(Long id, Long componentId, String componentName,
                           String type, BigDecimal amount) {
        static ExtraDto of(StructureExtraComponent e) {
            return new ExtraDto(e.getId(), e.getComponent().getId(), e.getComponent().getName(),
                    e.getComponent().getType().name(), e.getAmount());
        }
    }
}
