package com.businessos.modules.support.sla;

public class SLAPolicyMapper {

    public static SLAPolicyResponse toResponse(SLAPolicy entity) {
        if (entity == null) {
            return null;
        }

        return SLAPolicyResponse.builder()
                .id(entity.getId())
                .policyName(entity.getPolicyName())
                .description(entity.getDescription())
                .applicablePriority(entity.getApplicablePriority())
                .firstResponseTimeHours(entity.getFirstResponseTimeHours())
                .resolutionTimeHours(entity.getResolutionTimeHours())
                .businessHoursOnly(entity.isBusinessHoursOnly())
                .active(entity.isActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static SLAPolicy toEntity(SLAPolicyRequest request) {
        if (request == null) {
            return null;
        }

        return SLAPolicy.builder()
                .policyName(request.getPolicyName())
                .description(request.getDescription())
                .applicablePriority(request.getApplicablePriority())
                .firstResponseTimeHours(request.getFirstResponseTimeHours())
                .resolutionTimeHours(request.getResolutionTimeHours())
                .businessHoursOnly(request.isBusinessHoursOnly())
                .active(request.isActive())
                .notes(request.getNotes())
                .build();
    }
}
