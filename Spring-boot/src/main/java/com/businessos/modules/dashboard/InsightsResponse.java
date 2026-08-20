package com.businessos.modules.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InsightsResponse {
    private String insights;
    private long generatedInMs;
}
