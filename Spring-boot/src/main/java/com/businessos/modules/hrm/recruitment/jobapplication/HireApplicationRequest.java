package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.enums.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HR-supplied onboarding details for hiring an OFFERED candidate.
 * Applicant name/email/phone come from the JobApplication itself; everything
 * here is what the application doesn't already know (portal password, salary,
 * org placement) — same split CreateEmployeeRequest uses for a manual hire.
 */
@Data
public class HireApplicationRequest {
    @NotBlank(message = "Password is required")
    @Size(min = 8)
    private String password;

    @Size(max = 255)
    private String officialEmail;

    private Long departmentId;
    private Long designationId;
    private Long reportingManagerId;
    private Long shiftId;

    /** Falls back to the job posting's employment type if not set. */
    private EmploymentType employmentType;

    /** Falls back to today if not set. */
    private LocalDate hireDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;

    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;

    @Size(max = 100)
    private String bankName;
    @Size(max = 100)
    private String bankAccountNumber;
    @Size(max = 100)
    private String emergencyContactName;
    @Size(max = 30)
    private String emergencyContactPhone;
    @Size(max = 50)
    private String emergencyContactRelation;
}
