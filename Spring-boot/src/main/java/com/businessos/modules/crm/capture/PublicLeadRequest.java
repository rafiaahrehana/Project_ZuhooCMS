package com.businessos.modules.crm.capture;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * What an anonymous visitor may submit. Every field is length-capped: this is
 * the one endpoint in the CRM that the whole internet can post to.
 */
@Getter
@Setter
public class PublicLeadRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @Email(message = "A valid email is required")
    @Size(max = 160)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 160)
    private String companyName;

    @Size(max = 2000)
    private String message;

    /**
     * Tenant-site captures pass the portal subdomain; the platform landing page
     * sends nothing and the configured platform company receives the lead.
     */
    @Size(max = 80)
    private String subdomain;

    /**
     * Honeypot. The form renders this input invisibly and humans leave it
     * empty; bulk spam bots fill every field. Named to look real to a bot.
     */
    @Size(max = 200)
    private String website;
}
