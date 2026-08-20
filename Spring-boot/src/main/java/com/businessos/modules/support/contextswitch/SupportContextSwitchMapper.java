package com.businessos.modules.support.contextswitch;

public class SupportContextSwitchMapper {

    public static SupportContextSwitchResponse toResponse(SupportContextSwitch entity) {
        if (entity == null) {
            return null;
        }

        return SupportContextSwitchResponse.builder()
                .id(entity.getId())
                .supportAgentId(entity.getSupportAgent() != null ? entity.getSupportAgent().getId() : null)
                .supportAgentName(entity.getSupportAgent() != null ? entity.getSupportAgent().getFullName() : null)
                .viewedCompanyId(entity.getViewedCompany() != null ? entity.getViewedCompany().getId() : null)
                .viewedCompanyName(entity.getViewedCompany() != null ? entity.getViewedCompany().getCompanyName() : null)
                .switchedInTime(entity.getSwitchedInTime())
                .switchedOutTime(entity.getSwitchedOutTime())
                .purpose(entity.getPurpose())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .stillActive(entity.isStillActive())
                .build();
    }
}
