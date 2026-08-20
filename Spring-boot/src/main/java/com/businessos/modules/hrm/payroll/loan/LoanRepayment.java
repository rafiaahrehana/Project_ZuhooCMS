package com.businessos.modules.hrm.payroll.loan;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.payroll.Payroll;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One installment actually recovered - written when the payroll it rode on reaches PAID. */
@Entity
@Table(name = "hrm_loan_repayments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanRepayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_advance_id", nullable = false)
    private LoanAdvance loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    private Payroll payroll;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paidDate;

    /** Balance immediately after this installment - saves recomputing history to show a running balance. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;
}
