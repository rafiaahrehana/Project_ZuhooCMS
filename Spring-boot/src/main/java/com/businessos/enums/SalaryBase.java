package com.businessos.enums;

/**
 * Which figure a derived amount is calculated from - the basic salary alone, or
 * the full gross. Used for absence deductions and overtime rates, which
 * different companies base on different figures.
 */
public enum SalaryBase {
    BASIC,
    GROSS
}
