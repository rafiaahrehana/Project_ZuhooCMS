package com.businessos.modules.hrm.payroll.salarysheet;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyMapper;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.pdf.SimpleTablePdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The salary sheet as a downloadable PDF.
 *
 * Uses the shared SimpleTablePdfRenderer rather than a bespoke layout: this is
 * a list export like any other, and the renderer already carries the company
 * header block that the finance exports use.
 */
@Component
@RequiredArgsConstructor
public class SalarySheetPdf {

    private final SimpleTablePdfRenderer renderer;
    private final CompanyRepository companyRepository;
    private final EmailBranding emailBranding;
    private final SecurityUtil securityUtil;

    public record Document(byte[] content, String fileName) {}

    /**
     * Transactional because the company's address is a lazy association and the
     * header block dereferences it. The controller is not itself transactional,
     * so without this the render dies on a LazyInitializationException the
     * moment a company has an address on file.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Document render(SalarySheetResponse sheet) {
        List<String> headers = List.of(
                "Employee", "Basic", "House Rent", "Medical", "Transport",
                "Others", "Gross Pay", "Deductions", "Net Payable");

        List<List<String>> rows = new ArrayList<>();
        for (SalarySheetRow r : sheet.getRows()) {
            rows.add(List.of(
                    employeeCell(r),
                    money(r.getBasic()),
                    money(r.getHouseRent()),
                    money(r.getMedical()),
                    money(r.getTransport()),
                    money(others(r)),
                    money(r.getGrossEarnings()),
                    money(r.getTotalDeductions()),
                    money(r.getNetPayable())));
        }

        // The totals ride as a final row rather than a separate block: the
        // renderer draws one table, and a column total is only useful directly
        // under the column it totals.
        rows.add(List.of(
                "TOTAL (" + sheet.getRows().size() + " employees)",
                money(sheet.getTotalBasic()),
                money(sheet.getTotalHouseRent()),
                money(sheet.getTotalMedical()),
                money(sheet.getTotalTransport()),
                money(totalOthers(sheet)),
                money(sheet.getTotalGrossEarnings()),
                money(sheet.getTotalDeductions()),
                money(sheet.getTotalNetPayable())));

        Company company = currentCompany();
        EmailBranding.Data branding = emailBranding.from(company);

        String period = monthName(sheet.getPayMonth()) + " " + sheet.getPayYear();
        String subtitle = "Pay period " + period
                + "  ·  per-day basis " + readableBasis(sheet.getPerDayBasis())
                + " (÷ " + sheet.getPerDayDivisor() + ")"
                + (sheet.isOvertimeEnabled()
                    ? "  ·  overtime ×" + trim(sheet.getOvertimeMultiplier())
                    : "  ·  overtime off")
                + "  ·  all amounts in BDT";

        byte[] pdf = renderer.render(
                "Salary Sheet", subtitle, headers, rows,
                branding.getCompanyName(),
                branding.getLogoUrl(),
                company != null ? company.getCompanyEmail() : null,
                company != null ? CompanyMapper.formatAddress(company.getLocationDetail()) : null);

        String fileName = String.format("salary-sheet-%04d-%02d.pdf", sheet.getPayYear(), sheet.getPayMonth());
        return new Document(pdf, fileName);
    }

    /** Name, employee number and position in one cell - the export has no room for three columns of identity. */
    private String employeeCell(SalarySheetRow r) {
        StringBuilder sb = new StringBuilder(r.getEmployeeName() != null ? r.getEmployeeName() : "-");
        if (notBlank(r.getEmployeeNumber())) sb.append(" (").append(r.getEmployeeNumber()).append(")");
        if (notBlank(r.getPosition())) sb.append(" - ").append(r.getPosition());
        // A row with no salary structure would otherwise print as a line of
        // zeroes with no explanation.
        if (notBlank(r.getNote())) sb.append(" [").append(r.getNote()).append("]");
        return sb.toString();
    }

    /** Food, special and overtime collapse into one "Others" column, as on screen. */
    private BigDecimal others(SalarySheetRow r) {
        return nz(r.getFood()).add(nz(r.getSpecial())).add(nz(r.getOvertimePayment()));
    }

    private BigDecimal totalOthers(SalarySheetResponse s) {
        return nz(s.getTotalFood()).add(nz(s.getTotalSpecial())).add(nz(s.getTotalOvertimePayment()));
    }

    private String readableBasis(Object basis) {
        if (basis == null) return "-";
        return switch (basis.toString()) {
            case "CALENDAR_DAYS" -> "calendar days";
            case "FIXED_30" -> "fixed 30-day month";
            case "FIXED_26" -> "fixed 26-day month";
            case "ACTUAL_WORKING_DAYS" -> "actual working days";
            default -> basis.toString();
        };
    }

    private Company currentCompany() {
        Long id = securityUtil.getCurrentCompanyId();
        return id != null ? companyRepository.findById(id).orElse(null) : null;
    }

    private String monthName(int m) {
        return m >= 1 && m <= 12 ? Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH) : String.valueOf(m);
    }

    private String money(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trim(BigDecimal v) {
        return v == null ? "1" : v.stripTrailingZeros().toPlainString();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
