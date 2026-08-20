package com.businessos.modules.crm.opportunity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class PipelineSummaryResponse {

    private List<StageSummary> stages;
    private BigDecimal openPipelineValue;
    private BigDecimal weightedForecast;
    private BigDecimal wonValue;
    private long totalOpenDeals;

    @Getter @Setter
    public static class StageSummary {
        private OpportunityStage stage;
        private long dealCount;
        private BigDecimal totalAmount;
        private BigDecimal weightedAmount;
    }
}
