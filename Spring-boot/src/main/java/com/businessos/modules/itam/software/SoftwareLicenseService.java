package com.businessos.modules.itam.software;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SoftwareLicenseService {

    SoftwareLicenseResponse create(SoftwareLicenseRequest request);
    SoftwareLicenseResponse getById(Long id);
    SoftwareLicenseResponse getByLicenseKey(String licenseKey);
    Page<SoftwareLicenseResponse> getAll(Pageable pageable);
    Page<SoftwareLicenseResponse> getByStatus(LicenseStatus status, Pageable pageable);
    SoftwareLicenseResponse update(Long id, SoftwareLicenseRequest request);
    void delete(Long id);
    List<SoftwareLicenseResponse> getExpiringLicenses();
    List<SoftwareLicenseResponse> getExpiredLicenses();
    void assignSeat(Long licenseId, Long employeeId);
    void releaseSeat(Long licenseId, Long employeeId);
    List<SoftwareLicenseSeatResponse> getSeatHolders(Long licenseId);
    List<SoftwareLicenseSeatResponse> getLicensesForEmployee(Long employeeId);
}
