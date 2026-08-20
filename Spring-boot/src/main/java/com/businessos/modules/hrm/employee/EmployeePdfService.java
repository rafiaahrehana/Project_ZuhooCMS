package com.businessos.modules.hrm.employee;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.EmploymentStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyMapper;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.pdf.SimpleTablePdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeePdfService {

    private static final List<String> HEADERS =
            List.of("ID", "Name", "Email", "Phone", "Designation", "Role", "Department", "Hire Date");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final EmployeeService employeeService;
    private final SimpleTablePdfRenderer pdfRenderer;
    private final CompanyRepository companyRepository;
    private final EmailBranding emailBranding;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public byte[] generateListPdf(Long departmentId, EmploymentStatus status, String search, boolean excludeOwner) {
        authorizationService.checkPermission(PermissionCode.EMPLOYEE_VIEW);
        List<EmployeeResponse> employees = employeeService.listAll(
                departmentId, status, search, excludeOwner,
                PageRequest.of(0, 5000, Sort.by("createdAt").descending())
        ).getContent();

        List<List<String>> rows = new ArrayList<>();
        for (EmployeeResponse e : employees) {
            rows.add(toRow(e));
        }

        Company company = resolveCompany();
        EmailBranding.Data branding = emailBranding.from(company);
        String location = company != null ? CompanyMapper.formatAddress(company.getLocationDetail()) : null;
        String email = company != null ? company.getCompanyEmail() : null;

        return pdfRenderer.render("Employees", employees.size() + " employee(s)", HEADERS, rows,
                branding.getCompanyName(), branding.getLogoUrl(), email, location);
    }

    private Company resolveCompany() {
        Long companyId = securityUtil.getCurrentCompanyId();
        return companyId != null ? companyRepository.findById(companyId).orElse(null) : null;
    }

    private List<String> toRow(EmployeeResponse e) {
        String name = ((e.getFirstName() != null ? e.getFirstName() : "") + " "
                + (e.getLastName() != null ? e.getLastName() : "")).trim();
        String phone = e.getWorkPhone() != null && !e.getWorkPhone().isBlank() ? e.getWorkPhone() : e.getPhone();
        String role = e.getCustomRoleName() != null && !e.getCustomRoleName().isBlank() ? e.getCustomRoleName() : "Employee";
        return List.of(
                orDash(e.getEmployeeNumber()),
                orDash(name),
                orDash(e.getEmail()),
                orDash(phone),
                orDash(e.getDesignationName()),
                orDash(role),
                orDash(e.getDepartmentName()),
                e.getHireDate() != null ? e.getHireDate().format(DATE_FMT) : "-"
        );
    }

    private String orDash(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}
