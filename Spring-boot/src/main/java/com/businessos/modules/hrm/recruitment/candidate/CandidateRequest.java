package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.enums.ApplicationSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateRequest {
    private String name;
    private String email;
    private String phone;
    private String resumeUrl;
    private String linkedInUrl;
    private String portfolioUrl;
    private String currentTitle;
    private String skills;
    private ApplicationSource source;
    private String notes;
}
