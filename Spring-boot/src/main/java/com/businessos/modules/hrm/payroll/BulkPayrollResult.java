package com.businessos.modules.hrm.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Result of generateForAllEmployees() - a per-employee list, not just a count, so HR can see exactly who was skipped and why. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPayrollResult {
    private List<String> created;
    private List<String> skippedAlreadyExists;
    private List<String> skippedNoSalaryStructure;
}
