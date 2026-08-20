package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.enums.ApplicationSource;
import com.businessos.enums.ApplicationStatus;
import com.businessos.enums.AtsParseStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
public class JobApplicationResponse {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String resumeUrl;
    private String linkedInUrl;
    private String portfolioUrl;
    private String coverLetter;
    private ApplicationSource source;
    private ApplicationStatus status;
    private String notes;
    private Long jobPostingId;
    private String jobPostingTitle;
    private Long reviewedById;
    private String reviewedByName;
    private Long convertedEmployeeId;
    private LocalDateTime convertedAt;
    private Integer scoreEducation;
    private Integer scoreExperience;
    private Integer scoreTechnicalSkills;
    private Integer scoreInterview;
    private Integer scoreCommunication;
    private Double overallScore;
    private Integer atsScore;
    private String atsMatchedRequiredSkills;
    private String atsMissingRequiredSkills;
    private String atsMatchedPreferredSkills;
    private Integer atsExtractedExperienceYears;
    private Boolean atsMeetsEducationRequirement;
    private AtsParseStatus atsParseStatus;
    private Instant atsParsedAt;
    private LocalDateTime createdAt;
}
