package com.businessos.modules.hrm.employee;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.salary.SalaryStructure;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.designation.Designation;
import com.businessos.modules.hrm.attendance.shift.Shift;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.enums.EmploymentStatus;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "employee_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Column(name = "employee_number")
    private String employeeNumber;
    private String officialEmail;
    private String workPhone;
    private String profileImageUrl;
    private String nationalId;
    private String taxId;
    private String jobTitle;
    private String costCenter;
    private String officeLocation;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.PROBATION;

    // personal information
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate dateOfBirth;
    
    @Column(length = 100)
    private String fatherName;
    
    @Column(length = 100)
    private String motherName;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private com.businessos.shared.address.Address location;
    
    private LocalDate hireDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate contractEndDate;

    // Set once ContractExpiryScheduler notifies HR this contract is ending soon;
    // reset to null whenever contractEndDate itself changes (extended/cleared),
    // so a renewed contract gets its own future reminder instead of staying
    // permanently suppressed.
    private java.time.LocalDateTime contractEndReminderSentAt;

    // Salary
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;

    // Fixed pay rate for approved billable timesheet hours - added on top of the
    // salary components above when payroll is generated for a period.
    private BigDecimal billableRate;

    // Bank
    private String bankName;
    private String bankAccountNumber;
    private String bankRoutingNumber;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    @Builder.Default
    private boolean active = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_structure_id")
    private SalaryStructure salaryStructure;

    public String getFullName() {
        if (user != null) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            return (first + " " + last).trim();
        }
        return employeeNumber != null ? employeeNumber : String.valueOf(getId());
    }
}
