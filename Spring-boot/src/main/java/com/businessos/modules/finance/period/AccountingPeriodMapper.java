package com.businessos.modules.finance.period;

import java.time.format.DateTimeFormatter;

public class AccountingPeriodMapper {

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    public static AccountingPeriodResponse toResponse(AccountingPeriod entity) {
        if (entity == null) return null;
        return AccountingPeriodResponse.builder()
                .id(entity.getId())
                .fiscalYear(entity.getFiscalYear())
                .periodNumber(entity.getPeriodNumber())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .label(entity.getStartDate() != null ? entity.getStartDate().format(LABEL_FMT) : null)
                .status(entity.getStatus())
                .closedBy(entity.getClosedBy())
                .closedAt(entity.getClosedAt())
                .reopenedBy(entity.getReopenedBy())
                .reopenedAt(entity.getReopenedAt())
                .build();
    }
}
