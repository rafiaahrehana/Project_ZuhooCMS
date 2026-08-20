package com.businessos.modules.finance.invoice;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyMapper;
import com.businessos.modules.crm.client.Client;
import com.businessos.shared.email.EmailBranding;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;


@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    public byte[] generate(ClientInvoice invoice, Company company, EmailBranding.Data branding) {
        String html = buildHtml(invoice, company, branding);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate invoice PDF", ex);
        }
        return os.toByteArray();
    }

    private String buildHtml(ClientInvoice invoice, Company company, EmailBranding.Data branding) {
        String currency = invoice.getCurrency() != null && !invoice.getCurrency().isBlank() ? invoice.getCurrency() : "BDT";
        Client client = invoice.getClient();
        String accent = branding.getPrimaryColor() != null ? branding.getPrimaryColor() : "#14300F";
        String clientName = client != null && client.getUser() != null
                ? client.getUser().getFirstName() + " " + client.getUser().getLastName() : "";
        String clientCompany = client != null ? client.getClientCompanyName() : null;
        String clientEmail = client != null && client.getUser() != null ? client.getUser().getEmail() : "";
        String billingAddress = client != null ? client.getBillingAddress() : null;

        String sellerAddress = company != null ? CompanyMapper.formatAddress(company.getLocationDetail()) : null;
        String sellerPhone = company != null ? company.getCompanyPhone() : null;
        String sellerEmail = company != null ? company.getCompanyEmail() : null;
        String sellerTaxId = company != null ? company.getTaxRegistrationNumber() : null;
        boolean hasBankDetails = company != null && (notBlank(company.getBankName()) || notBlank(company.getBankAccountNumber()));

        StringBuilder rows = new StringBuilder();
        if (invoice.getItems() != null) {
            for (ClientInvoiceItem item : invoice.getItems()) {
                rows.append("<tr>")
                        .append("<td>").append(escape(item.getDescription())).append("</td>")
                        .append("<td class=\"num\">").append(formatQty(item.getQuantity())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(item.getUnitPrice(), currency)).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(item.getLineTotal(), currency)).append("</td>")
                        .append("</tr>");
            }
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:Helvetica,Arial,sans-serif;color:#1f2937;font-size:11px;}"
                + ".header{display:table;width:100%;margin-bottom:16px;}"
                + ".header .company{display:table-cell;vertical-align:top;}"
                + ".header .title{display:table-cell;text-align:right;vertical-align:top;}"
                + ".logo{max-height:40px;max-width:180px;margin-bottom:6px;}"
                + ".company-name{font-size:18px;font-weight:bold;color:" + accent + ";}"
                + ".company-meta{color:#6b7280;line-height:1.5;margin-top:2px;}"
                + ".invoice-title{font-size:24px;font-weight:bold;color:" + accent + ";letter-spacing:2px;}"
                + ".meta{margin-top:4px;color:#6b7280;}"
                + ".section{margin-bottom:18px;}"
                + ".label{color:#6b7280;text-transform:uppercase;font-size:9px;letter-spacing:1px;margin-bottom:2px;}"
                + "table.items{width:100%;border-collapse:collapse;margin-top:8px;}"
                + "table.items th{background:#f3f4f6;text-align:left;padding:6px 8px;font-size:9px;text-transform:uppercase;color:#6b7280;}"
                + "table.items td{padding:6px 8px;border-bottom:1px solid #e5e7eb;}"
                + "table.items .num{text-align:right;}"
                + ".totals{width:280px;margin-left:auto;margin-top:12px;}"
                + ".totals tr td{padding:4px 0;}"
                + ".totals .num{text-align:right;}"
                + ".totals .grand td{font-weight:bold;font-size:13px;border-top:2px solid " + accent + ";padding-top:8px;}"
                + ".totals .due td{font-weight:bold;color:" + accent + ";}"
                + ".status{display:inline-block;padding:3px 10px;border-radius:10px;font-size:10px;font-weight:bold;color:#fff;background:" + accent + ";}"
                + ".payment-box{margin-top:24px;padding:12px 14px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:6px;}"
                + ".payment-box .label{margin-bottom:6px;}"
                + ".payment-box div{line-height:1.6;}"
                + ".footer{margin-top:24px;color:#9ca3af;font-size:9px;border-top:1px solid #e5e7eb;padding-top:8px;}"
                + "</style></head><body>"

                + "<div class=\"header\">"
                + "<div class=\"company\">"
                + (branding.getLogoUrl() != null ? "<img class=\"logo\" src=\"" + escape(branding.getLogoUrl()) + "\"/><br/>" : "")
                + "<div class=\"company-name\">" + escape(branding.getCompanyName()) + "</div>"
                + "<div class=\"company-meta\">"
                + (sellerAddress != null && !sellerAddress.isBlank() ? escape(sellerAddress) + "<br/>" : "")
                + (notBlank(sellerPhone) ? escape(sellerPhone) + " " : "")
                + (notBlank(sellerEmail) ? escape(sellerEmail) : "")
                + (notBlank(sellerTaxId) ? "<br/>Tax Reg. No: " + escape(sellerTaxId) : "")
                + "</div>"
                + "</div>"
                + "<div class=\"title\"><div class=\"invoice-title\">INVOICE</div>"
                + "<div class=\"meta\">" + escape(invoice.getInvoiceNumber()) + "</div></div>"
                + "</div>"

                + "<div class=\"header\">"
                + "<div class=\"company\">"
                + "<div class=\"label\">Bill To</div>"
                + "<div>" + escape(clientCompany != null && !clientCompany.isBlank() ? clientCompany : clientName) + "</div>"
                + (clientCompany != null && !clientCompany.isBlank() ? "<div>" + escape(clientName) + "</div>" : "")
                + "<div>" + escape(clientEmail) + "</div>"
                + (billingAddress != null && !billingAddress.isBlank() ? "<div>" + escape(billingAddress) + "</div>" : "")
                + "</div>"
                + "<div class=\"title\">"
                + "<div class=\"label\">Invoice Date</div><div>" + formatDate(invoice.getInvoiceDate()) + "</div>"
                + "<div class=\"label\" style=\"margin-top:8px;\">Due Date</div><div>" + formatDate(invoice.getDueDate()) + "</div>"
                + "<div style=\"margin-top:8px;\"><span class=\"status\">" + escape(invoice.getStatus() != null ? invoice.getStatus().name() : "") + "</span></div>"
                + "</div>"
                + "</div>"

                + (invoice.getDescription() != null && !invoice.getDescription().isBlank()
                    ? "<div class=\"section\"><div class=\"label\">Description</div><div>" + escape(invoice.getDescription()) + "</div></div>"
                    : "")

                + "<table class=\"items\"><thead><tr><th>Description</th><th class=\"num\">Qty</th><th class=\"num\">Unit Price</th><th class=\"num\">Total</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"

                + "<table class=\"totals\">"
                + "<tr><td>Subtotal</td><td class=\"num\">" + formatMoney(invoice.getSubtotal(), currency) + "</td></tr>"
                + (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                    ? "<tr><td>Discount</td><td class=\"num\">-" + formatMoney(invoice.getDiscountAmount(), currency) + "</td></tr>" : "")
                + (invoice.getTaxAmount() != null && invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
                    ? "<tr><td>Tax" + (invoice.getTaxRatePercent() != null ? " (" + formatQty(invoice.getTaxRatePercent()) + "%)" : "") + "</td><td class=\"num\">" + formatMoney(invoice.getTaxAmount(), currency) + "</td></tr>" : "")
                + "<tr class=\"grand\"><td>Total</td><td class=\"num\">" + formatMoney(invoice.getTotalAmount(), currency) + "</td></tr>"
                + "<tr><td>Paid</td><td class=\"num\">" + formatMoney(invoice.getPaidAmount(), currency) + "</td></tr>"
                + (invoice.getCreditedAmount() != null && invoice.getCreditedAmount().compareTo(BigDecimal.ZERO) > 0
                    ? "<tr><td>Credited</td><td class=\"num\">-" + formatMoney(invoice.getCreditedAmount(), currency) + "</td></tr>" : "")
                + "<tr class=\"due\"><td>Balance Due</td><td class=\"num\">" + formatMoney(invoice.getBalanceAmount(), currency) + "</td></tr>"
                + "</table>"

                + (hasBankDetails
                    ? "<div class=\"payment-box\"><div class=\"label\">Payment Details</div>"
                        + "<div>" + (notBlank(company.getBankName()) ? "Bank: " + escape(company.getBankName()) : "") + "</div>"
                        + "<div>" + (notBlank(company.getBankAccountName()) ? "Account Name: " + escape(company.getBankAccountName()) : "") + "</div>"
                        + "<div>" + (notBlank(company.getBankAccountNumber()) ? "Account Number: " + escape(company.getBankAccountNumber()) : "") + "</div>"
                        + "<div>" + (notBlank(company.getBankBranch()) ? "Branch: " + escape(company.getBankBranch()) : "") + "</div>"
                        + "</div>"
                    : "")

                + (invoice.getNotes() != null && !invoice.getNotes().isBlank()
                    ? "<div class=\"section\" style=\"margin-top:20px;\"><div class=\"label\">Notes</div><div>" + escape(invoice.getNotes()) + "</div></div>"
                    : "")

                + "<div class=\"footer\">Generated by " + escape(branding.getCompanyName()) + "</div>"
                + "</body></html>";
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String formatMoney(BigDecimal amount, String currency) {
        String value = amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        return currency + " " + value;
    }

    private String formatQty(BigDecimal qty) {
        return qty == null ? "1" : qty.stripTrailingZeros().toPlainString();
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
