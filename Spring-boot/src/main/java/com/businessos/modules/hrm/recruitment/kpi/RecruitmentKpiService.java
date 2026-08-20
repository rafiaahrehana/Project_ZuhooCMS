package com.businessos.modules.hrm.recruitment.kpi;

import java.time.LocalDate;

public interface RecruitmentKpiService {

    /**
     * Date bounds are optional/inclusive and filter on JobApplication's applied
     * date; null/null means all-time. minScore is optional and only narrows the
     * Top Evaluated Candidates list, not the rest of the report.
     */
    RecruitmentKpiResponse getSummary(LocalDate from, LocalDate to, Double minScore);
}
