package com.businessos.modules.crm.opportunity;

import com.businessos.modules.hrm.employee.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OpportunityMapper {

    public static OpportunityResponse toResponse(Opportunity opportunity) {
        OpportunityResponse response = new OpportunityResponse();
        response.setId(opportunity.getId());
        response.setName(opportunity.getName());
        response.setDescription(opportunity.getDescription());
        response.setStage(opportunity.getStage());
        response.setSource(opportunity.getSource());
        response.setAmount(opportunity.getAmount());
        response.setProbability(opportunity.getProbability());
        response.setWeightedAmount(weighted(opportunity));
        response.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        response.setActualCloseDate(opportunity.getActualCloseDate());
        response.setNextStep(opportunity.getNextStep());
        response.setLostReason(opportunity.getLostReason());
        response.setLostReasonCode(opportunity.getLostReasonCode());

        if (opportunity.getClient() != null) {
            response.setClientId(opportunity.getClient().getId());
            response.setClientCompanyName(opportunity.getClient().getClientCompanyName());
        }
        if (opportunity.getContact() != null) {
            response.setContactId(opportunity.getContact().getId());
            response.setContactName(opportunity.getContact().getFullName());
        }
        if (opportunity.getSourceLead() != null) {
            response.setSourceLeadId(opportunity.getSourceLead().getId());
        }
        Employee owner = opportunity.getOwner();
        if (owner != null) {
            response.setOwnerId(owner.getId());
            response.setOwnerName(owner.getUser() != null ? owner.getUser().getFullName() : null);
        }

        response.setLastActivityAt(opportunity.getLastActivityAt());
        response.setStageChangedAt(opportunity.getStageChangedAt());
        response.setCreatedAt(opportunity.getCreatedAt());
        response.setUpdatedAt(opportunity.getUpdatedAt());
        response.setTags(opportunity.getTags() != null
                ? com.businessos.modules.crm.tag.TagMapper.toResponseList(opportunity.getTags())
                : java.util.List.of());
        return response;
    }

    private static BigDecimal weighted(Opportunity opportunity) {
        if (opportunity.getAmount() == null || opportunity.getProbability() == null) {
            return BigDecimal.ZERO;
        }
        return opportunity.getAmount()
            .multiply(BigDecimal.valueOf(opportunity.getProbability()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
