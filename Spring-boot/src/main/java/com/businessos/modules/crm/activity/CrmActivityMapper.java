package com.businessos.modules.crm.activity;

import com.businessos.auth.user.User;

public class CrmActivityMapper {

    public static CrmActivityResponse toResponse(CrmActivity activity) {
        CrmActivityResponse response = new CrmActivityResponse();
        response.setId(activity.getId());
        response.setType(activity.getType());
        response.setSubject(activity.getSubject());
        response.setDescription(activity.getDescription());
        response.setActivityDate(activity.getActivityDate());
        response.setScheduledAt(activity.getScheduledAt());
        response.setCompleted(activity.isCompleted());
        response.setSystemGenerated(activity.isSystemGenerated());
        response.setClientId(activity.getClient() != null ? activity.getClient().getId() : null);
        if (activity.getOpportunity() != null) {
            response.setOpportunityId(activity.getOpportunity().getId());
            response.setOpportunityName(activity.getOpportunity().getName());
        }
        User performedBy = activity.getPerformedBy();
        if (performedBy != null) {
            response.setPerformedById(performedBy.getId());
            response.setPerformedByName(performedBy.getFullName());
        }
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }
}
