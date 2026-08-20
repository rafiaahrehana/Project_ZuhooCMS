package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.Map;

public class CreateServiceRequestRequest {

    @SerializedName("title")
    private final String title;
    @SerializedName("description")
    private final String description;
    @SerializedName("hubServiceId")
    private final Long hubServiceId;
    @SerializedName("priority")
    private final String priority;
    @SerializedName("agreedPrice")
    private final BigDecimal agreedPrice;

    // Answers to the service's custom fields, keyed by field id as a string. Null when the
    // service has no custom fields or none were filled in — the backend treats it as optional.
    @SerializedName("formData")
    private final Map<String, String> formData;

    public CreateServiceRequestRequest(String title, String description, Long hubServiceId,
                                       String priority, BigDecimal agreedPrice,
                                       Map<String, String> formData) {
        this.title = title;
        this.description = description;
        this.hubServiceId = hubServiceId;
        this.priority = priority;
        this.agreedPrice = agreedPrice;
        this.formData = formData;
    }
}
