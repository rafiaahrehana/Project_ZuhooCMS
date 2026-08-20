package com.businessos.modules.finance.fixedasset;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository assetRepository;
    private final DepreciationRunRepository runRepository;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Transactional
    public FixedAssetDtos.FixedAssetResponse create(FixedAssetDtos.FixedAssetRequest request) {
        authorizationService.checkPermission(PermissionCode.FIXED_ASSET_MANAGE);
        Long companyId = requireCompanyId();

        BigDecimal salvage = request.getSalvageValue() != null ? request.getSalvageValue() : BigDecimal.ZERO;
        if (salvage.compareTo(request.getCost()) >= 0) {
            throw new BadRequestException("Salvage value must be less than cost");
        }

        FixedAsset asset = FixedAsset.builder()
                .companyId(companyId)
                .name(request.getName().trim())
                .assetTag(request.getAssetTag())
                .category(request.getCategory())
                .cost(request.getCost())
                .salvageValue(salvage)
                .usefulLifeMonths(request.getUsefulLifeMonths())
                .acquisitionDate(request.getAcquisitionDate())
                .notes(request.getNotes())
                .status(FixedAssetStatus.ACTIVE)
                .build();
        asset = assetRepository.save(asset);

        // Capitalize the purchase: Dr Fixed Assets / Cr Cash, dated the acquisition date.
        boolean postPurchase = request.getPostPurchaseToLedger() == null || request.getPostPurchaseToLedger();
        if (postPurchase) {
            ChartOfAccount fixedAssets = accountResolver.fixedAssets(companyId);
            ChartOfAccount cash = accountResolver.cash(companyId);
            glService.recordBalancedTransaction(companyId, List.of(
                            LedgerLine.debit(fixedAssets.getId(), asset.getCost()),
                            LedgerLine.credit(cash.getId(), asset.getCost())),
                    "Fixed asset purchase: " + asset.getName(),
                    GlReferenceType.FIXED_ASSET_PURCHASE, asset.getId(), asset.getAssetTag(),
                    asset.getAcquisitionDate() != null ? asset.getAcquisitionDate() : LocalDate.now());
        }

        return FixedAssetDtos.toResponse(asset);
    }

    @Transactional(readOnly = true)
    public Page<FixedAssetDtos.FixedAssetResponse> list(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.FIXED_ASSET_VIEW);
        return assetRepository.findByCompanyId(requireCompanyId(), pageable)
                .map(FixedAssetDtos::toResponse);
    }

    @Transactional
    public FixedAssetDtos.FixedAssetResponse dispose(Long id) {
        authorizationService.checkPermission(PermissionCode.FIXED_ASSET_MANAGE);
        FixedAsset asset = findInTenant(id);
        if (asset.getStatus() == FixedAssetStatus.DISPOSED) {
            throw new BadRequestException("Asset is already disposed");
        }
        // Simple disposal at zero proceeds: write the remaining book value off the books.
        // Dr Accumulated Depreciation (its balance for this asset) + Dr Depreciation
        // Expense (remaining book value, as a loss) / Cr Fixed Assets (full cost).
        BigDecimal accumulated = asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;
        BigDecimal bookValue = asset.bookValue();

        Long companyId = asset.getCompanyId();
        ChartOfAccount fixedAssets = accountResolver.fixedAssets(companyId);
        ChartOfAccount accumDep = accountResolver.accumulatedDepreciation(companyId);
        ChartOfAccount depExpense = accountResolver.depreciationExpense(companyId);

        List<LedgerLine> lines = new java.util.ArrayList<>();
        if (accumulated.compareTo(BigDecimal.ZERO) > 0) lines.add(LedgerLine.debit(accumDep.getId(), accumulated));
        if (bookValue.compareTo(BigDecimal.ZERO) > 0) lines.add(LedgerLine.debit(depExpense.getId(), bookValue));
        lines.add(LedgerLine.credit(fixedAssets.getId(), asset.getCost()));
        glService.recordBalancedTransaction(companyId, lines,
                "Disposal of fixed asset: " + asset.getName(),
                GlReferenceType.FIXED_ASSET_PURCHASE, asset.getId(), asset.getAssetTag(), LocalDate.now());

        asset.setStatus(FixedAssetStatus.DISPOSED);
        asset = assetRepository.save(asset);
        return FixedAssetDtos.toResponse(asset);
    }

    /**
     * Runs straight-line depreciation for one calendar month across every ACTIVE asset
     * acquired on/before that month's end. Idempotent - a month can only run once.
     */
    @Transactional
    public FixedAssetDtos.DepreciationRunResponse runDepreciation(int year, int month) {
        authorizationService.checkPermission(PermissionCode.FIXED_ASSET_MANAGE);
        Long companyId = requireCompanyId();
        if (month < 1 || month > 12) throw new BadRequestException("Month must be 1-12");
        if (runRepository.existsByCompanyIdAndYearAndMonth(companyId, year, month)) {
            throw new BadRequestException("Depreciation for " + year + "-" + String.format("%02d", month) + " has already been run");
        }
        YearMonth target = YearMonth.of(year, month);
        if (target.isAfter(YearMonth.now())) {
            throw new BadRequestException("Cannot depreciate a future month");
        }
        LocalDate monthEnd = target.atEndOfMonth();

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        List<LedgerLine> lines = new java.util.ArrayList<>();
        ChartOfAccount depExpense = accountResolver.depreciationExpense(companyId);
        ChartOfAccount accumDep = accountResolver.accumulatedDepreciation(companyId);

        for (FixedAsset asset : assetRepository.findByCompanyIdAndStatus(companyId, FixedAssetStatus.ACTIVE)) {
            if (asset.getAcquisitionDate() != null && asset.getAcquisitionDate().isAfter(monthEnd)) continue;
            BigDecimal charge = asset.monthlyDepreciation();
            if (charge.compareTo(BigDecimal.ZERO) <= 0) continue;

            asset.setAccumulatedDepreciation(
                    (asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO)
                            .add(charge));
            if (asset.getAccumulatedDepreciation().compareTo(asset.depreciableBase()) >= 0) {
                asset.setStatus(FixedAssetStatus.FULLY_DEPRECIATED);
            }
            assetRepository.save(asset);

            total = total.add(charge);
            count++;
        }

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(LedgerLine.debit(depExpense.getId(), total));
            lines.add(LedgerLine.credit(accumDep.getId(), total));
            glService.recordBalancedTransaction(companyId, lines,
                    "Monthly depreciation " + year + "-" + String.format("%02d", month),
                    GlReferenceType.DEPRECIATION, null, year + "-" + String.format("%02d", month), monthEnd);
        }

        DepreciationRun run = DepreciationRun.builder()
                .companyId(companyId)
                .year(year)
                .month(month)
                .totalAmount(total)
                .assetsDepreciated(count)
                .runBy(securityUtil.getCurrentUser().getUsername())
                .runAt(LocalDateTime.now())
                .build();
        run = runRepository.save(run);
        return FixedAssetDtos.toResponse(run);
    }

    @Transactional(readOnly = true)
    public List<FixedAssetDtos.DepreciationRunResponse> listRuns() {
        authorizationService.checkPermission(PermissionCode.FIXED_ASSET_VIEW);
        return runRepository.findByCompanyIdOrderByYearDescMonthDesc(requireCompanyId())
                .stream()
                .map(FixedAssetDtos::toResponse)
                .collect(Collectors.toList());
    }

    private FixedAsset findInTenant(Long id) {
        return assetRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Fixed asset not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
