package com.businessos.modules.finance.vendor;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A bill received FROM a vendor (Accounts Payable) - the mirror image of ClientInvoice.
 * Approving it recognizes the expense and the liability (Dr Expense / Cr Accounts
 * Payable); paying it settles the liability (Dr AP / Cr Cash).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "vendor_bills", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "bill_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorBill extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "bill_number", nullable = false)
    private String billNumber; // BILL-2026-000001

    // The vendor's own invoice/reference number on their paperwork
    private String vendorReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private LocalDate billDate;
    private LocalDate dueDate;

    // The COA expense account this bill's cost posts to on approval - optional,
    // falls back to the generic Operating Expenses account.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_id")
    private com.businessos.modules.finance.chartofaccounts.ChartOfAccount expenseAccount;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VendorBillStatus status = VendorBillStatus.DRAFT;

    private String description;

    private String createdBy;
    private String approvedBy;
    private LocalDate approvedDate;

    public void calculateTotals() {
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        totalAmount = sub.add(tax);
        balanceAmount = totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
}
