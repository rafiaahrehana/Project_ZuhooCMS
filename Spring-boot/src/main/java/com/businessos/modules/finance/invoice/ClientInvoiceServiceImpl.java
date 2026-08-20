package com.businessos.modules.finance.invoice;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.InvoiceSummaryPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.company.Company;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.businessos.enums.InvoiceStatus;
import com.businessos.enums.NotificationType;
import com.businessos.enums.RefundStatus;

@Service
@RequiredArgsConstructor
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private final ClientInvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtil securityUtil;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final AuthorizationService authorizationService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final RefundRepository refundRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final NotificationService notificationService;
    private final InvoicePdfService invoicePdfService;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new com.businessos.shared.exception.BadRequestException("No company context");
        return id;
    }

    private ClientInvoice findInTenant(Long id) {
        return invoiceRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new com.businessos.shared.exception.ResourceNotFoundException("Invoice not found: " + id));
    }

    @Override
    @Transactional
    public ClientInvoiceResponse create(ClientInvoiceRequest request) {
        authorizationService.checkPermission(PermissionCode.INVOICE_CREATE);
        return createInternal(securityUtil.getCurrentCompanyId(), request);
    }

    @Override
    @Transactional
    public ClientInvoiceResponse createForServiceRequest(Long companyId, ClientInvoiceRequest request) {
        return createInternal(companyId, request);
    }

    // Shared by the staff-facing create() (INVOICE_CREATE-gated) and
    // createForServiceRequest() - a client submitting their own paid service request
    // triggers this as a side effect of an action they're already authorized to take,
    // not a direct "create an invoice" call, so it must not require staff's INVOICE_CREATE.
    private ClientInvoiceResponse createInternal(Long companyId, ClientInvoiceRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency().trim().toUpperCase() : "BDT";
        BigDecimal exchangeRate = resolveExchangeRate(client, currency, request.getExchangeRate());

        String invoiceNumber = generateInvoiceNumber(companyId);

        ClientInvoice invoice = ClientInvoice.builder()
                .companyId(companyId)
                .invoiceNumber(invoiceNumber)
                .client(client)
                .serviceRequest(request.getServiceRequestId() != null
                        ? serviceRequestRepository.getReferenceById(request.getServiceRequestId())
                        : null)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .taxRatePercent(request.getTaxRatePercent())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .currency(currency)
                .exchangeRate(exchangeRate)
                .paymentTerms(request.getPaymentTerms())
                .description(request.getDescription())
                .notes(request.getNotes())
                .status(InvoiceStatus.DRAFT)
                .build();

        // Add items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ClientInvoiceItem> items = request.getItems().stream()
                    .map(itemRequest -> {
                        ClientInvoiceItem item = ClientInvoiceItem.builder()
                                .invoice(invoice)
                                .description(itemRequest.getDescription())
                                .quantity(itemRequest.getQuantity())
                                .unitPrice(itemRequest.getUnitPrice())
                                .notes(itemRequest.getNotes())
                                .build();
                        item.calculateLineTotal();
                        return item;
                    })
                    .collect(Collectors.toList());
            invoice.setItems(items);
        }

        invoice.calculateTotals();
        ClientInvoice savedInvoice = invoiceRepository.save(invoice);

        return ClientInvoiceMapper.toResponse(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientInvoiceResponse getById(Long id) {
        // Same shape as generatePdf() below: staff need INVOICE_VIEW and can read any invoice in
        // the tenant; a client has no such permission and is narrowed to their own invoice
        // instead. Without this the client app can't open an invoice it is already allowed to
        // download as a PDF, and has to scan /me to find one it already has the id for.
        ClientInvoice invoice = findInTenant(id);
        if (!authorizationService.hasPermission(PermissionCode.INVOICE_VIEW)) {
            requireOwnInvoice(invoice);
        }
        return ClientInvoiceMapper.toResponse(invoice);
    }

    // No @Transactional here on purpose: the reads run inside aiTx.load(), which
    // commits before the provider call so no DB connection is held across it -
    // see AiTransactionBoundary. The lazy client/user/serviceRequest/items
    // associations must all be read inside the callback.
    @Override
    public InvoiceSummaryDraftResponse draftSummaryWithAi(Long id) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);

        String prompt = aiTx.load(() -> {
            ClientInvoice invoice = findInTenant(id);

            String clientName = invoice.getClient() != null && invoice.getClient().getClientCompanyName() != null
                ? invoice.getClient().getClientCompanyName()
                : (invoice.getClient() != null && invoice.getClient().getUser() != null
                    ? invoice.getClient().getUser().getFullName() : "Client");
            String serviceName = invoice.getServiceRequest() != null ? invoice.getServiceRequest().getTitle()
                : invoice.getItems() != null && !invoice.getItems().isEmpty() ? invoice.getItems().get(0).getDescription()
                : "Services rendered";

            return InvoiceSummaryPromptBuilder.builder()
                .setClientName(clientName)
                .setServiceName(serviceName)
                .setAmount(invoice.getTotalAmount())
                .setPeriod(invoice.getInvoiceDate() + " to " + invoice.getDueDate())
                .build();
        });

        InvoiceSummaryDraftResponse response = new InvoiceSummaryDraftResponse();
        response.setSummary(aiService.generateRaw(AiFeature.INVOICE_SUMMARY, prompt));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        ClientInvoice invoice = findInTenant(id);
        if (!authorizationService.hasPermission(PermissionCode.INVOICE_VIEW)) {
            requireOwnInvoice(invoice);
        }
        Company company = invoice.getClient() != null ? invoice.getClient().getCompany() : null;
        EmailBranding.Data branding = emailBranding.from(company);
        return invoicePdfService.generate(invoice, company, branding);
    }

    private void requireOwnInvoice(ClientInvoice invoice) {
        var currentUser = securityUtil.getCurrentUser();
        Client myClient = currentUser != null
                ? clientRepository.findByUserId(currentUser.getId()).orElse(null)
                : null;
        if (myClient == null || invoice.getClient() == null
                || !invoice.getClient().getId().equals(myClient.getId())) {
            throw new ForbiddenException("Access denied: you can only download your own invoices");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClientInvoiceResponse getByInvoiceNumber(String number) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        ClientInvoice invoice = invoiceRepository.findByCompanyIdAndInvoiceNumber(requireCompanyId(), number)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return ClientInvoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientInvoiceResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return invoiceRepository.findByCompanyId(companyId, pageable)
                .map(ClientInvoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientInvoiceResponse> getByStatus(InvoiceStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return invoiceRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                .map(ClientInvoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientInvoiceResponse> getByClient(Long clientId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        return findByClientUnchecked(clientId, pageable);
    }

    private Page<ClientInvoiceResponse> findByClientUnchecked(Long clientId, Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        return invoiceRepository.findByCompanyIdAndClientId(companyId, clientId, pageable)
                .map(ClientInvoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientInvoiceResponse> getMyInvoices(Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        Long userId = securityUtil.getCurrentUser().getId();
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client profile not found"));
        Page<ClientInvoiceResponse> page = findByClientUnchecked(client.getId(), pageable);

        // Surface the latest refund's status (if any) so the client can see
        // "Refund Requested" / "Refund Rejected" without a separate call - a
        // processed refund is already visible via the invoice's own REFUNDED status.
        List<Long> invoiceIds = page.getContent().stream()
                .map(ClientInvoiceResponse::getId)
                .collect(Collectors.toList());
        if (!invoiceIds.isEmpty()) {
            Map<Long, RefundStatus> latestByInvoice = new HashMap<>();
            for (Refund r : refundRepository.findByClientInvoiceIdInAndCompanyIdOrderByCreatedAtDesc(invoiceIds, companyId)) {
                latestByInvoice.putIfAbsent(r.getClientInvoice().getId(), r.getStatus());
            }
            page.getContent().forEach(inv -> inv.setRefundStatus(latestByInvoice.get(inv.getId())));
        }

        return page;
    }

    @Override
    @Transactional
    public ClientInvoiceResponse update(Long id, ClientInvoiceRequest request) {
        authorizationService.checkPermission(PermissionCode.INVOICE_UPDATE);
        ClientInvoice invoice = findInTenant(id);  // tenant-scoped

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new com.businessos.shared.exception.BadRequestException("Only DRAFT invoices can be updated");
        }

        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setTaxAmount(request.getTaxAmount());
        invoice.setTaxRatePercent(request.getTaxRatePercent());
        invoice.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            String currency = request.getCurrency().trim().toUpperCase();
            invoice.setCurrency(currency);
            invoice.setExchangeRate(resolveExchangeRate(invoice.getClient(), currency, request.getExchangeRate()));
        }
        invoice.setPaymentTerms(request.getPaymentTerms());
        invoice.setDescription(request.getDescription());
        invoice.setNotes(request.getNotes());

        if (request.getItems() != null) {
            invoice.getItems().clear();
            final ClientInvoice invoiceRef = invoice;
            List<ClientInvoiceItem> items = request.getItems().stream()
                    .map(itemRequest -> {
                        ClientInvoiceItem item = ClientInvoiceItem.builder()
                                .invoice(invoiceRef)
                                .description(itemRequest.getDescription())
                                .quantity(itemRequest.getQuantity())
                                .unitPrice(itemRequest.getUnitPrice())
                                .notes(itemRequest.getNotes())
                                .build();
                        item.calculateLineTotal();
                        return item;
                    })
                    .collect(Collectors.toList());
            invoice.getItems().addAll(items);
        }

        invoice.calculateTotals();
        invoice = invoiceRepository.save(invoice);

        return ClientInvoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public void sendInvoice(Long id) {
        authorizationService.checkPermission(PermissionCode.INVOICE_SEND);
        sendInvoiceInternal(id);
    }

    @Override
    @Transactional
    public void sendInvoiceForServiceRequest(Long id) {
        sendInvoiceInternal(id);
    }

    private void sendInvoiceInternal(Long id) {
        ClientInvoice invoice = findInTenant(id);  // tenant-scoped
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setSentDate(LocalDate.now());
        invoiceRepository.save(invoice);

        postInvoiceToLedger(invoice);

        // "Send Invoice" previously only flipped a status flag - the client was
        // never actually told an invoice existed.
        try {
            Client client = invoice.getClient();
            if (client != null && client.getUser() != null && client.getCompany() != null) {
                EmailBranding.Data branding = emailBranding.from(client.getCompany());
                emailService.sendInvoiceEmail(client.getUser().getEmail(), client.getUser().getFirstName(), branding);
            }
        } catch (Exception ex) {
            // Email failure must not roll back the status change and GL posting.
            // Log and continue - the invoice is already legally issued.
        }
    }

    /**
     * subtotal - discountAmount, floored at zero - what's actually recognized as
     * revenue once a discount is applied. totalAmount = this + taxAmount, so using
     * it (instead of raw subtotal) keeps every GL posting below balanced.
     */
    private BigDecimal recognizedRevenue(ClientInvoice invoice) {
        BigDecimal discount = invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO;
        return invoice.getSubtotal().subtract(discount).max(BigDecimal.ZERO);
    }

    /** Rate = 1 for base-currency invoices; required from the request otherwise. */
    private BigDecimal resolveExchangeRate(Client client, String currency, BigDecimal requestedRate) {
        String base = client != null && client.getCompany() != null && client.getCompany().getBaseCurrency() != null
                ? client.getCompany().getBaseCurrency() : "BDT";
        if (currency.equalsIgnoreCase(base)) {
            return BigDecimal.ONE;
        }
        if (requestedRate == null || requestedRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Exchange rate to " + base + " is required for a " + currency + " invoice");
        }
        return requestedRate;
    }

    /**
     * Converts an invoice-currency amount into the company's base currency for GL
     * posting - the ledger is single-currency; foreign invoices carry their issue-time
     * rate. (FX gain/loss on payment-date rate differences is out of scope: payments
     * convert at the invoice's own rate.)
     */
    private BigDecimal toBase(ClientInvoice invoice, BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal rate = invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE;
        if (rate.compareTo(BigDecimal.ONE) == 0) return amount;
        return amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Revenue recognition: without this, invoices never touched the General Ledger at
     * all, so Profit & Loss / Balance Sheet showed no revenue even for fully paid
     * invoices - the two systems were completely disconnected.
     * Dr Accounts Receivable (full total) / Cr Sales Revenue (subtotal - discount) +
     * Cr Tax Payable (tax, if any) - stays balanced since revenue + tax = totalAmount.
     */
    private void postInvoiceToLedger(ClientInvoice invoice) {
        Long companyId = invoice.getCompanyId();
        String clientName = invoice.getClient() != null ? invoice.getClient().getClientCompanyName() : "client";
        String description = "Invoice " + invoice.getInvoiceNumber() + " issued to " + clientName;
        // Revenue is recognized as of the invoice's own date, not whenever staff happened
        // to click Send - otherwise a backdated invoice's revenue lands in the wrong period.
        LocalDate transactionDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();

        ChartOfAccount ar = accountResolver.accountsReceivable(companyId);
        ChartOfAccount revenue = accountResolver.salesRevenue(companyId);

        // Convert each credit leg, then make the AR debit their exact sum - rounding
        // totalAmount independently could drift a cent from revenue+tax and trip the
        // balanced-batch check.
        BigDecimal revenueBase = toBase(invoice, recognizedRevenue(invoice));
        BigDecimal taxBase = invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
                ? toBase(invoice, invoice.getTaxAmount()) : BigDecimal.ZERO;

        List<LedgerLine> lines = new java.util.ArrayList<>();
        lines.add(LedgerLine.debit(ar.getId(), revenueBase.add(taxBase)));
        lines.add(LedgerLine.credit(revenue.getId(), revenueBase));
        if (taxBase.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount tax = accountResolver.taxPayable(companyId);
            lines.add(LedgerLine.credit(tax.getId(), taxBase));
        }

        glService.recordBalancedTransaction(companyId, lines, description,
                GlReferenceType.INVOICE, invoice.getId(), invoice.getInvoiceNumber(), transactionDate);
    }

    @Override
    @Transactional
    public void recordPayment(Long id, BigDecimal amount) {
        authorizationService.checkPermission(PermissionCode.INVOICE_PAYMENT);
        recordPaymentForCompany(requireCompanyId(), id, amount);
    }

    @Override
    @Transactional
    public void recordPaymentForCompany(Long companyId, Long id, BigDecimal amount) {
        recordPaymentForCompany(companyId, id, amount, LocalDate.now());
    }

    @Override
    @Transactional
    public void recordPaymentForCompany(Long companyId, Long id, BigDecimal amount, LocalDate paymentDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.businessos.shared.exception.BadRequestException("Payment amount must be positive");
        }

        ClientInvoice invoice = invoiceRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new com.businessos.shared.exception.ResourceNotFoundException("Invoice not found: " + id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new com.businessos.shared.exception.BadRequestException("Cannot record payment for a cancelled invoice");
        }

        BigDecimal credited = invoice.getCreditedAmount() != null ? invoice.getCreditedAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount()).subtract(credited);
        if (amount.compareTo(outstanding) > 0) {
            throw new com.businessos.shared.exception.BadRequestException("Payment amount exceeds outstanding balance");
        }

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(newPaidAmount);

        if (newPaidAmount.add(credited).compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(paymentDate);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoice.calculateTotals();
        invoiceRepository.save(invoice);

        // Cash received against a receivable: Dr Cash / Cr Accounts Receivable
        // (converted at the invoice's issue-time rate for foreign-currency invoices).
        // Posted on the source document's own date (e.g. the payment receipt's
        // paymentDate), not today - otherwise a payment received last month but
        // confirmed today posted into today's period, so the period-lock check
        // protected the wrong date entirely.
        String description = "Payment received for invoice " + invoice.getInvoiceNumber();
        BigDecimal amountBase = toBase(invoice, amount);
        ChartOfAccount cash = accountResolver.cash(companyId);
        ChartOfAccount ar = accountResolver.accountsReceivable(companyId);
        glService.recordBalancedTransaction(companyId, List.of(
                        LedgerLine.debit(cash.getId(), amountBase),
                        LedgerLine.credit(ar.getId(), amountBase)),
                description, GlReferenceType.INVOICE, invoice.getId(), invoice.getInvoiceNumber(), paymentDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientInvoiceResponse> getOverdueInvoices() {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        Long companyId = requireCompanyId();
        List<InvoiceStatus> paidStatuses = List.of(InvoiceStatus.PAID, InvoiceStatus.CANCELLED);
        return invoiceRepository.findOverdueInvoices(companyId, paidStatuses)
                .stream()
                .map(ClientInvoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelInvoice(Long id) {
        authorizationService.checkPermission(PermissionCode.INVOICE_CANCEL);
        ClientInvoice invoice = findInTenant(id);

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new com.businessos.shared.exception.BadRequestException("Invoice is already cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new com.businessos.shared.exception.BadRequestException(
                    "Cannot cancel a fully paid invoice - issue a refund/credit note instead");
        }

        // DRAFT invoices were never posted to the ledger, so there's nothing to reverse.
        boolean wasPosted = invoice.getStatus() != InvoiceStatus.DRAFT;
        BigDecimal alreadyPaid = invoice.getPaidAmount();

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        if (wasPosted) {
            reverseInvoiceLedger(invoice, alreadyPaid);
        }
    }

    /**
     * Undoes exactly what postInvoiceToLedger() (and any recorded payments) posted:
     * Cr Accounts Receivable / Dr Sales Revenue + Dr Tax Payable for the full invoice,
     * and if any payment had already been received, Cr Cash / Dr Accounts Receivable
     * for that portion too - otherwise a cancelled invoice leaves permanently-wrong
     * AR/Revenue/Cash balances in the books.
     */
    private void reverseInvoiceLedger(ClientInvoice invoice, BigDecimal alreadyPaid) {
        reverseInvoiceLedger(invoice, alreadyPaid, GlReferenceType.INVOICE_CANCEL, "cancelled");
    }

    private void reverseInvoiceLedger(ClientInvoice invoice, BigDecimal alreadyPaid,
                                       GlReferenceType referenceType, String reasonWord) {
        Long companyId = invoice.getCompanyId();
        String description = "Invoice " + invoice.getInvoiceNumber() + " " + reasonWord + " - reversal";

        ChartOfAccount ar = accountResolver.accountsReceivable(companyId);
        ChartOfAccount revenue = accountResolver.salesRevenue(companyId);

        BigDecimal revenueBase = toBase(invoice, recognizedRevenue(invoice));
        BigDecimal taxBase = invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
                ? toBase(invoice, invoice.getTaxAmount()) : BigDecimal.ZERO;

        List<LedgerLine> lines = new java.util.ArrayList<>();
        lines.add(LedgerLine.credit(ar.getId(), revenueBase.add(taxBase)));
        lines.add(LedgerLine.debit(revenue.getId(), revenueBase));

        if (taxBase.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount tax = accountResolver.taxPayable(companyId);
            lines.add(LedgerLine.debit(tax.getId(), taxBase));
        }

        if (alreadyPaid != null && alreadyPaid.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal paidBase = toBase(invoice, alreadyPaid);
            ChartOfAccount cash = accountResolver.cash(companyId);
            lines.add(LedgerLine.credit(cash.getId(), paidBase));
            lines.add(LedgerLine.debit(ar.getId(), paidBase));
        }

        // Undo whatever credit note(s) already posted (Dr Revenue / Cr AR) - otherwise
        // the blanket totalAmount reversal above double-reduces AR for that portion.
        BigDecimal credited = invoice.getCreditedAmount() != null ? invoice.getCreditedAmount() : BigDecimal.ZERO;
        if (credited.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal creditedBase = toBase(invoice, credited);
            lines.add(LedgerLine.credit(revenue.getId(), creditedBase));
            lines.add(LedgerLine.debit(ar.getId(), creditedBase));
        }

        glService.recordBalancedTransaction(companyId, lines, description,
                referenceType, invoice.getId(), invoice.getInvoiceNumber(), LocalDate.now());
    }

    @Override
    @Transactional
    public void cancelOrRefundForServiceRequest(Long companyId, Long invoiceId) {
        ClientInvoice invoice = invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            // Money already collected - needs staff review, not an instant reversal.
            if (refundRepository.existsByClientInvoiceIdAndCompanyIdAndStatus(invoiceId, companyId, RefundStatus.REQUESTED)) {
                return; // already has a pending refund request
            }
            Refund refund = Refund.builder()
                    .companyId(companyId)
                    .clientInvoice(invoice)
                    .requestedAmount(invoice.getPaidAmount())
                    .status(RefundStatus.REQUESTED)
                    .build();
            refundRepository.save(refund);
            return;
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOIDED
                || invoice.getStatus() == InvoiceStatus.REFUNDED) {
            return; // nothing to do
        }

        boolean wasPosted = invoice.getStatus() != InvoiceStatus.DRAFT;
        BigDecimal alreadyPaid = invoice.getPaidAmount();
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        if (wasPosted) {
            reverseInvoiceLedger(invoice, alreadyPaid);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponse> listRefunds(RefundStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        Long companyId = requireCompanyId();
        Page<Refund> page = status != null
                ? refundRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                : refundRepository.findByCompanyId(companyId, pageable);
        return page.map(RefundMapper::toResponse);
    }

    @Override
    @Transactional
    public void processRefund(Long refundId) {
        authorizationService.checkPermission(PermissionCode.INVOICE_REFUND);
        Long companyId = requireCompanyId();
        Refund refund = refundRepository.findByIdAndCompanyId(refundId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundId));

        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BadRequestException("Only a requested refund can be processed");
        }

        ClientInvoice invoice = refund.getClientInvoice();
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new BadRequestException("Invoice is no longer in a paid state");
        }

        // Same reversal postInvoiceToLedger()/recordPaymentForCompany() posted, undone
        // here so the money genuinely leaves the company's books.
        reverseInvoiceLedger(invoice, invoice.getPaidAmount(), GlReferenceType.INVOICE_REFUND, "refunded");
        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoiceRepository.save(invoice);

        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedBy(securityUtil.getCurrentUser());
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);

        notifyClientOfRefundDecision(invoice, NotificationType.REFUND_PROCESSED, "Refund Processed",
                "Your refund of " + refund.getRequestedAmount() + " for invoice "
                        + invoice.getInvoiceNumber() + " has been processed.");
    }

    @Override
    @Transactional
    public void rejectRefund(Long refundId, String reason) {
        authorizationService.checkPermission(PermissionCode.INVOICE_REFUND);
        Long companyId = requireCompanyId();
        Refund refund = refundRepository.findByIdAndCompanyId(refundId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundId));

        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BadRequestException("Only a requested refund can be rejected");
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(reason);
        refundRepository.save(refund);

        notifyClientOfRefundDecision(refund.getClientInvoice(), NotificationType.REFUND_REJECTED, "Refund Rejected",
                "Your refund request for invoice " + refund.getClientInvoice().getInvoiceNumber()
                        + " was rejected." + (reason != null && !reason.isBlank() ? " Reason: " + reason : ""));
    }

    private void notifyClientOfRefundDecision(ClientInvoice invoice, NotificationType type, String title, String message) {
        try {
            Client client = invoice.getClient();
            if (client != null && client.getUser() != null) {
                notificationService.send(CreateNotificationRequest.of(
                        type, title, message, "/client/payments", client.getUser().getId(), invoice.getCompanyId()));
            }
        } catch (Exception ex) {
            // Notification failure must not roll back the refund decision itself.
        }
    }

    @Override
    @Transactional
    public CreditNoteResponse issueCreditNote(CreditNoteRequest request) {
        authorizationService.checkPermission(PermissionCode.INVOICE_CREDIT_NOTE);
        Long companyId = requireCompanyId();
        ClientInvoice invoice = invoiceRepository.findByIdAndCompanyId(request.getClientInvoiceId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + request.getClientInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new BadRequestException("Cannot issue a credit note against a " + invoice.getStatus() + " invoice");
        }

        BigDecimal alreadyCredited = invoice.getCreditedAmount() != null ? invoice.getCreditedAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount()).subtract(alreadyCredited);
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new BadRequestException("Credit note amount exceeds the invoice's outstanding balance ("
                    + outstanding + ")");
        }

        CreditNote creditNote = CreditNote.builder()
                .companyId(companyId)
                .creditNoteNumber(generateCreditNoteNumber(companyId))
                .clientInvoice(invoice)
                .amount(request.getAmount())
                .reason(request.getReason())
                .issuedBy(securityUtil.getCurrentUser())
                .issuedAt(LocalDateTime.now())
                .build();
        creditNoteRepository.save(creditNote);

        invoice.setCreditedAmount(alreadyCredited.add(request.getAmount()));
        invoice.calculateTotals();
        // calculateTotals() correctly zeroes balanceAmount but never touched status -
        // the overdue scheduler only checks status, not balance, so a fully-credited
        // $0-owed invoice kept flipping to OVERDUE and generating a false "money is
        // due" owner notification. Same PAID condition recordPayment() already uses
        // (paid + credited >= total), since PAID means "balance resolved" regardless
        // of whether that resolution was cash or a credit note.
        if (invoice.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0
                && invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            if (invoice.getPaidDate() == null) invoice.setPaidDate(LocalDate.now());
        }
        invoiceRepository.save(invoice);

        // Dr Sales Revenue / Cr Accounts Receivable - the company recognizes less
        // revenue and the client owes less, but no cash moves either direction.
        String description = "Credit note " + creditNote.getCreditNoteNumber() + " for invoice " + invoice.getInvoiceNumber();
        BigDecimal creditBase = toBase(invoice, request.getAmount());
        ChartOfAccount revenue = accountResolver.salesRevenue(companyId);
        ChartOfAccount ar = accountResolver.accountsReceivable(companyId);
        glService.recordBalancedTransaction(companyId, List.of(
                        LedgerLine.debit(revenue.getId(), creditBase),
                        LedgerLine.credit(ar.getId(), creditBase)),
                description, GlReferenceType.INVOICE_CREDIT_NOTE, invoice.getId(), creditNote.getCreditNoteNumber(), LocalDate.now());

        return CreditNoteMapper.toResponse(creditNote);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreditNoteResponse> listCreditNotes(Long invoiceId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.INVOICE_VIEW);
        Long companyId = requireCompanyId();
        Page<CreditNote> page = invoiceId != null
                ? creditNoteRepository.findByCompanyIdAndClientInvoiceId(companyId, invoiceId, pageable)
                : creditNoteRepository.findByCompanyId(companyId, pageable);
        return page.map(CreditNoteMapper::toResponse);
    }

    /**
     * Same per-company, per-year sequential scheme as generateInvoiceNumber().
     * Format: CN-YYYY-NNNNNN
     */
    private String generateCreditNoteNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "CN-" + year + "-";
        String maxNumber = creditNoteRepository
            .findMaxCreditNoteNumberByCompanyAndPrefix(companyId, prefix)
            .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.INVOICE_DELETE);
        ClientInvoice invoice = findInTenant(id);  // tenant-scoped
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new com.businessos.shared.exception.BadRequestException(
                    "Only DRAFT invoices can be deleted - use cancel for an already-sent invoice");
        }
        invoice.softDelete();
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void markAsOverdue(Long id) {
        ClientInvoice invoice = findInTenant(id);
        if (invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.CANCELLED) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
    }

    /**
     * Generates a unique, per-company, per-year invoice number.
     * Format: INV-YYYY-NNNNNN (e.g., INV-2026-000042)
     *
     * Uses MAX on existing numbers instead of COUNT to be safe against
     * concurrent inserts and soft-deleted records skewing the count.
     */
    private String generateInvoiceNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "INV-" + year + "-";
        String maxNumber = invoiceRepository
            .findMaxInvoiceNumberByCompanyAndPrefix(companyId, prefix)
            .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }
}