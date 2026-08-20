package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class CompanyServiceResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("price")
    private BigDecimal price;
    @SerializedName("currency")
    private String currency;
    @SerializedName("categoryId")
    private Long categoryId;
    @SerializedName("categoryName")
    private String categoryName;
    @SerializedName("featured")
    private boolean featured;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isFeatured() {
        return featured;
    }

    @Override
    public String toString() {
        if (price == null) {
            return name;
        }
        return name + " (" + (currency != null ? currency : "") + " " + price + ")";
    }
}
