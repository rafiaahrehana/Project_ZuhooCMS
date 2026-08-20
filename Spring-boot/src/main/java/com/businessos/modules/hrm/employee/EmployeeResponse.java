package com.businessos.modules.hrm.employee;

import com.businessos.enums.EmploymentStatus;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmployeeResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String image;
    private String employeeNumber;
    private String officialEmail;
    private String workPhone;
    private String profileImageUrl;
    private String nationalId;
    private String taxId;
    private String costCenter;
    private String officeLocation;
    private String jobTitle;
    private EmploymentType employmentType;
    private EmploymentStatus employmentStatus;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String fatherName;
    private String motherName;
    private com.businessos.shared.address.AddressResponse location;
    private LocalDate hireDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;
    private Long reportingManagerId;
    private String reportingManagerName;
    private Long shiftId;
    private String shiftName;
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal billableRate;
    private String bankName;
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private boolean active;
    private LocalDateTime createdAt;
    private Long customRoleId;
    private String customRoleName;
}
