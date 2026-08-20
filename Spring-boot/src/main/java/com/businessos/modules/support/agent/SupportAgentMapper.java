package com.businessos.modules.support.agent;

public class SupportAgentMapper {

    public static SupportAgentResponse toResponse(SupportAgent entity) {
        if (entity == null) return null;

        return SupportAgentResponse.builder()
                .id(entity.getId())
                .userName(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .fullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .email(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .department(entity.getDepartment())
                .specialization(entity.getSpecialization())
                .status(entity.getStatus())
                .totalTicketsHandled(entity.getTotalTicketsHandled())
                .avgResponseTimeMinutes(entity.getAvgResponseTimeMinutes())
                .avgResolutionTimeMinutes(entity.getAvgResolutionTimeMinutes())
                .satisfactionScore(entity.getSatisfactionScore())
                .acceptingTickets(entity.isAcceptingTickets())
                .maxConcurrentTickets(entity.getMaxConcurrentTickets())
                .lastActiveTime(entity.getLastActiveTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static SupportAgent toEntity(SupportAgentRequest request) {
        if (request == null) return null;

        return SupportAgent.builder()
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .status(request.getStatus())
                .maxConcurrentTickets(request.getMaxConcurrentTickets())
                .notes(request.getNotes())
                .build();
    }
}
