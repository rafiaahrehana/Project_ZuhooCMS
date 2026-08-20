package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class TimesheetResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("workDate")
    private String workDate;
    @SerializedName("hoursWorked")
    private double hoursWorked;
    @SerializedName("projectName")
    private String projectName;
    @SerializedName("description")
    private String description;
    // NOT_SUBMITTED / SUBMITTED / APPROVED
    @SerializedName("status")
    private String status;

    public Long getId() {
        return id;
    }

    public String getWorkDate() {
        return workDate;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
