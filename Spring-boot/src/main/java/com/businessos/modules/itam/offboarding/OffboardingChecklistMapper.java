package com.businessos.modules.itam.offboarding;

public class OffboardingChecklistMapper {

    /**
     * getFullName() forces Hibernate to initialize the Employee proxy, which
     * re-runs the entity's own @SQLRestriction("deleted = false") - a
     * terminated (soft-deleted) employee's proxy throws EntityNotFoundException
     * on any field access beyond its id, which would 500 the whole offboarding
     * list the moment one entry's employee has actually been terminated -
     * exactly the case this checklist exists to track.
     */
    private static String safeFullName(com.businessos.modules.hrm.employee.Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getFullName();
        } catch (Exception e) {
            return null;
        }
    }

    public static OffboardingChecklistResponse toResponse(OffboardingChecklist entity) {
        if (entity == null) {
            return null;
        }

        return OffboardingChecklistResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(safeFullName(entity.getEmployee()))
                .offboardingDate(entity.getOffboardingDate())
                .targetCompletionDate(entity.getTargetCompletionDate())
                .hardwareCollected(entity.isHardwareCollected())
                .hardwareCollectedDate(entity.getHardwareCollectedDate())
                .hardwareCollectedBy(entity.getHardwareCollectedBy())
                .hardwareNotes(entity.getHardwareNotes())
                .licensesRevoked(entity.isLicensesRevoked())
                .licensesRevokedDate(entity.getLicensesRevokedDate())
                .licensesNotes(entity.getLicensesNotes())
                .accessRevoked(entity.isAccessRevoked())
                .accessRevokedDate(entity.getAccessRevokedDate())
                .accessNotes(entity.getAccessNotes())
                .dataHandedOver(entity.isDataHandedOver())
                .dataHandoverDate(entity.getDataHandoverDate())
                .dataHandoverNotes(entity.getDataHandoverNotes())
                .exitInterviewCompleted(entity.isExitInterviewCompleted())
                .exitInterviewDate(entity.getExitInterviewDate())
                .exitInterviewNotes(entity.getExitInterviewNotes())
                .completed(entity.isCompleted())
                .completionDate(entity.getCompletionDate())
                .completedBy(entity.getCompletedBy())
                .completionPercentage(entity.getCompletionPercentage())
                .overallNotes(entity.getOverallNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
