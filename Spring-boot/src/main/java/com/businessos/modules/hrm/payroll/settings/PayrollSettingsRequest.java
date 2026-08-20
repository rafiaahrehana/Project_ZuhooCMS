package com.businessos.modules.hrm.payroll.settings;

import com.businessos.enums.PerDayBasis;
import com.businessos.enums.SalaryBase;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Every field is nullable and only applied when present, so the settings page
 * can send a partial update without wiping the fields it did not render.
 */
@Data
public class PayrollSettingsRequest {

    private PerDayBasis perDayBasis;
    private SalaryBase absenceDeductionBase;

    private Boolean overtimeEnabled;
    private BigDecimal overtimeMultiplier;
    private SalaryBase overtimeBase;
    private BigDecimal standardHoursPerDay;

    private BigDecimal houseRentPercent;
    private BigDecimal medicalPercent;
    private BigDecimal transportPercent;
    private BigDecimal foodPercent;
    private BigDecimal providentFundPercent;
    private BigDecimal taxPercent;
}
