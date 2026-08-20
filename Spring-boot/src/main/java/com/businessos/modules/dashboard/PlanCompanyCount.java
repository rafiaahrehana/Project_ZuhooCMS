package com.businessos.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** How many companies sit on a given catalog plan right now - see PlatformSummaryResponse. */
@Getter
@AllArgsConstructor
public class PlanCompanyCount {
    private String code;
    private String name;
    private long count;
}
