package com.businessos.modules.hrm.employee;

/**
 * Generates a unique, per-company, sequential employee number.
 * Format: EMP-NNNN (e.g., EMP-0042). Shared by EmployeeServiceImpl (new hires)
 * and EmployeeNumberBackfillInitializer (pre-existing employees with none set).
 */
public final class EmployeeNumberGenerator {

    private static final String PREFIX = "EMP-";

    private EmployeeNumberGenerator() {}

    public static String next(EmployeeRepository employeeRepository, Long companyId) {
        String maxNumber = employeeRepository
                .findMaxEmployeeNumberByCompanyAndPrefix(companyId, PREFIX)
                .orElse(PREFIX + "0000");
        long sequence = Long.parseLong(maxNumber.substring(PREFIX.length())) + 1;
        return String.format("%s%04d", PREFIX, sequence);
    }
}
