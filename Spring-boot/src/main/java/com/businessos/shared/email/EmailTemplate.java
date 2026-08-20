package com.businessos.shared.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplate {

    public static String buildVerificationCodeTemplate(String name, String code, int expiryMinutes, EmailBranding.Data branding) {
        String title = "Verify your email address";
        String body = "<p>Hi " + name + ",</p>"
                    + "<p>Use the code below to verify your email and activate your " + branding.getCompanyName() + " workspace.</p>"
                    + "<div style=\"text-align:center;margin:30px 0;\">"
                    + "<div style=\"display:inline-block;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px 40px;\">"
                    + "<p style=\"margin:0 0 8px;font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#64748b;\">Verification code</p>"
                    + "<p style=\"margin:0;font-size:36px;font-weight:700;letter-spacing:8px;color:#0f172a;font-family:monospace;\">" + code + "</p>"
                    + "</div></div>"
                    + "<p style=\"text-align:center;color:#64748b;font-size:13px;\">This code will expire " + expiryMinutes + " minutes after it was sent.</p>"
                    + "<p style=\"color:#94a3b8;font-size:13px;\">If you didn't request this, you can safely ignore this email.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildPasswordResetCodeTemplate(String name, String code, int expiryMinutes, EmailBranding.Data branding) {
        String title = "Reset your password";
        String body = "<p>Hi " + name + ",</p>"
                    + "<p>We received a request to reset your " + branding.getCompanyName() + " password. Use the code below to continue.</p>"
                    + "<div style=\"text-align:center;margin:30px 0;\">"
                    + "<div style=\"display:inline-block;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px 40px;\">"
                    + "<p style=\"margin:0 0 8px;font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#64748b;\">Reset code</p>"
                    + "<p style=\"margin:0;font-size:36px;font-weight:700;letter-spacing:8px;color:#0f172a;font-family:monospace;\">" + code + "</p>"
                    + "</div></div>"
                    + "<p style=\"text-align:center;color:#64748b;font-size:13px;\">This code will expire " + expiryMinutes + " minutes after it was sent.</p>"
                    + "<p style=\"color:#94a3b8;font-size:13px;\">If you didn't request this, you can safely ignore this email - your password won't change.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildTicketAssignedTemplate(String name, String ticketTitle, EmailBranding.Data branding) {
        String title = "New Ticket Assigned";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>A new service desk ticket (<strong>" + ticketTitle + "</strong>) has been assigned to you.</p>"
                    + "<p>Please login to the portal to review and take action.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildSalaryRevisionTemplate(String name, EmailBranding.Data branding) {
        String title = "Salary Revision Notification";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>Your salary structure has been revised.</p>"
                    + "<p>Please login to the employee portal to view your updated salary details.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildTerminationTemplate(String name, EmailBranding.Data branding) {
        String title = "Offboarding Notification";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>Your employment status has been updated to terminated. Thank you for your service.</p>"
                    + "<p>If you have any questions regarding your final settlement, please contact HR.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildPerformanceReviewTemplate(String name, EmailBranding.Data branding) {
        String title = "Performance Review Scheduled";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>A new performance review has been created for you.</p>"
                    + "<p>Please login to the portal to review your goals and feedback.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildPaymentReceiptTemplate(String name, String invoiceNumber, String amount, EmailBranding.Data branding) {
        String title = "Payment Receipt";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>Thank you for your payment!</p>"
                    + "<p>We have successfully received your payment of <strong>" + amount + "</strong> for Invoice <strong>" + invoiceNumber + "</strong>.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildExpenseStatusTemplate(String name, String expenseTitle, String status, EmailBranding.Data branding) {
        String title = "Expense Request " + status;
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>Your expense request for <strong>" + expenseTitle + "</strong> has been <strong>" + status.toLowerCase() + "</strong>.</p>"
                    + "<p>Please login to the portal to view the details.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    // Candidates aren't platform users - they have no login, so a "View Offer"
    // button pointing into the app (as the generic build() CTA template did,
    // at a route - /offer - that never existed either) would only dead-end
    // them at a login wall. HR still sends the actual offer details separately
    // (email attachment/call); this just confirms it was sent.
    public static String buildOfferLetterTemplate(String name, EmailBranding.Data branding) {
        String title = "Offer Letter from " + branding.getCompanyName();
        String body = "<p>Hi " + name + ",</p>"
                    + "<p>We are thrilled to offer you a position at " + branding.getCompanyName() + ".</p>"
                    + "<p>Our team will follow up with you directly with the full offer details and next steps.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildTicketCreatedTemplate(String name, String ticketTitle, EmailBranding.Data branding) {
        String title = "Support Ticket Created";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>We have received your support request: <strong>" + ticketTitle + "</strong>.</p>"
                    + "<p>Our team is reviewing it and will get back to you shortly.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    public static String buildTicketResolvedTemplate(String name, String ticketTitle, EmailBranding.Data branding) {
        String title = "Support Ticket Resolved";
        String body = "<p>Dear " + name + ",</p>"
                    + "<p>Your support ticket <strong>" + ticketTitle + "</strong> has been marked as resolved.</p>"
                    + "<p>If you have any further questions or if the issue persists, please reply to this email or reopen the ticket.</p>";
        return wrapInTenantTheme(title, body, branding);
    }

    private static String wrapInTenantTheme(String title, String body, EmailBranding.Data brand) {
        String color = brand.getPrimaryColor() != null ? brand.getPrimaryColor() : "#2563eb";
        String logoUrl = brand.getLogoUrl() != null ? brand.getLogoUrl() : "";

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;background:#f1f5f9;font-family:Arial;">
                <table width="100%%">
                <tr>
                <td align="center">
                <div style="max-width:600px;background:white;border-radius:15px;overflow:hidden;margin-top:20px;">
                <div style="background:%s;padding:30px;text-align:center;color:white;">
                %s
                <h2>%s</h2>
                </div>
                <div style="padding:35px">
                %s
                </div>
                <div style="padding:20px;text-align:center;background:#f8fafc;color:#64748b;">
                © 2026 %s
                </div>
                </div>
                </td>
                </tr>
                </table>
                </body>
                </html>
                """
                .formatted(
                        color,
                        logoUrl.isEmpty() ? "" : "<img src=\"" + logoUrl + "\" width=\"100\" style=\"margin-bottom:15px\" />",
                        title,
                        body,
                        brand.getCompanyName()
                );
    }

    public String build(
            EmailBranding.Data brand,
            String title,
            String message,
            String buttonText,
            String buttonUrl
    ) {
        String color = brand.getPrimaryColor() != null
                ? brand.getPrimaryColor()
                : "#2563eb";

        String logoUrl = brand.getLogoUrl() != null
                ? brand.getLogoUrl()
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;background:#f1f5f9;font-family:Arial;">
                <table width="100%%">
                <tr>
                <td align="center">
                <div style="max-width:600px;background:white;border-radius:15px;overflow:hidden;">
                <div style="background:%s;padding:30px;text-align:center;color:white;">
                %s
                <h2>%s</h2>
                </div>
                <div style="padding:35px">
                <p style="font-size:16px">%s</p>
                <div style="text-align:center;margin:30px">
                <a href="%s" style="background:%s;color:white;padding:14px 30px;border-radius:8px;text-decoration:none;">%s</a>
                </div>
                </div>
                <div style="padding:20px;text-align:center;background:#f8fafc;color:#64748b;">
                © 2026 %s
                </div>
                </div>
                </td>
                </tr>
                </table>
                </body>
                </html>
                """
                .formatted(
                        color,
                        logoUrl.isEmpty() ? "" : "<img src=\"" + logoUrl + "\" width=\"100\" style=\"margin-bottom:15px\" />",
                        brand.getCompanyName(),
                        message,
                        buttonUrl,
                        color,
                        buttonText,
                        brand.getCompanyName()
                );
    }
}