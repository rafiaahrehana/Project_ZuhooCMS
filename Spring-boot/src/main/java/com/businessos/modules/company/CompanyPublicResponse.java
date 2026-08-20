package com.businessos.modules.company;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyPublicResponse {
    private Long id;
    private String companyName;
    private String subdomain;
    private String logo;
    private String primaryColor;
    private String secondaryColor;
    private String tagline;
    private String portalAbout;
    private String website;
    private String location;
    private String companyPhone;
    private String companyEmail;
}
