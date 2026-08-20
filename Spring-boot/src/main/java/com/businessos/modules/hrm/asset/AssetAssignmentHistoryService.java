package com.businessos.modules.hrm.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetAssignmentHistoryService {

    Page<AssetAssignmentHistoryResponse> historyForAsset(Long assetId, Pageable pageable);

    Page<AssetAssignmentHistoryResponse> historyForEmployee(Long employeeId, Pageable pageable);

    Page<AssetAssignmentHistoryResponse> listAll(Pageable pageable);
}