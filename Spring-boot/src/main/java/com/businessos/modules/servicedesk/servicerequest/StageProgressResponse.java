package com.businessos.modules.servicedesk.servicerequest;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StageProgressResponse {
    private Long serviceRequestId;
    private Integer currentStage;
    private Integer totalStages;
    private List<StageItem> stages;

    @Getter
    @Setter
    public static class StageItem {
        private Long stageId;
        private String name;
        private Integer stageOrder;
        private Integer slaHours;
        private Boolean requiresApproval;
        private boolean completed;
        private boolean current;
        private String approvalStatus;
        private Boolean requiresPayment;
        private Integer paymentPercent;
    }
}
