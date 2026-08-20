package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.enums.ApplicationSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobApplicationRequest {
    @NotBlank(message = "Applicant name is required")
    @Size(max = 150)
    private String applicantName;
    @NotBlank(message = "Email is required")
    @Email
    private String applicantEmail;
    @Size(max = 30)
    private String applicantPhone;
    @Size(max = 500)
    private String resumeUrl;
    private String linkedInUrl;
    private String portfolioUrl;
    private String coverLetter;
    /** How staff sourced this candidate - e.g. EMPLOYEE_REFERRAL, AGENCY. Defaults to DIRECT when omitted. */
    private ApplicationSource source;
    /** Set only when source is EMPLOYEE_REFERRAL - the referring employee's own id, never a caller-suppliable other employee's id. */
    private Long referredByEmployeeId;
}
