package com.businessos.modules.hrm.payroll;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyMapper;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.shared.email.EmailBranding;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a payslip as a PDF, laid out the way a payslip conventionally is:
 * earnings on the left, deductions on the right, and the net as the one figure
 * that stands out.
 *
 * Built on the same openhtmltopdf pipeline as InvoicePdfService so there is one
 * way documents are produced in this codebase rather than two.
 */
@Service
@lombok.RequiredArgsConstructor
public class PayslipPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final com.businessos.modules.hrm.salary.SalaryStructureRepository salaryStructureRepository;
    private final com.businessos.modules.hrm.payroll.components.StructureExtraComponentRepository extraComponentRepository;

    public byte[] generate(Payroll payroll, Company company, EmailBranding.Data branding) {
        String html = buildHtml(payroll, company, branding);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate payslip PDF", ex);
        }
        return os.toByteArray();
    }

    /** e.g. "payslip-2026-08-EMP001.pdf" - sorts chronologically in a folder. */
    public String fileName(Payroll payroll) {
        Employee e = payroll.getEmployee();
        String who = e != null && e.getEmployeeNumber() != null && !e.getEmployeeNumber().isBlank()
                ? e.getEmployeeNumber()
                : (e != null && e.getId() != null ? "EMP" + e.getId() : "employee");
        return String.format("payslip-%04d-%02d-%s.pdf", payroll.getPayYear(), payroll.getPayMonth(), sanitise(who));
    }

    private String buildHtml(Payroll p, Company company, EmailBranding.Data branding) {
        String accent = branding.getPrimaryColor() != null ? branding.getPrimaryColor() : "#14300F";
        String currency = "BDT";

        Employee emp = p.getEmployee();
        String employeeName = displayName(emp);
        String employeeNumber = emp != null ? emp.getEmployeeNumber() : null;
        String jobTitle = emp != null ? emp.getJobTitle() : null;
        String department = emp != null && emp.getDepartment() != null ? emp.getDepartment().getName() : null;
        String joinDate = emp != null && emp.getHireDate() != null ? emp.getHireDate().format(DATE_FMT) : null;

        String companyAddress = company != null ? CompanyMapper.formatAddress(company.getLocationDetail()) : null;

        // Earnings. Every standard line prints even at 0.00 - a payslip is a
        // statement of the full pay policy, and a missing row reads as an
        // omission rather than a zero.
        List<String[]> earnings = new ArrayList<>();
        addLine(earnings, "Basic Salary", p.getBasicSalary(), currency);
        addLine(earnings, "House Rent", p.getHouseRent(), currency);
        addLine(earnings, "Medical Allowance", p.getMedicalAllowance(), currency);
        addLine(earnings, "Transport Allowance", p.getTransportAllowance(), currency);
        addLine(earnings, "Food Allowance", p.getFoodAllowance(), currency);
        addLine(earnings, "Special Allowance", p.getSpecialAllowance(), currency);
        addLine(earnings, "Bonus", p.getBonus(), currency);
        {
            String label = "Billable Pay";
            if (isPositive(p.getBillablePay()) && isPositive(p.getBillableHours()) && isPositive(p.getBillableRate())) {
                label += " (" + trim(p.getBillableHours()) + " hrs × " + trim(p.getBillableRate()) + ")";
            }
            earnings.add(new String[]{label, money(p.getBillablePay(), currency)});
        }
        // Overtime shows its own working: an employee's first question about an
        // overtime line is always how many hours and at what rate.
        {
            String label = "Overtime";
            if (isPositive(p.getOvertimePay()) && isPositive(p.getOvertimeHours())) {
                label += " (" + trim(p.getOvertimeHours()) + " hrs";
                if (isPositive(p.getOvertimeRate())) {
                    label += " × " + money(p.getOvertimeRate(), currency) + "/hr";
                }
                label += ")";
            }
            earnings.add(new String[]{label, money(p.getOvertimePay(), currency)});
        }
        appendComponentLines(earnings, p, currency, true);

        List<String[]> deductions = new ArrayList<>();
        addLine(deductions, "Tax", p.getTaxDeduction(), currency);
        addLine(deductions, "Provident Fund", p.getProvidentFundDeduction(), currency);
        addLine(deductions, "Insurance", p.getInsuranceDeduction(), currency);
        {
            String label = "Absence";
            if (p.getAbsentDays() != null && p.getAbsentDays() > 0) {
                label += " (" + p.getAbsentDays() + (p.getAbsentDays() == 1 ? " day" : " days") + ")";
            }
            deductions.add(new String[]{label, money(p.getAttendanceDeduction(), currency)});
        }
        addLine(deductions, "Other Deductions", p.getDeductions(), currency);
        if (isPositive(p.getLoanDeductionAmount())) {
            deductions.add(new String[]{"Loan Repayment", money(p.getLoanDeductionAmount(), currency)});
        }
        appendComponentLines(deductions, p, currency, false);

        BigDecimal grossTotal = sum(earnings.size(), p);
        BigDecimal deductionTotal = totalDeductions(p);

        // The two columns are rendered as one table so the rows stay aligned
        // regardless of which side has more entries.
        int rowCount = Math.max(earnings.size(), deductions.size());
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            String[] e = i < earnings.size() ? earnings.get(i) : null;
            String[] d = i < deductions.size() ? deductions.get(i) : null;
            body.append("<tr>")
                    .append("<td>").append(e != null ? escape(e[0]) : "").append("</td>")
                    .append("<td class=\"num\">").append(e != null ? e[1] : "").append("</td>")
                    .append("<td class=\"gap\"></td>")
                    .append("<td>").append(d != null ? escape(d[0]) : "").append("</td>")
                    .append("<td class=\"num\">").append(d != null ? d[1] : "").append("</td>")
                    .append("</tr>");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:Helvetica,Arial,sans-serif;color:#1f2937;font-size:11px;}"
                + ".header{display:table;width:100%;margin-bottom:18px;}"
                + ".header .company{display:table-cell;vertical-align:top;}"
                + ".header .title{display:table-cell;text-align:right;vertical-align:top;}"
                + ".logo{max-height:40px;max-width:180px;margin-bottom:6px;}"
                + ".company-name{font-size:18px;font-weight:bold;color:" + accent + ";}"
                + ".company-meta{color:#6b7280;line-height:1.5;margin-top:2px;}"
                + ".doc-title{font-size:22px;font-weight:bold;color:" + accent + ";letter-spacing:2px;}"
                + ".meta{margin-top:4px;color:#6b7280;}"
                + ".label{color:#6b7280;text-transform:uppercase;font-size:9px;letter-spacing:1px;margin-bottom:2px;}"
                + "table.info{width:100%;border-collapse:collapse;margin-bottom:16px;"
                + "background:#f9fafb;border:1px solid #e5e7eb;}"
                + "table.info td{padding:6px 10px;vertical-align:top;width:25%;}"
                + "table.lines{width:100%;border-collapse:collapse;margin-top:4px;}"
                + "table.lines th{background:" + accent + ";color:#fff;text-align:left;padding:6px 8px;"
                + "font-size:9px;text-transform:uppercase;letter-spacing:1px;}"
                + "table.lines td{padding:5px 8px;border-bottom:1px solid #eef1f5;}"
                + "table.lines .num{text-align:right;}"
                + "table.lines .gap{width:18px;border-bottom:none;}"
                + "table.lines tr.totals td{font-weight:bold;border-top:2px solid #d1d5db;border-bottom:none;padding-top:7px;}"
                + ".net{margin-top:18px;padding:10px 14px;background:" + accent + ";color:#fff;"
                + "display:table;width:100%;box-sizing:border-box;}"
                + ".net .t{display:table-cell;font-size:12px;letter-spacing:1px;text-transform:uppercase;}"
                + ".net .v{display:table-cell;text-align:right;font-size:17px;font-weight:bold;}"
                + ".status{display:inline-block;padding:3px 10px;border-radius:10px;font-size:10px;"
                + "font-weight:bold;color:#fff;background:" + accent + ";}"
                + ".footer{margin-top:26px;color:#9ca3af;font-size:9px;border-top:1px solid #e5e7eb;padding-top:8px;}"
                + "</style></head><body>"

                + "<div class=\"header\">"
                + "<div class=\"company\">"
                + (branding.getLogoUrl() != null ? "<img class=\"logo\" src=\"" + escape(branding.getLogoUrl()) + "\"/><br/>" : "")
                + "<div class=\"company-name\">" + escape(branding.getCompanyName()) + "</div>"
                + (companyAddress != null && !companyAddress.isBlank()
                    ? "<div class=\"company-meta\">" + escape(companyAddress) + "</div>" : "")
                + "</div>"
                + "<div class=\"title\"><div class=\"doc-title\">PAYSLIP</div>"
                + "<div class=\"meta\">" + escape(periodLabel(p)) + "</div>"
                + "<div style=\"margin-top:6px;\"><span class=\"status\">"
                + escape(p.getStatus() != null ? p.getStatus().name() : "") + "</span></div>"
                + "</div>"
                + "</div>"

                + "<table class=\"info\"><tr>"
                + infoCell("Employee", employeeName)
                + infoCell("Employee ID", employeeNumber)
                + infoCell("Designation", jobTitle)
                + infoCell("Department", department)
                + "</tr><tr>"
                + infoCell("Pay Period", periodLabel(p))
                + infoCell("Date of Joining", joinDate)
                + infoCell("Payment Method", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                + infoCell("Paid On", p.getPaidAt() != null ? p.getPaidAt().format(DATE_FMT) : null)
                + "</tr></table>"

                + "<table class=\"lines\">"
                + "<thead><tr><th>Earnings</th><th class=\"num\">Amount</th><th class=\"gap\"></th>"
                + "<th>Deductions</th><th class=\"num\">Amount</th></tr></thead>"
                + "<tbody>" + body
                + "<tr class=\"totals\">"
                + "<td>Gross Earnings</td><td class=\"num\">" + money(grossTotal, currency) + "</td>"
                + "<td class=\"gap\"></td>"
                + "<td>Total Deductions</td><td class=\"num\">" + money(deductionTotal, currency) + "</td>"
                + "</tr></tbody></table>"

                + "<div class=\"net\"><div class=\"t\">Net Pay</div>"
                + "<div class=\"v\">" + money(p.getNetSalary(), currency) + "</div></div>"

                + (p.getPaymentReference() != null && !p.getPaymentReference().isBlank()
                    ? "<div style=\"margin-top:12px;\"><span class=\"label\">Payment Reference</span> "
                      + escape(p.getPaymentReference()) + "</div>" : "")

                + (p.getNotes() != null && !p.getNotes().isBlank()
                    ? "<div style=\"margin-top:12px;\"><div class=\"label\">Notes</div><div>"
                      + escape(p.getNotes()) + "</div></div>" : "")

                + "<div class=\"footer\">This is a computer-generated payslip and does not require a signature. "
                + "Generated by " + escape(branding.getCompanyName()) + " on " + LocalDate.now().format(DATE_FMT) + ".</div>"
                + "</body></html>";
    }

    private String infoCell(String label, String value) {
        return "<td><div class=\"label\">" + escape(label) + "</div><div>"
                + (value != null && !value.isBlank() ? escape(value) : "-") + "</div></td>";
    }

    private String periodLabel(Payroll p) {
        int m = p.getPayMonth();
        String month = m >= 1 && m <= 12
                ? java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                : String.valueOf(m);
        return month + " " + p.getPayYear();
    }

    private String displayName(Employee e) {
        if (e == null) return "-";
        if (e.getUser() != null) {
            String full = e.getUser().getFullName();
            if (full != null && !full.isBlank()) return full;
        }
        return e.getEmployeeNumber() != null ? e.getEmployeeNumber() : "Employee #" + e.getId();
    }

    private void addLine(List<String[]> target, String label, BigDecimal amount, String currency) {
        target.add(new String[]{label, money(amount, currency)});
    }

    private boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Itemizes the frozen component earnings/deductions BY NAME (Internet
     * Allowance, Loan Deduction...) when the structure's current extra
     * components still sum to what this payroll froze. If the structure
     * changed since, restating today's names against an old payroll would
     * lie - so it falls back to one honest lump line.
     */
    private void appendComponentLines(java.util.List<String[]> target, Payroll p,
                                      String currency, boolean earningsSide) {
        var type = earningsSide
                ? com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.EARNING
                : com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.DEDUCTION;
        BigDecimal frozen = nz(earningsSide ? p.getOtherEarnings() : p.getOtherDeductions());
        if (frozen.signum() == 0) {
            // Nothing frozen: still print the group at 0.00 so the slip shows the full shape of pay.
            target.add(new String[]{
                    earningsSide ? "Other Allowances (components)" : "Component Deductions",
                    money(BigDecimal.ZERO, currency)});
            return;
        }

        var structure = salaryStructureRepository.findActiveForEmployeeOnDate(
                p.getEmployee().getId(), java.time.LocalDate.of(p.getPayYear(), p.getPayMonth(), 1)).orElse(null);
        var lines = structure == null ? java.util.List.<com.businessos.modules.hrm.payroll.components.StructureExtraComponent>of()
                : extraComponentRepository.findByStructureIdOrderByIdAsc(structure.getId()).stream()
                    .filter(x -> Boolean.TRUE.equals(x.getComponent().getActive()))
                    .filter(x -> x.getComponent().getType() == type)
                    .toList();
        BigDecimal sum = lines.stream().map(x -> nz(x.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(frozen) == 0 && !lines.isEmpty()) {
            for (var x : lines) {
                target.add(new String[]{x.getComponent().getName(), money(x.getAmount(), currency)});
            }
        } else {
            target.add(new String[]{
                    earningsSide ? "Other Allowances (components)" : "Component Deductions",
                    money(frozen, currency)});
        }
    }

    /** Gross is recomputed from the components rather than stored, so it always ties to the lines printed above it. */
    private BigDecimal sum(int ignored, Payroll p) {
        return nz(p.getBasicSalary()).add(nz(p.getHouseRent())).add(nz(p.getMedicalAllowance()))
                .add(nz(p.getTransportAllowance())).add(nz(p.getFoodAllowance()))
                .add(nz(p.getSpecialAllowance())).add(nz(p.getBonus()))
                .add(nz(p.getBillablePay())).add(nz(p.getOvertimePay()))
                .add(nz(p.getOtherEarnings()));
    }

    private BigDecimal totalDeductions(Payroll p) {
        return nz(p.getTaxDeduction()).add(nz(p.getProvidentFundDeduction()))
                .add(nz(p.getInsuranceDeduction())).add(nz(p.getAttendanceDeduction()))
                .add(nz(p.getDeductions())).add(nz(p.getOtherDeductions())).add(nz(p.getLoanDeductionAmount()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String money(BigDecimal amount, String currency) {
        return currency + " " + nz(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trim(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private String sanitise(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
