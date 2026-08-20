package com.businessos.modules.hrm.salary;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
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
@Table(name = "salary_structures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal houseRent = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    @Builder.Default


    @Column(precision = 12, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Builder.Default


    @Column(precision = 12, scale = 2)
    private BigDecimal foodAllowance = BigDecimal.ZERO;

    @Builder.Default


    @Column(precision = 12, scale = 2)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Builder.Default


    @Column(precision = 12, scale = 2)
    private BigDecimal providentFund = BigDecimal.ZERO;

    @Builder.Default


    @Column(precision = 12, scale = 2)
    private BigDecimal taxDeduction = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;
    public BigDecimal computeNetSalary() {
        BigDecimal net = grossSalary != null ? grossSalary : BigDecimal.ZERO;
        if (providentFund != null) net = net.subtract(providentFund);
        if (taxDeduction != null) net = net.subtract(taxDeduction);
        return net;
    }
}
