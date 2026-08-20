package com.businessos.modules.finance.chartofaccounts;

public class ChartOfAccountMapper {

    public static ChartOfAccountResponse toResponse(ChartOfAccount entity) {
        if (entity == null) return null;

        return ChartOfAccountResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .accountCode(entity.getAccountCode())
                .accountName(entity.getAccountName())
                .type(entity.getType())
                .balance(entity.getBalance())
                .isHeaderAccount(entity.isHeaderAccount())
                .isBankAccount(entity.isBankAccount())
                .allowDirectPosting(entity.isAllowDirectPosting())
                .active(entity.isActive())
                .description(entity.getDescription())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ChartOfAccount toEntity(ChartOfAccountRequest request) {
        if (request == null) return null;

        return ChartOfAccount.builder()
                .accountCode(request.getAccountCode())
                .accountName(request.getAccountName())
                .type(request.getType())
                .isHeaderAccount(request.isHeaderAccount())
                .isBankAccount(request.isBankAccount())
                .allowDirectPosting(request.isAllowDirectPosting())
                .active(true)
                .description(request.getDescription())
                .notes(request.getNotes())
                .build();
    }
}