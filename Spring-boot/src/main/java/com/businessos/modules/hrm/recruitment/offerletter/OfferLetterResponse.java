package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.enums.LetterType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class OfferLetterResponse {
    private Long id;
    private LetterType letterType;
    private String referenceNumber;
    private LocalDate issueDate;
    private String content;
    private String signedBy;
    private String fileUrl;
    private boolean issued;
    private Long employeeId;
    private String employeeName;
    private Long jobApplicationId;
    private String recipientName;
    private String recipientEmail;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
