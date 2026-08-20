package com.businessos.modules.itam.software;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareLicenseSeatRepository extends JpaRepository<SoftwareLicenseSeat, Long> {

    List<SoftwareLicenseSeat> findByLicenseIdAndReleasedAtIsNull(Long licenseId);

    List<SoftwareLicenseSeat> findByEmployeeIdAndCompanyIdAndReleasedAtIsNull(Long employeeId, Long companyId);

    Optional<SoftwareLicenseSeat> findByLicenseIdAndEmployeeIdAndReleasedAtIsNull(Long licenseId, Long employeeId);
}
