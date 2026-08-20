package com.businessos.modules.finance.vendor;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorBillService {

    private final VendorBillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository coaRepository;

    private ChartOfAccount resolveExpenseAccount(Long companyId, Long accountId) {
        if (accountId == null) return null;
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense account not found: " + accountId));
        if (account.getType() != com.businessos.modules.finance.chartofaccounts.AccountType.EXPENSE) {
            throw new BadRequestException("Account " + account.getAccountCode() + " is " + account.getType()
                    + " - vendor bills must post to an EXPENSE account");
        }
        return account;
    }

    /** The account this bill's cost posts to - its linked account, or generic Operating Expenses. */
    private ChartOfAccount expenseAccountFor(VendorBill bill) {
        return bill.getExpenseAccount() != null
                ? bill.getExpenseAccount()
                : accountResolver.operatingExpenses(bill.getCompanyId());
    }

    @Transactional
    public VendorBillDtos.VendorBillResponse create(VendorBillDtos.VendorBillRequest request) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_CREATE);
        Long companyId = requireCompanyId();
        Vendor vendor = vendorRepository.findByIdAndCompanyId(request.getVendorId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + request.getVendorId()));

        VendorBill bill = VendorBill.builder()
                .companyId(companyId)
                .billNumber(generateBillNumber(companyId))
                .vendorReference(request.getVendorReference())
                .vendor(vendor)
                .billDate(request.getBillDate())
                .dueDate(request.getDueDate())
                .expenseAccount(resolveExpenseAccount(companyId, request.getExpenseAccountId()))
                .subtotal(request.getSubtotal())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .description(request.getDescription())
                .status(VendorBillStatus.DRAFT)
                .createdBy(securityUtil.getCurrentUser().getUsername())
                .build();
        bill.calculateTotals();
        bill = billRepository.save(bill);
        return VendorBillDtos.toResponse(bill);
    }

    /**
     * Approving a bill is when the expense and the liability become real:
     * Dr Operating Expenses (full total incl. tax - input tax credit handling is out of
     * scope) / Cr Accounts Payable, dated the bill's own date. Maker-checker enforced.
     */
    @Transactional
    public VendorBillDtos.VendorBillResponse approve(Long id) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_APPROVE);
        VendorBill bill = findInTenant(id);
        if (bill.getStatus() != VendorBillStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT bills can be approved");
        }
        String approver = securityUtil.getCurrentUser().getUsername();
        if (approver != null && approver.equalsIgnoreCase(bill.getCreatedBy())) {
            throw new BadRequestException("You entered this bill - a different user must approve it");
        }

        bill.setStatus(VendorBillStatus.APPROVED);
        bill.setApprovedBy(approver);
        bill.setApprovedDate(LocalDate.now());
        bill = billRepository.save(bill);

        Long companyId = bill.getCompanyId();
        String description = "Vendor bill " + bill.getBillNumber() + " from " + bill.getVendor().getName();
        LocalDate transactionDate = bill.getBillDate() != null ? bill.getBillDate() : LocalDate.now();
        ChartOfAccount expense = expenseAccountFor(bill);
        ChartOfAccount ap = accountResolver.accountsPayable(companyId);
        glService.recordBalancedTransaction(companyId, List.of(
                        LedgerLine.debit(expense.getId(), bill.getTotalAmount()),
                        LedgerLine.credit(ap.getId(), bill.getTotalAmount())),
                description, GlReferenceType.VENDOR_BILL, bill.getId(), bill.getBillNumber(), transactionDate);

        return VendorBillDtos.toResponse(bill);
    }

    /** Paying (part of) an approved bill: Dr Accounts Payable / Cr Cash. */
    @Transactional
    public VendorBillDtos.VendorBillResponse recordPayment(Long id, BigDecimal amount) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_PAYMENT);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be positive");
        }
        VendorBill bill = findInTenant(id);
        if (bill.getStatus() != VendorBillStatus.APPROVED && bill.getStatus() != VendorBillStatus.PARTIALLY_PAID
                && bill.getStatus() != VendorBillStatus.OVERDUE) {
            throw new BadRequestException("Only approved bills can be paid");
        }
        BigDecimal outstanding = bill.getTotalAmount().subtract(bill.getPaidAmount());
        if (amount.compareTo(outstanding) > 0) {
            throw new BadRequestException("Payment amount exceeds the outstanding balance (" + outstanding + ")");
        }

        bill.setPaidAmount(bill.getPaidAmount().add(amount));
        bill.setStatus(bill.getPaidAmount().compareTo(bill.getTotalAmount()) >= 0
                ? VendorBillStatus.PAID : VendorBillStatus.PARTIALLY_PAID);
        bill.calculateTotals();
        bill = billRepository.save(bill);

        Long companyId = bill.getCompanyId();
        String description = "Payment on vendor bill " + bill.getBillNumber() + " to " + bill.getVendor().getName();
        ChartOfAccount ap = accountResolver.accountsPayable(companyId);
        ChartOfAccount cash = accountResolver.cash(companyId);
        glService.recordBalancedTransaction(companyId, List.of(
                        LedgerLine.debit(ap.getId(), amount),
                        LedgerLine.credit(cash.getId(), amount)),
                description, GlReferenceType.VENDOR_BILL_PAYMENT, bill.getId(), bill.getBillNumber(), LocalDate.now());

        return VendorBillDtos.toResponse(bill);
    }

    /** Cancelling an approved bill reverses its expense/liability posting; DRAFT just flips. */
    @Transactional
    public VendorBillDtos.VendorBillResponse cancel(Long id) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_CANCEL);
        VendorBill bill = findInTenant(id);
        if (bill.getStatus() == VendorBillStatus.CANCELLED) {
            throw new BadRequestException("Bill is already cancelled");
        }
        if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot cancel a bill that already has payments recorded against it");
        }

        boolean wasPosted = bill.getStatus() == VendorBillStatus.APPROVED;
        bill.setStatus(VendorBillStatus.CANCELLED);
        bill = billRepository.save(bill);

        if (wasPosted) {
            Long companyId = bill.getCompanyId();
            String description = "Vendor bill " + bill.getBillNumber() + " cancelled - reversal";
            // Must reverse against the same account the approval posted to.
            ChartOfAccount expense = expenseAccountFor(bill);
            ChartOfAccount ap = accountResolver.accountsPayable(companyId);
            glService.recordBalancedTransaction(companyId, List.of(
                            LedgerLine.credit(expense.getId(), bill.getTotalAmount()),
                            LedgerLine.debit(ap.getId(), bill.getTotalAmount())),
                    description, GlReferenceType.VENDOR_BILL, bill.getId(), bill.getBillNumber(), LocalDate.now());
        }
        return VendorBillDtos.toResponse(bill);
    }

    @Transactional(readOnly = true)
    public Page<VendorBillDtos.VendorBillResponse> list(VendorBillStatus status, Long vendorId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_VIEW);
        Long companyId = requireCompanyId();
        Page<VendorBill> page;
        if (vendorId != null) {
            page = billRepository.findByCompanyIdAndVendorId(companyId, vendorId, pageable);
        } else if (status != null) {
            page = billRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        } else {
            page = billRepository.findByCompanyId(companyId, pageable);
        }
        return page.map(VendorBillDtos::toResponse);
    }

    /** AP ageing: how much we owe each vendor, bucketed by how overdue it is - the mirror of AR ageing. */
    @Transactional(readOnly = true)
    public VendorBillDtos.ApAgeingReport apAgeing(LocalDate asOfDate) {
        authorizationService.checkPermission(PermissionCode.VENDOR_BILL_VIEW);
        Long companyId = requireCompanyId();
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();

        List<VendorBill> outstanding = billRepository.findOutstandingByCompanyId(companyId,
                List.of(VendorBillStatus.APPROVED, VendorBillStatus.PARTIALLY_PAID, VendorBillStatus.OVERDUE));

        BigDecimal current = BigDecimal.ZERO, d1to30 = BigDecimal.ZERO, d31to60 = BigDecimal.ZERO,
                d61to90 = BigDecimal.ZERO, over90 = BigDecimal.ZERO;
        List<VendorBillDtos.ApAgeingLine> lines = new ArrayList<>();

        for (VendorBill bill : outstanding) {
            long daysOverdue = bill.getDueDate() != null ? ChronoUnit.DAYS.between(bill.getDueDate(), asOf) : 0;
            BigDecimal balance = bill.getBalanceAmount();
            String bucket;
            if (daysOverdue <= 0) { bucket = "CURRENT"; current = current.add(balance); }
            else if (daysOverdue <= 30) { bucket = "1-30"; d1to30 = d1to30.add(balance); }
            else if (daysOverdue <= 60) { bucket = "31-60"; d31to60 = d31to60.add(balance); }
            else if (daysOverdue <= 90) { bucket = "61-90"; d61to90 = d61to90.add(balance); }
            else { bucket = "90+"; over90 = over90.add(balance); }

            lines.add(VendorBillDtos.ApAgeingLine.builder()
                    .billId(bill.getId())
                    .billNumber(bill.getBillNumber())
                    .vendorId(bill.getVendor() != null ? bill.getVendor().getId() : null)
                    .vendorName(bill.getVendor() != null ? bill.getVendor().getName() : null)
                    .dueDate(bill.getDueDate())
                    .balanceAmount(balance)
                    .daysOverdue(daysOverdue)
                    .bucket(bucket)
                    .build());
        }

        return VendorBillDtos.ApAgeingReport.builder()
                .asOfDate(asOf)
                .current(current)
                .days1to30(d1to30)
                .days31to60(d31to60)
                .days61to90(d61to90)
                .over90(over90)
                .totalOutstanding(current.add(d1to30).add(d31to60).add(d61to90).add(over90))
                .lines(lines)
                .build();
    }

    private String generateBillNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "BILL-" + year + "-";
        String maxNumber = billRepository.findMaxBillNumberByCompanyAndPrefix(companyId, prefix)
                .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }

    private VendorBill findInTenant(Long id) {
        return billRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
