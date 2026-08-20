package com.businessos.modules.hrm.asset;

import com.businessos.enums.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssetService {

    /** ADMIN / OWNER: add a new asset to the company inventory */
    AssetResponse create(AssetRequest request);

    /** ADMIN / OWNER: get asset by id */
    AssetResponse getById(Long id);

    /** ADMIN / OWNER: list all assets with optional requeststatus filter */
    Page<AssetResponse> listAll(AssetStatus status, Pageable pageable);

    /** ADMIN / OWNER / EMPLOYEE: list assets currently assigned to an employee */
    List<AssetResponse> listForEmployee(Long employeeId);

    /** ADMIN / OWNER: update asset details */
    AssetResponse update(Long id, AssetRequest request);

    /** ADMIN / OWNER: assign an available asset to an employee */
    AssetResponse assign(Long id, Long employeeId);

    /** ADMIN / OWNER: unassign asset and mark it as available */
    AssetResponse unassign(Long id);

    /** ADMIN / OWNER: take an unassigned asset in/out of maintenance */
    AssetResponse setMaintenance(Long id, boolean underMaintenance);

    /** ADMIN / OWNER: permanently retire an unassigned asset */
    AssetResponse dispose(Long id, String reason);

    /** ADMIN / OWNER: soft-delete an unassigned asset */
    void delete(Long id);
}
