package com.businessos.modules.finance.payment;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.modules.finance.invoice.ClientInvoiceService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReceiptServiceImpl implements PaymentReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final ClientInvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtil securityUtil;
    private final ClientInvoiceService clientInvoiceService;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final AuthorizationService authorizationService;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentReceiptResponse create(PaymentReceiptRequest request) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();

        Client client = clientRepository.findByIdAndCompanyId(request.getClientId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ClientInvoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndCompanyId(request.getInvoiceId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        }

        String receiptNumber = generateReceiptNumber(companyId);

        PaymentReceipt receipt = PaymentReceipt.builder()
                .companyId(companyId)
                .receiptNumber(receiptNumber)
                .invoice(invoice)
                .client(client)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .status(PaymentStatus.PENDING)
                .notes(request.getNotes())
                .build();

        receipt = receiptRepository.save(receipt);
        return PaymentReceiptMapper.toResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReceiptResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_VIEW);
        return PaymentReceiptMapper.toResponse(findInTenant(id));
    }

    private PaymentReceipt findInTenant(Long id) {
        return receiptRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment receipt not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentReceiptResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return receiptRepository.findByCompanyId(companyId, pageable)
                .map(PaymentReceiptMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentReceiptResponse> getMyReceipts(Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        Long userId = securityUtil.getCurrentUser().getId();
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client profile not found"));
        return receiptRepository.findByCompanyIdAndClientId(companyId, client.getId(), pageable)
                .map(PaymentReceiptMapper::toResponse);
    }

    @Override
    @Transactional
    public void confirmPayment(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_CONFIRM);
        PaymentReceipt receipt = findInTenant(id);
        if (receipt.getStatus() == PaymentStatus.CONFIRMED || receipt.getStatus() == PaymentStatus.DEPOSITED) {
            throw new BadRequestException("Payment receipt is already confirmed");
        }
        // Previously this ran unconditionally before the invoice-status check
        // below, so confirming against a CANCELLED invoice still flipped the
        // receipt to CONFIRMED while silently skipping both the invoice update
        // and the GL posting - staff saw "Confirmed" and reasonably believed the
        // cash was recorded, when it wasn't.
        if (receipt.getInvoice() != null) {
            ClientInvoice linkedInvoice = invoiceRepository
                    .findByIdAndCompanyId(receipt.getInvoice().getId(), receipt.getCompanyId()).orElse(null);
            if (linkedInvoice != null && linkedInvoice.getStatus() == com.businessos.enums.InvoiceStatus.CANCELLED) {
                throw new BadRequestException(
                        "Cannot confirm this payment: the linked invoice has been cancelled.");
            }
        }
        receipt.confirmPayment();
        receiptRepository.save(receipt);

        // Previously confirming a receipt only flipped its own status - it never
        // touched the invoice it was recorded against (paidAmount/status stayed
        // stale) and never hit the General Ledger, so invoices could sit "ISSUED"
        // forever even after being fully paid, and revenue/cash never showed in
        // Finance reports.
        if (receipt.getInvoice() != null) {
            ClientInvoice invoice = invoiceRepository.findByIdAndCompanyId(receipt.getInvoice().getId(), receipt.getCompanyId()).orElse(null);
            if (invoice != null && invoice.getStatus() != com.businessos.enums.InvoiceStatus.CANCELLED) {
                clientInvoiceService.recordPaymentForCompany(receipt.getCompanyId(), receipt.getInvoice().getId(),
                        receipt.getAmount(), receipt.getPaymentDate() != null ? receipt.getPaymentDate() : LocalDate.now());
            }
        } else {
            String description = "Payment received (receipt " + receipt.getReceiptNumber() + ")";
            ChartOfAccount cash = accountResolver.cash(receipt.getCompanyId());
            ChartOfAccount ar = accountResolver.accountsReceivable(receipt.getCompanyId());
            glService.recordBalancedTransaction(receipt.getCompanyId(), List.of(
                            LedgerLine.debit(cash.getId(), receipt.getAmount()),
                            LedgerLine.credit(ar.getId(), receipt.getAmount())),
                    description, GlReferenceType.PAYMENT_RECEIPT, receipt.getId(), receipt.getReceiptNumber(),
                    receipt.getPaymentDate() != null ? receipt.getPaymentDate() : LocalDate.now());
        }

        notifyPaymentReceived(receipt);
    }

    // NotificationType.PAYMENT_RECEIVED existed but was never used anywhere -
    // overdue invoices correctly notified the owner; a payment actually
    // arriving notified no one.
    private void notifyPaymentReceived(PaymentReceipt receipt) {
        Company company = companyRepository.findById(receipt.getCompanyId()).orElse(null);
        if (company == null || company.getOwner() == null) return;
        notificationService.send(com.businessos.shared.notification.CreateNotificationRequest.of(
                com.businessos.enums.NotificationType.PAYMENT_RECEIVED,
                "Payment received",
                "Payment of " + receipt.getAmount() + " (receipt " + receipt.getReceiptNumber() + ") has been confirmed.",
                "/finance/payments",
                company.getOwner().getId(),
                company.getId()));
    }

    @Override
    @Transactional
    public void reverse(Long id, String reason) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_CONFIRM);
        PaymentReceipt receipt = findInTenant(id);
        if (receipt.getStatus() != PaymentStatus.CONFIRMED && receipt.getStatus() != PaymentStatus.DEPOSITED) {
            throw new BadRequestException("Only a confirmed or deposited payment can be reversed");
        }

        Long companyId = receipt.getCompanyId();
        String description = "Payment reversal (receipt " + receipt.getReceiptNumber() + ")"
                + (reason != null && !reason.isBlank() ? " - " + reason : "");

        // Exact mirror of what confirmPayment posted: the cash goes back out of the
        // books and the receivable is owed again. Foreign-currency invoice payments
        // convert at the invoice's issue-time rate, same as the original posting.
        BigDecimal amountBase = receipt.getAmount();
        if (receipt.getInvoice() != null && receipt.getInvoice().getExchangeRate() != null
                && receipt.getInvoice().getExchangeRate().compareTo(BigDecimal.ONE) != 0) {
            amountBase = receipt.getAmount().multiply(receipt.getInvoice().getExchangeRate())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        ChartOfAccount cash = accountResolver.cash(companyId);
        ChartOfAccount ar = accountResolver.accountsReceivable(companyId);
        glService.recordBalancedTransaction(companyId, List.of(
                        LedgerLine.credit(cash.getId(), amountBase),
                        LedgerLine.debit(ar.getId(), amountBase)),
                description, GlReferenceType.PAYMENT_REVERSAL, receipt.getId(), receipt.getReceiptNumber(), LocalDate.now());

        // Restore the linked invoice: the client owes this money again.
        if (receipt.getInvoice() != null) {
            ClientInvoice invoice = invoiceRepository
                    .findByIdAndCompanyId(receipt.getInvoice().getId(), companyId).orElse(null);
            if (invoice != null) {
                BigDecimal newPaid = invoice.getPaidAmount().subtract(receipt.getAmount()).max(BigDecimal.ZERO);
                invoice.setPaidAmount(newPaid);
                if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                    invoice.setStatus(com.businessos.enums.InvoiceStatus.PARTIALLY_PAID);
                } else if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())) {
                    invoice.setStatus(com.businessos.enums.InvoiceStatus.OVERDUE);
                } else {
                    invoice.setStatus(com.businessos.enums.InvoiceStatus.ISSUED);
                }
                invoice.setPaidDate(null);
                invoice.calculateTotals();
                invoiceRepository.save(invoice);
            }
        }

        receipt.setStatus(PaymentStatus.REVERSED);
        receipt.setReversedDate(LocalDate.now());
        receipt.setReversalReason(reason);
        receiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void markAsDeposited(Long id, String bank) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_CONFIRM);
        PaymentReceipt receipt = findInTenant(id);
        receipt.markAsDeposited(bank);
        receiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYMENT_RECEIPT_DELETE);
        PaymentReceipt receipt = findInTenant(id);
        // A confirmed/deposited receipt has already posted a balanced GL entry
        // and, if linked to an invoice, moved its paidAmount/status - deleting
        // it outright would leave those orphaned with no way back. reverse()
        // exists specifically to unwind both correctly; only an unconfirmed
        // (PENDING) receipt can be deleted outright.
        if (receipt.getStatus() == PaymentStatus.CONFIRMED || receipt.getStatus() == PaymentStatus.DEPOSITED) {
            throw new BadRequestException(
                    "Cannot delete a " + receipt.getStatus() + " payment receipt - reverse it instead");
        }
        receipt.softDelete();
        receiptRepository.save(receipt);
    }

    private String generateReceiptNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "RCP-" + year + "-";
        String maxNumber = receiptRepository
                .findMaxReceiptNumberByCompanyAndPrefix(companyId, prefix)
                .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }
}
