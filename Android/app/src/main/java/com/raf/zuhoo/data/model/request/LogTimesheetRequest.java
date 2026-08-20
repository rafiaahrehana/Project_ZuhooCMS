package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class LogTimesheetRequest {

    @SerializedName("workDate")
    private final String workDate;
    @SerializedName("hoursWorked")
    private final double hoursWorked;
    @SerializedName("projectName")
    private final String projectName;
    @SerializedName("description")
    private final String description;

    public LogTimesheetRequest(String workDate, double hoursWorked, String projectName, String description) {
        this.workDate = workDate;
        this.hoursWorked = hoursWorked;
        this.projectName = projectName;
        this.description = description;
    }
}
