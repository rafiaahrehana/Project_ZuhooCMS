package com.businessos.modules.ai.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class AiUsageSummaryResponse {

    private LocalDate date;
    private long totalRequests;
    private long totalTokens;
    private double avgResponseTimeMs;
    private Map<String, Long> requestsByFeature;
    private Map<String, Long> tokensByFeature;
}
