package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class HolidayResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("holidayDate")
    private String holidayDate;
    @SerializedName("holidayType")
    private String holidayType;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHolidayDate() {
        return holidayDate;
    }

    public String getHolidayType() {
        return holidayType;
    }
}
