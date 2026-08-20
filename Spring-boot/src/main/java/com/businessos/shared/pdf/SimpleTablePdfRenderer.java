package com.businessos.shared.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a simple title + table as a downloadable PDF (list exports - employees, leads,
 * etc). Builds a hand-written XHTML string (openhtmltopdf requires well-formed XML input,
 * not lenient HTML5) and converts it with openhtmltopdf/PDFBox, same approach as
 * InvoicePdfService but generic enough to share across any "export this list" endpoint.
 */
@Component
public class SimpleTablePdfRenderer {

    private static final DateTimeFormatter GENERATED_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public byte[] render(String title, String subtitle, List<String> headers, List<List<String>> rows) {
        return render(title, subtitle, headers, rows, null, null, null, null);
    }

    public byte[] render(String title, String subtitle, List<String> headers, List<List<String>> rows,
                          String companyName, String logoUrl, String email, String location) {
        String html = buildHtml(title, subtitle, headers, rows, companyName, logoUrl, email, location);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate PDF", ex);
        }
        return os.toByteArray();
    }

    private String buildHtml(String title, String subtitle, List<String> headers, List<List<String>> rows,
                              String companyName, String logoUrl, String email, String location) {
        boolean hasCompanyHeader = notBlank(companyName) || notBlank(logoUrl) || notBlank(email) || notBlank(location);
        StringBuilder headerRow = new StringBuilder();
        for (String h : headers) {
            headerRow.append("<th>").append(escape(h)).append("</th>");
        }

        StringBuilder body = new StringBuilder();
        for (List<String> row : rows) {
            body.append("<tr>");
            for (String cell : row) {
                body.append("<td>").append(escape(cell)).append("</td>");
            }
            body.append("</tr>");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE html>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "<head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:Helvetica,Arial,sans-serif;color:#1f2937;font-size:10px;}"
                + ".company-header{margin-bottom:16px;padding-bottom:10px;border-bottom:2px solid #14300F;}"
                + ".company-header .logo{max-height:40px;max-width:180px;margin-bottom:6px;}"
                + ".company-header .company-name{font-size:16px;font-weight:bold;color:#14300F;}"
                + ".company-header .company-meta{color:#6b7280;font-size:9px;line-height:1.5;margin-top:2px;}"
                + ".title{font-size:20px;font-weight:bold;color:#14300F;}"
                + ".subtitle{color:#6b7280;margin-top:2px;margin-bottom:14px;}"
                + "table.data{width:100%;border-collapse:collapse;}"
                + "table.data th{background:#f3f4f6;text-align:left;padding:6px 8px;font-size:9px;text-transform:uppercase;color:#6b7280;border-bottom:2px solid #e5e7eb;}"
                + "table.data td{padding:6px 8px;border-bottom:1px solid #e5e7eb;}"
                + "table.data tr:nth-child(even) td{background:#fafafa;}"
                + ".footer{margin-top:16px;color:#9ca3af;font-size:9px;}"
                + "</style></head><body>"
                + (hasCompanyHeader
                    ? "<div class=\"company-header\">"
                        + (notBlank(logoUrl) ? "<img class=\"logo\" src=\"" + escape(logoUrl) + "\"/><br/>" : "")
                        + (notBlank(companyName) ? "<div class=\"company-name\">" + escape(companyName) + "</div>" : "")
                        + "<div class=\"company-meta\">"
                        + (notBlank(location) ? escape(location) + "<br/>" : "")
                        + (notBlank(email) ? escape(email) : "")
                        + "</div>"
                        + "</div>"
                    : "")
                + "<div class=\"title\">" + escape(title) + "</div>"
                + (subtitle != null && !subtitle.isBlank() ? "<div class=\"subtitle\">" + escape(subtitle) + "</div>" : "")
                + "<table class=\"data\"><thead><tr>" + headerRow + "</tr></thead><tbody>" + body + "</tbody></table>"
                + "<div class=\"footer\">Generated " + LocalDateTime.now().format(GENERATED_FMT) + "</div>"
                + "</body></html>";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
