package com.businessos.modules.hrm.dashboard;

public interface HrDashboardService {

    /** Aggregated HR figures for the active company. */
    HrDashboardResponse getSummary();
}
