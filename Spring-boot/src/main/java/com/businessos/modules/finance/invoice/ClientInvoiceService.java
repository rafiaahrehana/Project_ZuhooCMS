package com.businessos.modules.finance.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.businessos.enums.InvoiceStatus;

public interface ClientInvoiceService {

    ClientInvoiceResponse create(ClientInvoiceRequest request);

    /**
     * System entry point for auto-generating an invoice as a side effect of a
     * client's own authorized action (e.g. submitting a paid service request) -
     * skips the staff-only INVOICE_CREATE permission check that create() enforces.
     */
    ClientInvoiceResponse createForServiceRequest(Long companyId, ClientInvoiceRequest request);
    ClientInvoiceResponse getById(Long id);

    /**
     * Renders the invoice as a PDF. Staff (INVOICE_VIEW) may download any invoice in
     * their company; a CLIENT may only download their own.
     */
    byte[] generatePdf(Long id);
    ClientInvoiceResponse getByInvoiceNumber(String number);
    Page<ClientInvoiceResponse> getAll(Pageable pageable);
    Page<ClientInvoiceResponse> getByStatus(InvoiceStatus status, Pageable pageable);
    Page<ClientInvoiceResponse> getByClient(Long clientId, Pageable pageable);

    /** The caller's own invoices - resolves their Client record from the security context. */
    Page<ClientInvoiceResponse> getMyInvoices(Pageable pageable);
    ClientInvoiceResponse update(Long id, ClientInvoiceRequest request);
    void sendInvoice(Long id);

    /** Same system-entry-point exemption as createForServiceRequest() - see its javadoc. */
    void sendInvoiceForServiceRequest(Long id);
    void recordPayment(Long id, java.math.BigDecimal amount);

    /** System entry point (e.g. payment gateway callbacks) - no security context. */
    void recordPaymentForCompany(Long companyId, Long id, java.math.BigDecimal amount);

    /** Same as above, but posts the GL entry on the given date instead of today - for
     *  callers whose source document (e.g. a payment receipt) carries its own date. */
    void recordPaymentForCompany(Long companyId, Long id, java.math.BigDecimal amount, java.time.LocalDate paymentDate);
    void markAsOverdue(Long id);
    List<ClientInvoiceResponse> getOverdueInvoices();

    /** Reverses any GL posting the invoice already made, then marks it CANCELLED. */
    void cancelInvoice(Long id);

    /** DRAFT only - anything already sent/posted must go through cancelInvoice(). */
    void delete(Long id);

    /**
     * System entry point called when a client cancels their own service request
     * (see ServiceRequestServiceImpl#cancel). If the invoice was already fully
     * PAID, files a Refund request for staff review instead of touching the
     * ledger; otherwise cancels and reverses it immediately, same as cancelInvoice().
     */
    void cancelOrRefundForServiceRequest(Long companyId, Long invoiceId);

    /** Pending (or any status) refund requests for staff review - permission INVOICE_VIEW. */
    Page<RefundResponse> listRefunds(com.businessos.enums.RefundStatus status, Pageable pageable);

    /** Reverses the invoice's GL postings (cash actually leaves the company's books) and marks it REFUNDED. */
    void processRefund(Long refundId);

    /** Leaves the invoice untouched (still PAID) - no money moves. */
    void rejectRefund(Long refundId, String reason);

    /** Draft a concise invoice summary note with AI from the invoice's real client/amount/service data - not persisted */
    InvoiceSummaryDraftResponse draftSummaryWithAi(Long id);

    /**
     * Issues a credit note against an invoice - a partial write-down of what's owed,
     * without reversing cash/revenue like a refund. Posts Dr Sales Revenue / Cr
     * Accounts Receivable for the credited amount. Amount must not exceed the
     * invoice's current outstanding balance.
     */
    CreditNoteResponse issueCreditNote(CreditNoteRequest request);

    Page<CreditNoteResponse> listCreditNotes(Long invoiceId, Pageable pageable);
}
