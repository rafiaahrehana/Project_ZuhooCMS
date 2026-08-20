package com.businessos.modules.website;

public final class WebsiteRequests {

    private WebsiteRequests() {}

    public record ContactRequest(String name, String email, String phone, String subject, String message) {}
    public record NewsletterRequest(String email) {}
    public record ServiceRequestPayload(Long serviceId, String serviceTitle, String name, String email, String phone, String message) {}
    public record CareerApplicationRequest(String name, String email, String phone, String position, String resumeUrl, String coverLetter) {}
}
