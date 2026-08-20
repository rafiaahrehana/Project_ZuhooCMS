package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class EmployeeResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("lastName")
    private String lastName;
    // Directory-only fields. The backend's EmployeeResponse also carries salary, bank account,
    // and national ID fields — deliberately not declared here so they're never parsed into
    // memory on a screen that lists every employee in the company.
    @SerializedName("jobTitle")
    private String jobTitle;
    @SerializedName("departmentName")
    private String departmentName;
    @SerializedName("phone")
    private String phone;
    @SerializedName("email")
    private String email;
    @SerializedName("profileImageUrl")
    private String profileImageUrl;

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
