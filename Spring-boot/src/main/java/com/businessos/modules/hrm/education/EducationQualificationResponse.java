package com.businessos.modules.hrm.education;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EducationQualificationResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String degree;
    private String institution;
    private String fieldOfStudy;
    private Integer passingYear;
    private String result;
    private String notes;
    private LocalDateTime createdAt;
}
