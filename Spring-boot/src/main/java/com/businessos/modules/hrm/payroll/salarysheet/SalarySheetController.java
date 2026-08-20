package com.businessos.modules.hrm.payroll.salarysheet;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * The company salary sheet. Gated on PAYROLL_VIEW: this shows every employee's
 * pay, which is exactly what the payroll permission exists to protect.
 */
@RestController
@RequestMapping("/api/hr/salary-sheet")
@RequiredArgsConstructor
public class SalarySheetController {

    private final SalarySheetService service;
    private final SalarySheetPdf salarySheetPdf;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<SalarySheetResponse> get(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);

        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(service.build(
                month != null ? month : now.getMonthValue(),
                year != null ? year : now.getYear()));
    }

    /**
     * The same sheet as a PDF, for the month-end pack that gets signed off or
     * filed. Rendered server-side from the same service call the screen uses,
     * so the export cannot drift from what was on screen.
     */
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);

        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        SalarySheetPdf.Document doc = salarySheetPdf.render(service.build(m, y));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + doc.fileName() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(doc.content());
    }
}
