package com.businessos.modules.hrm.employee;

import com.businessos.enums.EmploymentStatus;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateEmployeeRequest {
    @Size(max = 100)
    private String jobTitle;
    private Long designationId;
    private EmploymentType employmentType;
    private EmploymentStatus employmentStatus;
    private Gender gender;
    private LocalDate dateOfBirth;
    @Size(max = 100)
    private String fatherName;
    @Size(max = 100)
    private String motherName;
    private com.businessos.shared.address.AddressRequest location;
    private LocalDate hireDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;
    private Long departmentId;
    private Long reportingManagerId;
    private Long shiftId;
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal billableRate;
    @Size(max = 100)
    private String bankName;
    @Size(max = 100)
    private String bankAccountNumber;
    /** 9 digits: bank 3 + district 2 + branch 3 + check 1. See CreateEmployeeRequest. */
    @Pattern(regexp = "^$|^[0-9]{9}$",
             message = "Routing number must be exactly 9 digits")
    private String bankRoutingNumber;
    @Size(max = 100)
    private String emergencyContactName;
    @Size(max = 30)
    private String emergencyContactPhone;
    @Size(max = 50)
    private String emergencyContactRelation;
    @Size(max = 50)
    private String nationalId;
    @Size(max = 50)
    private String taxId;
    @Size(max = 100)
    private String costCenter;
    @Size(max = 150)
    private String officeLocation;
    @Size(max = 30)
    private String workPhone;
    @Email
    @Size(max = 255)
    private String officialEmail;
    @Size(max = 500)
    private String profileImageUrl;
}
