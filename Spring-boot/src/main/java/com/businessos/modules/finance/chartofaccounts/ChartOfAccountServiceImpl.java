package com.businessos.modules.finance.chartofaccounts;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceImpl implements ChartOfAccountService {

    private final ChartOfAccountRepository coaRepository;
    private final com.businessos.modules.finance.generalledger.GeneralLedgerRepository glRepository;
    private final com.businessos.modules.finance.generalledger.GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public ChartOfAccountResponse create(ChartOfAccountRequest request) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();

        // Check if account code already exists
        if (coaRepository.findByCompanyIdAndAccountCode(companyId, request.getAccountCode()).isPresent()) {
            throw new BadRequestException("Account code already exists: " + request.getAccountCode());
        }

        ChartOfAccount account = ChartOfAccountMapper.toEntity(request);
        account.setCompanyId(companyId);
        account.setBalance(java.math.BigDecimal.ZERO);
        account = coaRepository.save(account);

        // Opening balance for companies migrating from a previous system: a real
        // balanced posting (normal side of the account / offset to Opening Balance
        // Equity), so the ledger, Trial Balance, and Balance Sheet all back it up -
        // never a raw balance overwrite.
        if (request.getOpeningBalance() != null
                && request.getOpeningBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (request.getType() == AccountType.EQUITY) {
                throw new BadRequestException(
                        "Equity accounts can't take an opening balance this way - post a journal entry instead");
            }
            ChartOfAccount obe = accountResolver.openingBalanceEquity(companyId);
            java.time.LocalDate date = request.getOpeningBalanceDate() != null
                    ? request.getOpeningBalanceDate() : java.time.LocalDate.now();
            String description = "Opening balance - " + account.getAccountCode() + " " + account.getAccountName();
            boolean creditNormal = account.getType().isCreditNormal();

            glService.recordBalancedTransaction(companyId, java.util.List.of(
                            creditNormal
                                    ? com.businessos.modules.finance.generalledger.LedgerLine.credit(account.getId(), request.getOpeningBalance())
                                    : com.businessos.modules.finance.generalledger.LedgerLine.debit(account.getId(), request.getOpeningBalance()),
                            creditNormal
                                    ? com.businessos.modules.finance.generalledger.LedgerLine.debit(obe.getId(), request.getOpeningBalance())
                                    : com.businessos.modules.finance.generalledger.LedgerLine.credit(obe.getId(), request.getOpeningBalance())),
                    description, com.businessos.modules.finance.generalledger.GlReferenceType.OPENING_BALANCE,
                    account.getId(), account.getAccountCode(), date);
        }

        return ChartOfAccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_VIEW);
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Chart of Account not found"));
        return ChartOfAccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountResponse getByCode(String code) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        ChartOfAccount account = coaRepository.findByCompanyIdAndAccountCode(companyId, code)
                .orElseThrow(() -> new ResourceNotFoundException("Account code not found: " + code));
        return ChartOfAccountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChartOfAccountResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return coaRepository.findByCompanyId(companyId, pageable)
                .map(ChartOfAccountMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChartOfAccountResponse> getByType(AccountType type, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return coaRepository.findByCompanyIdAndTypeAndActiveTrue(companyId, type, pageable)
                .map(ChartOfAccountMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountResponse> getActiveAccounts() {
        Long companyId = securityUtil.getCurrentCompanyId();
        return coaRepository.findByCompanyIdAndActive(companyId, true)
                .stream()
                .map(ChartOfAccountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChartOfAccountResponse update(Long id, ChartOfAccountRequest request) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_UPDATE);
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Chart of Account not found"));

        // Validate code uniqueness if changed
        if (!account.getAccountCode().equals(request.getAccountCode())) {
            if (coaRepository.findByCompanyIdAndAccountCode(account.getCompanyId(), request.getAccountCode()).isPresent()) {
                throw new BadRequestException("Account code already exists: " + request.getAccountCode());
            }
            account.setAccountCode(request.getAccountCode());
        }

        account.setAccountName(request.getAccountName());
        account.setType(request.getType());
        account.setDescription(request.getDescription());
        account.setAllowDirectPosting(request.isAllowDirectPosting());
        account.setHeaderAccount(request.isHeaderAccount());
        account.setBankAccount(request.isBankAccount());
        account.setActive(request.isActive());
        account.setNotes(request.getNotes());

        account = coaRepository.save(account);
        return ChartOfAccountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.CHART_OF_ACCOUNT_DELETE);
        Long companyId = securityUtil.getCurrentCompanyId();
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Chart of Account not found"));
        if (glRepository.existsByAccountIdAndCompanyId(id, companyId)) {
            throw new BadRequestException("Cannot delete account with existing ledger entries");
        }
        account.softDelete();
        coaRepository.save(account);
    }
}
