package com.businessos.modules.finance.chartofaccounts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChartOfAccountService {

    ChartOfAccountResponse create(ChartOfAccountRequest request);
    ChartOfAccountResponse getById(Long id);
    ChartOfAccountResponse getByCode(String code);
    Page<ChartOfAccountResponse> getAll(Pageable pageable);
    Page<ChartOfAccountResponse> getByType(AccountType type, Pageable pageable);
    List<ChartOfAccountResponse> getActiveAccounts();
    ChartOfAccountResponse update(Long id, ChartOfAccountRequest request);
    void delete(Long id);
}
