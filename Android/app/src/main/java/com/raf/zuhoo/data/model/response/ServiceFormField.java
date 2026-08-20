package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

// A custom field an admin configured for one service. Answers go back in the request's
// formData map, keyed by this field's id as a string.
public class ServiceFormField {

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_TEXTAREA = "TEXTAREA";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_DROPDOWN = "DROPDOWN";
    public static final String TYPE_CHECKBOX = "CHECKBOX";
    public static final String TYPE_RADIO = "RADIO";
    public static final String TYPE_DATE = "DATE";
    public static final String TYPE_FILE_UPLOAD = "FILE_UPLOAD";
    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_PHONE = "PHONE";
    public static final String TYPE_FORMULA = "FORMULA";

    @SerializedName("id")
    private Long id;
    @SerializedName("serviceId")
    private Long serviceId;
    @SerializedName("label")
    private String label;
    @SerializedName("fieldType")
    private String fieldType;
    @SerializedName("required")
    private boolean required;
    @SerializedName("validationRules")
    private String validationRules;
    @SerializedName("sortOrder")
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getFieldType() {
        return fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * DROPDOWN and RADIO options live as a comma-separated list inside validationRules — the
     * admin form builder has no dedicated options column, and the web client reads them the same
     * way. Anything else leaves this empty.
     */
    public List<String> options() {

        List<String> options = new ArrayList<>();

        if (validationRules == null) {
            return options;
        }

        for (String option : validationRules.split(",")) {
            String trimmed = option.trim();
            if (!trimmed.isEmpty()) {
                options.add(trimmed);
            }
        }

        return options;
    }
}
