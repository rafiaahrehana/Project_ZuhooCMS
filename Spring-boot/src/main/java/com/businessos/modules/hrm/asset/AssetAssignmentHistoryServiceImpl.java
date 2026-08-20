package com.businessos.modules.hrm.asset;


import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetAssignmentHistoryServiceImpl implements AssetAssignmentHistoryService {

    private final AssetAssignmentHistoryRepository historyRepository;
    private final SecurityUtil                     securityUtil;
    private final AuthorizationService             authorizationService;

    @Transactional(readOnly = true)
    @Override
    public Page<AssetAssignmentHistoryResponse> historyForAsset(Long assetId, Pageable pageable) {
        Long companyId = requireCompanyId();
        return historyRepository.findByCompanyIdAndAssetIdOrderByAssignedAtDesc(companyId, assetId, pageable)
            .map(AssetAssignmentHistoryMapper::toAssetHistoryResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AssetAssignmentHistoryResponse> historyForEmployee(Long employeeId, Pageable pageable) {
        Long companyId = requireCompanyId();
        return historyRepository.findByCompanyIdAndEmployeeIdOrderByAssignedAtDesc(companyId, employeeId, pageable)
            .map(AssetAssignmentHistoryMapper::toAssetHistoryResponse);
    }

    // This single endpoint backs both the ITAM Assignments page and the HRM asset
    // history view, so either permission unlocks it.
    @Transactional(readOnly = true)
    @Override
    public Page<AssetAssignmentHistoryResponse> listAll(Pageable pageable) {
        authorizationService.checkAnyPermission(PermissionCode.ASSET_ASSIGNMENT_VIEW, PermissionCode.ASSET_VIEW);
        Long companyId = requireCompanyId();
        return historyRepository.findByCompanyIdOrderByAssignedAtDesc(companyId, pageable)
            .map(AssetAssignmentHistoryMapper::toAssetHistoryResponse);
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
