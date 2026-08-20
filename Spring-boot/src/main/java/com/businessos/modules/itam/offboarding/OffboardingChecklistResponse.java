package com.businessos.modules.itam.offboarding;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OffboardingChecklistResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;

    private LocalDate offboardingDate;
    private LocalDate targetCompletionDate;

    private boolean hardwareCollected;
    private LocalDate hardwareCollectedDate;
    private String hardwareCollectedBy;
    private String hardwareNotes;

    private boolean licensesRevoked;
    private LocalDate licensesRevokedDate;
    private String licensesNotes;

    private boolean accessRevoked;
    private LocalDate accessRevokedDate;
    private String accessNotes;

    private boolean dataHandedOver;
    private LocalDate dataHandoverDate;
    private String dataHandoverNotes;

    private boolean exitInterviewCompleted;
    private LocalDate exitInterviewDate;
    private String exitInterviewNotes;

    private boolean completed;
    private LocalDate completionDate;
    private String completedBy;

    private int completionPercentage;
    private String overallNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
