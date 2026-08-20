package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class CompanyPublicResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("companyName")
    private String companyName;
    @SerializedName("subdomain")
    private String subdomain;
    @SerializedName("logo")
    private String logo;

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getLogo() {
        return logo;
    }

    @Override
    public String toString() {
        return companyName;
    }
}
