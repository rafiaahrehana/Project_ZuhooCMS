package com.businessos.modules.hrm.attendance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateAndAbsentReport {
    private LocalDate reportDate;
    private long lateCount;
    private long absentCount;
}
