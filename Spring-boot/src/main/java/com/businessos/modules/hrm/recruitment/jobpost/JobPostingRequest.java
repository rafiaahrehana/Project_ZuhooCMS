package com.businessos.modules.hrm.recruitment.jobpost;

import com.businessos.enums.EducationLevel;
import com.businessos.enums.EmploymentType;

import com.businessos.enums.JobPostingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class JobPostingRequest {
    @NotBlank(message = "Job posting title is required")
    @Size(max = 150)
    private String title;
    @Size(max = 100)
    private String jobTitle;
    private String description;
    private String requirements;
    private String responsibilities;
    @Size(max = 150)
    private String location;
    private EmploymentType employmentType;
    private JobPostingStatus status;
    @Min(value = 1, message = "Must have at least 1 vacancy")
    private Integer vacancies;
    @DecimalMin(value = "0.00")
    private BigDecimal salaryMin;
    @DecimalMin(value = "0.00")
    private BigDecimal salaryMax;
    private LocalDate deadline;
    private boolean remote;
    private Long departmentId;

    @Size(max = 500)
    private String requiredSkills;
    @Size(max = 500)
    private String preferredSkills;
    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer minExperienceYears;
    private EducationLevel minEducationLevel;
}
