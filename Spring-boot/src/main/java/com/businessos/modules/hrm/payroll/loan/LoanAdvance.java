package com.businessos.modules.hrm.payroll.loan;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An employee loan or salary advance recovered automatically through payroll.
 * remainingBalance only moves when a payroll actually reaches PAID
 * (PayrollServiceImpl.markPaid) - a DRAFT/APPROVED payroll referencing this
 * loan can still be deleted without needing to reverse anything.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "hrm_loan_advances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanAdvance extends BaseEntity {

    public enum Type { LOAN, ADVANCE }
    public enum Status { ACTIVE, CLOSED, CANCELLED }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Type type = Type.LOAN;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false)
    private LocalDate disbursedDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyInstallment;

    /** Starts equal to principalAmount; decremented as installments are actually paid. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
